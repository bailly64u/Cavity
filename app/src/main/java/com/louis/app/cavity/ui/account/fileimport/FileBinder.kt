package com.louis.app.cavity.ui.account.fileimport

import android.app.Application
import android.net.Uri

interface FileBinder {
    @Throws(NumberFormatException::class)
    suspend fun bind(app: Application, uri: Uri)

    @Throws(NumberFormatException::class)
    fun getBindedObjectId(name: String): Long {
        val end = name.split("-").last().split(".").first()

        return if (end.startsWith("f")) {
            end.substring(1).toLong()
        } else {
            end.toLong()
        }
    }
}
