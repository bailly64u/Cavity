package com.louis.app.cavity.domain.repository

import android.app.Application
import androidx.room.withTransaction
import com.louis.app.cavity.db.CavityDatabase
import com.louis.app.cavity.model.County

class CountyRepository private constructor(app: Application) {
    companion object {
        @Volatile
        var instance: CountyRepository? = null

        fun getInstance(app: Application) =
            instance ?: synchronized(this) {
                instance ?: CountyRepository(app).also { instance = it }
            }
    }

    private val database = CavityDatabase.getInstance(app)
    private val countyDao = database.countyDao()

    suspend fun <T> transaction(databaseQueries: suspend () -> T) = database.withTransaction {
        databaseQueries()
    }

    // County
    suspend fun insertCounty(county: County) {
        if (county.name.isBlank()) {
            throw IllegalArgumentException("County name is blank.")
        }

        countyDao.insertCounties(county)
    }

    suspend fun insertCounties(counties: List<County>) {
        if (counties.any { it.name.isBlank() }) {
            throw IllegalArgumentException("County name is blank.")
        }

        countyDao.insertCounties(counties)
    }

    suspend fun updateCounty(county: County) {
        if (county.name.isBlank()) {
            throw IllegalArgumentException("County name is blank.")
        }

        countyDao.updateCounty(county)
    }

    fun getAllCounties() = countyDao.getAllCounties()
    fun getNonEmptyCounties() = countyDao.getNonEmptyCounties()
    suspend fun getAllCountiesNotLive() = countyDao.getAllCountiesNotLive()
    suspend fun updateCounties(counties: List<County>) = countyDao.updateCounties(counties)
    suspend fun deleteCounty(countyId: Long) = countyDao.deleteCounty(countyId)
    suspend fun deleteAllCounties() = countyDao.deleteAll()
}
