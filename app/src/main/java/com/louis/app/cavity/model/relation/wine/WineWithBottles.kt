package com.louis.app.cavity.model.relation.wine

import androidx.room.Embedded
import androidx.room.Relation
import com.louis.app.cavity.model.Bottle
import com.louis.app.cavity.model.Wine

data class WineWithBottles(
    @Embedded val wine: Wine,
    @Relation(
        parentColumn = "id",
        entityColumn = "wine_id"
    )
    val bottles: List<Bottle>
)