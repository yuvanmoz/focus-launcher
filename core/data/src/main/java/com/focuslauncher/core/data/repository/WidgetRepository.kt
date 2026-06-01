package com.focuslauncher.core.data.repository

import com.focuslauncher.core.database.dao.WidgetDao
import com.focuslauncher.core.database.entity.WidgetItemEntity
import com.focuslauncher.core.model.WidgetItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface WidgetRepository {
    fun getWidgetsForProfileFlow(profileId: Long): Flow<List<WidgetItem>>
    suspend fun addWidget(widget: WidgetItem): Long
    suspend fun removeWidget(widgetId: Long)
    suspend fun removeByAppWidgetId(appWidgetId: Int)
    suspend fun updateWidgetPosition(widgetId: Long, cellX: Int, cellY: Int)
    suspend fun clearWidgetsForProfile(profileId: Long)
}

class WidgetRepositoryImpl(
    private val widgetDao: WidgetDao
) : WidgetRepository {

    override fun getWidgetsForProfileFlow(profileId: Long): Flow<List<WidgetItem>> {
        return widgetDao.getWidgetsForProfileFlow(profileId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun addWidget(widget: WidgetItem): Long = withContext(Dispatchers.IO) {
        widgetDao.insertWidget(WidgetItemEntity.fromDomain(widget))
    }

    override suspend fun removeWidget(widgetId: Long) = withContext(Dispatchers.IO) {
        widgetDao.deleteWidgetById(widgetId)
    }

    override suspend fun removeByAppWidgetId(appWidgetId: Int) = withContext(Dispatchers.IO) {
        widgetDao.deleteByAppWidgetId(appWidgetId)
    }

    override suspend fun updateWidgetPosition(widgetId: Long, cellX: Int, cellY: Int) = withContext(Dispatchers.IO) {
        widgetDao.updatePosition(widgetId, cellX, cellY)
    }

    override suspend fun clearWidgetsForProfile(profileId: Long) = withContext(Dispatchers.IO) {
        widgetDao.clearWidgetsForProfile(profileId)
    }
}
