package com.focuslauncher.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.focuslauncher.core.database.entity.WidgetItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WidgetDao {

    @Query("SELECT * FROM home_screen_widgets WHERE profileId = :profileId")
    fun getWidgetsForProfileFlow(profileId: Long): Flow<List<WidgetItemEntity>>

    @Query("SELECT * FROM home_screen_widgets WHERE profileId = :profileId")
    suspend fun getWidgetsForProfile(profileId: Long): List<WidgetItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWidget(widget: WidgetItemEntity): Long

    @Query("DELETE FROM home_screen_widgets WHERE id = :widgetId")
    suspend fun deleteWidgetById(widgetId: Long)

    @Query("DELETE FROM home_screen_widgets WHERE appWidgetId = :appWidgetId")
    suspend fun deleteByAppWidgetId(appWidgetId: Int)

    @Query("DELETE FROM home_screen_widgets WHERE profileId = :profileId")
    suspend fun clearWidgetsForProfile(profileId: Long)

    @Query("UPDATE home_screen_widgets SET cellX = :cellX, cellY = :cellY WHERE id = :id")
    suspend fun updatePosition(id: Long, cellX: Int, cellY: Int)
}
