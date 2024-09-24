package com.louis.app.cavity.domain.backup

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.louis.app.cavity.domain.error.SentryErrorReporter
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.InputStream

class FileProcessor(private val context: Context, fileAssoc: FileAssoc) {
    private val uri = Uri.parse(fileAssoc.getFilePath())
    private val externalFilename = fileAssoc.getExternalFilename()
    private val externalPath = context.getExternalFilesDir(null)!!.path
    private val outputFile = File("$externalPath/${externalFilename}.$extension")
    private val errorReporter = SentryErrorReporter.getInstance(context)

    private val extension: String?
        get() = MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(context.contentResolver.getType(uri))
            ?: uri.path?.substringAfterLast(".", "")

    private val inputStream: InputStream?
        get() = try {
            context.contentResolver.openInputStream(uri)
        } catch (e: FileNotFoundException) {
            null
        } catch (e: SecurityException) {
            errorReporter.captureMessage("SecurityException for file $outputFile")
            null
        }

    fun copyToExternalDir() {
        val inputFileExists = inputStream != null

        // Most likely: the file doesn't exists or doesn't exists anymore
        if (extension == null || extension?.isBlank() == true || !inputFileExists) {
            return
        }

        try {
            if (!outputFile.exists()) {
                outputFile.createNewFile()
            }

            FileOutputStream(outputFile, false).use {
                inputStream.use { inputStream ->
                    inputStream?.copyTo(it)
                }
            }
        } catch (e: Exception) {
            // Do nothing
        }
    }
}
