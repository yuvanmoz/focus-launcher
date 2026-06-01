package com.focuslauncher.core.model

data class WidgetItem(
    val id: Long = 0L,
    val profileId: Long,
    val appWidgetId: Int,
    val providerPackage: String,
    val providerClass: String,
    val cellX: Int,
    val cellY: Int,
    val spanX: Int,
    val spanY: Int
)
