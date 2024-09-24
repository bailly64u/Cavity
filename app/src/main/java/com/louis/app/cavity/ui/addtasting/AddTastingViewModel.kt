package com.louis.app.cavity.ui.addtasting

import android.app.Application
import androidx.lifecycle.*
import com.louis.app.cavity.R
import com.louis.app.cavity.db.dao.BoundedBottle
import com.louis.app.cavity.domain.repository.BottleRepository
import com.louis.app.cavity.domain.repository.FriendRepository
import com.louis.app.cavity.domain.repository.TastingRepository
import com.louis.app.cavity.model.Friend
import com.louis.app.cavity.model.Tasting
import com.louis.app.cavity.model.TastingAction
import com.louis.app.cavity.model.TastingBottle
import com.louis.app.cavity.util.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddTastingViewModel(app: Application) : AndroidViewModel(app) {
    private val bottleRepository = BottleRepository.getInstance(app)
    private val tastingRepository = TastingRepository.getInstance(app)
    private val friendRepository = FriendRepository.getInstance(app)

    private val _userFeedback = MutableLiveData<Event<Int>>()
    val userFeedback: LiveData<Event<Int>>
        get() = _userFeedback

    private val _tastingSaved = MutableLiveData<Event<Tasting>>()
    val tastingSaved: LiveData<Event<Tasting>>
        get() = _tastingSaved

    private val _cancelTastingAlarms = MutableLiveData<Event<List<Tasting>>>()
    val cancelTastingAlarms: LiveData<Event<List<Tasting>>>
        get() = _cancelTastingAlarms

    private val _selectedBottles = MutableLiveData<MutableList<BoundedBottle>>(mutableListOf())
    val selectedBottles: LiveData<MutableList<BoundedBottle>>
        get() = _selectedBottles

    val tastingBottles = _selectedBottles.switchMap { updateTastingBottles(it) }

    val friends = friendRepository.getAllFriends()

    private var currentTasting: Tasting? = null
    private var selectedFriends: List<Long> = emptyList()
    var tastingDate: Long = System.currentTimeMillis()

    fun submitTasting(
        opportunity: String,
        isMidday: Boolean,
        friends: List<Friend>
    ): Boolean {
        if (friends.isEmpty()) {
            _userFeedback.postOnce(R.string.no_friend)
            return false
        }

        currentTasting = Tasting(0, tastingDate, isMidday, opportunity)
        selectedFriends = friends.map { it.id }

        return true
    }

    fun saveTasting() {
        val tasting = currentTasting

        if (tasting == null) {
            _userFeedback.postOnce(R.string.base_error)
            return
        }

        viewModelScope.launch(IO) {
            val bottleIds = _selectedBottles.value?.map { it.bottle.id } ?: emptyList()
            val tastingId = tastingRepository.insertTasting(tasting)

            // Updating tasting id so that we can reuse it later to schedule alarms
            currentTasting = currentTasting!!.copy(id = tastingId)

            tastingRepository.run {
                transaction {
                    bottleRepository.boundBottlesToTasting(tastingId, bottleIds)
                    insertTastingFriendXRef(tastingId, selectedFriends)
                    generateTastingActions(currentTasting!!, tastingBottles.value)
                }
            }
        }
    }

    fun onBottleStateChanged(bottle: BoundedBottle, isSelected: Boolean) {
        _selectedBottles.let {
            if (isSelected) it += bottle else it -= bottle
        }
    }

    private fun updateTastingBottles(selectedBottles: List<BoundedBottle>) =
        liveData(IO) {
            val bottleIds = selectedBottles.map { it.bottle.id }
            val occupiedBottles = bottleRepository.getTastingBottleIdsIn(bottleIds)

            val result = selectedBottles.map {
                TastingBottle(
                    it.bottle.id,
                    it.wine,
                    it.bottle.vintage,
                    it.bottle.bottleSize,
                    showOccupiedWarning = it.bottle.id in occupiedBottles
                )
            }

            emit(result)
        }

    private suspend fun generateTastingActions(
        tasting: Tasting,
        tastingBottles: List<TastingBottle>?
    ) {
        if (tastingBottles == null) {
            return
        }

        val occupiedBottles = tastingBottles
            .filter { it.showOccupiedWarning }
            .map { it.bottleId }

        if (occupiedBottles.isNotEmpty()) {
            cleanTastings(occupiedBottles)
        }

        val actions = mutableListOf<TastingAction>()

        for (tastingBottle in tastingBottles) {
            tastingRepository.deleteTastingActionsForBottle(tastingBottle.bottleId)

            if (tastingBottle.shouldFridge.toBoolean()) {
                val action = TastingAction(
                    0,
                    TastingAction.Action.SET_TO_FRIDGE,
                    tastingBottle.bottleId,
                    false.toInt()
                )

                actions += action
            }

            if (tastingBottle.shouldJug.toBoolean()) {
                val action = TastingAction(
                    0,
                    TastingAction.Action.SET_TO_JUG,
                    tastingBottle.bottleId,
                    false.toInt()
                )

                actions += action
            }

            if (tastingBottle.shouldUncork.toBoolean()) {
                val action = TastingAction(
                    0,
                    TastingAction.Action.UNCORK,
                    tastingBottle.bottleId,
                    false.toInt()
                )

                actions += action
            }
        }

        tastingRepository.insertTastingActions(actions)
        _tastingSaved.postOnce(tasting)
    }

    // Migrating bottles from one tasting to another one might create empty tasting.
    // We need to get rid of those to avoid useless system alarm
    // We also need to remove previous tasting actions
    private suspend fun cleanTastings(tastingBottleIds: List<Long>) {
        withContext(IO) {
            val emptyTastings = tastingRepository.getEmptyTastings()

            if (emptyTastings.isNotEmpty()) {
                _cancelTastingAlarms.postOnce(emptyTastings)
                tastingRepository.deleteTastings(emptyTastings)
            }

            tastingBottleIds.forEach {
                tastingRepository.deleteTastingActionsForBottle(it)
            }
        }
    }
}
