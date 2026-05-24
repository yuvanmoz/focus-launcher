package com.focuslauncher.core.model

data class AppMetadata(
    val packageName: String,
    val activityName: String,
    val label: String
) {
    // Unique ID for the launcher app shortcut
    val uniqueId: String get() = "$packageName/$activityName"
}

data class BlockedAppInfo(
    val packageName: String,
    val appName: String,
    val profileName: String
)
