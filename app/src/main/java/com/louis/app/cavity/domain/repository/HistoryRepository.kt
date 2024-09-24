package com.louis.app.cavity.domain.repository

import android.app.Application
import androidx.room.withTransaction
import com.louis.app.cavity.db.CavityDatabase
import com.louis.app.cavity.model.HistoryEntry
import com.louis.app.cavity.model.HistoryXFriend

class HistoryRepository private constructor(app: Application) {
    companion object {
        @Volatile
        var instance: HistoryRepository? = null

        fun getInstance(app: Application) =
            instance ?: synchronized(this) {
                instance ?: HistoryRepository(app).also { instance = it }
            }
    }

    private val database = CavityDatabase.getInstance(app)
    private val historyDao = database.historyDao()
    private val historyXFriendDao = database.historyXFriendDao()

    suspend fun <T> transaction(databaseQueries: suspend () -> T) = database.withTransaction {
        databaseQueries()
    }

    suspend fun updateEntry(entry: HistoryEntry) = historyDao.updateEntry(entry)
    fun getAllEntries() = historyDao.getAllEntries()
    suspend fun getAllEntriesNotPagedNotLive() = historyDao.getAllEntriesNotPagedNotLive()
    fun getYears() = historyDao.getYears()
    fun getEntriesByType(type1: Int, type2: Int) = historyDao.getEntriesByType(type1, type2)
    fun getEntriesForWine(wineId: Long) = historyDao.getEntriesForWine(wineId)
    fun getEntriesForBottle(bottleId: Long) = historyDao.getEntriesForBottle(bottleId)
    fun getReplenishmentForBottleNotPaged(bottleId: Long) =
        historyDao.getReplenishmentForBottleNotPaged(bottleId)

    fun getEntriesForDate(date: Long) = historyDao.getEntriesForDate(date)
    fun getFavoriteEntries() = historyDao.getFavoriteEntries()

    suspend fun clearExistingReplenishments(bottleId: Long) =
        historyDao.clearExistingReplenishments(bottleId)

    suspend fun insertHistoryEntry(entry: HistoryEntry) = historyDao.insertEntry(entry)
    suspend fun insertHistoryEntries(entries: List<HistoryEntry>) =
        historyDao.insertEntries(entries)

    // TODO: handle duplicata
    suspend fun insertFriendHistoryXRefs(fxh: List<HistoryXFriend>) =
        historyXFriendDao.insertHistoryXFriends(fxh)

    suspend fun insertHistoryEntryAndFriends(entry: HistoryEntry, friends: List<Long>) {
        if (!database.inTransaction()) {
            throw IllegalStateException("This method should be called inside a transaction")
        }

        val historyId = insertHistoryEntry(entry)
        val historyXFriends = friends.map { HistoryXFriend(historyId, it) }
        insertFriendHistoryXRefs(historyXFriends)
    }

    suspend fun deleteAllHistoryEntries() = historyDao.deleteAll()
}
