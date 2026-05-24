package com.focuslauncher.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.focuslauncher.core.model.FocusProfile

@Entity(tableName = "focus_profiles")
data class FocusProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val wallpaperPath: String?,
    val isSystemWallpaper: Boolean,
    val gridRows: Int,
    val gridCols: Int,
    val themeMode: String
) {
    fun toDomain() = FocusProfile(
        id = id,
        name = name,
        wallpaperPath = wallpaperPath,
        isSystemWallpaper = isSystemWallpaper,
        gridRows = gridRows,
        gridCols = gridCols,
        themeMode = themeMode
    )

    companion object {
        fun fromDomain(domain: FocusProfile) = FocusProfileEntity(
            id = domain.id,
            name = domain.name,
            wallpaperPath = domain.wallpaperPath,
            isSystemWallpaper = domain.isSystemWallpaper,
            gridRows = domain.gridRows,
            gridCols = domain.gridCols,
            themeMode = domain.themeMode
        )
    }
}
