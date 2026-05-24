package com.focuslauncher.core.model

data class HomeScreenItem(
    val id: Long = 0L,
    val profileId: Long,
    val packageName: String,
    val activityName: String,
    val cellX: Int,
    val cellY: Int,
    val screenIndex: Int = 0
)
