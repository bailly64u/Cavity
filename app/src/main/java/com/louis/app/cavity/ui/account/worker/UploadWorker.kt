package com.louis.app.cavity.ui.account.worker

import android.app.Application
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.louis.app.cavity.domain.repository.AccountRepository
import com.louis.app.cavity.domain.backup.BackupBuilder
import com.louis.app.cavity.domain.error.SentryErrorReporter

class UploadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val accountRepository = AccountRepository.getInstance(context as Application)
    private val backupBuilder = BackupBuilder(context)
    private val errorReporter = SentryErrorReporter.getInstance(context)

    override suspend fun doWork(): Result {
        return try {
            backupBuilder.backup(accountRepository)
            Result.success()
        } catch (e: BackupBuilder.UncompleteExportException) {
            if (runAttemptCount < 1) {
                Result.retry()
            } else {
                errorReporter.captureException(e)
                Result.failure()
            }
        } catch (e: Exception) {
                errorReporter.captureException(e)
            Result.failure()
        }
    }

    companion object {
        const val WORK_TAG = "com.louis.app.cavity.upload-db"
    }
}
