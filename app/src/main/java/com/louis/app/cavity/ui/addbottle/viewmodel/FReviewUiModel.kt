package com.louis.app.cavity.ui.addbottle.viewmodel

import com.louis.app.cavity.model.relation.review.FilledReviewAndReview

data class FReviewUiModel(val name: String, var type: Int, var value: Int) {
    companion object {
        fun fromFReview(fReview: FilledReviewAndReview): FReviewUiModel {
            return FReviewUiModel(
                fReview.review.contestName,
                fReview.review.type,
                fReview.fReview.value
            )
        }
    }
}

data class ReviewUiModel(val name: String, val type: Int, var isChecked: Boolean)
