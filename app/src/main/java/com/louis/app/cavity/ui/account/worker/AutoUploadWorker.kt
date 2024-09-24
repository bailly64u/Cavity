package com.louis.app.cavity.ui.account.worker

import android.app.Application
import android.content.Context
import androidx.annotation.StringRes
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.louis.app.cavity.R
import com.louis.app.cavity.domain.repository.AccountRepository
import com.louis.app.cavity.domain.repository.PrefsRepository
import com.louis.app.cavity.domain.backup.AutoBackup
import com.louis.app.cavity.domain.backup.BackupFinishedListener
import com.louis.app.cavity.domain.error.SentryErrorReporter
import com.louis.app.cavity.domain.repository.HistoryRepository
import com.louis.app.cavity.ui.notifications.NotificationBuilder
import kotlinx.coroutines.delay

// This worker is designed to be used with a one shot work or a periodic work,
// using progress as a return value when used periodically
class AutoUploadWorker(private val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private val app = context as Application
    private val historyRepository = HistoryRepository.getInstance(app)
    private val accountRepository = AccountRepository.getInstance(app)
    private val prefsRepository = PrefsRepository.getInstance(app)
    private val healthCheckOnly = inputData.getBoolean(WORK_DATA_HEALTHCHECK_ONLY, false)
    private val errorReporter = SentryErrorReporter.getInstance(context)

    override suspend fun doWork(): Result {
        val listener = object : BackupFinishedListener<Data> {
            override fun onSuccess(): Data {
                sendNotification(R.string.auto_backup_done_title, R.string.auto_backup_done)

                return Data.Builder()
                    .putInt(WORK_DATA_HEALTH_STATE_KEY, HEALTH_STATE_SUCCESS)
                    .build()
            }

            override fun onFailure(canRetry: Boolean, exception: Exception?): Data {
                exception?.let { errorReporter.captureException(it) }
                sendNotification(R.string.auto_backup_failed_title, R.string.base_error)

                return Data.Builder()
                    .putInt(WORK_DATA_HEALTH_STATE_KEY, HEALTH_STATE_FAILED)
                    .build()

//                return if (canRetry && runAttemptCount < 1) Result.retry() else Result.failure(data)
            }

            override fun onUnauthorized(): Data {
                prefsRepository.setApiToken("")
                sendNotification(
                    R.string.auto_backup_failed_title,
                    R.string.auto_backup_unauthorized
                )

                return Data.Builder()
                    .putInt(WORK_DATA_HEALTH_STATE_KEY, HEALTH_STATE_UNAUTHORIZED)
                    .build()
            }

            override fun onPreventOverwriting(): Data {
                sendNotification(
                    R.string.auto_backup_failed_title,
                    R.string.auto_backup_overwrite_data
                )

                return Data.Builder()
                    .putInt(WORK_DATA_HEALTH_STATE_KEY, HEALTH_STATE_PREVENT_OVERWRITE)
                    .build()
            }

            override fun onPreventAccountSwitch(): Data {
                sendNotification(
                    R.string.auto_backup_failed_title,
                    R.string.auto_backup_not_matching
                )

                return Data.Builder()
                    .putInt(WORK_DATA_HEALTH_STATE_KEY, HEALTH_STATE_PREVENT_ACCOUNT_SWITCH)
                    .build()
            }
        }

        val autoBackup = AutoBackup(historyRepository, accountRepository, context, listener)

        return try {
            // Tricky part: if this worker is run as non periodic (e.g. as healthcheck)
            // We have to pass the health result in the Result.success() call to get picked up by the healthcheck observers
            // Note also that periodic work never really succeed, so we cant rely only on Result.success() since periodic work observer
            // wont receive the data
            val result = autoBackup.tryBackup(healthCheckOnly)
            setProgress(result)     // If this worker is run periodically, we can get this data by using WorkInfo#progress
            delay(200)      // Let the observers catch the progress
            Result.success(result)  // If this worker is run in one shot, we can get this data by using WorkInfo#outputData
        } catch (e: Exception) {
            errorReporter.captureException(e)
            val data = Data.Builder()
                .putInt(WORK_DATA_HEALTH_STATE_KEY, HEALTH_STATE_FAILED)
                .build()

            setProgress(data)
            Result.failure()
        }
    }

    private fun sendNotification(@StringRes title: Int, @StringRes content: Int) {
        if (healthCheckOnly) {
            return
        }

        val notification = NotificationBuilder.buildAutoBackupNotification(context, title, content)
        NotificationBuilder.notify(context, notification)
    }

    companion object {
        const val WORK_TAG = "com.louis.app.cavity.auto-upload-db"
        const val WORK_TAG_HEALTHCHECK = "com.louis.app.cavity.auto-upload-healthcheck"
        const val WORK_DATA_HEALTH_STATE_KEY = "com.louis.app.cavity.WORK_DATA_HEALTH_STATE_KEY"
        const val WORK_DATA_HEALTHCHECK_ONLY = "com.louis.app.cavity.WORK_DATA_HEALTHCHECK_ONLY"
        const val HEALTH_STATE_SUCCESS = 0
        const val HEALTH_STATE_FAILED = 1
        const val HEALTH_STATE_PREVENT_OVERWRITE = 2
        const val HEALTH_STATE_PREVENT_ACCOUNT_SWITCH = 3
        const val HEALTH_STATE_UNAUTHORIZED = 4
        const val HEALTH_STATE_USER_DISABLED = 5
    }

}
