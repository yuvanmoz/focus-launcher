package com.focuslauncher.feature.launcher.ui

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.graphics.BitmapFactory
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.focuslauncher.core.model.FocusProfile
import com.focuslauncher.core.model.WidgetItem
import com.focuslauncher.feature.launcher.LauncherViewModel
import java.io.File
import kotlin.math.roundToInt

@Composable
fun WidgetPage(
    viewModel: LauncherViewModel,
    appWidgetHost: AppWidgetHost,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val activeProfile by viewModel.activeProfile.collectAsState()
    val widgetItems by viewModel.widgetItems.collectAsState()
    val allowedPackages by viewModel.allowedPackageNames.collectAsState()
    val activeProfileId by viewModel.activeProfileId.collectAsState()

    var showWidgetPicker by remember { mutableStateOf(false) }

    // Drag state for repositioning widgets
    var draggingWidget by remember { mutableStateOf<WidgetItem?>(null) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }
    var showRemoveZone by remember { mutableStateOf(false) }

    val cols = 5
    val rows = 6

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Blurred wallpaper background (iOS-inspired)
        BlurredWallpaperBackground(profile = activeProfile)

        // 2. Widget grid workspace
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Remove zone at top (visible when dragging)
            AnimatedVisibility(
                visible = showRemoveZone,
                enter = fadeIn(animationSpec = tween(200)),
                exit = fadeOut(animationSpec = tween(200))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Color.Red.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✕  Remove Widget",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = if (showRemoveZone) 0.dp else 16.dp)
            ) {
                val cellWidth = maxWidth / cols
                val cellHeight = maxHeight / rows
                val cellWidthPx = with(density) { cellWidth.toPx() }
                val cellHeightPx = with(density) { cellHeight.toPx() }

                val currentWidgetItems by rememberUpdatedState(widgetItems)

                // Tap detection for long-press on empty space
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = { offset ->
                                    val cellX = (offset.x / cellWidthPx).toInt().coerceIn(0, cols - 1)
                                    val cellY = (offset.y / cellHeightPx).toInt().coerceIn(0, rows - 1)
                                    // Check if long press is on empty space
                                    val hitWidget = currentWidgetItems.find { widget ->
                                        cellX >= widget.cellX && cellX < widget.cellX + widget.spanX &&
                                                cellY >= widget.cellY && cellY < widget.cellY + widget.spanY
                                    }
                                    if (hitWidget == null) {
                                        showWidgetPicker = true
                                    }
                                }
                            )
                        }
                ) {
                    // Empty state
                    if (widgetItems.isEmpty() && draggingWidget == null) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Widgets",
                                style = TextStyle(
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 22.sp,
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
                                text = "Long press to add widgets",
                                style = TextStyle(
                                    color = Color.White.copy(alpha = 0.4f),
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
                    }

                    // Render placed widgets
                    widgetItems.forEach { widgetItem ->
                        val isThisDragging = draggingWidget?.id == widgetItem.id
                        val widgetWidthDp = cellWidth * widgetItem.spanX
                        val widgetHeightDp = cellHeight * widgetItem.spanY

                        Box(
                            modifier = Modifier
                                .offset(
                                    x = cellWidth * widgetItem.cellX,
                                    y = cellHeight * widgetItem.cellY
                                )
                                .size(width = widgetWidthDp, height = widgetHeightDp)
                                .graphicsLayer {
                                    alpha = if (isThisDragging) 0.3f else 1.0f
                                }
                                .clip(RoundedCornerShape(16.dp))
                                .pointerInput(widgetItem) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { offset ->
                                            draggingWidget = widgetItem
                                            showRemoveZone = true
                                            dragPosition = Offset(
                                                cellWidthPx * widgetItem.cellX + offset.x,
                                                cellHeightPx * widgetItem.cellY + offset.y
                                            )
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragPosition += dragAmount
                                        },
                                        onDragEnd = {
                                            showRemoveZone = false
                                            // Check if dragged to remove zone (top ~56dp area)
                                            val removeThresholdPx = with(density) { 56.dp.toPx() }
                                            if (dragPosition.y < removeThresholdPx) {
                                                // Remove the widget
                                                viewModel.removeWidget(
                                                    widgetItem.id,
                                                    widgetItem.appWidgetId,
                                                    appWidgetHost
                                                )
                                            } else {
                                                val finalX = (dragPosition.x / cellWidthPx).toInt()
                                                    .coerceIn(0, cols - widgetItem.spanX)
                                                val finalY = (dragPosition.y / cellHeightPx).toInt()
                                                    .coerceIn(0, rows - widgetItem.spanY)
                                                viewModel.moveWidget(widgetItem.id, finalX, finalY)
                                            }
                                            draggingWidget = null
                                        },
                                        onDragCancel = {
                                            draggingWidget = null
                                            showRemoveZone = false
                                        }
                                    )
                                }
                        ) {
                            // Render AppWidgetHostView
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
                                        // If widget fails to load, show placeholder
                                        FrameLayout(ctx).apply {
                                            setBackgroundColor(android.graphics.Color.argb(40, 255, 255, 255))
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // Floating drag ghost
                    if (draggingWidget != null) {
                        val dw = draggingWidget!!
                        val widgetWidthDp = cellWidth * dw.spanX
                        val widgetHeightDp = cellHeight * dw.spanY

                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        x = (dragPosition.x - cellWidthPx * dw.spanX / 2).roundToInt(),
                                        y = (dragPosition.y - cellHeightPx * dw.spanY / 2).roundToInt()
                                    )
                                }
                                .size(width = widgetWidthDp, height = widgetHeightDp)
                                .graphicsLayer {
                                    scaleX = 1.05f
                                    scaleY = 1.05f
                                    shadowElevation = 12f
                                    alpha = 0.85f
                                }
                                .clip(RoundedCornerShape(16.dp))
                        ) {
                            val appWidgetManager = remember { AppWidgetManager.getInstance(context) }
                            AndroidView(
                                factory = { ctx ->
                                    try {
                                        appWidgetHost.createView(
                                            ctx,
                                            dw.appWidgetId,
                                            appWidgetManager.getAppWidgetInfo(dw.appWidgetId)
                                        ).apply {
                                            layoutParams = FrameLayout.LayoutParams(
                                                FrameLayout.LayoutParams.MATCH_PARENT,
                                                FrameLayout.LayoutParams.MATCH_PARENT
                                            )
                                        }
                                    } catch (e: Exception) {
                                        FrameLayout(ctx)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // Bottom spacer
            Spacer(
                modifier = Modifier
                    .height(32.dp)
                    .navigationBarsPadding()
            )
        }
    }

    // Widget picker bottom sheet
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
                                scaleX = 1.1f,
                                scaleY = 1.1f
                            )
                            .blur(25.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
        // Dark overlay for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f))
        )
    }
}
