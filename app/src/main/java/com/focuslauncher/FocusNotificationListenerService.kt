package com.focuslauncher

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.focuslauncher.core.data.repository.ProfileRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.util.concurrent.ConcurrentLinkedQueue

class FocusNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val saveLock = Any()

    private fun saveSuppressedNotifications(list: List<SuppressedNotification>) {
        try {
            val array = org.json.JSONArray()
            list.forEach { item ->
                val obj = org.json.JSONObject().apply {
                    put("packageName", item.packageName)
                    put("appName", item.appName)
                    put("title", item.title ?: "")
                    put("text", item.text ?: "")
                    put("whenMs", item.whenMs)
                }
                array.put(obj)
            }
            val sharedPrefs = getSharedPreferences("suppressed_notifications_prefs", Context.MODE_PRIVATE)
            sharedPrefs.edit().putString("suppressed_list", array.toString()).commit()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun loadSuppressedNotifications(): List<SuppressedNotification> {
        val result = mutableListOf<SuppressedNotification>()
        try {
            val sharedPrefs = getSharedPreferences("suppressed_notifications_prefs", Context.MODE_PRIVATE)
            val jsonStr = sharedPrefs.getString("suppressed_list", null) ?: return emptyList()
            val array = org.json.JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(
                    SuppressedNotification(
                        packageName = obj.getString("packageName"),
                        appName = obj.getString("appName"),
                        title = obj.optString("title").takeIf { it.isNotEmpty() },
                        text = obj.optString("text").takeIf { it.isNotEmpty() },
                        whenMs = obj.getLong("whenMs")
                    )
                )
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return result
    }

    private fun addSuppressedNotification(suppressed: SuppressedNotification) {
        synchronized(saveLock) {
            val list = loadSuppressedNotifications().toMutableList()
            list.add(suppressed)
            saveSuppressedNotifications(list)
        }
    }

    private fun popAllSuppressedNotifications(): List<SuppressedNotification> {
        synchronized(saveLock) {
            val list = loadSuppressedNotifications()
            val sharedPrefs = getSharedPreferences("suppressed_notifications_prefs", Context.MODE_PRIVATE)
            sharedPrefs.edit().remove("suppressed_list").commit()
            return list
        }
    }
    
    private var activeProfileId: Long = -99L

    data class SuppressedNotification(
        val packageName: String,
        val appName: String,
        val title: String?,
        val text: String?,
        val whenMs: Long
    )

    override fun onCreate() {
        super.onCreate()
        
        // Listen to active profile changes to handle toggling and flushing dynamically
        serviceScope.launch {
            val app = applicationContext as FocusApplication
            val profileRepo = app.profileRepository
            profileRepo.activeProfileIdFlow.collectLatest { profileId ->
                activeProfileId = profileId
                if (profileId == -99L) {
                    flushSuppressedNotifications()
                }
            }
        }

        // Periodic check to verify Focus Launcher is still default. If not, reset profile to flush notifications.
        // We bypass this check during a 30 seconds grace period to allow the user to select Focus Launcher as default.
        serviceScope.launch {
            while (isActive) {
                try {
                    val app = applicationContext as FocusApplication
                    val profileRepo = app.profileRepository
                    val currentProfileId = profileRepo.getActiveProfileId()
                    
                    val sharedPrefs = getSharedPreferences("focus_launcher_prefs", Context.MODE_PRIVATE)
                    val lastChange = sharedPrefs.getLong("last_profile_change_time", 0)
                    val isGracePeriod = (System.currentTimeMillis() - lastChange) < 30000
                    
                    if (currentProfileId != -99L && !isFocusLauncherDefault(applicationContext) && !isGracePeriod) {
                        profileRepo.setActiveProfile(-99L)
                    }
                } catch (e: Exception) {}
                delay(1000)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        flushSuppressedNotifications()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName
        
        // If "No Mode", do not suppress anything
        if (activeProfileId == -99L) {
            return
        }

        // Whitelist check
        val whitelist = getSystemWhitelist(this)
        if (whitelist.contains(packageName)) {
            return
        }

        serviceScope.launch {
            val app = applicationContext as FocusApplication
            val profileRepo = app.profileRepository
            val allowedApps = profileRepo.getAllowedApps(activeProfileId)
            val allowedPackages = allowedApps.map { it.substringBefore('/') }.toSet()

            if (!allowedPackages.contains(packageName)) {
                // Suppress!
                val extras = sbn.notification.extras
                val title = extras.getString(Notification.EXTRA_TITLE)
                val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                val appName = getAppName(packageName)

                val suppressed = SuppressedNotification(
                    packageName = packageName,
                    appName = appName,
                    title = title,
                    text = text,
                    whenMs = sbn.postTime
                )
                addSuppressedNotification(suppressed)

                cancelNotification(sbn.key)
            }
        }
    }

    private fun flushSuppressedNotifications() {
        val suppressedList = popAllSuppressedNotifications()
        if (suppressedList.isEmpty()) return
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Honey Launcher Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Intercepted and restored notifications from Honey Launcher"
            }
            notificationManager.createNotificationChannel(channel)
        }

        var id = 1000
        suppressedList.forEach { suppressed ->
            val titleText = if (suppressed.title.isNullOrEmpty()) {
                suppressed.appName
            } else {
                "${suppressed.appName}: ${suppressed.title}"
            }
            
            val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setContentTitle(titleText)
                .setContentText(suppressed.text ?: "")
                .setWhen(suppressed.whenMs)
                .setAutoCancel(true)
                .setShowWhen(true)

            notificationManager.notify(id++, builder.build())
        }
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

    private fun isFocusLauncherDefault(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val defaultLauncherPackage = resolveInfo?.activityInfo?.packageName
        return defaultLauncherPackage == context.packageName
    }

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
            "com.google.android.packageinstaller"
        )
        
        val pm = context.packageManager
        
        // Keyboards (Input Methods): Dynamically whitelist all active/enabled keyboards
        try {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
            imm?.enabledInputMethodList?.forEach { info ->
                packages.add(info.packageName)
            }
        } catch (e: Exception) {}
        
        // Dialer (Phone)
        try {
            val dialIntent = Intent(Intent.ACTION_DIAL)
            val info = pm.resolveActivity(dialIntent, PackageManager.MATCH_DEFAULT_ONLY)
            info?.activityInfo?.packageName?.let { packages.add(it) }
        } catch (e: Exception) {}

        // SMS (Messages)
        try {
            val smsIntent = Intent(Intent.ACTION_SENDTO, android.net.Uri.parse("smsto:"))
            val info = pm.resolveActivity(smsIntent, PackageManager.MATCH_DEFAULT_ONLY)
            info?.activityInfo?.packageName?.let { packages.add(it) }
        } catch (e: Exception) {}

        // Calendar
        try {
            val calIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_CALENDAR) }
            val info = pm.resolveActivity(calIntent, PackageManager.MATCH_DEFAULT_ONLY)
            info?.activityInfo?.packageName?.let { packages.add(it) }
        } catch (e: Exception) {}

        // Contacts
        try {
            val contactsIntent = Intent(Intent.ACTION_VIEW, android.provider.ContactsContract.Contacts.CONTENT_URI)
            val info = pm.resolveActivity(contactsIntent, PackageManager.MATCH_DEFAULT_ONLY)
            info?.activityInfo?.packageName?.let { packages.add(it) }
        } catch (e: Exception) {}
        
        return packages
    }

    companion object {
        private const val CHANNEL_ID = "focus_launcher_suppressed_channel"
    }
}
