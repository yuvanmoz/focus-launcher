package com.focuslauncher.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.focuslauncher.core.database.entity.HomeScreenItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HomeScreenDao {

    @Query("SELECT * FROM home_screen_items WHERE profileId = :profileId")
    fun getItemsForProfileFlow(profileId: Long): Flow<List<HomeScreenItemEntity>>

    @Query("SELECT * FROM home_screen_items WHERE profileId = :profileId")
    suspend fun getItemsForProfile(profileId: Long): List<HomeScreenItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: HomeScreenItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<HomeScreenItemEntity>)

    @Delete
    suspend fun deleteItem(item: HomeScreenItemEntity)

    @Query("DELETE FROM home_screen_items WHERE profileId = :profileId")
    suspend fun clearItemsForProfile(profileId: Long)

    @Transaction
    suspend fun updateItemsForProfile(profileId: Long, items: List<HomeScreenItemEntity>) {
        clearItemsForProfile(profileId)
        if (items.isNotEmpty()) {
            insertItems(items)
        }
    }
}
