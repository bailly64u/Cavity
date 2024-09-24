package com.louis.app.cavity.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.louis.app.cavity.domain.backup.FileAssoc

@Entity(tableName = "friend", indices = [Index(value = ["name"], unique = true)])
data class Friend(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "img_path") val imgPath: String,
) : Chipable, FileAssoc {
    override fun getItemId() = id
    override fun getChipText() = name

    override fun getFilePath() = imgPath
    override fun getExternalFilename() = "${this.name}-f${this.id}"
}
