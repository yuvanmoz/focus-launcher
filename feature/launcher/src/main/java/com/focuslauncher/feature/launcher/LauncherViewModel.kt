package com.focuslauncher.feature.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focuslauncher.core.data.repository.AppRepository
import com.focuslauncher.core.data.repository.ProfileRepository
import com.focuslauncher.core.model.AppMetadata
import com.focuslauncher.core.model.FocusProfile
import com.focuslauncher.core.model.HomeScreenItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeScreenApp(
    val app: AppMetadata,
    val item: HomeScreenItem
)

@OptIn(ExperimentalCoroutinesApi::class)
class LauncherViewModel(
    private val context: Context,
    private val appRepository: AppRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    // Drawer search query
    val searchQuery = MutableStateFlow("")

    val blockedApp: StateFlow<com.focuslauncher.core.model.BlockedAppInfo?> = profileRepository.blockedAppFlow

    // All available focus profiles
    val profiles: StateFlow<List<FocusProfile>> = profileRepository.getAllProfilesFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Currently active profile ID
    val activeProfileId: StateFlow<Long> = profileRepository.activeProfileIdFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, -1L)

    val globalTheme: StateFlow<String> = profileRepository.globalThemeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "SYSTEM")



    // Current active focus profile details
    val activeProfile: StateFlow<FocusProfile?> = activeProfileId.flatMapLatest { id ->
        if (id == -1L || id == -99L) flowOf(null) else profileRepository.getProfileByIdFlow(id)
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    // All installed apps on the device
    val installedApps: StateFlow<List<AppMetadata>> = appRepository.installedAppsFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Allowed app package/activity combinations (uniqueId strings) for active profile
    private val allowedAppIds: StateFlow<List<String>> = activeProfileId.flatMapLatest { id ->
        if (id == -1L || id == -99L) flowOf(emptyList()) else profileRepository.getAllowedAppsFlow(id)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Pinned Home Screen items for active profile
    private val homeScreenItems = activeProfileId.flatMapLatest { id ->
        if (id == -1L || id == -99L) flowOf(emptyList()) else profileRepository.getHomeScreenItemsFlow(id)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Pinned apps mapped to their home screen coordinate slots
    val homeScreenApps: StateFlow<List<HomeScreenApp>> = combine(
        installedApps,
        allowedAppIds,
        homeScreenItems
    ) { apps, allowedIds, items ->
        val allowedSet = allowedIds.toSet()
        val appMap = apps.associateBy { it.uniqueId }

        items.mapNotNull { item ->
            val appId = "${item.packageName}/${item.activityName}"
            if (allowedSet.contains(appId)) {
                appMap[appId]?.let { app ->
                    HomeScreenApp(app, item)
                }
            } else null
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Dynamic resolution of One UI Dock applications (Phone, Messages, Calendar, Contacts), supporting custom ordering
    val dockApps: StateFlow<List<AppMetadata>> = combine(
        installedApps,
        profileRepository.dockAppsOrderFlow
    ) { apps, savedOrder ->
        val resolved = mutableListOf<AppMetadata>()
        val pm = context.packageManager

        // 1. Phone
        val dialPackage = pm.resolveActivity(Intent(Intent.ACTION_DIAL), PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName
        val dialApp = apps.find { it.packageName == dialPackage }
            ?: apps.find { it.packageName.contains("dialer", ignoreCase = true) || it.packageName.contains("phone", ignoreCase = true) }
        dialApp?.let { resolved.add(it) }

        // 2. Messages (SMS)
        val smsPackage = pm.resolveActivity(Intent(Intent.ACTION_SENDTO, android.net.Uri.parse("smsto:")), PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName
        val smsApp = apps.find { it.packageName == smsPackage }
            ?: apps.find { it.packageName.contains("messaging", ignoreCase = true) || it.packageName.contains("message", ignoreCase = true) || it.packageName.contains("mms", ignoreCase = true) }
        smsApp?.let { resolved.add(it) }

        // 3. Calendar
        val calIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_CALENDAR) }
        val calPackage = pm.resolveActivity(calIntent, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName
        val calApp = apps.find { it.packageName == calPackage }
            ?: apps.find { it.packageName.contains("calendar", ignoreCase = true) }
        calApp?.let { resolved.add(it) }

        // 4. Contacts
        val contactsIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_CONTACTS) }
        val contactsPackage = pm.resolveActivity(contactsIntent, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName
        val contactsApp = apps.find { it.packageName == contactsPackage }
            ?: apps.find { it.packageName.contains("contacts", ignoreCase = true) }
        contactsApp?.let { resolved.add(it) }

        val distinctDefault = resolved.distinctBy { it.uniqueId }

        if (savedOrder.isEmpty()) {
            distinctDefault
        } else {
            val orderedList = mutableListOf<AppMetadata>()
            savedOrder.forEach { pkg ->
                distinctDefault.find { it.packageName == pkg }?.let {
                    orderedList.add(it)
                }
            }
            distinctDefault.forEach { app ->
                if (!orderedList.any { it.packageName == app.packageName }) {
                    orderedList.add(app)
                }
            }
            orderedList
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun moveDockApp(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val current = dockApps.value.toMutableList()
            if (fromIndex in current.indices && toIndex in current.indices) {
                val item = current.removeAt(fromIndex)
                current.add(toIndex, item)
                profileRepository.setDockAppsOrder(current.map { it.packageName })
            }
        }
    }

    fun selectProfile(profileId: Long) {
        viewModelScope.launch {
            profileRepository.setActiveProfile(profileId)
        }
    }

    fun updateAppPosition(itemId: Long, cellX: Int, cellY: Int) {
        viewModelScope.launch {
            val activeId = activeProfileId.value
            if (activeId == -1L || activeId == -99L) return@launch

            val currentList = homeScreenItems.value
            val draggedItem = currentList.find { it.id == itemId } ?: return@launch

            // Check if there is an item already at the target cell coordinates
            val targetOccupant = currentList.find { it.cellX == cellX && it.cellY == cellY && it.id != itemId }

            val updatedItems = currentList.map { item ->
                if (item.id == draggedItem.id) {
                    item.copy(cellX = cellX, cellY = cellY)
                } else if (targetOccupant != null && item.id == targetOccupant.id) {
                    // Swap occupant to dragged item's original cell
                    item.copy(cellX = draggedItem.cellX, cellY = draggedItem.cellY)
                } else {
                    item
                }
            }
            profileRepository.updateHomeScreenItems(activeId, updatedItems)
        }
    }

    fun launchApp(context: Context, app: AppMetadata) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getAppIcon(app: AppMetadata) = appRepository.getAppIcon(app.packageName)

    fun dismissBlockPopup() {
        profileRepository.setBlockedApp(null)
    }
}
