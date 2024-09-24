package com.louis.app.cavity.ui.addwine

import android.app.Application
import androidx.lifecycle.*
import com.louis.app.cavity.R
import com.louis.app.cavity.domain.repository.CountyRepository
import com.louis.app.cavity.domain.repository.WineRepository
import com.louis.app.cavity.model.County
import com.louis.app.cavity.model.Wine
import com.louis.app.cavity.model.WineColor
import com.louis.app.cavity.util.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddWineViewModel(app: Application) : AndroidViewModel(app) {
    private val countyRepository = CountyRepository.getInstance(app)
    private val wineRepository = WineRepository.getInstance(app)

    private val _userFeedback = MutableLiveData<Event<Int>>()
    val userFeedback: LiveData<Event<Int>>
        get() = _userFeedback

    private val _wineUpdatedEvent = MutableLiveData<Event<Int>>()
    val wineUpdatedEvent: LiveData<Event<Int>>
        get() = _wineUpdatedEvent

    private val _updatedWine = MutableLiveData<Wine>()
    val updatedWine: LiveData<Wine>
        get() = _updatedWine

    private val _image = MutableLiveData<String>()
    val image: LiveData<String>
        get() = _image

    private val _countyId = MutableLiveData<Long>()

    private val isEditMode: Boolean
        get() = wineId != 0L

    val namings = _countyId.switchMap { wineRepository.getNamingsForCounty(it) }

    private var wineId = 0L

    fun start(wineId: Long) {
        this.wineId = wineId

        if (wineId != 0L) {
            viewModelScope.launch(IO) {
                val wine = wineRepository.getWineByIdNotLive(wineId)

                _countyId.postValue(wine.countyId)
                _updatedWine.postValue(wine)
                _image.postValue(wine.imgPath)
            }
        }
    }

    fun getAllCounties() = countyRepository.getAllCounties()

    fun saveWine(
        name: String,
        naming: String,
        cuvee: String,
        isOrganic: Int,
        colorChipId: Int,
        county: County?
    ) {
        if (county == null) {
            _userFeedback.postOnce(R.string.no_county)
            return
        }

        val color = when (colorChipId) {
            R.id.colorRed -> WineColor.RED
            R.id.colorWhite -> WineColor.WHITE
            R.id.colorSweet -> WineColor.SWEET
            else /* R.id.colorRose */ -> WineColor.ROSE
        }

        val wine = Wine(
            wineId,
            name,
            naming,
            color,
            cuvee,
            isOrganic,
            _image.value ?: "",
            county.id
        )

        viewModelScope.launch(IO) {
            val duplicate = getSimilarWineIfAny(wine)

            if (duplicate != null) {
                when {
                    duplicate.hidden.toBoolean() && !isEditMode -> {
                        wineRepository.updateWine(duplicate.copy(hidden = false.toInt()))
                        _wineUpdatedEvent.postOnce(R.string.wine_already_exists_emergence)
                    }

                    else -> _userFeedback.postOnce(R.string.wine_already_exists)
                }

                return@launch
            }

            when {
                isEditMode -> {
                    wineRepository.updateWine(wine)
                    _wineUpdatedEvent.postOnce(R.string.wine_updated)
                    reset()
                }

                else -> {
                    wineRepository.insertWine(wine)
                    _wineUpdatedEvent.postOnce(R.string.wine_added)
                    reset()
                }
            }
        }
    }

    fun setImage(imagePath: String) {
        _image.postValue(imagePath)
    }

    fun setCountyId(countyId: Long?) {
        countyId?.let {
            _countyId.postValue(it)
        }
    }

    private fun reset() {
        wineId = 0
    }

    private suspend fun getSimilarWineIfAny(wine: Wine): Wine? {
        val hiddenWines = wineRepository.getWineByAttributes(wine.color, wine.isOrganic, wine.cuvee)

        return withContext(Default) {
            for (hiddenWine in hiddenWines) {
                val hiddenWineName = hiddenWine.name.lowercase()
                val wineName = wine.name.lowercase()
                val hiddenWineNaming = hiddenWine.naming.lowercase()
                val wineNaming = wine.naming.lowercase()

                val hasCloseNames =
                    levenshtein(hiddenWineName, wineName) <= WINE_DUPLICATE_THRESHOLD
                val hasCloseNamings =
                    levenshtein(hiddenWineNaming, wineNaming) <= WINE_DUPLICATE_THRESHOLD
                val isSelf = hiddenWine.id == wineId && isEditMode

                if (hasCloseNames && hasCloseNamings && !isSelf) {
                    return@withContext hiddenWine
                }
            }

            return@withContext null
        }
    }

    private fun levenshtein(lhs: CharSequence, rhs: CharSequence): Int {
        val lhsLength = lhs.length + 1
        val rhsLength = rhs.length + 1

        var cost = Array(lhsLength) { it }
        var newCost = Array(lhsLength) { 0 }

        for (i in 1 until rhsLength) {
            newCost[0] = i

            for (j in 1 until lhsLength) {
                val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1

                val costReplace = cost[j - 1] + match
                val costInsert = cost[j] + 1
                val costDelete = newCost[j - 1] + 1

                newCost[j] = costInsert.coerceAtMost(costDelete).coerceAtMost(costReplace)
            }

            val swap = cost
            cost = newCost
            newCost = swap
        }

        return cost[lhsLength - 1]
    }

    companion object {
        private const val WINE_DUPLICATE_THRESHOLD = 3
    }
}
