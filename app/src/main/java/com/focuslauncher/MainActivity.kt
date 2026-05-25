package com.focuslauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import com.focuslauncher.core.common.theme.FocusLauncherTheme
import com.focuslauncher.feature.launcher.LauncherViewModel
import com.focuslauncher.feature.launcher.ui.HomeScreen
import com.focuslauncher.feature.settings.SettingsViewModel
import com.focuslauncher.feature.settings.ui.AppSelectorScreen
import com.focuslauncher.feature.settings.ui.GeneralSettingsScreen
import com.focuslauncher.feature.settings.ui.OnboardingScreen
import com.focuslauncher.feature.settings.ui.SettingsScreen
import com.focuslauncher.feature.settings.ui.ProfileEditScreen
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import android.app.role.RoleManager
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.enableEdgeToEdge
import android.os.Build
import android.content.Intent
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        
        // Show system wallpaper behind activity window
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        
        // Retrieve singletons from Application class
        val app = application as FocusApplication
        val appRepo = app.appRepository
        val profileRepo = app.profileRepository

        // Custom factories for ViewModel injection
        val launcherViewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LauncherViewModel(applicationContext, appRepo, profileRepo) as T
            }
        }

        val settingsViewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(appRepo, profileRepo) as T
            }
        }

        setContent {
            val navController = rememberNavController()
            
            // Instantiating ViewModels at Activity scope to share edit drafts
            val launcherViewModel = ViewModelProvider(this, launcherViewModelFactory)[LauncherViewModel::class.java]
            val settingsViewModel = ViewModelProvider(this, settingsViewModelFactory)[SettingsViewModel::class.java]

            val globalTheme by settingsViewModel.globalTheme.collectAsState()
            val activeProfileId by settingsViewModel.activeProfileId.collectAsState()
            
            val startDestination = remember {
                if (settingsViewModel.isFirstInstall()) {
                    "onboarding"
                } else if (settingsViewModel.activeProfileId.value == -99L) {
                    "settings"
                } else {
                    "launcher"
                }
            }

            androidx.compose.runtime.LaunchedEffect(activeProfileId) {
                if (activeProfileId != -99L && navController.currentDestination?.route != "launcher") {
                    navController.navigate("launcher") {
                        popUpTo("launcher") { inclusive = true }
                    }
                }
            }
            
            val darkTheme = when (globalTheme) {
                "DARK" -> true
                "LIGHT" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            FocusLauncherTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.ui.graphics.Color.Transparent
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        enterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(300)
                            )
                        },
                        exitTransition = {
                            // Stay in place — the entering screen slides over this one
                            ExitTransition.None
                        },
                        popEnterTransition = {
                            // Stay in place — the popping screen slides away to reveal this one
                            EnterTransition.None
                        },
                        popExitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = tween(300)
                            )
                        }
                    ) {
                        composable(
                            route = "onboarding",
                            enterTransition = { fadeIn(animationSpec = tween(300)) },
                            exitTransition = { fadeOut(animationSpec = tween(300)) },
                            popEnterTransition = { fadeIn(animationSpec = tween(300)) },
                            popExitTransition = { fadeOut(animationSpec = tween(300)) }
                        ) {
                            OnboardingScreen(
                                viewModel = settingsViewModel,
                                onNavigateToModes = {
                                    navController.navigate("settings") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(
                            route = "launcher",
                            enterTransition = { fadeIn(animationSpec = tween(300)) },
                            exitTransition = { ExitTransition.None },
                            popEnterTransition = { EnterTransition.None },
                            popExitTransition = { ExitTransition.None }
                        ) {
                            HomeScreen(
                                viewModel = launcherViewModel,
                                onNavigateToSettings = {
                                    navController.navigate("settings")
                                },
                                onLockScreen = {
                                    lockScreen()
                                }
                            )
                        }
                        
                        composable("settings") {
                            SettingsScreen(
                                viewModel = settingsViewModel,
                                onNavigateToEditProfile = { profileId ->
                                    navController.navigate("edit_profile/$profileId")
                                },
                                onNavigateToGeneralSettings = {
                                    navController.navigate("general_settings")
                                },
                                onBack = {
                                    if (!navController.popBackStack()) {
                                        navController.navigate("launcher") {
                                            popUpTo("settings") { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }

                        composable("general_settings") {
                            GeneralSettingsScreen(
                                viewModel = settingsViewModel,
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        
                        composable(
                            route = "edit_profile/{profileId}",
                            arguments = listOf(navArgument("profileId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            ProfileEditScreen(
                                viewModel = settingsViewModel,
                                onNavigateToAppSelector = {
                                    navController.navigate("app_selector")
                                },
                                onBack = {
                                    navController.popBackStack()
                                },
                                onRequestSetDefaultLauncher = {
                                    lifecycleScope.launch {
                                        val pm = packageManager
                                        val aliasComponent = android.content.ComponentName(this@MainActivity, "com.focuslauncher.HomeActivityAlias")
                                        val currentState = pm.getComponentEnabledSetting(aliasComponent)
                                        if (currentState != PackageManager.COMPONENT_ENABLED_STATE_ENABLED && 
                                            currentState != PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {
                                            pm.setComponentEnabledSetting(
                                                aliasComponent,
                                                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                                                PackageManager.DONT_KILL_APP
                                            )
                                            delay(600) // Wait for HomeActivityAlias to be registered by the OS
                                        } else {
                                            delay(100) // Brief delay to let database update
                                        }
                                        if (!isDefaultLauncher()) {
                                            promptDefaultLauncher()
                                        }
                                    }
                                },
                                onRequestTurnOff = {
                                    finishAndRemoveTask()
                                }
                            )
                        }
                        
                        composable("app_selector") {
                            AppSelectorScreen(
                                viewModel = settingsViewModel,
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
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

    private fun promptDefaultLauncher() {
        // Try RoleManager first (shows a nice popup on stock Android Q+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                if (!roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                    try {
                        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                        startActivityForResult(intent, 1001)
                        return
                    } catch (e: Exception) {
                        // Fall through to system settings
                    }
                }
            }
        }
        
        // Fallback: Open system home settings directly (most reliable on Samsung One UI)
        try {
            val intent = Intent(Settings.ACTION_HOME_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                startActivity(intent)
            } catch (ex: Exception) {
            }
        }
    }

    private fun lockScreen() {
        val dpm = getSystemService(android.content.Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
        val adminComponent = android.content.ComponentName(this, FocusDeviceAdminReceiver::class.java)
        if (dpm.isAdminActive(adminComponent)) {
            dpm.lockNow()
        } else {
            // Prompt the user to enable Device Admin
            val intent = Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                putExtra(android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Honey Launcher needs admin permission to lock the screen when you double-tap.")
            }
            startActivity(intent)
        }
    }
}
