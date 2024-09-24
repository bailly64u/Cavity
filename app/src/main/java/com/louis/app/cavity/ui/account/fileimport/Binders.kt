package com.louis.app.cavity.ui.account.fileimport

import android.app.Application
import android.net.Uri
import com.louis.app.cavity.domain.repository.BottleRepository
import com.louis.app.cavity.domain.repository.FriendRepository
import com.louis.app.cavity.domain.repository.WineRepository

class WineBinder(private val name: String) : FileBinder {
    override suspend fun bind(app: Application, uri: Uri) {
        val id = getBindedObjectId(name)
        val wineRepository = WineRepository.getInstance(app)
        val wine = wineRepository.getWineByIdNotLive(id)
        wineRepository.updateWine(wine.copy(imgPath = uri.toString()))
    }
}

class FriendBinder(private val name: String) : FileBinder {
    override suspend fun bind(app: Application, uri: Uri) {
        val id = getBindedObjectId(name)
        val friendRepository = FriendRepository.getInstance(app)
        val friend = friendRepository.getFriendByIdNotLive(id)
        friendRepository.updateFriend(friend.copy(imgPath = uri.toString()))
    }
}

class BottleBinder(private val name: String) : FileBinder {
    override suspend fun bind(app: Application, uri: Uri) {
        val id = getBindedObjectId(name)
        val bottleRepository = BottleRepository.getInstance(app)
        val bottle = bottleRepository.getBottleByIdNotLive(id)
        bottleRepository.updateBottle(bottle.copy(pdfPath = uri.toString()))
    }
}
