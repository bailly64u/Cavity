package com.louis.app.cavity.domain.repository

import android.app.Application
import androidx.room.withTransaction
import com.louis.app.cavity.db.CavityDatabase
import com.louis.app.cavity.model.Wine
import com.louis.app.cavity.model.WineColor

class WineRepository private constructor(app: Application) {
    companion object {
        @Volatile
        var instance: WineRepository? = null

        fun getInstance(app: Application) =
            instance ?: synchronized(this) {
                instance ?: WineRepository(app).also { instance = it }
            }
    }

    private val database = CavityDatabase.getInstance(app)

    private val wineDao = database.wineDao()
    private val bottleDao = database.bottleDao()
    private val countyDao = database.countyDao()

    suspend fun <T> transaction(databaseQueries: suspend () -> T) = database.withTransaction {
        databaseQueries()
    }

    suspend fun insertWine(wine: Wine) = wineDao.insertWine(wine)
    suspend fun insertWines(wines: List<Wine>) = wineDao.insertWines(wines)
    suspend fun updateWine(wine: Wine) = wineDao.updateWine(wine)
    suspend fun hideWineById(wineId: Long) = wineDao.hideWineById(wineId)
    suspend fun deleteWineById(wineId: Long) = wineDao.deleteWineById(wineId)
    fun getWineById(wineId: Long) = wineDao.getWineById(wineId)
    suspend fun getWineByIdNotLive(wineId: Long) = wineDao.getWineByIdNotLive(wineId)
    suspend fun getAllWinesNotLive() = wineDao.getAllWinesNotLive()
    suspend fun getWineByAttributes(color: WineColor, isOrganic: Int, cuvee: String) =
        wineDao.getWineByAttributes(color, isOrganic, cuvee)

    fun getNamingsForCounty(countyId: Long) = wineDao.getNamingsForCounty(countyId)
    fun getWineWithBottlesByCounty(countyId: Long) = wineDao.getWineWithBottlesByCounty(countyId)

    fun getCountiesWithWines() = countyDao.getCountiesWithWines()

    fun getBoundedBottles() = bottleDao.getBoundedBottles()
    suspend fun getBoundedBottleByIdNotLive(bottleId: Long) =
        bottleDao.getBoundedBottleByIdNotLive(bottleId)

    suspend fun deleteAllWines() = wineDao.deleteAll()
}
