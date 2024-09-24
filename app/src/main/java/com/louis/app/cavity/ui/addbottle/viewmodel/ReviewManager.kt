package com.louis.app.cavity.ui.addbottle.viewmodel

import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.louis.app.cavity.R
import com.louis.app.cavity.domain.repository.ReviewRepository
import com.louis.app.cavity.model.Bottle
import com.louis.app.cavity.model.Review
import com.louis.app.cavity.util.Event
import com.louis.app.cavity.util.minusAssign
import com.louis.app.cavity.util.plusAssign
import com.louis.app.cavity.util.postOnce
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReviewManager(
    private val viewModelScope: CoroutineScope,
    private val repository: ReviewRepository,
    private val editedBottle: Bottle?,
    private val _userFeedback: MutableLiveData<Event<Int>>
) {
    private val _reviewDialogEvent = MutableLiveData<Event<List<ReviewUiModel>>>()
    val reviewDialogEvent: LiveData<Event<List<ReviewUiModel>>>
        get() = _reviewDialogEvent

    private val _fReviews = MutableLiveData<MutableList<FReviewUiModel>>(mutableListOf())
    val fReviews: LiveData<MutableList<FReviewUiModel>>
        get() = _fReviews

    init {
        if (editedBottle != null) {
            viewModelScope.launch(IO) {
                val fReviews = repository.getFReviewAndReviewForBottleNotLive(editedBottle.id)
                val uiReviews = fReviews.map { FReviewUiModel.fromFReview(it) }.toMutableList()
                _fReviews.postValue(uiReviews)
            }
        }
    }

    fun addReviewAndFReview(contestName: String, type: Int) {
        viewModelScope.launch(IO) {
            try {
                val review = Review(0, contestName, type)
                val reviewId = repository.insertReview(review)
                val defaultValue = getDefaultValue(type)

                withContext(Main) {
                    _fReviews += FReviewUiModel(reviewId, contestName, type, defaultValue)
                }
            } catch (e: IllegalArgumentException) {
                _userFeedback.postOnce(R.string.empty_contest_name)
            } catch (e: SQLiteConstraintException) {
                _userFeedback.postOnce(R.string.contest_name_already_exists)
            }
        }
    }

    private fun addFilledReview(reviewId: Long, contestName: String, type: Int) {
        _fReviews += FReviewUiModel(reviewId, contestName, type, getDefaultValue(type))
    }

    fun updateFilledReview(fReview: FReviewUiModel, contestValue: Int) {
        _fReviews.run {
            val index = value?.indexOfFirst { it.reviewId == fReview.reviewId } ?: return

            value?.set(index, value!![index].copy(value = contestValue))
            postValue(value)
        }
    }

    // Delete from recycler view
    fun removeFilledReview(fReview: FReviewUiModel) {
        _fReviews -= fReview
    }

    // Delete from dialog
    private fun removeFilledReview(contestName: String) {
        val fReview = _fReviews.value?.find { it.name == contestName } ?: return
        removeFilledReview(fReview)
    }

    fun submitCheckedReviews(checkableReviews: List<ReviewUiModel>) {
        for (checkableReview in checkableReviews) {
            val (id, name, type, isChecked) = checkableReview
            val oldOne =
                _reviewDialogEvent.value?.peekContent()?.find { it.id == id }

            when {
                isChecked && oldOne?.isChecked != true ->
                    addFilledReview(id, name, type)
                !isChecked && oldOne?.isChecked != false -> removeFilledReview(name)
            }

            // Not updating the value of the _grapeDialogEvent LiveData. This will be done
            // when requestGrapeDialog() is called only
        }
    }

    fun requestReviewDialog() {
        viewModelScope.launch(IO) {
            val reviews = repository.getAllReviewsNotLive()
            val fReviews = _fReviews.value?.map { it.name } ?: emptyList<FReviewUiModel>()
            val currentCheckedReviews =
                reviews.map {
                    ReviewUiModel(
                        it.id,
                        it.contestName,
                        it.type,
                        isChecked = it.contestName in fReviews
                    )
                }

            _reviewDialogEvent.postOnce(currentCheckedReviews)
        }
    }

    private fun getDefaultValue(type: Int) = when (type) {
        0 -> 1
        1 -> 15
        2 -> 80
        3 -> 1
        else -> 0
    }
}
