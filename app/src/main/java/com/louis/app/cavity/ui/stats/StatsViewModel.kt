package com.louis.app.cavity.ui.stats

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.*
import com.louis.app.cavity.db.dao.Year
import com.louis.app.cavity.domain.repository.HistoryRepository
import com.louis.app.cavity.domain.repository.StatsRepository
import com.louis.app.cavity.util.combine

class StatsViewModel(app: Application) : AndroidViewModel(app) {
    private val statsRepository = StatsRepository.getInstance(app)
    private val historyRepository = HistoryRepository.getInstance(app)

    private val groupedYears = Year("Combiner", 0L, System.currentTimeMillis())
    private val year = MutableLiveData(groupedYears)
    private val comparisonYear = MutableLiveData(groupedYears)

    private val statFactory = LiveDataStatsFactory(statsRepository, year, comparisonYear)

    val comparisonText = year.combine(comparisonYear) { year, cYear ->
        "$year <> $cYear"
    }

    val results = statFactory.results

    val years: LiveData<List<Year>> = historyRepository.getYears().map {
        it.toMutableList().apply {
            reverse()
            add(0, groupedYears)
            add(groupedYears)
        }
    }

    private val _currentItemPosition = MutableLiveData<Int>()
    val currentItemPosition: LiveData<Int>
        get() = _currentItemPosition

    val details = currentItemPosition.switchMap { results[it] }

    val comparisonDetails by lazy {
        currentItemPosition.switchMap { statFactory.comparisons[it] }
    }

    private val _showYearPicker = MutableLiveData(false)
    val showYearPicker: LiveData<Boolean>
        get() = _showYearPicker

    private val _comparison = MutableLiveData(false)
    val comparison: LiveData<Boolean>
        get() = _comparison

    fun getTotalPriceByCurrency() = statsRepository.getTotalPriceByCurrency()

    fun getTotalConsumed() = statsRepository.getTotalConsumedBottles()

    fun getTotalStock() = statsRepository.getTotalStockBottles()

    fun setStatType(viewPagerPos: Int, statType: StatType) {
        statFactory.applyStatType(viewPagerPos, statType)
    }

    fun notifyPageChanged(position: Int) {
        _currentItemPosition.value = position
    }

    fun setYear(year: Year) {
        val currentYear = this.year.value!!

        if (year != currentYear) {
            this.year.value = year
        }
    }

    fun setComparisonYear(year: Year) {
        this._comparison.value = true
        this.comparisonYear.value = year
    }

    fun setShouldShowYearPicker(show: Boolean) {
        val currentValue = _showYearPicker.value!!

        if (show != currentValue) {
            _showYearPicker.value = show

            if (!show) {
                stopComparison()
            }
        }
    }

    @StringRes
    fun getStatTypeLabel(): Int {
        return currentItemPosition.value?.let {
            statFactory.getStatTypeLabel(it)
        } ?: -1
    }

    private fun stopComparison() {
        _comparison.value = false
    }
}

enum class StatType {
    STOCK,
    REPLENISHMENTS,
    CONSUMPTIONS,
}


