package com.focuslauncher

import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import com.focuslauncher.core.model.BlockedAppInfo
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

class FocusBlockerService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitorJob: Job? = null
    private fun getSystemWhitelist(context: Context): Set<String> {
        val packages = mutableSetOf(
            "com.focuslauncher",
            "com.android.settings",
            "android",
            "com.android.systemui",
            "com.sec.android.app.launcher",
            "com.google.android.providers.media.module",
            "com.android.providers.media.module",
            "com.google.android.providers.media",
            "com.android.providers.media",
            "com.android.documentsui",
            "com.google.android.documentsui",
            "com.google.android.apps.photos",
            "com.google.android.apps.photosgo",
            "com.sec.android.gallery3d",
            "com.android.gallery3d",
            "com.android.gallery",
            "com.samsung.android.gallery.app",
            // System Photo Picker & Wallpapers
            "com.google.android.photopicker",
            "com.android.photopicker",
            "com.google.android.apps.wallpaper",
            "com.android.wallpaper",
            "com.google.android.apps.wallpaper.picker",
            "com.android.wallpaper.picker",
            "com.android.settings.intelligence",
            // OEM Galleries
            "com.miui.gallery",
            "com.huawei.photos",
            "com.coloros.gallery3d",
            "com.oneplus.gallery",
            "com.vivo.gallery",
            "com.oppo.gallery3d",
            // System Permission Controllers & Package Installers
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
            "com.samsung.android.permissioncontroller",
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
            // Telephony & Call handling (In-Call UI, Telecom, etc.)
            "com.android.phone",
            "com.android.server.telecom",
            "com.android.incallui",
            "com.google.android.dialer",
            "com.samsung.android.incallui",
            "com.samsung.android.dialer",
            "com.sec.android.app.dialer",
            "com.samsung.android.app.telephonyui",
            "com.android.providers.telephony",
            "com.google.android.ims",
            "com.samsung.ims.android",
            "com.oneplus.dialer",
            "com.oppo.dialer",
            "com.coloros.phonemanager",
            "com.vivo.dialer",
            "com.xiaomi.phone",
            // Contacts subfunctions & sync
            "com.samsung.android.contacts",
            "com.google.android.contacts",
            "com.android.contacts",
            "com.android.providers.contacts",
            "com.oneplus.contacts",
            "com.oppo.contacts",
            "com.vivo.contacts",
            // SMS & Messaging subfunctions
            "com.samsung.android.messaging",
            "com.google.android.apps.messaging",
            "com.android.mms",
            "com.android.messaging",
            // Calendar subfunctions & sync
            "com.samsung.android.calendar",
            "com.google.android.calendar",
            "com.android.calendar",
            "com.android.providers.calendar"
        )
        
        val pm = context.packageManager
        
        // Keyboards (Input Methods): Dynamically whitelist all active/enabled keyboards
        try {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
            imm?.enabledInputMethodList?.forEach { info ->
                packages.add(info.packageName)
            }
        } catch (e: Exception) {}
        
        // Dialer (Phone) - Dock App 1
        try {
            val dialIntent = Intent(Intent.ACTION_DIAL)
            val info = pm.resolveActivity(dialIntent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            info?.activityInfo?.packageName?.let { packages.add(it) }
        } catch (e: Exception) {}

        // SMS (Messages) - Dock App 2
        try {
            val smsIntent = Intent(Intent.ACTION_SENDTO, android.net.Uri.parse("smsto:"))
            val info = pm.resolveActivity(smsIntent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            info?.activityInfo?.packageName?.let { packages.add(it) }
        } catch (e: Exception) {}

        // Calendar - Dock App 3
        try {
            val calIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_CALENDAR) }
            val info = pm.resolveActivity(calIntent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            info?.activityInfo?.packageName?.let { packages.add(it) }
        } catch (e: Exception) {}

        // Contacts - Dock App 4
        try {
            val contactsIntent = Intent(Intent.ACTION_VIEW, android.provider.ContactsContract.Contacts.CONTENT_URI)
            val info = pm.resolveActivity(contactsIntent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            info?.activityInfo?.packageName?.let { packages.add(it) }
        } catch (e: Exception) {}

        // Dynamically whitelist all system packages related to phone, calls, contacts, messaging, calendar
        try {
            val systemApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (appInfo in systemApps) {
                val isSystem = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0 ||
                               (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                if (isSystem) {
                    val pkgName = appInfo.packageName
                    if (pkgName.contains("dialer", ignoreCase = true) ||
                        pkgName.contains("phone", ignoreCase = true) ||
                        pkgName.contains("telephony", ignoreCase = true) ||
                        pkgName.contains("incallui", ignoreCase = true) ||
                        pkgName.contains("contacts", ignoreCase = true) ||
                        pkgName.contains("messaging", ignoreCase = true) ||
                        pkgName.contains("mms", ignoreCase = true) ||
                        pkgName.contains("calendar", ignoreCase = true) ||
                        pkgName.contains("ims", ignoreCase = true)
                    ) {
                        packages.add(pkgName)
                    }
                }
            }
        } catch (e: Exception) {}
        
        return packages
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startMonitoring()
        return START_STICKY
    }

    override fun onDestroy() {
        stopMonitoring()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startMonitoring() {
        if (monitorJob != null) return
        monitorJob = serviceScope.launch {
            while (isActive) {
                try {
                    checkForegroundApp()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(250) // Poll every 250ms for low latency blocking
            }
        }
    }

    private fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
    }

    private fun isFocusLauncherDefault(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val defaultLauncherPackage = resolveInfo?.activityInfo?.packageName
        return defaultLauncherPackage == context.packageName
    }

    private suspend fun checkForegroundApp() {
        val app = applicationContext as FocusApplication
        val profileRepo = app.profileRepository
        val activeProfileId = profileRepo.getActiveProfileId()

        // If in No Mode (-99L), we should stop blocking and stop the service
        if (activeProfileId == -99L) {
            withContext(Dispatchers.Main) {
                stopSelf()
            }
            return
        }

        // If Focus Launcher is no longer the default launcher, reset profile to No Mode and stop service.
        // We bypass this check during a 30 seconds grace period to allow the user to select Focus Launcher as default.
        val sharedPrefs = getSharedPreferences("focus_launcher_prefs", Context.MODE_PRIVATE)
        val lastChange = sharedPrefs.getLong("last_profile_change_time", 0)
        val isGracePeriod = (System.currentTimeMillis() - lastChange) < 30000

        if (!isFocusLauncherDefault(this) && !isGracePeriod) {
            profileRepo.setActiveProfile(-99L)
            withContext(Dispatchers.Main) {
                stopSelf()
            }
            return
        }

        val activeProfile = profileRepo.getProfileById(activeProfileId) ?: return
        
        // Fetch allowed apps for the active profile
        val allowedApps = profileRepo.getAllowedApps(activeProfileId)
        val allowedPackages = allowedApps.map { it.substringBefore('/') }.toSet()

        val foregroundPackage = getForegroundPackage() ?: return

        // Skip check if the package is in the system whitelist or explicitly allowed in profile
        val whitelist = getSystemWhitelist(this)
        if (whitelist.contains(foregroundPackage) || allowedPackages.contains(foregroundPackage)) {
            return
        }

        // If we reach here, the app is not allowed. We block it!
        blockApp(foregroundPackage, activeProfile.name)
    }

    private fun blockApp(packageName: String, profileName: String) {
        val app = applicationContext as FocusApplication
        val profileRepo = app.profileRepository

        // 1. Pull user back to home screen
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)

        // 2. Set the blocked app details in the repository to trigger the UI popup
        val appLabel = getAppName(packageName)
        val blockedInfo = BlockedAppInfo(
            packageName = packageName,
            appName = appLabel,
            profileName = profileName
        )
        
        profileRepo.setBlockedApp(blockedInfo)
    }

    private fun getForegroundPackage(): String? {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
        val endTime = System.currentTimeMillis()
        val startTime = endTime - TimeUnit.SECONDS.toMillis(10)
        
        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        var lastForegroundApp: String? = null
        
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                lastForegroundApp = event.packageName
            }
        }
        
        // Fallback to queryUsageStats if no events were found in the last 10 seconds
        if (lastForegroundApp == null) {
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY, 
                endTime - TimeUnit.MINUTES.toMillis(1), 
                endTime
            )
            if (!stats.isNullOrEmpty()) {
                val sortedStats = stats.sortedByDescending { it.lastTimeUsed }
                lastForegroundApp = sortedStats.firstOrNull()?.packageName
            }
        }
        
        return lastForegroundApp
    }

    private fun getAppName(packageName: String): String {
        return try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }
}
