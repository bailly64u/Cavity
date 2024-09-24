package com.louis.app.cavity.ui.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import com.louis.app.cavity.domain.repository.StatsRepository

class StatsDetailsViewModel(app: Application) : AndroidViewModel(app) {
    val repository = StatsRepository.getInstance(app)

    private val bottlesIds = MutableLiveData<List<Long>>()

    val bottles = bottlesIds.switchMap {
        repository.getBottlesByIds(it)
    }

    fun setBottlesIds(ids: List<Long>) {
        bottlesIds.value = ids
    }
}
