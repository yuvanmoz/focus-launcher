package com.focuslauncher.core.data.repository

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.focuslauncher.core.model.AppMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

interface AppRepository {
    val installedAppsFlow: Flow<List<AppMetadata>>
    fun getAppIcon(packageName: String): Drawable?
    suspend fun refreshInstalledApps()
}

class AppRepositoryImpl(
    private val context: Context
) : AppRepository {

    private val packageManager = context.packageManager
    private val _installedAppsFlow = MutableStateFlow<List<List<AppMetadata>>>(emptyList()) // wrapped or direct list
    private val appsState = MutableStateFlow<List<AppMetadata>>(emptyList())
    
    override val installedAppsFlow: Flow<List<AppMetadata>> = appsState.asStateFlow()

    init {
        // Run initial load in background
        CoroutineScope(Dispatchers.IO).launch {
            refreshInstalledApps()
        }
        registerPackageReceiver()
    }

    override suspend fun refreshInstalledApps() {
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        
        val list = packageManager.queryIntentActivities(
            launcherIntent,
            PackageManager.MATCH_ALL
        ).map { resolveInfo ->
            AppMetadata(
                packageName = resolveInfo.activityInfo.packageName,
                activityName = resolveInfo.activityInfo.name,
                label = resolveInfo.loadLabel(packageManager).toString()
            )
        }.distinctBy { it.uniqueId }
        .sortedBy { it.label.lowercase() }
        
        appsState.value = list
    }

    override fun getAppIcon(packageName: String): Drawable? {
        return try {
            packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            null
        }
    }

    private fun registerPackageReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                CoroutineScope(Dispatchers.IO).launch {
                    refreshInstalledApps()
                }
            }
        }
        context.registerReceiver(receiver, filter)
    }
}
