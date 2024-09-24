package com.louis.app.cavity.domain.repository

import android.app.Application
import androidx.room.withTransaction
import com.louis.app.cavity.db.CavityDatabase
import com.louis.app.cavity.model.FReview
import com.louis.app.cavity.model.Review

class ReviewRepository private constructor(app: Application) {
    companion object {
        @Volatile
        var instance: ReviewRepository? = null

        fun getInstance(app: Application) =
            instance ?: synchronized(this) {
                instance ?: ReviewRepository(app).also { instance = it }
            }
    }

    private val database = CavityDatabase.getInstance(app)
    private val reviewDao = database.reviewDao()
    private val fReviewDao = database.fReviewDao()

    suspend fun <T> transaction(databaseQueries: suspend () -> T) = database.withTransaction {
        databaseQueries()
    }

    suspend fun insertReview(review: Review): Long {
        if (review.contestName.isBlank()) {
            throw IllegalArgumentException("Review contestName is blank.")
        }

        return reviewDao.insertReview(review)
    }

    suspend fun insertReviews(reviews: List<Review>) = reviewDao.insertReviews(reviews)

    suspend fun updateReview(review: Review) {
        if (review.contestName.isBlank()) {
            throw IllegalArgumentException("Review contestName is blank.")
        }

        reviewDao.updateReview(review)
    }

    suspend fun deleteReview(review: Review) = reviewDao.deleteReview(review)
    fun getAllReviews() = reviewDao.getAllReviews()
    suspend fun getAllReviewsNotLive() = reviewDao.getAllReviewsNotLive()
    fun getReviewWithFilledReviews() = reviewDao.getReviewWithFilledReviews()
    suspend fun getAllFReviewsNotLive() = fReviewDao.getAllFReviewsNotLive()
    suspend fun insertFilledReviews(fReviews: List<FReview>) = fReviewDao.insertFReviews(fReviews)

    suspend fun getFReviewAndReviewForBottleNotLive(bottleId: Long) =
        fReviewDao.getFReviewAndReviewForBottleNotLive(bottleId)

    suspend fun replaceFReviewsForBottle(bottleId: Long, fReviews: List<FReview>) {
        if (!database.inTransaction()) {
            throw IllegalStateException("This method should be called inside a transaction")
        }

        fReviewDao.clearAllFReviewsForBottle(bottleId)
        fReviewDao.insertFReviews(fReviews)
    }

    suspend fun deleteAllReviews() = reviewDao.deleteAll()
    suspend fun deleteAllFReviews() = fReviewDao.deleteAll()
}
