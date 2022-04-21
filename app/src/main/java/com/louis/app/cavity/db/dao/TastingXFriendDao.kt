package com.louis.app.cavity.db.dao

import androidx.room.*
import com.louis.app.cavity.model.TastingXFriend

@Dao
interface TastingXFriendDao {
    @Insert
    suspend fun insertTastingXFriend(tasting: TastingXFriend)

    @Update
    suspend fun updateTastingXFriend(tasting: TastingXFriend)

    @Delete
    suspend fun deleteTastingXFriend(tasting: TastingXFriend)

    @Query("SELECT * FROM tasting_friend_xref")
    suspend fun getAllTastingXFriendsNotLive(): List<TastingXFriend>
}
