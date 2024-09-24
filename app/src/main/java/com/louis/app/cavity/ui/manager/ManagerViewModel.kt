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
import com.louis.app.cavity.domain.repository.WineRepository
import com.louis.app.cavity.model.County
import com.louis.app.cavity.model.Friend
import com.louis.app.cavity.model.Grape
import com.louis.app.cavity.model.Review
import com.louis.app.cavity.util.Event
import com.louis.app.cavity.util.postOnce
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

class ManagerViewModel(app: Application) : AndroidViewModel(app) {
    private val wineRepository = WineRepository.getInstance(app)
    private val countyRepository = CountyRepository.getInstance(app)
    private val grapeRepository = GrapeRepository.getInstance(app)
    private val reviewRepository = ReviewRepository.getInstance(app)
    private val friendRepository = FriendRepository.getInstance(app)

    private val _userFeedback = MutableLiveData<Event<Int>>()
    val userFeedback: LiveData<Event<Int>>
        get() = _userFeedback

    var friendPickingImage: Friend? = null

    fun getCountiesWithWines() = wineRepository.getCountiesWithWines()

    fun getGrapeWithQuantifiedGrapes() = grapeRepository.getGrapeWithQuantifiedGrapes()

    fun getReviewWithFilledReviews() = reviewRepository.getReviewWithFilledReviews()

    fun getAllFriends() = friendRepository.getAllFriends()

    fun updateCounty(county: County) {
        viewModelScope.launch(IO) {
            try {
                countyRepository.updateCounty(county)
                _userFeedback.postOnce(R.string.county_renamed)
            } catch (e: IllegalArgumentException) {
                _userFeedback.postOnce(R.string.empty_county_name)
            } catch (e: SQLiteConstraintException) {
                _userFeedback.postOnce(R.string.county_already_exists)
            }
        }
    }

    fun updateCounties(counties: List<County>) {
        viewModelScope.launch(IO) {
            val currentCounties = countyRepository.getAllCountiesNotLive()

            // Don't trigger observers for nothing
            if (counties != currentCounties) {
                countyRepository.updateCounties(counties)
            }
        }
    }

    fun deleteCounty(countyId: Long) {
        viewModelScope.launch(IO) {
            countyRepository.deleteCounty(countyId)
            _userFeedback.postOnce(R.string.county_deleted)
        }
    }

    fun updateGrape(grape: Grape) {
        viewModelScope.launch(IO) {
            try {
                grapeRepository.updateGrape(grape)
                _userFeedback.postOnce(R.string.grape_renamed)
            } catch (e: IllegalArgumentException) {
                _userFeedback.postOnce(R.string.empty_grape_name)
            } catch (e: SQLiteConstraintException) {
                _userFeedback.postOnce(R.string.grape_already_exists)
            }
        }
    }

    fun deleteGrape(grape: Grape) {
        viewModelScope.launch(IO) {
            grapeRepository.deleteGrape(grape)
            _userFeedback.postOnce(R.string.grape_deleted)
        }
    }

    fun updateReview(review: Review) {
        viewModelScope.launch(IO) {
            try {
                reviewRepository.updateReview(review)
                _userFeedback.postOnce(R.string.review_renamed)
            } catch (e: IllegalArgumentException) {
                _userFeedback.postOnce(R.string.empty_contest_name)
            } catch (e: SQLiteConstraintException) {
                _userFeedback.postOnce(R.string.contest_name_already_exists)
            }
        }
    }

    fun deleteReview(review: Review) {
        viewModelScope.launch(IO) {
            reviewRepository.deleteReview(review)
            _userFeedback.postOnce(R.string.review_deleted)
        }
    }

    fun updateFriend(friend: Friend, newName: String) {
        viewModelScope.launch(IO) {
            try {
                val newFriend = friend.copy(name = newName)
                friendRepository.updateFriend(newFriend)
                _userFeedback.postOnce(R.string.friend_renamed)
            } catch (e: IllegalArgumentException) {
                _userFeedback.postOnce(R.string.input_error)
            } catch (e: SQLiteConstraintException) {
                _userFeedback.postOnce(R.string.friend_already_exists)
            }
        }
    }

    fun deleteFriend(friend: Friend) {
        viewModelScope.launch(IO) {
            friendRepository.deleteFriend(friend)
            _userFeedback.postOnce(R.string.friend_deleted)
        }
    }

    fun setImageForCurrentFriend(imagePath: String) {
        viewModelScope.launch(IO) {
            friendPickingImage?.copy(imgPath = imagePath)?.let {
                friendRepository.updateFriend(it)
            }

            friendPickingImage = null
        }
    }
}
