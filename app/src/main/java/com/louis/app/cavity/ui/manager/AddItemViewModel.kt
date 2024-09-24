package com.louis.app.cavity.ui.manager

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.louis.app.cavity.R
import com.louis.app.cavity.domain.repository.CountyRepository
import com.louis.app.cavity.domain.repository.FriendRepository
import com.louis.app.cavity.domain.repository.GrapeRepository
import com.louis.app.cavity.domain.repository.ReviewRepository
import com.louis.app.cavity.model.County
import com.louis.app.cavity.model.Friend
import com.louis.app.cavity.model.Grape
import com.louis.app.cavity.model.Review
import com.louis.app.cavity.util.Event
import com.louis.app.cavity.util.postOnce
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * This ViewModel is used to insert simple items, since we can do that from multiple screens across
 * the app.
 */

// TODO: refactor wine repository by splitting it. Also, this logic (exception handlingd and unicity check should be on repository
class AddItemViewModel(app: Application) : AndroidViewModel(app) {
    private val countyRepository = CountyRepository.getInstance(app)
    private val grapeRepository = GrapeRepository.getInstance(app)
    private val reviewRepository = ReviewRepository.getInstance(app)
    private val friendRepository = FriendRepository.getInstance(app)

    private val _userFeedback = MutableLiveData<Event<Int>>()
    val userFeedback: LiveData<Event<Int>>
        get() = _userFeedback

    fun insertCounty(countyName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val counties = countyRepository.getAllCountiesNotLive()

            if (checkCountyAlredyExists(counties, countyName)) {
                _userFeedback.postOnce(R.string.county_already_exists)
                return@launch
            }

            try {
                countyRepository.insertCounty(County(name = countyName, prefOrder = counties.size))
                _userFeedback.postOnce(R.string.county_added)
            } catch (e: IllegalArgumentException) {
                _userFeedback.postOnce(R.string.empty_county_name)
            } catch (e: SQLiteConstraintException) {
                _userFeedback.postOnce(R.string.county_already_exists)
            }
        }
    }

    fun insertGrape(grapeName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                grapeRepository.insertGrape(Grape(0, grapeName))
                _userFeedback.postOnce(R.string.grape_added)
            } catch (e: IllegalArgumentException) {
                _userFeedback.postOnce(R.string.empty_grape_name)
            } catch (e: SQLiteConstraintException) {
                _userFeedback.postOnce(R.string.grape_already_exists)
            }
        }
    }

    fun insertReview(contestName: String, type: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                reviewRepository.insertReview(Review(0, contestName, type))
                _userFeedback.postOnce(R.string.review_added)
            } catch (e: IllegalArgumentException) {
                _userFeedback.postOnce(R.string.empty_contest_name)
            } catch (e: SQLiteConstraintException) {
                _userFeedback.postOnce(R.string.contest_name_already_exists)
            }
        }
    }

    fun insertFriend(nameLastName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                friendRepository.insertFriend(Friend(0, nameLastName, ""))
                _userFeedback.postOnce(R.string.friend_added)
            } catch (e: IllegalArgumentException) {
                _userFeedback.postOnce(R.string.input_error)
            } catch (e: SQLiteConstraintException) {
                _userFeedback.postOnce(R.string.friend_already_exists)
            }
        }
    }

    private fun checkCountyAlredyExists(counties: List<County>, countyName: String): Boolean {
        val names = counties.map { it.name.lowercase() }
        return countyName.lowercase() in names
    }
}
