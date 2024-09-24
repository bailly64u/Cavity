package com.louis.app.cavity.ui.stats

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import com.louis.app.cavity.R
import com.louis.app.cavity.db.dao.Stat
import com.louis.app.cavity.db.dao.Year
import com.louis.app.cavity.domain.repository.StatsRepository

class LiveDataStatsFactory(
    private val repository: StatsRepository,
    private val year: MutableLiveData<Year>,
    private val comparisonYear: MutableLiveData<Year>
) {

    private val statTypes = MutableList(4) {
        MutableLiveData(StatType.STOCK)
    }

    private val _comparisons: MutableList<LiveData<List<Stat>>> = MutableList(4) {
        createComparisonLiveStat(it)
    }
    val comparisons: List<LiveData<List<Stat>>>
        get() = _comparisons

    private val _results: MutableList<LiveData<List<Stat>>> = MutableList(4) {
        createLiveStat(it)
    }
    val results: List<LiveData<List<Stat>>>
        get() = _results

    @StringRes
    fun getStatTypeLabel(position: Int): Int {
        return when (statTypes[position].value) {
            StatType.STOCK -> R.string.stock
            StatType.REPLENISHMENTS -> R.string.replenishments
            StatType.CONSUMPTIONS -> R.string.consumptions
            else -> -1
        }
    }


    fun applyStatType(position: Int, statType: StatType) {
        statTypes[position].value = statType
    }

    private fun createLiveStat(position: Int) = year.switchMap { year ->
        statTypes[position].switchMap { statType ->
            getStat(position, year, statType)
        }
    }

    private fun createComparisonLiveStat(position: Int) = comparisonYear.switchMap { comparisonY ->
        statTypes[position].switchMap { statType ->
            getStat(position, comparisonY, statType)
        }
    }

    // It can't be anything else.
    @Suppress("UNCHECKED_CAST")
    private fun getStat(position: Int, year: Year, statType: StatType): LiveData<List<Stat>> {
        val start = year.yearStart
        val end = year.yearEnd

        return when (position) {
            0 -> when (statType) {
                StatType.STOCK -> repository.getStockByCounty()
                StatType.REPLENISHMENTS -> repository.getReplenishmentsByCounty(start, end)
                StatType.CONSUMPTIONS -> repository.getConsumptionsByCounty(start, end)
            } as LiveData<List<Stat>>

            1 -> when (statType) {
                StatType.STOCK -> repository.getStockByColor()
                StatType.REPLENISHMENTS -> repository.getReplenishmentsByColor(start, end)
                StatType.CONSUMPTIONS -> repository.getConsumptionsByColor(start, end)
            } as LiveData<List<Stat>>

            2 -> when (statType) {
                StatType.STOCK -> repository.getStockByVintage()
                StatType.REPLENISHMENTS -> repository.getReplenishmentsByVintage(start, end)
                StatType.CONSUMPTIONS -> repository.getConsumptionsByVintage(start, end)
            } as LiveData<List<Stat>>

            else -> when (statType) {
                StatType.STOCK -> repository.getStockByNaming()
                StatType.REPLENISHMENTS -> repository.getReplenishmentsByNaming(start, end)
                StatType.CONSUMPTIONS -> repository.getConsumptionsByNaming(start, end)
            } as LiveData<List<Stat>>
        }
    }
}
