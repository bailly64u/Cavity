package com.louis.app.cavity.ui.tasting

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.louis.app.cavity.domain.repository.FriendRepository
import com.louis.app.cavity.domain.repository.TastingRepository
import com.louis.app.cavity.util.Event

class TastingViewModel(app: Application) : AndroidViewModel(app) {
    private val tastingRepository = TastingRepository.getInstance(app)
    private val friendRepository = FriendRepository.getInstance(app)

    private val _userFeedback = MutableLiveData<Event<Int>>()
    val userFeedback: LiveData<Event<Int>>
        get() = _userFeedback

    val undoneTastings = tastingRepository.getUndoneTastings()
    val friends = friendRepository.getAllFriends()
}
