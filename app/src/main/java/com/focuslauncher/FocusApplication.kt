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
        
        if (profileId == -99L) {
            // "No Mode" (Return to Samsung One UI):
            
            // 1. Disable the HomeActivityAlias (removes Focus Launcher as a launcher option)
            pm.setComponentEnabledSetting(
                aliasComponent,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            
            // 2. Stop the background app blocker service
            stopService(Intent(this, FocusBlockerService::class.java))
            
            // 3. Force go to Home (reverts to system/Samsung launcher)
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
            
        } else {
            // A Focus Profile is selected (e.g. Work, Trading, etc.):
            
            // 1. Enable the HomeActivityAlias
            pm.setComponentEnabledSetting(
                aliasComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            
            // 2. Start the blocker service
            startService(Intent(this, FocusBlockerService::class.java))
            
        }
    }
}
