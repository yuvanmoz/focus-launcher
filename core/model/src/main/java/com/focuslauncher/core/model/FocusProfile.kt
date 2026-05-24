package com.focuslauncher.core.model

data class FocusProfile(
    val id: Long = 0L,
    val name: String,
    val wallpaperPath: String? = null,
    val isSystemWallpaper: Boolean = false,
    val gridRows: Int = 6,
    val gridCols: Int = 5,
    val themeMode: String = "SYSTEM"
)
