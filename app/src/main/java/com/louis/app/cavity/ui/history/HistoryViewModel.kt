package com.louis.app.cavity.ui.history

import android.app.Application
import androidx.annotation.IdRes
import androidx.lifecycle.*
import androidx.paging.*
import com.louis.app.cavity.R
import com.louis.app.cavity.db.dao.BoundedHistoryEntry
import com.louis.app.cavity.domain.repository.HistoryRepository
import com.louis.app.cavity.model.HistoryEntry
import com.louis.app.cavity.util.DateFormatter
import com.louis.app.cavity.util.Event
import com.louis.app.cavity.util.postOnce
import com.louis.app.cavity.util.toBoolean
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryViewModel(app: Application) : AndroidViewModel(app) {
    private val historyRepository = HistoryRepository.getInstance(app)

    private val _scrollTo = MutableLiveData<Event<Int>>()
    val scrollTo: LiveData<Event<Int>>
        get() = _scrollTo

    private val _selectedEntry = MutableLiveData<BoundedHistoryEntry?>(null)
    val selectedEntry: LiveData<BoundedHistoryEntry?>
        get() = _selectedEntry

    private val _filter = MutableLiveData<HistoryFilter>(HistoryFilter.NoFilter)
    val filter: LiveData<HistoryFilter>
        get() = _filter

    private val _showDatePicker = MutableLiveData<Event<Long>>(null)
    val showDatePicker: LiveData<Event<Long>>
        get() = _showDatePicker

    val entries: LiveData<PagingData<HistoryUiModel>> = filter.switchMap {
        Pager(PagingConfig(pageSize = 50, prefetchDistance = 20, enablePlaceholders = true)) {
            getDataSource(it)
        }.liveData.map { pagingData ->
            pagingData
                .map { HistoryUiModel.EntryModel(it) }
                .insertSeparators { before, after ->
                    if (shouldSeparate(before, after))
                        HistoryUiModel.HeaderModel(after?.model?.historyEntry?.date ?: 0L)
                    else null
                }
        }.cachedIn(viewModelScope)
    }

    fun applyExternalFilters(wineId: Long, bottleId: Long) {
        if (wineId != -1L) {
            setFilter(HistoryFilter.WineFilter(wineId))
        }

        if (bottleId != -1L) {
            setFilter(HistoryFilter.BottleFilter(bottleId))
        }
    }

    fun requestScrollToDate(timestamp: Long) {
        viewModelScope.launch(IO) {
            val filter = _filter.value ?: HistoryFilter.NoFilter
            val entries = historyRepository.getAllEntriesNotPagedNotLive()
            val filtered = rawFilter(entries, filter)
            val offset = 1

            withContext(Default) {
                var headerCount = 0
                var currentDay = -1L
                var dateFounded = false

                for ((position, entry) in filtered.withIndex()) {
                    val day = DateFormatter.roundToDay(entry.date)

                    if (day != currentDay) {
                        currentDay = day
                        headerCount++
                    }

                    if (day <= timestamp) {
                        dateFounded = true
                        _scrollTo.postOnce(position + headerCount - offset)
                        break
                    }

                }

                if (!dateFounded) {
                    _scrollTo.postOnce(entries.size + headerCount)
                }
            }
        }
    }

    fun requestDatePicker() {
        viewModelScope.launch(IO) {
            val filter = _filter.value ?: HistoryFilter.NoFilter
            val entries = historyRepository.getAllEntriesNotPagedNotLive()
            val filtered = rawFilter(entries, filter)

            if (filtered.isEmpty() || filter is HistoryFilter.WineFilter) {
                return@launch
            }

            val oldestEntryDate = filtered.last().date
            _showDatePicker.postOnce(oldestEntryDate)
        }
    }

    fun setFilter(filter: HistoryFilter) {
        _selectedEntry.postValue(null)
        _filter.postValue(filter)
    }

    fun setSelectedHistoryEntry(entry: BoundedHistoryEntry?) {
        _selectedEntry.postValue(entry)
    }

    fun updateHistoryEntry(entry: HistoryEntry) {
        viewModelScope.launch(IO) {
            historyRepository.updateEntry(entry)
        }
    }

    private fun shouldSeparate(
        before: HistoryUiModel.EntryModel?,
        after: HistoryUiModel?
    ): Boolean {
        if (before == null && after == null) {
            return false
        }

        return if (after is HistoryUiModel.EntryModel?) {
            val beforeTimestamp =
                DateFormatter.roundToDay(before?.model?.historyEntry?.date ?: return true)
            val afterTimestamp =
                DateFormatter.roundToDay(after?.model?.historyEntry?.date ?: return false)

            beforeTimestamp != afterTimestamp
        } else false
    }

    private fun getDataSource(filter: HistoryFilter): PagingSource<Int, BoundedHistoryEntry> {
        return when (filter) {
            is HistoryFilter.TypeFilter -> when (filter.chipId) {
                R.id.chipReplenishments -> historyRepository.getEntriesByType(1, 3)
                R.id.chipComsumptions -> historyRepository.getEntriesByType(0, 2)
                R.id.chipTastings -> historyRepository.getEntriesByType(4, 4)
                R.id.chipGiftedTo -> historyRepository.getEntriesByType(2, 2)
                R.id.chipGiftedBy -> historyRepository.getEntriesByType(3, 3)
                R.id.chipFavorites -> historyRepository.getFavoriteEntries()
                else -> historyRepository.getAllEntries()
            }

            is HistoryFilter.WineFilter -> historyRepository.getEntriesForWine(filter.wineId)
            is HistoryFilter.BottleFilter -> historyRepository.getEntriesForBottle(filter.bottleId)
            is HistoryFilter.NoFilter -> historyRepository.getAllEntries()
        }
    }

    // Meant to be used when user wants to use date navigation with a filter on.
    // We have to get all history entries (not paged) & reapply filters ourselves without SQL query
    // to get correct position
    private fun rawFilter(source: List<HistoryEntry>, filter: HistoryFilter): List<HistoryEntry> {
        return when (filter) {
            is HistoryFilter.TypeFilter -> when (filter.chipId) {
                R.id.chipReplenishments -> source.filter { it.type == 1 || it.type == 3 }
                R.id.chipComsumptions -> source.filter { it.type == 0 || it.type == 2 }
                R.id.chipTastings -> source.filter { it.type == 4 }
                R.id.chipGiftedTo -> source.filter { it.type == 2 }
                R.id.chipGiftedBy -> source.filter { it.type == 3 }
                R.id.chipFavorites -> source.filter { it.favorite.toBoolean() }
                else -> source
            }

            is HistoryFilter.WineFilter -> source // No support for wines
            is HistoryFilter.BottleFilter -> source.filter { it.bottleId == filter.bottleId }
            is HistoryFilter.NoFilter -> source
        }
    }

}

sealed class HistoryFilter {
    class TypeFilter(@IdRes val chipId: Int) : HistoryFilter()
    class WineFilter(val wineId: Long) : HistoryFilter()
    class BottleFilter(val bottleId: Long) : HistoryFilter()
    data object NoFilter : HistoryFilter()
}
