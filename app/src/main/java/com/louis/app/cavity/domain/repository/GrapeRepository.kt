package com.louis.app.cavity.domain.repository

import android.app.Application
import androidx.room.withTransaction
import com.louis.app.cavity.db.CavityDatabase
import com.louis.app.cavity.model.Grape
import com.louis.app.cavity.model.QGrape

class GrapeRepository private constructor(app: Application) {
    companion object {
        @Volatile
        var instance: GrapeRepository? = null

        fun getInstance(app: Application) =
            instance ?: synchronized(this) {
                instance ?: GrapeRepository(app).also { instance = it }
            }
    }

    private val database = CavityDatabase.getInstance(app)
    private val grapeDao = database.grapeDao()
    private val qGrapeDao = database.qGrapeDao()

    suspend fun <T> transaction(databaseQueries: suspend () -> T) = database.withTransaction {
        databaseQueries()
    }

    suspend fun updateGrape(grape: Grape) {
        if (grape.name.isBlank()) {
            throw IllegalArgumentException("Grape name is blank.")
        }

        grapeDao.updateGrape(grape)
    }

    suspend fun insertGrape(grape: Grape): Long {
        if (grape.name.isBlank()) {
            throw IllegalArgumentException("Grape name is blank.")
        }

        return grapeDao.insertGrape(grape)
    }

    suspend fun insertGrapes(grapes: List<Grape>) = grapeDao.insertGrapes(grapes)
    suspend fun deleteGrape(grape: Grape) = grapeDao.deleteGrape(grape)
    fun getAllGrapes() = grapeDao.getAllGrapes()
    suspend fun getAllGrapesNotLive() = grapeDao.getAllGrapesNotLive()
    fun getGrapeWithQuantifiedGrapes() = grapeDao.getGrapeWithQuantifiedGrapes()
    suspend fun getAllQGrapesNotLive() = qGrapeDao.getAllQGrapesNotLive()
    suspend fun insertQGrapes(grapes: List<QGrape>) = qGrapeDao.insertQGrapes(grapes)

    fun getQGrapesAndGrapeForBottle(bottleId: Long) =
        qGrapeDao.getQGrapesAndGrapeForBottle(bottleId)

    suspend fun getQGrapesAndGrapeForBottleNotLive(bottleId: Long) =
        qGrapeDao.getQGrapesAndGrapeForBottleNotLive(bottleId)

    suspend fun replaceQGrapesForBottle(bottleId: Long, qGrapes: List<QGrape>) {
        if (!database.inTransaction()) {
            throw IllegalStateException("This method should be called inside a transaction")
        }

        qGrapeDao.clearAllQGrapesForBottle(bottleId)
        qGrapeDao.insertQGrapes(qGrapes)
    }

    suspend fun deleteAllGrapes() = grapeDao.deleteAll()
    suspend fun deleteAllQGrapes() = qGrapeDao.deleteAll()
}
