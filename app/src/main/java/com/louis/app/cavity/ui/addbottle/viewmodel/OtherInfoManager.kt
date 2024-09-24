package com.louis.app.cavity.ui.addbottle.viewmodel

import androidx.annotation.IdRes
import com.louis.app.cavity.R
import com.louis.app.cavity.domain.repository.FriendRepository
import com.louis.app.cavity.model.Bottle
import com.louis.app.cavity.model.BottleSize
import com.louis.app.cavity.util.toInt

class OtherInfoManager(
    private val repository: FriendRepository,
    editedBottle: Bottle?
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

    fun submitOtherInfo(
        otherInfo: String,
        @IdRes checkedSize: Int,
        addToFavorite: Boolean,
        friendId: Long?
    ) {
        val size = when (checkedSize) {
            R.id.rbSlim -> BottleSize.SLIM
            R.id.rbSmall -> BottleSize.SMALL
            R.id.rbNormal -> BottleSize.NORMAL
            else /* R.id.rbMagnum */ -> BottleSize.MAGNUM
        }

        partialBottle = Step4Bottle(otherInfo, size, addToFavorite.toInt(), pdfPath, friendId)
    }

    fun getAllFriends() = repository.getAllFriends()

    data class Step4Bottle(
        val otherInfo: String,
        val size: BottleSize,
        val isFavorite: Int,
        val pdfPath: String,
        val giftedBy: Long?
    )
}
