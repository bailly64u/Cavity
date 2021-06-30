package com.louis.app.cavity.ui.addbottle.viewmodel

import androidx.lifecycle.MutableLiveData
import com.louis.app.cavity.db.WineRepository
import com.louis.app.cavity.model.Bottle
import com.louis.app.cavity.util.Event
import com.louis.app.cavity.util.toInt
import kotlinx.coroutines.CoroutineScope

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

    data class Step4Bottle(
        val otherInfo: String,
        val isFavorite: Int,
        val pdfPath: String,
        val giftedBy: Long?
    )
}
