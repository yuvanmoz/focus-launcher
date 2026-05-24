package com.focuslauncher.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "profile_apps",
    primaryKeys = ["profileId", "packageName", "activityName"],
    foreignKeys = [
        ForeignKey(
            entity = FocusProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["profileId"])]
)
data class ProfileAppEntity(
    val profileId: Long,
    val packageName: String,
    val activityName: String
)
