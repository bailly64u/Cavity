package com.louis.app.cavity.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "grape_variety")
data class GrapeVariety (val name: String, val percentage: Int, val bottle_id: Long) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_grape_variety")
    var id_grape_variety: Long = 0
}