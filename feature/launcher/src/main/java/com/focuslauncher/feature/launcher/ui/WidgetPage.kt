package com.focuslauncher.feature.launcher.ui

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.graphics.BitmapFactory
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.focuslauncher.core.common.rememberFrictionFlingBehavior
import com.focuslauncher.core.common.rememberRubberBandNestedScrollConnection
import com.focuslauncher.core.common.rememberRubberBandOverscrollState
import com.focuslauncher.core.common.rubberBandStretch
import com.focuslauncher.core.model.FocusProfile
import com.focuslauncher.core.model.WidgetItem
import com.focuslauncher.feature.launcher.LauncherViewModel
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WidgetPage(
    viewModel: LauncherViewModel,
    appWidgetHost: AppWidgetHost,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeProfile by viewModel.activeProfile.collectAsState()
    val widgetItems by viewModel.widgetItems.collectAsState()
    val allowedPackages by viewModel.allowedPackageNames.collectAsState()

    var showWidgetPicker by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }

    val sortedWidgets = remember(widgetItems) {
        widgetItems.sortedBy { it.cellY }
    }

    val overscrollState = rememberRubberBandOverscrollState()
    val nestedScrollConnection = rememberRubberBandNestedScrollConnection(overscrollState)
    val flingBehavior = rememberFrictionFlingBehavior()

    val totalItems = sortedWidgets.size

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                enabled = !isEditMode,
                onClick = { /* Consume clicks to prevent click-through */ }
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        isEditMode = true
                    }
                )
            }
    ) {
        // 1. Frosted glass wallpaper background
        BlurredWallpaperBackground(profile = activeProfile)

        // 2. Widget List
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Edit Mode HUD Header
            AnimatedVisibility(
                visible = isEditMode,
                enter = fadeIn(animationSpec = tween(250)),
                exit = fadeOut(animationSpec = tween(250))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Widgets",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    IconButton(
                        onClick = { isEditMode = false },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Done",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            CompositionLocalProvider(
                LocalOverscrollConfiguration provides null
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .nestedScroll(nestedScrollConnection)
                ) {
                    if (sortedWidgets.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Widgets",
                                style = TextStyle(
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    shadow = Shadow(
                                        color = Color.Black.copy(alpha = 0.5f),
                                        offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                                        blurRadius = 6f
                                    )
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Long press to add widgets",
                                style = TextStyle(
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center,
                                    shadow = Shadow(
                                        color = Color.Black.copy(alpha = 0.5f),
                                        offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                                        blurRadius = 4f
                                    )
                                )
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            IconButton(
                                onClick = { showWidgetPicker = true },
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Widget",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 20.dp,   // 20.dp left margin
                                end = 20.dp,     // 20.dp right margin
                                top = 16.dp,
                                bottom = 96.dp   // Leave room for bottom bar / padding
                            ),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            flingBehavior = flingBehavior
                        ) {
                            itemsIndexed(
                                items = sortedWidgets,
                                key = { _, item -> item.id }
                            ) { index, widgetItem ->
                                val heightDp = when (widgetItem.spanY) {
                                    1 -> 120.dp
                                    2 -> 180.dp
                                    3 -> 260.dp
                                    else -> 180.dp
                                }

                                Box(
                                    modifier = Modifier
                                        .rubberBandStretch(index, totalItems, overscrollState)
                                        .fillMaxWidth()
                                        .height(heightDp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                                        .pointerInput(widgetItem) {
                                            detectTapGestures(
                                                onLongPress = {
                                                    isEditMode = true
                                                }
                                            )
                                        }
                                ) {
                                    // Host the third-party AppWidgetHostView
                                    val appWidgetManager = remember { AppWidgetManager.getInstance(context) }
                                    AndroidView(
                                        factory = { ctx ->
                                            try {
                                                val hostView = appWidgetHost.createView(
                                                    ctx,
                                                    widgetItem.appWidgetId,
                                                    appWidgetManager.getAppWidgetInfo(widgetItem.appWidgetId)
                                                )
                                                hostView.apply {
                                                    layoutParams = FrameLayout.LayoutParams(
                                                        FrameLayout.LayoutParams.MATCH_PARENT,
                                                        FrameLayout.LayoutParams.MATCH_PARENT
                                                    )
                                                }
                                            } catch (e: Exception) {
                                                FrameLayout(ctx).apply {
                                                    setBackgroundColor(android.graphics.Color.argb(20, 255, 255, 255))
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    // Overlaid controls in Edit Mode
                                    if (isEditMode) {
                                        // Semi-translucent overlay to capture clicks and visually dim widget
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.45f))
                                                .clickable(
                                                    enabled = true,
                                                    onClick = { /* Intercept and consume touch inputs */ }
                                                )
                                        )

                                        // Move/Delete Control Bar Pill
                                        Row(
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .background(Color(0xFF1E1E1E).copy(alpha = 0.92f), RoundedCornerShape(16.dp))
                                                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Move Up button
                                            IconButton(
                                                onClick = { viewModel.moveWidgetUp(widgetItem) },
                                                enabled = index > 0,
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowUp,
                                                    contentDescription = "Move Up",
                                                    tint = if (index > 0) Color.White else Color.White.copy(alpha = 0.25f),
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }

                                            // Move Down button
                                            IconButton(
                                                onClick = { viewModel.moveWidgetDown(widgetItem) },
                                                enabled = index < totalItems - 1,
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowDown,
                                                    contentDescription = "Move Down",
                                                    tint = if (index < totalItems - 1) Color.White else Color.White.copy(alpha = 0.25f),
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }

                                            // Spacer Divider
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 1.dp, height = 20.dp)
                                                    .background(Color.White.copy(alpha = 0.2f))
                                            )

                                            // Delete button
                                            IconButton(
                                                onClick = {
                                                    viewModel.removeWidget(
                                                        widgetItem.id,
                                                        widgetItem.appWidgetId,
                                                        appWidgetHost
                                                    )
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Remove Widget",
                                                    tint = Color(0xFFFF5252),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Floating action button to quickly add widgets
                        if (!isEditMode) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = 24.dp, end = 24.dp),
                                contentAlignment = Alignment.BottomEnd
                            ) {
                                IconButton(
                                    onClick = { showWidgetPicker = true },
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.12f))
                                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Widget",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showWidgetPicker) {
        WidgetPickerSheet(
            viewModel = viewModel,
            appWidgetHost = appWidgetHost,
            allowedPackages = allowedPackages,
            onWidgetSelected = { appWidgetId, provider, spanX, spanY ->
                viewModel.addWidget(
                    appWidgetId = appWidgetId,
                    provider = provider,
                    cellX = 0,
                    cellY = 0,
                    spanX = spanX,
                    spanY = spanY
                )
                showWidgetPicker = false
            },
            onDismiss = { showWidgetPicker = false }
        )
    }
}

@Composable
private fun BlurredWallpaperBackground(profile: FocusProfile?) {
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
                        contentDescription = "Blurred Wallpaper",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = 1.15f,
                                scaleY = 1.15f
                            )
                            .blur(30.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
        // Frosted glass light translucent white overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.12f))
        )
    }
}
