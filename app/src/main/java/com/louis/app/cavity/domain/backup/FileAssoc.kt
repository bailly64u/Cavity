package com.louis.app.cavity.domain.backup

interface FileAssoc {
    fun getFilePath(): String
    fun getExternalFilename(): String
}
