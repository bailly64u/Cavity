package com.louis.app.cavity.domain.repository

import android.app.Application
import androidx.room.withTransaction
import com.louis.app.cavity.db.CavityDatabase
import com.louis.app.cavity.model.Tasting
import com.louis.app.cavity.model.TastingAction
import com.louis.app.cavity.model.TastingXFriend

class TastingRepository private constructor(app: Application) {
    companion object {
        @Volatile
        var instance: TastingRepository? = null

        fun getInstance(app: Application) =
            instance ?: synchronized(this) {
                instance ?: TastingRepository(app).also { instance = it }
            }
    }

    private val database = CavityDatabase.getInstance(app)
    private val tastingXFriendDao = database.tastingXFriendDao()
    private val tastingDao = database.tastingDao()
    private val tastingActionDao = database.tastingActionDao()

    suspend fun <T> transaction(databaseQueries: suspend () -> T) = database.withTransaction {
        databaseQueries()
    }

    suspend fun insertTasting(tasting: Tasting) = tastingDao.insertTasting(tasting)
    suspend fun insertTastings(tastings: List<Tasting>) = tastingDao.insertTastings(tastings)
    suspend fun updateTasting(tasting: Tasting) = tastingDao.updateTasting(tasting)
    suspend fun deleteTastings(tastings: List<Tasting>) = tastingDao.deleteTastings(tastings)
    suspend fun deleteAllTastings() = tastingDao.deleteAll()
    suspend fun getAllTastingsNotLive() = tastingDao.getAllTastingsNotLive()
    suspend fun getEmptyTastings() = tastingDao.getEmptyTastings()
    fun getUndoneTastings() = tastingDao.getUndoneTastings()
    suspend fun getTastingById(tastingId: Long) = tastingDao.getTastingById(tastingId)
    suspend fun getBoundedTastingById(tastingId: Long) =
        tastingDao.getTastingWithFriendsById(tastingId)

    suspend fun getAllTastingXFriendsNotLive() = tastingXFriendDao.getAllTastingXFriendsNotLive()

    suspend fun insertTastingFriendXRefs(tastings: List<TastingXFriend>) =
        tastingXFriendDao.insertTastingXFriends(tastings)

    suspend fun insertTastingFriendXRef(tastingId: Long, friends: List<Long>) {
        if (!database.inTransaction()) {
            throw IllegalStateException("This method should be called inside a transaction")
        }

        friends.forEach {
            tastingXFriendDao.insertTastingXFriend(TastingXFriend(tastingId, it))
        }
    }

    fun getBottlesWithTastingActionsForTasting(tastingId: Long) =
        tastingDao.getBottlesWithTastingActionsForTasting(tastingId)

    suspend fun getBottlesWithTastingActionsForTastingNotLive(tastingId: Long) =
        tastingDao.getBottlesWithTastingActionsForTastingNotLive(tastingId)

    suspend fun deleteAllTastingFriendXRefs() = tastingXFriendDao.deleteAll()

    suspend fun insertTastingActions(tastingActions: List<TastingAction>) =
        tastingActionDao.insertTastingActions(tastingActions)

    suspend fun updateTastingAction(tastingAction: TastingAction) =
        tastingActionDao.updateTastingAction(tastingAction)

    suspend fun getTastingActionById(tastingActionId: Long) =
        tastingActionDao.getTastingActionById(tastingActionId)

    suspend fun getAllTastingActionsNotLive() = tastingActionDao.getAllTastingActionsNotLive()

    suspend fun deleteTastingActionsForBottle(bottleId: Long) =
        tastingActionDao.deleteTastingActionsForBottle(bottleId)

    suspend fun deleteAllTastingActions() = tastingActionDao.deleteAll()

}
