package com.louis.app.cavity.model.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.louis.app.cavity.model.Friend
import com.louis.app.cavity.model.Tasting

data class TastingWithPersons(
    @Embedded val tasting: Tasting,
    @Relation(
        parentColumn = "id",
        entityColumn = "tasting_id",
        associateBy = Junction(TastingPersonXRef::class)
    )
    val friends: List<Friend>
)
