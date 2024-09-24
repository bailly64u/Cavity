package com.louis.app.cavity.domain.repository

import android.app.Application
import androidx.room.withTransaction
import com.louis.app.cavity.db.CavityDatabase
import com.louis.app.cavity.model.Friend
import com.louis.app.cavity.model.HistoryXFriend

class FriendRepository private constructor(app: Application) {
    companion object {
        @Volatile
        var instance: FriendRepository? = null

        fun getInstance(app: Application) =
            instance ?: synchronized(this) {
                instance ?: FriendRepository(app).also { instance = it }
            }
    }

    private val database = CavityDatabase.getInstance(app)
    private val friendDao = database.friendDao()
    private val historyXFriendDao = database.historyXFriendDao()

    suspend fun <T> transaction(databaseQueries: suspend () -> T) = database.withTransaction {
        databaseQueries()
    }

    suspend fun insertFriend(friend: Friend) {
        if (friend.name.isBlank()) {
            throw IllegalArgumentException("Friend name is blank.")
        }

        friendDao.insertFriend(friend)
    }

    suspend fun insertFriends(friends: List<Friend>) = friendDao.insertFriends(friends)

    suspend fun updateFriend(friend: Friend) {
        if (friend.name.isBlank()) {
            throw IllegalArgumentException("Friend name is blank.")
        }

        friendDao.updateFriend(friend)
    }

    suspend fun deleteFriend(friend: Friend) = friendDao.deleteFriend(friend)
    suspend fun deleteAllFriends() = friendDao.deleteAll()
    fun getAllFriends() = friendDao.getAllFriends()
    suspend fun getFriendByIdNotLive(friendId: Long) = friendDao.getFriendByIdNotLive(friendId)
    suspend fun getAllFriendsNotLive() = friendDao.getAllFriendsNotLive()

    suspend fun insertFriendHistoryXRefs(fxh: List<HistoryXFriend>) =
        historyXFriendDao.insertHistoryXFriends(fxh)

    suspend fun getAllHistoryXFriendsNotLive() = historyXFriendDao.getAllHistoryXFriendsNotLive()
    suspend fun deleteAllFriendHistoryXRefs() = historyXFriendDao.deleteAll()
}
