package com.louis.app.cavity.domain.grape

import com.louis.app.cavity.db.dao.QGrapeAndGrape
import com.louis.app.cavity.ui.addbottle.viewmodel.QGrapeUiModel

class QuantifiedGrapeHelper {

    companion object {
        const val MAX_PERCENTAGE = 100
    }

    private var total = 0

    private val defaultPercentage: Int
        get() = if (total == 0) 25 else 0

    private val fillValue: Int
        get() = MAX_PERCENTAGE - total

    fun requestAddQGrape(): Int {
        return defaultPercentage.also { total += defaultPercentage }
    }

    fun requestUpdateQGrape(oldValue: Int, newValue: Int): Int {
        val diff = newValue - oldValue

        return if (diff < 0) {
            newValue.also { total += diff }
        } else {
            if (diff > fillValue) {
                oldValue + fillValue.also { total += fillValue }
            } else {
                newValue.also { total += diff }
            }
        }
    }

    fun requestRemoveQGrape(qGrape: QGrapeUiModel) {
        total -= qGrape.percentage
    }

    fun submitQGrapes(qGrapes: List<QGrapeAndGrape>) {
        total = qGrapes.sumOf { it.qGrape.percentage }
    }
}
