package com.louis.app.cavity.model

import androidx.room.*
import com.louis.app.cavity.R
import com.louis.app.cavity.domain.backup.FileAssoc
import java.util.*

@Entity(
    tableName = "bottle",
    foreignKeys = [ForeignKey(
        entity = Wine::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("wine_id"),
        onDelete = ForeignKey.CASCADE
    )]
)
data class Bottle(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "wine_id", index = true) val wineId: Long,
    val vintage: Int,
    val apogee: Int?,
    @ColumnInfo(name = "is_favorite") var isFavorite: Int,
    val price: Float,
    val currency: String,
    @ColumnInfo(name = "other_info") var otherInfo: String,
    @ColumnInfo(name = "buy_location") val buyLocation: String,
    @ColumnInfo(name = "buy_date") val buyDate: Long,
    @ColumnInfo(name = "tasting_taste_comment") val tastingTasteComment: String,
    @ColumnInfo(name = "bottle_size") val bottleSize: BottleSize,
    @ColumnInfo(name = "pdf_path") var pdfPath: String,
    var consumed: Int,
    @ColumnInfo(name = "tasting_id", index = true) var tastingId: Long? = null
) : Chipable, FileAssoc {

    @Ignore
    var isSelected: Boolean = false


    fun isReadyToDrink(): Boolean {
        val year = Calendar.getInstance().get(Calendar.YEAR)
        return if (apogee != null) year >= apogee else false
    }

    fun hasPdf() = pdfPath.isNotBlank()

    override fun getItemId() = id
    override fun getChipText() = vintage.toString()
    override fun getIcon() = R.drawable.ic_glass

    override fun getFilePath() = pdfPath
    override fun getExternalFilename() = "${this.vintage}-${this.id}"
}
