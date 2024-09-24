package com.louis.app.cavity.domain.worker

import android.app.Application
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.louis.app.cavity.domain.repository.AccountRepository
import com.louis.app.cavity.domain.repository.WineRepository
import com.louis.app.cavity.domain.error.SentryErrorReporter
import com.louis.app.cavity.domain.repository.BottleRepository
import com.louis.app.cavity.domain.repository.CountyRepository
import com.louis.app.cavity.domain.repository.FriendRepository
import com.louis.app.cavity.domain.repository.GrapeRepository
import com.louis.app.cavity.domain.repository.HistoryRepository
import com.louis.app.cavity.domain.repository.ReviewRepository
import com.louis.app.cavity.domain.repository.TastingRepository
import com.louis.app.cavity.network.response.ApiResponse
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext

class DownloadWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private val app = context as Application
    private val countyRepository = CountyRepository.getInstance(app)
    private val wineRepository = WineRepository.getInstance(app)
    private val bottleRepository = BottleRepository.getInstance(app)
    private val grapeRepository = GrapeRepository.getInstance(app)
    private val reviewRepository = ReviewRepository.getInstance(app)
    private val historyRepository = HistoryRepository.getInstance(app)
    private val friendRepository = FriendRepository.getInstance(app)
    private val tastingRepository = TastingRepository.getInstance(app)
    private val accountRepository = AccountRepository.getInstance(app)
    private val errorReporter = SentryErrorReporter.getInstance(context)

    override suspend fun doWork(): Result {
        return try {
            downloadDatabase()
            Result.success()
        } catch (e: UncompleteImportException) {
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

    private suspend fun downloadDatabase() = withContext(IO) {
        with(accountRepository) {
            wineRepository.transaction {
                countyRepository.deleteAllCounties()
                wineRepository.deleteAllWines()
                bottleRepository.deleteAllBottles()
                friendRepository.deleteAllFriends()
                grapeRepository.deleteAllGrapes()
                reviewRepository.deleteAllReviews()
                historyRepository.deleteAllHistoryEntries()
                tastingRepository.deleteAllTastings()
                tastingRepository.deleteAllTastingActions()
                reviewRepository.deleteAllFReviews()
                grapeRepository.deleteAllQGrapes()
                friendRepository.deleteAllFriendHistoryXRefs()
                tastingRepository.deleteAllTastingFriendXRefs()

                countyRepository.insertCounties(validateResponse(getCounties()))
                wineRepository.insertWines(validateResponse(getWines()))
                bottleRepository.insertBottles(validateResponse(getBottles()))
                friendRepository.insertFriends(validateResponse(getFriends()))
                grapeRepository.insertGrapes(validateResponse(getGrapes()))
                reviewRepository.insertReviews(validateResponse(getReviews()))
                historyRepository.insertHistoryEntries(validateResponse(getHistoryEntries()))
                tastingRepository.insertTastings(validateResponse(getTastings()))
                tastingRepository.insertTastingActions(validateResponse(getTastingActions()))
                reviewRepository.insertFilledReviews(validateResponse(getFReviews()))
                grapeRepository.insertQGrapes(validateResponse(getQGrapes()))
                friendRepository.insertFriendHistoryXRefs(validateResponse(getHistoryXFriend()))
                tastingRepository.insertTastingFriendXRefs(validateResponse(getTastingXFriend()))
            }
        }
    }

    private fun <T> validateResponse(response: ApiResponse<List<T>>): List<T> {
        if (response is ApiResponse.Success) {
            return response.value
        }

        throw UncompleteImportException()
    }

    class UncompleteImportException : Exception()

    companion object {
        const val WORK_TAG = "com.louis.app.cavity.download-db"
    }
}
