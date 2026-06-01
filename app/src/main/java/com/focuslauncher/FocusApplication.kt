package com.focuslauncher

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.app.role.RoleManager
import android.os.Build
import com.focuslauncher.core.data.repository.AppRepository
import com.focuslauncher.core.data.repository.AppRepositoryImpl
import com.focuslauncher.core.data.repository.ProfileRepository
import com.focuslauncher.core.data.repository.ProfileRepositoryImpl
import com.focuslauncher.core.data.repository.WidgetRepository
import com.focuslauncher.core.data.repository.WidgetRepositoryImpl
import com.focuslauncher.core.database.FocusDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class FocusApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Database and repositories as lazy singletons (Service Locator pattern)
    val database by lazy { FocusDatabase.getInstance(this) }
    
    val appRepository: AppRepository by lazy {
        AppRepositoryImpl(this)
    }
    
    val profileRepository: ProfileRepository by lazy {
        ProfileRepositoryImpl(
            context = this,
            profileDao = database.profileDao(),
            homeScreenDao = database.homeScreenDao()
        )
    }

    val widgetRepository: WidgetRepository by lazy {
        WidgetRepositoryImpl(database.widgetDao())
    }

    override fun onCreate() {
        super.onCreate()
        
        // Ensure Notification Listener is enabled in registry to prevent permission resets
        val pm = packageManager
        val notificationServiceComponent = ComponentName(this, FocusNotificationListenerService::class.java)
        pm.setComponentEnabledSetting(
            notificationServiceComponent,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
        
        // Listen to active profile changes to manage the launcher status dynamically
        applicationScope.launch {
            profileRepository.activeProfileIdFlow.collectLatest { profileId ->
                handleProfileChange(profileId)
            }
        }
    }

    private fun handleProfileChange(profileId: Long) {
        val pm = packageManager
        val aliasComponent = ComponentName(this, "com.focuslauncher.HomeActivityAlias")
        
        val isDefault = isDefaultLauncher()
        val shouldBeEnabled = !isDefault || (profileId != -99L)
        
        val currentState = pm.getComponentEnabledSetting(aliasComponent)
        if (shouldBeEnabled) {
            if (currentState != PackageManager.COMPONENT_ENABLED_STATE_ENABLED && 
                currentState != PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {
                pm.setComponentEnabledSetting(
                    aliasComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
        } else {
            if (currentState != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                pm.setComponentEnabledSetting(
                    aliasComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
        }

        if (profileId == -99L) {
            // "No Mode" (Return to Samsung One UI):
            
            // Stop the background app blocker service
            stopService(Intent(this, FocusBlockerService::class.java))
            
            // Force go to Home (reverts to system/Samsung launcher) ONLY if we are currently default launcher
            if (isDefaultLauncher()) {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                try {
                    startActivity(homeIntent)
                } catch (e: Exception) {
                    // Ignore if activity cannot be started (e.g. during early boot)
                }
            }
            
        } else {
            // A Focus Profile is selected (e.g. Work, Trading, etc.):
            
            // Start the blocker service
            startService(Intent(this, FocusBlockerService::class.java))
        }
    }

    private fun isDefaultLauncher(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val currentDefaultPackage = resolveInfo?.activityInfo?.packageName
        return currentDefaultPackage == packageName
    }
}
