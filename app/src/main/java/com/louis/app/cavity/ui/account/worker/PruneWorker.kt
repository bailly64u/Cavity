package com.louis.app.cavity.ui.account.worker

import android.app.Application
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.louis.app.cavity.domain.repository.AccountRepository
import com.louis.app.cavity.domain.error.SentryErrorReporter
import com.louis.app.cavity.network.response.ApiResponse
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PruneWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val accountRepository = AccountRepository.getInstance(context as Application)
    private val errorReporter = SentryErrorReporter.getInstance(context)

    override suspend fun doWork(): Result {
        return try {
            pruneDatabase()
            Result.success()
        } catch (e: UncompletePruneException) {
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

    private suspend fun pruneDatabase() = withContext(IO) {
        with(accountRepository) {
            launch {
                listOf(
                    deleteCounties(),
                    deleteWines(),
                    deleteBottles(),
                    deleteFriends(),
                    deleteGrapes(),
                    deleteReviews(),
                    deleteHistoryEntries(),
                    deleteTastings(),
                    deleteTastingActions(),
                    deleteFReviews(),
                    deleteQGrapes(),
                    deleteTastingXFriend(),
                    deleteHistoryXFriend()
                ).forEach {
                    if (it !is ApiResponse.Success) {
                        throw UncompletePruneException()
                    }
                }
            }
        }
    }

    class UncompletePruneException : Exception()

    companion object {
        const val WORK_TAG = "com.louis.app.cavity.prune-db"
    }
}
