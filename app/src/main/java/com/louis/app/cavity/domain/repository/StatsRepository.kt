package com.louis.app.cavity.domain.repository

import android.app.Application
import androidx.room.withTransaction
import com.louis.app.cavity.db.CavityDatabase

class StatsRepository private constructor(app: Application) {
    companion object {
        @Volatile
        var instance: StatsRepository? = null

        fun getInstance(app: Application) =
            instance ?: synchronized(this) {
                instance ?: StatsRepository(app).also { instance = it }
            }
    }

    private val database = CavityDatabase.getInstance(app)
    private val statsDao = database.statsDao()

    suspend fun <T> transaction(databaseQueries: suspend () -> T) = database.withTransaction {
        databaseQueries()
    }

    fun getBottleCountForCounty(countyId: Long) = statsDao.getBottleCountForCounty(countyId)
    fun getPriceByCurrencyForCounty(countyId: Long) = statsDao.getPriceByCurrencyForCounty(countyId)
    fun getNamingsStatsForCounty(countyId: Long) = statsDao.getNamingsForCounty(countyId)
    fun getVintagesStatsForCounty(countyId: Long) = statsDao.getVintagesForCounty(countyId)
    fun getStockByCounty() = statsDao.getStockByCounty()
    fun getReplenishmentsByCounty(start: Long, end: Long) =
        statsDao.getReplenishmentsByCounty(start, end)

    fun getConsumptionsByCounty(start: Long, end: Long) =
        statsDao.getConsumptionsByCounty(start, end)

    fun getStockByColor() = statsDao.getStockByColor()
    fun getReplenishmentsByColor(start: Long, end: Long) =
        statsDao.getReplenishmentsByColor(start, end)

    fun getConsumptionsByColor(start: Long, end: Long) =
        statsDao.getConsumptionsByColor(start, end)

    fun getStockByVintage() = statsDao.getStockByVintage()
    fun getReplenishmentsByVintage(start: Long, end: Long) =
        statsDao.getReplenishmentsByVintage(start, end)

    fun getConsumptionsByVintage(start: Long, end: Long) =
        statsDao.getConsumptionsByVintage(start, end)

    fun getStockByNaming() = statsDao.getStockByNaming()
    fun getReplenishmentsByNaming(start: Long, end: Long) =
        statsDao.getReplenishmentsByNaming(start, end)

    fun getConsumptionsByNaming(start: Long, end: Long) =
        statsDao.getConsumptionsByNaming(start, end)

    fun getTotalPriceByCurrency() = statsDao.getTotalPriceByCurrency()
    fun getTotalConsumedBottles() = statsDao.getTotalConsumedBottles()
    fun getTotalStockBottles() = statsDao.getTotalStockBottles()
    fun getBottlesByIds(ids: List<Long>) = statsDao.getBottlesByIds(ids)
}
