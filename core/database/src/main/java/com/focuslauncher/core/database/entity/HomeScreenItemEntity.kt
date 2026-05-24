package com.focuslauncher.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.focuslauncher.core.model.HomeScreenItem

@Entity(
    tableName = "home_screen_items",
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
data class HomeScreenItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val profileId: Long,
    val packageName: String,
    val activityName: String,
    val cellX: Int,
    val cellY: Int,
    val screenIndex: Int
) {
    fun toDomain() = HomeScreenItem(
        id = id,
        profileId = profileId,
        packageName = packageName,
        activityName = activityName,
        cellX = cellX,
        cellY = cellY,
        screenIndex = screenIndex
    )

    companion object {
        fun fromDomain(domain: HomeScreenItem) = HomeScreenItemEntity(
            id = domain.id,
            profileId = domain.profileId,
            packageName = domain.packageName,
            activityName = domain.activityName,
            cellX = domain.cellX,
            cellY = domain.cellY,
            screenIndex = domain.screenIndex
        )
    }
}
