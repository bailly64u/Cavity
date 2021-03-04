package com.louis.app.cavity.model.relation.history

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.louis.app.cavity.model.Bottle
import com.louis.app.cavity.model.Friend
import com.louis.app.cavity.model.HistoryEntry
import com.louis.app.cavity.model.Tasting
import com.louis.app.cavity.model.relation.bottle.BottleAndWine
import com.louis.app.cavity.model.relation.crossref.FriendHistoryEntryXRef
import com.louis.app.cavity.model.relation.tasting.TastingWithBottles

// TODO: Might need optimization
data class HistoryEntryWithBottleAndTastingAndFriends(
    @Embedded
    val historyEntry: HistoryEntry,
    @Relation(
        entity = Bottle::class,
        parentColumn = "bottle_id",
        entityColumn = "id"
    )
    val bottleAndWine: BottleAndWine,
    @Relation(
        entity = Tasting::class,
        parentColumn = "tasting_id",
        entityColumn = "id"
    )
    var tasting: TastingWithBottles?,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = FriendHistoryEntryXRef::class,
            parentColumn = "history_entry_id",
            entityColumn = "friend_id"
        )
    )
    val friends: List<Friend>
)