package com.louis.app.cavity.ui.account.fileimport

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.louis.app.cavity.domain.error.SentryErrorReporter
import com.louis.app.cavity.domain.fileimport.BottleBinder
import com.louis.app.cavity.domain.fileimport.FileBinder
import com.louis.app.cavity.domain.fileimport.FriendBinder
import com.louis.app.cavity.domain.fileimport.WineBinder
import com.louis.app.cavity.util.Event
import com.louis.app.cavity.util.postOnce
import kotlinx.coroutines.launch
import java.io.File

class FileImportViewModel(private val app: Application) : AndroidViewModel(app) {
    private val errorReporter = SentryErrorReporter.getInstance(app)

    private val _fileImportedEvent = MutableLiveData<Event<Pair<Int, Int>>>()
    val fileImportedEvent: LiveData<Event<Pair<Int, Int>>>
        get() = _fileImportedEvent

    fun bindFiles(uris: List<Uri>, contentResolver: ContentResolver) {
        val total = uris.size
        var binded = 0

        viewModelScope.launch {
            for (uri in uris) {
                val binder = binderFactory(uri, contentResolver)

                if (binder != null) {
                    try {
                        // Might throw NPE even if kotlin thinks differently
                        binder.bind(app, uri)
                        binded++
                    } catch (e: NullPointerException) {
                        errorReporter.captureMessage(
                            "File import: NPE when retieving id from filename"
                        )
                    } catch (e: NumberFormatException) {
                        errorReporter.captureMessage(
                            "File import: NumberFormatException when retrieving id from filename"
                        )
                    }
                }
            }

            _fileImportedEvent.postOnce(binded to total)
        }
    }

    private fun binderFactory(uri: Uri, contentResolver: ContentResolver): FileBinder? {
        val filename = getFileName(uri, contentResolver)
        val split = filename?.split(".")

        // Weird file name. Don't bother.
        if (filename == null || split == null || split.size != 2) {
            return null
        }

        val extension = split.last()
        val isFriend = filename.matches(Regex(".*-f\\d*\\..*"))
        val name = filename.split(".").first()

        return when {
            extension == "pdf" -> BottleBinder(name)
            isFriend -> FriendBinder(name)
            else -> WineBinder(name)
        }
    }

    private fun getFileName(uri: Uri, contentResolver: ContentResolver): String? =
        when (uri.scheme) {
            ContentResolver.SCHEME_CONTENT -> getContentFileName(uri, contentResolver)
            else -> uri.path?.let(::File)?.name
        }

    private fun getContentFileName(uri: Uri, contentResolver: ContentResolver): String? =
        runCatching {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                cursor.moveToFirst()
                return@use cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
                    .let(cursor::getString)
            }
        }.getOrNull()
}
