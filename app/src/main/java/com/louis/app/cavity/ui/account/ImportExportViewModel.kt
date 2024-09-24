package com.louis.app.cavity.ui.account

import android.app.Application
import androidx.lifecycle.*
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.louis.app.cavity.R
import com.louis.app.cavity.domain.repository.AccountRepository
import com.louis.app.cavity.domain.backup.BackupBuilder
import com.louis.app.cavity.domain.repository.BottleRepository
import com.louis.app.cavity.domain.repository.HistoryRepository
import com.louis.app.cavity.network.response.ApiResponse
import com.louis.app.cavity.ui.account.worker.AutoUploadWorker
import com.louis.app.cavity.ui.account.worker.AutoUploadWorker.Companion.WORK_DATA_HEALTHCHECK_ONLY
import com.louis.app.cavity.ui.account.worker.DownloadWorker
import com.louis.app.cavity.ui.account.worker.PruneWorker
import com.louis.app.cavity.ui.account.worker.UploadWorker
import com.louis.app.cavity.util.Event
import com.louis.app.cavity.util.postOnce
import com.louis.app.cavity.util.toBoolean
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit

class ImportExportViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        private const val MIN_BACKOFF_SECONDS = 10L
        private const val AUTO_BACKUP_PERIODICITY_IN_DAYS = 15L
        private const val AUTO_BACKUP_INITIAL_DELAY_IN_HOURS = 1L
    }

    private val historyRepository = HistoryRepository.getInstance(app)
    private val bottleRepository = BottleRepository.getInstance(app)
    private val accountRepository = AccountRepository.getInstance(app)
    private val workManager = WorkManager.getInstance(app)
    private val backupBuilder = BackupBuilder(app)

    private val workRequestId = MutableLiveData<UUID>()
    val workProgress = workRequestId.switchMap {
        workManager.getWorkInfoByIdLiveData(it)
    }

    private val autoBackupWorkRequestId = MutableLiveData<UUID>()
    val autoBackupWorkProgress = autoBackupWorkRequestId.switchMap {
        workManager.getWorkInfoByIdLiveData(it)
    }

    private val healthCheckWorkRequestId = MutableLiveData<UUID>()
    val healthCheckWorkProgress = healthCheckWorkRequestId.switchMap {
        workManager.getWorkInfoByIdLiveData(it)
    }

    private val _health = MutableLiveData<Int?>()
    val health: LiveData<Int?>
        get() = _health

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean>
        get() = _isLoading

    private val _distantBottleCount = MutableLiveData<Int>()
    val distantBottleCount: LiveData<Int>
        get() = _distantBottleCount

    private val _localBottleCount = MutableLiveData<Int>()
    val localBottleCount: LiveData<Int>
        get() = _localBottleCount

    private val _navigateToLogin = MutableLiveData<Event<Unit>>()
    val navigateToLogin: LiveData<Event<Unit>>
        get() = _navigateToLogin

    private val _userFeedback = MutableLiveData<Event<Int>>()
    val userFeedback: LiveData<Event<Int>>
        get() = _userFeedback

    private val _userFeedbackString = MutableLiveData<Event<String>>()
    val userFeedbackString: LiveData<Event<String>>
        get() = _userFeedbackString

    var preventHealthCheckSpam = false
        get() = field.also { field = true }

    fun fetchHealth(isImport: Boolean) {
        val isExport = !isImport

        _isLoading.value = true

        viewModelScope.launch(IO) {
            try {
                val localEntries = historyRepository.getAllEntriesNotPagedNotLive()
                accountRepository.getHistoryEntries().let { response ->
                    when (response) {
                        is ApiResponse.Success -> {
                            val distantEntries = response.value
                            val target = if (isExport) distantEntries else localEntries
                            val source = if (isExport) localEntries else distantEntries
                            val health = backupBuilder.checkHealth(source, target)
                            val stringRes = backupBuilder.getTextForHealthResult(health, isExport)
                            _health.postValue(stringRes)
                        }

                        is ApiResponse.Failure -> _userFeedbackString.postOnce(response.message)
                        is ApiResponse.UnknownError -> _userFeedback.postOnce(R.string.base_error)
                        is ApiResponse.UnauthorizedError -> _navigateToLogin.postOnce(Unit)
                        is ApiResponse.UnregisteredError -> Unit
                    }
                }
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun fetchDistantBottleCount() {
        _isLoading.postValue(true)

        viewModelScope.launch(IO) {
            try {
                when (val response = accountRepository.getBottles()) {
                    is ApiResponse.Success -> {
                        val count = response.value.count { !it.consumed.toBoolean() }
                        _distantBottleCount.postValue(count)
                    }

                    is ApiResponse.Failure -> _userFeedbackString.postOnce(response.message)
                    is ApiResponse.UnknownError -> _userFeedback.postOnce(R.string.base_error)
                    is ApiResponse.UnauthorizedError -> _navigateToLogin.postOnce(Unit)
                    is ApiResponse.UnregisteredError -> Unit
                }
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun fetchLocalBottleCount() {
        viewModelScope.launch(IO) {
            val count = bottleRepository.getAllBottlesNotLive().count { !it.consumed.toBoolean() }
            _localBottleCount.postValue(count)
        }
    }

    fun export() {
        if (_isLoading.value == true) {
            return
        }

        workManager.cancelAllWorkByTag(UploadWorker.WORK_TAG)

        OneTimeWorkRequestBuilder<UploadWorker>()
            .addTag(UploadWorker.WORK_TAG)
            .setBackoffCriteria(BackoffPolicy.LINEAR, MIN_BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build().also {
                workRequestId.value = it.id
                workManager.enqueue(it)
            }
    }

    fun import() {
        if (_isLoading.value == true) {
            return
        }

        workManager.cancelAllWorkByTag(DownloadWorker.WORK_TAG)

        OneTimeWorkRequestBuilder<DownloadWorker>()
            .addTag(DownloadWorker.WORK_TAG)
            .setBackoffCriteria(BackoffPolicy.LINEAR, MIN_BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build().also {
                workRequestId.value = it.id
                workManager.enqueue(it)
            }
    }

    fun pruneWorks() = workManager.pruneWork()

    fun autoBackupHealthCheck() {
        workManager.cancelAllWorkByTag(AutoUploadWorker.WORK_TAG_HEALTHCHECK)

        OneTimeWorkRequestBuilder<AutoUploadWorker>()
            .addTag(AutoUploadWorker.WORK_TAG_HEALTHCHECK)
            .setInputData(Data.Builder().putBoolean(WORK_DATA_HEALTHCHECK_ONLY, true).build())
            .build().also {
                healthCheckWorkRequestId.value = it.id
                workManager.enqueue(it)
            }
    }

    fun enableAutoBackups() {
        cancelCurrentAutoBackup()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .build()

        PeriodicWorkRequestBuilder<AutoUploadWorker>(AUTO_BACKUP_PERIODICITY_IN_DAYS, TimeUnit.DAYS)
            .addTag(AutoUploadWorker.WORK_TAG)
            .setInitialDelay(AUTO_BACKUP_INITIAL_DELAY_IN_HOURS, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build().also {
                autoBackupWorkRequestId.value = it.id
                workManager.enqueue(it)
            }
    }

    fun cancelCurrentAutoBackup() {
        workManager.cancelAllWorkByTag(AutoUploadWorker.WORK_TAG)
    }

    fun cleanAccountDatabase() {
        workManager.cancelAllWorkByTag(PruneWorker.WORK_TAG)

        OneTimeWorkRequestBuilder<PruneWorker>()
            .addTag(PruneWorker.WORK_TAG)
            .setBackoffCriteria(BackoffPolicy.LINEAR, MIN_BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build().also {
                workRequestId.value = it.id
                workManager.enqueue(it)
            }
    }
}
