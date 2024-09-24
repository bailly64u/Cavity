package com.louis.app.cavity.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.louis.app.cavity.domain.backup.FileAssoc

@Entity(
    tableName = "wine",
    foreignKeys = [ForeignKey(
        entity = County::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("county_id"),
        onDelete = ForeignKey.CASCADE
    )]
)
data class Wine(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val naming: String,
    val color: WineColor,
    val cuvee: String,
    @ColumnInfo(name = "is_organic") val isOrganic: Int,
    @ColumnInfo(name = "img_path") val imgPath: String,
    @ColumnInfo(name = "county_id", index = true) val countyId: Long,
    val hidden: Int = 0
) : FileAssoc {
    override fun getFilePath() = imgPath
    override fun getExternalFilename() = "${this.name.replace(" ", "-")}-${this.id}"
}
