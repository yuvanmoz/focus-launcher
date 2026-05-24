package com.focuslauncher.feature.launcher.ui

import android.app.WallpaperManager
import android.graphics.BitmapFactory
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuslauncher.core.common.theme.FocusLauncherTheme
import com.focuslauncher.core.common.theme.PersonalAccent
import com.focuslauncher.core.common.theme.StudyAccent
import com.focuslauncher.core.common.theme.TradingAccent
import com.focuslauncher.core.common.theme.WorkAccent
import com.focuslauncher.core.model.AppMetadata
import com.focuslauncher.core.model.FocusProfile
import com.focuslauncher.feature.launcher.HomeScreenApp
import com.focuslauncher.feature.launcher.LauncherViewModel
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import kotlin.math.roundToInt
import android.content.ComponentName
import android.provider.Settings
import android.os.Build
import android.app.AppOpsManager
import android.os.Process
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton

@Composable
fun HomeScreen(
    viewModel: LauncherViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeProfile by viewModel.activeProfile.collectAsState()
    val homeApps by viewModel.homeScreenApps.collectAsState()
    val blockedApp by viewModel.blockedApp.collectAsState()
    val activeProfileId by viewModel.activeProfileId.collectAsState()
    val dockApps by viewModel.dockApps.collectAsState()
    
    var showUsagePrompt by remember { mutableStateOf(false) }
    var showNotificationPrompt by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(activeProfileId) {
        if (activeProfileId != -99L && activeProfileId != -1L) {
            val usageOk = hasUsageStatsPermission(context)
            showUsagePrompt = !usageOk
            showNotificationPrompt = usageOk && !isNotificationServiceEnabled(context)
        } else {
            showUsagePrompt = false
            showNotificationPrompt = false
        }
    }

    val globalTheme by viewModel.globalTheme.collectAsState()
    val darkTheme = when (globalTheme) {
        "DARK" -> true
        "LIGHT" -> false
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }

    val density = LocalDensity.current

    // Drag and drop states
    var draggingApp by remember { mutableStateOf<HomeScreenApp?>(null) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }

    val dragScale by animateFloatAsState(
        targetValue = if (draggingApp != null) 1.2f else 1.0f,
        animationSpec = tween(150),
        label = "DragScaleAnimation"
    )

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Transparent)
        ) {
            // 1. Dynamic Wallpaper
            WallpaperBackground(profile = activeProfile, isDimmed = draggingApp != null)

            // 2. Absolute drag-and-drop workspace layout + One UI Bottom Dock
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                val cols = 5
                val rows = 6

                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                ) {
                    val cellWidth = maxWidth / cols
                    val cellHeight = maxHeight / rows
                    val cellWidthPx = with(density) { cellWidth.toPx() }
                    val cellHeightPx = with(density) { cellHeight.toPx() }

                    // Workspace canvas area to detect long press & drag gestures
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(homeApps) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { offset ->
                                        val cellX = (offset.x / cellWidthPx).toInt().coerceIn(0, cols - 1)
                                        val cellY = (offset.y / cellHeightPx).toInt().coerceIn(0, rows - 1)
                                        val app = homeApps.find { it.item.cellX == cellX && it.item.cellY == cellY }
                                        if (app != null) {
                                            draggingApp = app
                                            dragPosition = offset
                                        } else {
                                            // Long pressed empty area triggers settings
                                            onNavigateToSettings()
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        if (draggingApp != null) {
                                            dragPosition += dragAmount
                                        }
                                    },
                                    onDragEnd = {
                                        if (draggingApp != null) {
                                            val finalX = (dragPosition.x / cellWidthPx).toInt().coerceIn(0, cols - 1)
                                            val finalY = (dragPosition.y / cellHeightPx).toInt().coerceIn(0, rows - 1)
                                            viewModel.updateAppPosition(draggingApp!!.item.id, finalX, finalY)
                                            draggingApp = null
                                        }
                                    },
                                    onDragCancel = {
                                        draggingApp = null
                                    }
                                )
                            }
                    ) {
                        // Draw fluid visual indicators if dragging
                        if (draggingApp != null) {
                            val occupiedCells = homeApps
                                .filter { it.item.id != draggingApp?.item?.id }
                                .map { it.item.cellX to it.item.cellY }
                                .toSet()

                            // Empty landing grid cells show a soft, faint circular placement zone
                            for (r in 0 until rows) {
                                for (c in 0 until cols) {
                                    if (!occupiedCells.contains(c to r)) {
                                        Box(
                                            modifier = Modifier
                                                .offset(x = cellWidth * c, y = cellHeight * r)
                                                .size(width = cellWidth, height = cellHeight),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .background(
                                                        color = Color.White.copy(alpha = 0.2f),
                                                        shape = CircleShape
                                                    )
                                            )
                                        }
                                    }
                                }
                            }

                            // Hover target focal ring (soft, blurred, circular)
                            val hoverX = (dragPosition.x / cellWidthPx).toInt().coerceIn(0, cols - 1)
                            val hoverY = (dragPosition.y / cellHeightPx).toInt().coerceIn(0, rows - 1)

                            Box(
                                modifier = Modifier
                                    .offset(x = cellWidth * hoverX, y = cellHeight * hoverY)
                                    .size(width = cellWidth, height = cellHeight),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .background(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = 0.25f),
                                                    Color.White.copy(alpha = 0.0f)
                                                )
                                            ),
                                            shape = CircleShape
                                        )
                                        .border(
                                            width = 1.5.dp,
                                            color = Color.White.copy(alpha = 0.6f),
                                            shape = CircleShape
                                        )
                                )
                            }
                        }

                        // Render home apps in their grid cells
                        if (homeApps.isEmpty() && draggingApp == null) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Focus Mode: ${activeProfile?.name ?: "No Mode"}",
                                    style = TextStyle(
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        shadow = Shadow(
                                            color = Color.Black.copy(alpha = 0.9f),
                                            offset = Offset(2f, 2f),
                                            blurRadius = 6f
                                        )
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Long press wallpaper to configure profiles",
                                    style = TextStyle(
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center,
                                        shadow = Shadow(
                                            color = Color.Black.copy(alpha = 0.9f),
                                            offset = Offset(1f, 1f),
                                            blurRadius = 4f
                                        )
                                    )
                                )
                            }
                        } else {
                            homeApps.forEach { homeApp ->
                                val isThisDragging = draggingApp?.item?.id == homeApp.item.id
                                Box(
                                    modifier = Modifier
                                        .offset(
                                            x = cellWidth * homeApp.item.cellX,
                                            y = cellHeight * homeApp.item.cellY
                                        )
                                        .size(width = cellWidth, height = cellHeight)
                                        .graphicsLayer {
                                            alpha = if (isThisDragging) 0.0f else 1.0f
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    AppIconItem(
                                        app = homeApp.app,
                                        viewModel = viewModel,
                                        onClick = {
                                            if (draggingApp == null) {
                                                viewModel.launchApp(context, homeApp.app)
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // Floating dragging icon layer
                        if (draggingApp != null) {
                            Box(
                                modifier = Modifier
                                    .offset {
                                        IntOffset(
                                            x = (dragPosition.x - cellWidthPx / 2).roundToInt(),
                                            y = (dragPosition.y - cellHeightPx / 2).roundToInt()
                                        )
                                    }
                                    .size(width = cellWidth, height = cellHeight)
                                    .graphicsLayer {
                                        scaleX = dragScale
                                        scaleY = dragScale
                                        shadowElevation = (12.dp * dragScale).toPx()
                                        clip = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                AppIconItem(
                                    app = draggingApp!!.app,
                                    viewModel = viewModel,
                                    onClick = {}
                                )
                            }
                        }
                    }
                }

                // Spacer to create a clear visual gap between workspace bottom and dock top
                Spacer(modifier = Modifier.height(24.dp))

                // Premium floating dock container like iOS with translucent frosted glass look
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 16.dp)
                        .navigationBarsPadding(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .background(
                                color = Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(30.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(30.dp)
                            )
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        dockApps.forEach { dockApp ->
                            AppIconItem(
                                app = dockApp,
                                viewModel = viewModel,
                                onClick = {
                                    viewModel.launchApp(context, dockApp)
                                },
                                showLabel = false
                            )
                        }
                    }
                }
            }

            // 3. Blocked App Bottom Sheet Overlay
            AnimatedVisibility(
                visible = blockedApp != null,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
                    .navigationBarsPadding()
            ) {
                blockedApp?.let { app ->
                    Box(
                        modifier = Modifier
                            .widthIn(max = 340.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                if (darkTheme) Color(0xFF1E1E1E) else Color.White
                            )
                            .padding(18.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 36.dp, height = 4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (darkTheme) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.15f))
                            )
                            
                            Text(
                                text = "App Blocked",
                                style = TextStyle(
                                    color = if (darkTheme) Color.White else Color.Black,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "${app.appName} is blocked during ${app.profileName} mode.",
                                style = TextStyle(
                                    color = if (darkTheme) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f),
                                    fontSize = 13.sp
                                ),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.dismissBlockPopup() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Got it", fontSize = 14.sp)
                                }

                                androidx.compose.material3.TextButton(
                                    onClick = { 
                                        viewModel.dismissBlockPopup()
                                        onNavigateToSettings() 
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Change Focus Settings",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Permission Dialog Prompts
            if (showUsagePrompt) {
                AlertDialog(
                    onDismissRequest = { /* Don't dismiss automatically */ },
                    title = { Text("Usage Stats Permission Required") },
                    text = { Text("Focus Launcher needs Usage Access permission to detect when unallowed apps are opened and block them during focus mode.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                try {
                                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Fallback
                                }
                                showUsagePrompt = false
                            }
                        ) {
                            Text("Grant Access")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showUsagePrompt = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (showNotificationPrompt && !showUsagePrompt) {
                AlertDialog(
                    onDismissRequest = { /* Don't dismiss automatically */ },
                    title = { Text("Notification Access Required") },
                    text = { Text("Focus Launcher needs Notification Listener access to temporarily suppress and queue notifications from blocked apps.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                try {
                                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Fallback
                                }
                                showNotificationPrompt = false
                            }
                        ) {
                            Text("Grant Access")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showNotificationPrompt = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }

@Composable
fun WallpaperBackground(profile: FocusProfile?, isDimmed: Boolean) {
    val path = profile?.wallpaperPath
    Box(modifier = Modifier.fillMaxSize()) {
        if (path != null) {
            val file = File(path)
            if (file.exists()) {
                val bitmap = remember(path) {
                    BitmapFactory.decodeFile(file.absolutePath)
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Wallpaper",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
            }
        }
        
        // Dimming overlay when dragging an app
        val dimAlpha by androidx.compose.animation.core.animateFloatAsState(
            targetValue = if (isDimmed) 0.35f else 0f,
            label = "WallpaperDim"
        )
        if (dimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = dimAlpha))
            )
        }
    }
}

private fun hasUsageStatsPermission(context: android.content.Context): Boolean {
    val appOps = context.getSystemService(android.content.Context.APP_OPS_SERVICE) as? android.app.AppOpsManager ?: return false
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
    } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
    }
    return mode == android.app.AppOpsManager.MODE_ALLOWED
}

private fun isNotificationServiceEnabled(context: android.content.Context): Boolean {
    val cn = ComponentName(context.packageName, "com.focuslauncher.FocusNotificationListenerService")
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat != null && flat.contains(cn.flattenToString())
}

@Composable
fun AppIconItem(
    app: AppMetadata,
    viewModel: LauncherViewModel,
    onClick: () -> Unit,
    showLabel: Boolean = true
) {
    val iconDrawable = remember(app.uniqueId) { viewModel.getAppIcon(app) }

    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(56.dp),
            contentAlignment = Alignment.Center
        ) {
            if (iconDrawable != null) {
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { context ->
                        android.widget.ImageView(context).apply {
                            setImageDrawable(iconDrawable)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                )
            }
        }

        if (showLabel) {
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = app.label,
                style = TextStyle(
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.85f),
                        offset = Offset(1.5f, 2f),
                        blurRadius = 4f
                    )
                ),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
