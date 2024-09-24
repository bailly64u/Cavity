package com.louis.app.cavity.domain.backup

import android.content.Context
import com.louis.app.cavity.domain.repository.AccountRepository
import com.louis.app.cavity.domain.repository.HistoryRepository
import com.louis.app.cavity.model.HistoryEntry
import com.louis.app.cavity.network.response.ApiResponse
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext

class AutoBackup<T>(
    private val historyRepository: HistoryRepository,
    private val accountRepository: AccountRepository,
    context: Context,
    private val listener: BackupFinishedListener<T>,
) {

    private val backupBuilder = BackupBuilder(context)

    suspend fun tryBackup(healthCheckOnly: Boolean): T = withContext(IO) {
        val localHistoryEntries = historyRepository.getAllEntriesNotPagedNotLive()
        val distantHistoryEntries: List<HistoryEntry> =
            accountRepository.getHistoryEntries().let { response ->
                when (response) {
                    is ApiResponse.Success -> response.value
                    is ApiResponse.UnauthorizedError -> {
                        return@withContext listener.onUnauthorized()
                    }

                    else -> {
                        return@withContext listener.onFailure()
                    }
                }
            }

        when (backupBuilder.checkHealth(localHistoryEntries, distantHistoryEntries)) {
            BackupBuilder.HealthResult.Ok -> {
                return@withContext try {
                    if (!healthCheckOnly) {
                        backupBuilder.backup(accountRepository)
                    }

                    listener.onSuccess()
                } catch (e: BackupBuilder.UncompleteExportException) {
                    listener.onFailure(canRetry = true, exception = e)
                } catch (e: Exception) {
                    listener.onFailure(exception = e)
                }
            }

            BackupBuilder.HealthResult.MayBeAccountSwitch -> listener.onPreventAccountSwitch()
            BackupBuilder.HealthResult.WillOverwriteTarget -> listener.onPreventOverwriting()
        }
    }
}

interface BackupFinishedListener<T> {
    fun onSuccess(): T
    fun onFailure(canRetry: Boolean = false, exception: Exception? = null): T
    fun onUnauthorized(): T
    fun onPreventOverwriting(): T
    fun onPreventAccountSwitch(): T
}
