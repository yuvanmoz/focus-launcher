package com.focuslauncher.feature.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focuslauncher.core.data.repository.AppRepository
import com.focuslauncher.core.data.repository.ProfileRepository
import com.focuslauncher.core.model.AppMetadata
import com.focuslauncher.core.model.FocusProfile
import com.focuslauncher.core.model.HomeScreenItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val appRepository: AppRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    // All available focus profiles
    val profiles: StateFlow<List<FocusProfile>> = profileRepository.getAllProfilesFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // All installed apps
    val installedApps: StateFlow<List<AppMetadata>> = appRepository.installedAppsFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Active profile id mapping
    val activeProfileId: StateFlow<Long> = profileRepository.activeProfileIdFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, -1L)

    // Edit Profile details state
    private val _editProfile = MutableStateFlow<FocusProfile?>(null)
    val editProfile: StateFlow<FocusProfile?> = _editProfile.asStateFlow()

    // Allowed app IDs (packageName/activityName)
    private val _allowedApps = MutableStateFlow<Set<String>>(emptySet())
    val allowedApps: StateFlow<Set<String>> = _allowedApps.asStateFlow()

    // Home screen pinned app IDs (packageName/activityName)
    private val _homeApps = MutableStateFlow<Set<String>>(emptySet())
    val homeApps: StateFlow<Set<String>> = _homeApps.asStateFlow()

    // Map of pinned app uniqueId -> Pair(cellX, cellY)
    private val _homeAppPositions = MutableStateFlow<Map<String, Pair<Int, Int>>>(emptyMap())
    val homeAppPositions: StateFlow<Map<String, Pair<Int, Int>>> = _homeAppPositions.asStateFlow()

    val globalTheme: StateFlow<String> = profileRepository.globalThemeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "SYSTEM")

    val isFirstInstall: StateFlow<Boolean> = profileRepository.isFirstInstallFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)


    fun setGlobalTheme(theme: String) {
        viewModelScope.launch {
            profileRepository.setGlobalTheme(theme)
        }
    }

    fun setFirstInstallComplete() {
        viewModelScope.launch {
            profileRepository.setFirstInstallComplete()
        }
    }


    fun isFirstInstall(): Boolean {
        return profileRepository.isFirstInstall()
    }

    fun getAppIcon(packageName: String) = appRepository.getAppIcon(packageName)

    fun selectProfile(profileId: Long) {
        viewModelScope.launch {
            profileRepository.setActiveProfile(profileId)
        }
    }

    private var profileJob: kotlinx.coroutines.Job? = null
    private var itemsJob: kotlinx.coroutines.Job? = null

    private fun loadEditingProfileFlows(profileId: Long) {
        profileJob?.cancel()
        itemsJob?.cancel()

        val profileFlow = profileRepository.getProfileByIdFlow(profileId)
        val itemsFlow = profileRepository.getHomeScreenItemsFlow(profileId)
        
        profileJob = viewModelScope.launch {
            profileFlow.collect { profile ->
                if (profile != null) {
                    _editProfile.value = profile
                    val allowed = profileRepository.getAllowedApps(profileId).toSet()
                    _allowedApps.value = allowed
                }
            }
        }
        
        itemsJob = viewModelScope.launch {
            itemsFlow.collect { items ->
                val homeSet = mutableSetOf<String>()
                val positionsMap = mutableMapOf<String, Pair<Int, Int>>()
                items.forEach { item ->
                    val key = "${item.packageName}/${item.activityName}"
                    homeSet.add(key)
                    positionsMap[key] = item.cellX to item.cellY
                }
                _homeApps.value = homeSet
                _homeAppPositions.value = positionsMap
            }
        }
    }

    fun startEditingProfile(profileId: Long) {
        viewModelScope.launch {
            if (profileId == -1L) {
                // Creating a new profile draft with ID 0L (unsaved)
                _editProfile.value = FocusProfile(
                    id = 0L,
                    name = "New Mode",
                    wallpaperPath = null,
                    isSystemWallpaper = false,
                    gridRows = 6,
                    gridCols = 5,
                    themeMode = "SYSTEM"
                )
                _allowedApps.value = emptySet()
                _homeApps.value = emptySet()
                _homeAppPositions.value = emptyMap()
            } else {
                loadEditingProfileFlows(profileId)
            }
        }
    }

    private suspend fun getOrCreateProfileId(): Long {
        val current = _editProfile.value ?: return -1L
        if (current.id != 0L) return current.id
        
        // Save to DB to initialize it and obtain a valid generated profile ID
        val newId = profileRepository.createProfile(current)
        val updated = current.copy(id = newId)
        _editProfile.value = updated
        loadEditingProfileFlows(newId)
        return newId
    }

    fun updateProfileDraft(updater: (FocusProfile) -> FocusProfile) {
        val current = _editProfile.value ?: return
        val updated = updater(current)
        _editProfile.value = updated
        
        viewModelScope.launch {
            val id = getOrCreateProfileId()
            if (id != -1L) {
                profileRepository.updateProfile(updated.copy(id = id))
            }
        }
    }

    fun toggleAppAllowed(appId: String) {
        val currentAllowed = _allowedApps.value.toMutableSet()
        val currentHome = _homeApps.value.toMutableSet()
        val currentPositions = _homeAppPositions.value.toMutableMap()

        if (currentAllowed.contains(appId)) {
            currentAllowed.remove(appId)
            currentHome.remove(appId)
            currentPositions.remove(appId)
        } else {
            currentAllowed.add(appId)
            currentHome.add(appId)
            val occupied = currentPositions.values.toSet()
            val freeCell = findFirstAvailableCell(occupied)
            currentPositions[appId] = freeCell
        }

        _allowedApps.value = currentAllowed
        _homeApps.value = currentHome
        _homeAppPositions.value = currentPositions

        viewModelScope.launch {
            val id = getOrCreateProfileId()
            if (id != -1L) {
                val allowedPairs = currentAllowed.map { idStr ->
                    val parts = idStr.split("/")
                    parts[0] to parts[1]
                }
                profileRepository.updateAllowedApps(id, allowedPairs)

                val homeItems = currentHome.map { idStr ->
                    val parts = idStr.split("/")
                    val coords = currentPositions[idStr] ?: (0 to 0)
                    HomeScreenItem(
                        profileId = id,
                        packageName = parts[0],
                        activityName = parts[1],
                        cellX = coords.first,
                        cellY = coords.second
                    )
                }
                profileRepository.updateHomeScreenItems(id, homeItems)
            }
        }
    }

    private fun findFirstAvailableCell(occupied: Set<Pair<Int, Int>>): Pair<Int, Int> {
        for (y in 0 until 6) {
            for (x in 0 until 5) {
                if (!occupied.contains(x to y)) {
                    return x to y
                }
            }
        }
        return 0 to 0
    }

    fun selectWallpaperImage(uri: Uri) {
        val profile = _editProfile.value ?: return
        viewModelScope.launch {
            val id = getOrCreateProfileId()
            if (id != -1L) {
                val path = profileRepository.saveWallpaper(id, uri)
                if (path != null) {
                    val updated = profile.copy(id = id, wallpaperPath = path)
                    _editProfile.value = updated
                    profileRepository.updateProfile(updated)
                }
            }
        }
    }

    fun clearCustomWallpaper() {
        val profile = _editProfile.value ?: return
        viewModelScope.launch {
            val id = getOrCreateProfileId()
            if (id != -1L) {
                val updated = profile.copy(id = id, wallpaperPath = null)
                _editProfile.value = updated
                profileRepository.updateProfile(updated)
            }
        }
    }

    fun deleteProfile(profile: FocusProfile) {
        viewModelScope.launch {
            profileRepository.deleteProfile(profile)
        }
    }
}
