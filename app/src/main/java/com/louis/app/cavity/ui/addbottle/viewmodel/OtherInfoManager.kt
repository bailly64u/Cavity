package com.louis.app.cavity.ui.addbottle.viewmodel

import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.MutableLiveData
import com.louis.app.cavity.R
import com.louis.app.cavity.db.WineRepository
import com.louis.app.cavity.model.Bottle
import com.louis.app.cavity.model.Friend
import com.louis.app.cavity.util.Event
import com.louis.app.cavity.util.postOnce
import com.louis.app.cavity.util.toInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

class OtherInfoManager(
    private val viewModelScope: CoroutineScope,
    private val repository: WineRepository,
    editedBottle: Bottle?,
    private val _userFeedback: MutableLiveData<Event<Int>>
) {
    private var pdfPath: String = ""

    val hasPdf: Boolean
        get() = pdfPath.isNotBlank()

    var partialBottle: Step4Bottle? = null

    init {
        editedBottle?.let {
            setPdfPath(it.pdfPath)
        }
    }

    fun setPdfPath(path: String) {
        pdfPath = path
    }

    fun submitOtherInfo(otherInfo: String, addToFavorite: Boolean, friendId: Long?) {
        partialBottle = Step4Bottle(otherInfo, addToFavorite.toInt(), pdfPath, friendId)
    }

    fun getAllFriends() = repository.getAllFriends()

    fun insertFriend(nameLastName: String) {
        viewModelScope.launch(IO) {
            try {
                repository.insertFriend(Friend(0, nameLastName, ""))
                _userFeedback.postOnce(R.string.friend_added)
            } catch (e: IllegalArgumentException) {
                _userFeedback.postOnce(R.string.input_error)
            } catch (e: SQLiteConstraintException) {
                _userFeedback.postOnce(R.string.friend_already_exists)
            }
        }
    }

    data class Step4Bottle(
        val otherInfo: String,
        val isFavorite: Int,
        val pdfPath: String,
        val giftedBy: Long?
    )
}