package com.louis.app.cavity.ui.home

import android.app.Application
import androidx.lifecycle.*
import com.louis.app.cavity.R
import com.louis.app.cavity.db.WineRepository
import com.louis.app.cavity.model.Bottle
import com.louis.app.cavity.util.Event
import com.louis.app.cavity.util.postOnce
import com.louis.app.cavity.util.toBoolean
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = WineRepository.getInstance(app)

    private val _userFeedback = MutableLiveData<Event<Int>>()
    val userFeedback: LiveData<Event<Int>>
        get() = _userFeedback

    private val observedCounty = MutableLiveData<Long>()

    val bottleCount = observedCounty.switchMap {
        repository.getBottleCountForCounty(it)
    }

    val bottlePrice = observedCounty.switchMap {
        repository.getCountyPriceByCurrency(it)
    }

    val namingCount = observedCounty.switchMap {
        repository.getNamingsStatsForCounty(it)
    }

    val vintagesCount = observedCounty.switchMap {
        repository.getVintagesStatsForCounty(it)
    }

    fun setObservedCounty(countyId: Long) {
        observedCounty.value = countyId
    }

    fun deleteOrHideWine(wineId: Long) = viewModelScope.launch(IO) {
        val wineBottles = repository.getBottlesForWineNotLive(wineId)
        val folder = mutableListOf<Bottle>() to mutableListOf<Bottle>()
        val (consumed, stock) = wineBottles.fold(folder) { pair, bottle ->
            pair.apply {
                when (bottle.consumed.toBoolean()) {
                    true -> first += bottle
                    else -> second += bottle
                }
            }
        }

        repository.deleteBottles(stock)

        when {
            consumed.size > 0 -> repository.hideWineById(wineId)
            else -> repository.deleteWineById(wineId)
        }

        // We dirty liers
        _userFeedback.postOnce(R.string.wine_deleted)
    }

    fun getAllCounties() = repository.getAllCounties()

    // This become unnecessary if we figure out how to implement Room's multimaps with standard SQL Join request
    fun getWinesWithBottlesByCounty(countyId: Long) = liveData(Default) {
        emitSource(
            repository.getWineWithBottlesByCounty(countyId).map { winesWithBottles ->
                winesWithBottles
                    .sortedBy { it.wine.color.order }
                    .map { wineWithBottles ->
                        wineWithBottles.copy(
                            bottles = wineWithBottles.bottles
                                .filter { !it.consumed.toBoolean() }
                                .sortedBy { it.vintage }
                        )
                    }
            }
        )
    }
}
