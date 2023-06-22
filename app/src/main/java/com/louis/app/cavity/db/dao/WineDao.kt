package com.louis.app.cavity.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.louis.app.cavity.model.Bottle
import com.louis.app.cavity.model.Wine
import com.louis.app.cavity.model.WineColor

@Dao
interface WineDao {
    @Insert
    suspend fun insertWine(wine: Wine)

    @Insert
    suspend fun insertWines(wine: List<Wine>)

    @Update
    suspend fun updateWine(wine: Wine)

    @Delete
    suspend fun deleteWine(wine: Wine)

    @Query("UPDATE wine SET hidden = 1 WHERE id =:wineId")
    suspend fun hideWineById(wineId: Long)

    @Query("DELETE FROM wine WHERE id =:wineId")
    suspend fun deleteWineById(wineId: Long)

    @Query("SELECT * FROM wine WHERE id =:wineId")
    fun getWineById(wineId: Long): LiveData<Wine>

    @Query("SELECT DISTINCT naming FROM wine WHERE county_id =:countyId ORDER BY naming")
    fun getNamingsForCounty(countyId: Long): LiveData<List<String>>

    @Query("SELECT * FROM wine WHERE color =:color AND is_organic =:isOrganic AND cuvee =:cuvee")
    suspend fun getWineByAttributes(color: WineColor, isOrganic: Int, cuvee: String): List<Wine>

    @Transaction
    @Query("SELECT * FROM wine WHERE id =:wineId")
    suspend fun getWineByIdNotLive(wineId: Long): Wine

    @Query("SELECT * FROM wine")
    suspend fun getAllWinesNotLive(): List<Wine>

    @Transaction
    @Query("SELECT * FROM wine WHERE id =:wineId")
    suspend fun getWineFullNamingByIdNotLive(wineId: Long): Wine

    @Transaction
    @Query("SELECT * FROM wine WHERE county_id =:countyId AND hidden != 1 ORDER BY color, naming")
    fun getWineWithBottlesByCounty(countyId: Long): LiveData<List<WineWithBottles>>

    @Query("DELETE FROM wine")
    suspend fun deleteAll()
}

data class WineWithBottles(
    @Embedded val wine: Wine,
    @Relation(
        parentColumn = "id",
        entityColumn = "wine_id"
    )
    val bottles: List<Bottle>
)
