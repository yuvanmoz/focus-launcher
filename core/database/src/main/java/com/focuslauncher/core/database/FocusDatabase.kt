package com.focuslauncher.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.focuslauncher.core.database.dao.HomeScreenDao
import com.focuslauncher.core.database.dao.ProfileDao
import com.focuslauncher.core.database.dao.WidgetDao
import com.focuslauncher.core.database.entity.FocusProfileEntity
import com.focuslauncher.core.database.entity.HomeScreenItemEntity
import com.focuslauncher.core.database.entity.ProfileAppEntity
import com.focuslauncher.core.database.entity.WidgetItemEntity

@Database(
    entities = [
        FocusProfileEntity::class,
        ProfileAppEntity::class,
        HomeScreenItemEntity::class,
        WidgetItemEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class FocusDatabase : RoomDatabase() {

    abstract fun profileDao(): ProfileDao
    abstract fun homeScreenDao(): HomeScreenDao
    abstract fun widgetDao(): WidgetDao

    companion object {
        @Volatile
        private var INSTANCE: FocusDatabase? = null

        fun getInstance(context: Context): FocusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FocusDatabase::class.java,
                    "focus_launcher.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
