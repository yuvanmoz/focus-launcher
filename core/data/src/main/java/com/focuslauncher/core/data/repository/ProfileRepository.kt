package com.focuslauncher.core.data.repository

import android.app.WallpaperManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.focuslauncher.core.database.dao.HomeScreenDao
import com.focuslauncher.core.database.dao.ProfileDao
import com.focuslauncher.core.database.entity.FocusProfileEntity
import com.focuslauncher.core.database.entity.HomeScreenItemEntity
import com.focuslauncher.core.database.entity.ProfileAppEntity
import com.focuslauncher.core.model.FocusProfile
import com.focuslauncher.core.model.HomeScreenItem
import com.focuslauncher.core.model.BlockedAppInfo
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

interface ProfileRepository {
    companion object {
        const val NO_MODE_ID = -99L
    }
    val activeProfileIdFlow: Flow<Long>
    val blockedAppFlow: StateFlow<BlockedAppInfo?>
    fun getActiveProfileId(): Long
    suspend fun setActiveProfile(profileId: Long)
    fun setBlockedApp(blockedApp: BlockedAppInfo?)
    fun getAllProfilesFlow(): Flow<List<FocusProfile>>
    fun getProfileByIdFlow(profileId: Long): Flow<FocusProfile?>
    suspend fun getProfileById(profileId: Long): FocusProfile?
    suspend fun createProfile(profile: FocusProfile): Long
    suspend fun updateProfile(profile: FocusProfile)
    suspend fun deleteProfile(profile: FocusProfile)
    fun getAllowedAppsFlow(profileId: Long): Flow<List<String>>
    suspend fun getAllowedApps(profileId: Long): List<String>
    suspend fun updateAllowedApps(profileId: Long, apps: List<Pair<String, String>>)
    fun getHomeScreenItemsFlow(profileId: Long): Flow<List<HomeScreenItem>>
    suspend fun updateHomeScreenItems(profileId: Long, items: List<HomeScreenItem>)
    suspend fun saveWallpaper(profileId: Long, uri: Uri): String?
    suspend fun applyWallpaper(profile: FocusProfile)
    
    val globalThemeFlow: Flow<String>
    fun getGlobalTheme(): String
    suspend fun setGlobalTheme(theme: String)
    
    val isFirstInstallFlow: Flow<Boolean>
    fun isFirstInstall(): Boolean
    suspend fun setFirstInstallComplete()
}

class ProfileRepositoryImpl(
    private val context: Context,
    private val profileDao: ProfileDao,
    private val homeScreenDao: HomeScreenDao
) : ProfileRepository {

    private val sharedPrefs: SharedPreferences = context.getSharedPreferences(
        "focus_launcher_prefs",
        Context.MODE_PRIVATE
    )
    
    private val _activeProfileId = MutableStateFlow(sharedPrefs.getLong("active_profile_id", -99L))
    override val activeProfileIdFlow: Flow<Long> = _activeProfileId.asStateFlow()

    private val _blockedApp = MutableStateFlow<BlockedAppInfo?>(null)
    override val blockedAppFlow: StateFlow<BlockedAppInfo?> = _blockedApp.asStateFlow()

    private val _globalTheme = MutableStateFlow(sharedPrefs.getString("global_theme", "SYSTEM") ?: "SYSTEM")
    override val globalThemeFlow: Flow<String> = _globalTheme.asStateFlow()

    private val _isFirstInstall = MutableStateFlow(sharedPrefs.getBoolean("is_first_install", true))
    override val isFirstInstallFlow: Flow<Boolean> = _isFirstInstall.asStateFlow()

    override fun isFirstInstall(): Boolean {
        return _isFirstInstall.value
    }

    override fun getGlobalTheme(): String {
        return _globalTheme.value
    }

    override suspend fun setGlobalTheme(theme: String) {
        sharedPrefs.edit().putString("global_theme", theme).apply()
        _globalTheme.value = theme
    }

    override suspend fun setFirstInstallComplete() {
        sharedPrefs.edit().putBoolean("is_first_install", false).apply()
        _isFirstInstall.value = false
    }


    override fun setBlockedApp(blockedApp: BlockedAppInfo?) {
        _blockedApp.value = blockedApp
    }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            prepopulateDefaultProfilesIfNeeded()
        }
    }

    private suspend fun prepopulateDefaultProfilesIfNeeded() {
        val existing = profileDao.getAllProfiles()
        if (existing.isEmpty()) {
            profileDao.insertProfile(
                FocusProfileEntity(name = "Work", wallpaperPath = null, isSystemWallpaper = false, gridRows = 6, gridCols = 5, themeMode = "DARK")
            )
            profileDao.insertProfile(
                FocusProfileEntity(name = "Personal", wallpaperPath = null, isSystemWallpaper = false, gridRows = 6, gridCols = 5, themeMode = "LIGHT")
            )
            profileDao.insertProfile(
                FocusProfileEntity(name = "Trading", wallpaperPath = null, isSystemWallpaper = false, gridRows = 6, gridCols = 5, themeMode = "DARK")
            )
            profileDao.insertProfile(
                FocusProfileEntity(name = "Study", wallpaperPath = null, isSystemWallpaper = false, gridRows = 6, gridCols = 5, themeMode = "LIGHT")
            )
            profileDao.insertProfile(
                FocusProfileEntity(name = "Games", wallpaperPath = null, isSystemWallpaper = false, gridRows = 6, gridCols = 5, themeMode = "DARK")
            )
            profileDao.insertProfile(
                FocusProfileEntity(name = "Exercise", wallpaperPath = null, isSystemWallpaper = false, gridRows = 6, gridCols = 5, themeMode = "LIGHT")
            )
            
            // Set first active profile to No Mode (-99L)
            setActiveProfile(-99L)
        } else if (_activeProfileId.value == -1L) {
            setActiveProfile(-99L)
        }
    }

    override fun getActiveProfileId(): Long {
        return _activeProfileId.value
    }

    override suspend fun getProfileById(profileId: Long): FocusProfile? = withContext(Dispatchers.IO) {
        profileDao.getProfileById(profileId)?.toDomain()
    }

    override suspend fun setActiveProfile(profileId: Long) {
        sharedPrefs.edit()
            .putLong("active_profile_id", profileId)
            .putLong("last_profile_change_time", System.currentTimeMillis())
            .apply()
        _activeProfileId.value = profileId
        
        // Trigger wallpaper updates in background
        withContext(Dispatchers.IO) {
            val profile = profileDao.getProfileById(profileId)?.toDomain()
            if (profile != null) {
                applyWallpaper(profile)
            }
        }
    }

    override fun getAllProfilesFlow(): Flow<List<FocusProfile>> {
        return profileDao.getAllProfilesFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getProfileByIdFlow(profileId: Long): Flow<FocusProfile?> {
        return profileDao.getProfileByIdFlow(profileId).map { it?.toDomain() }
    }

    override suspend fun createProfile(profile: FocusProfile): Long = withContext(Dispatchers.IO) {
        profileDao.insertProfile(FocusProfileEntity.fromDomain(profile))
    }

    override suspend fun updateProfile(profile: FocusProfile) = withContext(Dispatchers.IO) {
        profileDao.updateProfile(FocusProfileEntity.fromDomain(profile))
        
        // If updating the active profile, apply the wallpaper
        if (profile.id == _activeProfileId.value) {
            applyWallpaper(profile)
        }
    }

    override suspend fun deleteProfile(profile: FocusProfile) = withContext(Dispatchers.IO) {
        profileDao.deleteProfile(FocusProfileEntity.fromDomain(profile))
        if (_activeProfileId.value == profile.id) {
            val remaining = profileDao.getAllProfiles()
            if (remaining.isNotEmpty()) {
                setActiveProfile(remaining.first().id)
            }
        }
    }

    override fun getAllowedAppsFlow(profileId: Long): Flow<List<String>> {
        return profileDao.getAllowedAppsFlow(profileId).map { list ->
            list.map { "${it.packageName}/${it.activityName}" }
        }
    }

    override suspend fun getAllowedApps(profileId: Long): List<String> = withContext(Dispatchers.IO) {
        profileDao.getAllowedApps(profileId).map { "${it.packageName}/${it.activityName}" }
    }

    override suspend fun updateAllowedApps(profileId: Long, apps: List<Pair<String, String>>) = withContext(Dispatchers.IO) {
        val entities = apps.map { ProfileAppEntity(profileId, it.first, it.second) }
        profileDao.updateAllowedAppsForProfile(profileId, entities)
    }

    override fun getHomeScreenItemsFlow(profileId: Long): Flow<List<HomeScreenItem>> {
        return homeScreenDao.getItemsForProfileFlow(profileId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun updateHomeScreenItems(profileId: Long, items: List<HomeScreenItem>) = withContext(Dispatchers.IO) {
        val entities = items.map { HomeScreenItemEntity.fromDomain(it) }
        homeScreenDao.updateItemsForProfile(profileId, entities)
    }

    override suspend fun saveWallpaper(profileId: Long, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, "wallpapers")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val destinationFile = File(dir, "profile_$profileId.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            }
            destinationFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun applyWallpaper(profile: FocusProfile) = withContext(Dispatchers.IO) {
        val path = profile.wallpaperPath ?: return@withContext
        if (!profile.isSystemWallpaper) return@withContext // Render inside launcher UI only
        
        try {
            val file = File(path)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    val wallpaperManager = WallpaperManager.getInstance(context)
                    wallpaperManager.setBitmap(bitmap)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Ignore system wallpaper errors if permissions are missing
        }
    }
}
