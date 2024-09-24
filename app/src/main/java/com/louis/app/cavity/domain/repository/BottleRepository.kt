package com.louis.app.cavity.domain.repository

import android.app.Application
import androidx.room.withTransaction
import com.louis.app.cavity.db.CavityDatabase
import com.louis.app.cavity.model.Bottle
import com.louis.app.cavity.model.HistoryEntry
import com.louis.app.cavity.model.HistoryXFriend

class BottleRepository private constructor(app: Application) {
    companion object {
        @Volatile
        var instance: BottleRepository? = null

        fun getInstance(app: Application) =
            instance ?: synchronized(this) {
                instance ?: BottleRepository(app).also { instance = it }
            }
    }

    private val database = CavityDatabase.getInstance(app)
    private val bottleDao = database.bottleDao()
    private val fReviewDao = database.fReviewDao()
    private val historyDao = database.historyDao()
    private val historyXFriendDao = database.historyXFriendDao()

    suspend fun <T> transaction(databaseQueries: suspend () -> T) = database.withTransaction {
        databaseQueries()
    }

    suspend fun insertBottle(bottle: Bottle) = bottleDao.insertBottle(bottle)
    suspend fun insertBottles(bottles: List<Bottle>) = bottleDao.insertBottles(bottles)
    suspend fun updateBottle(bottle: Bottle) = bottleDao.updateBottle(bottle)
    suspend fun deleteBottles(bottles: List<Bottle>) = bottleDao.deleteBottles(bottles)
    suspend fun deleteBottleById(bottleId: Long) = bottleDao.deleteBottleById(bottleId)
    fun getBottleById(bottleId: Long) = bottleDao.getBottleById(bottleId)
    suspend fun getBottleByIdNotLive(bottleId: Long) = bottleDao.getBottleByIdNotLive(bottleId)
    suspend fun getAllBottlesNotLive() = bottleDao.getAllBottlesNotLive()
    fun getBottlesForWine(wineId: Long) = bottleDao.getBottlesForWine(wineId)
    suspend fun getBottlesForWineNotLive(wineId: Long) = bottleDao.getBottlesForWineNotLive(wineId)
    suspend fun consumeBottle(bottleId: Long) = bottleDao.consumeBottle(bottleId)
    suspend fun fav(bottleId: Long) = bottleDao.fav(bottleId)
    suspend fun unfav(bottleId: Long) = bottleDao.unfav(bottleId)
    suspend fun removeTastingForBottle(bottleId: Long) = bottleDao.removeTastingForBottle(bottleId)

    fun getFReviewAndReviewForBottle(bottleId: Long) =
        fReviewDao.getFReviewAndReviewForBottle(bottleId)

    suspend fun revertBottleConsumption(bottleId: Long) {
        database.withTransaction {
            bottleDao.revertBottleConsumption(bottleId)
            historyDao.onBottleConsumptionReverted(bottleId)
        }
    }

    suspend fun declareGiftedBottle(entry: HistoryEntry, friendId: Long) {
        if (!database.inTransaction()) {
            throw IllegalStateException("This method should be called inside a transaction")
        }

        val entryId = historyDao.insertEntry(entry)

        historyXFriendDao.insertHistoryXFriend(
            HistoryXFriend(
                entryId,
                friendId
            )
        )
    }

    suspend fun getTastingBottleIdsIn(bottles: List<Long>) =
        bottleDao.getTastingBottleIdsIn(bottles)

    suspend fun boundBottlesToTasting(tastingId: Long, bottles: List<Long>) =
        bottleDao.boundBottlesToTasting(tastingId, bottles)

    fun getAllBuyLocations() = bottleDao.getAllBuyLocations()

    suspend fun deleteAllBottles() = bottleDao.deleteAll()
}
