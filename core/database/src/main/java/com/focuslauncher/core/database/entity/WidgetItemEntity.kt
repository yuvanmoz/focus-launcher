package com.focuslauncher.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.focuslauncher.core.model.WidgetItem

@Entity(
    tableName = "home_screen_widgets",
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
data class WidgetItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val profileId: Long,
    val appWidgetId: Int,
    val providerPackage: String,
    val providerClass: String,
    val cellX: Int,
    val cellY: Int,
    val spanX: Int,
    val spanY: Int
) {
    fun toDomain() = WidgetItem(
        id = id,
        profileId = profileId,
        appWidgetId = appWidgetId,
        providerPackage = providerPackage,
        providerClass = providerClass,
        cellX = cellX,
        cellY = cellY,
        spanX = spanX,
        spanY = spanY
    )

    companion object {
        fun fromDomain(domain: WidgetItem) = WidgetItemEntity(
            id = domain.id,
            profileId = domain.profileId,
            appWidgetId = domain.appWidgetId,
            providerPackage = domain.providerPackage,
            providerClass = domain.providerClass,
            cellX = domain.cellX,
            cellY = domain.cellY,
            spanX = domain.spanX,
            spanY = domain.spanY
        )
    }
}
