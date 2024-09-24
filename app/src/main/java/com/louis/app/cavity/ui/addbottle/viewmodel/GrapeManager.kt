package com.louis.app.cavity.ui.addbottle.viewmodel

import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.louis.app.cavity.R
import com.louis.app.cavity.domain.grape.QuantifiedGrapeHelper
import com.louis.app.cavity.domain.repository.GrapeRepository
import com.louis.app.cavity.model.Bottle
import com.louis.app.cavity.model.Grape
import com.louis.app.cavity.util.Event
import com.louis.app.cavity.util.minusAssign
import com.louis.app.cavity.util.plusAssign
import com.louis.app.cavity.util.postOnce
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GrapeManager(
    private val viewModelScope: CoroutineScope,
    private val repository: GrapeRepository,
    private val editedBottle: Bottle?,
    private val _userFeedback: MutableLiveData<Event<Int>>
) {
    private val qGrapeHelper = QuantifiedGrapeHelper()

    private val _grapeDialogEvent = MutableLiveData<Event<List<GrapeUiModel>>>()
    val grapeDialogEvent: LiveData<Event<List<GrapeUiModel>>>
        get() = _grapeDialogEvent

    private val _qGrapes = MutableLiveData<MutableList<QGrapeUiModel>>(mutableListOf())
    val qGrapes: LiveData<MutableList<QGrapeUiModel>>
        get() = _qGrapes

    init {
        if (editedBottle != null) {
            viewModelScope.launch(IO) {
                val qGrapes = repository.getQGrapesAndGrapeForBottleNotLive(editedBottle.id)
                val uiGrapes = qGrapes.map { QGrapeUiModel.fromQGrape(it) }.toMutableList()
                qGrapeHelper.submitQGrapes(qGrapes)
                _qGrapes.postValue(uiGrapes)
            }
        }
    }

    fun addGrapeAndQGrape(grapeName: String) {
        viewModelScope.launch(IO) {
            try {
                val grape = Grape(0, grapeName)
                val grapeId = repository.insertGrape(grape)
                val defaultValue = qGrapeHelper.requestAddQGrape()

                withContext(Main) {
                    _qGrapes += QGrapeUiModel(grapeId, grapeName, defaultValue)
                }
            } catch (e: IllegalArgumentException) {
                _userFeedback.postOnce(R.string.empty_grape_name)
            } catch (e: SQLiteConstraintException) {
                _userFeedback.postOnce(R.string.grape_already_exists)
            }
        }
    }

    private fun addQuantifiedGrape(grapeId: Long, grapeName: String) {
        val defaultValue = qGrapeHelper.requestAddQGrape()
        _qGrapes += QGrapeUiModel(grapeId, grapeName, defaultValue)
    }

    // Return the checked value to tell the adapter to set the slider value accordingly
    fun updateQuantifiedGrape(qGrape: QGrapeUiModel, newValue: Int): Int {
        val checkedValue = qGrapeHelper.requestUpdateQGrape(qGrape.percentage, newValue)

        _qGrapes.run {
            val index = value?.indexOfFirst { it.grapeId == qGrape.grapeId } ?: return checkedValue

            value?.set(index, value!![index].copy(percentage = checkedValue))
            postValue(value)
        }

        return checkedValue
    }

    // Delete from recycler view
    fun removeQuantifiedGrape(qGrape: QGrapeUiModel) {
        qGrapeHelper.requestRemoveQGrape(qGrape)
        _qGrapes -= qGrape
    }

    // Delete from dialog
    private fun removeQuantifiedGrape(grapeId: Long) {
        val qGrape = _qGrapes.value?.find { it.grapeId == grapeId } ?: return
        removeQuantifiedGrape(qGrape)
    }

    fun submitCheckedGrapes(checkableGrapes: List<GrapeUiModel>) {
        for (checkableGrape in checkableGrapes) {
            val (id, name, isChecked) = checkableGrape
            val oldOne =
                _grapeDialogEvent.value?.peekContent()?.find { it.id == id }

            when {
                isChecked && oldOne?.isChecked != true -> addQuantifiedGrape(id, name)
                !isChecked && oldOne?.isChecked != false -> removeQuantifiedGrape(id)
            }

            // Not updating the value of the _grapeDialogEvent LiveData. This will be done
            // when requestGrapeDialog() is called only
        }
    }

    fun requestGrapeDialog() {
        viewModelScope.launch(IO) {
            val grapes = repository.getAllGrapesNotLive()
            val qGrapes = _qGrapes.value?.map { it.grapeId } ?: emptyList()
            val currentCheckedGrapes =
                grapes.map { GrapeUiModel(it.id, it.name, isChecked = it.id in qGrapes) }

            _grapeDialogEvent.postOnce(currentCheckedGrapes)
        }
    }
}
