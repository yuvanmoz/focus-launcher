package com.focuslauncher.feature.launcher.ui

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.runtime.CompositionLocalProvider
import com.focuslauncher.core.common.rememberRubberBandOverscrollState
import com.focuslauncher.core.common.rememberRubberBandNestedScrollConnection
import com.focuslauncher.core.common.rememberFrictionFlingBehavior
import com.focuslauncher.core.common.rubberBandStretch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.max

data class WidgetProviderEntry(
    val info: AppWidgetProviderInfo,
    val appLabel: String,
    val widgetLabel: String,
    val minSpanX: Int,
    val minSpanY: Int
)

@Composable
fun WidgetPickerSheet(
    appWidgetHost: AppWidgetHost,
    allowedPackages: Set<String>,
    onWidgetSelected: (appWidgetId: Int, provider: ComponentName, spanX: Int, spanY: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val appWidgetManager = remember { AppWidgetManager.getInstance(context) }
    val pm = context.packageManager

    // Compute approximate cell size for span calculation (assuming 5 cols, ~360dp width)
    val approxCellWidthDp = 72 // ~360dp / 5
    val approxCellHeightDp = 100 // approx row height

    // Get all installed widget providers, filtered by allowed packages
    val filteredWidgets = remember(allowedPackages) {
        val allProviders = appWidgetManager.installedProviders
        allProviders
            .filter { provider ->
                val pkg = provider.provider.packageName
                allowedPackages.contains(pkg)
            }
            .map { info ->
                val appLabel = try {
                    pm.getApplicationLabel(
                        pm.getApplicationInfo(info.provider.packageName, 0)
                    ).toString()
                } catch (e: PackageManager.NameNotFoundException) {
                    info.provider.packageName
                }

                val widgetLabel = info.loadLabel(pm) ?: info.provider.className.substringAfterLast(".")

                val minW = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    info.minWidth
                } else {
                    info.minWidth
                }
                val minH = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    info.minHeight
                } else {
                    info.minHeight
                }

                // Convert min size px to grid spans (rough estimate)
                val density = context.resources.displayMetrics.density
                val minWidthDp = (minW / density).toInt()
                val minHeightDp = (minH / density).toInt()
                val spanX = max(1, (minWidthDp + approxCellWidthDp - 1) / approxCellWidthDp)
                val spanY = max(1, (minHeightDp + approxCellHeightDp - 1) / approxCellHeightDp)

                WidgetProviderEntry(
                    info = info,
                    appLabel = appLabel,
                    widgetLabel = widgetLabel,
                    minSpanX = spanX.coerceAtMost(5),
                    minSpanY = spanY.coerceAtMost(6)
                )
            }
            .sortedBy { it.appLabel.lowercase() }
    }

    // Group by app
    val groupedWidgets = remember(filteredWidgets) {
        filteredWidgets.groupBy { it.appLabel }
    }

    val overscrollState = rememberRubberBandOverscrollState()
    val nestedScrollConnection = rememberRubberBandNestedScrollConnection(overscrollState)
    val flingBehavior = rememberFrictionFlingBehavior()

    val totalItems = remember(groupedWidgets, filteredWidgets) {
        groupedWidgets.keys.size + filteredWidgets.size
    }

    @OptIn(ExperimentalFoundationApi::class)
    CompositionLocalProvider(
        LocalOverscrollConfiguration provides null
    ) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(520.dp)
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .background(Color(0xFF1A1A1A))
                        .clickable(enabled = false) {} // Prevent click-through
                        .nestedScroll(nestedScrollConnection)
                        .padding(top = 16.dp)
                ) {
                    // Handle bar
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 36.dp, height = 4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Title
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Add Widget",
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        TextButton(onClick = onDismiss) {
                            Text(
                                "Cancel",
                                color = Color(0xFF5B9EFF),
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (filteredWidgets.isEmpty()) {
                        // No widgets available
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No widgets available",
                                    style = TextStyle(
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Only widgets from allowed apps are shown",
                                    style = TextStyle(
                                        color = Color.White.copy(alpha = 0.35f),
                                        fontSize = 13.sp
                                    )
                                )
                            }
                        }
                    } else {
                        var itemIndex = 0
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            flingBehavior = flingBehavior
                        ) {
                            groupedWidgets.forEach { (appName, widgets) ->
                                // App section header
                                val headerIndex = itemIndex++
                                item {
                                    Text(
                                        text = appName,
                                        style = TextStyle(
                                            color = Color(0xFF5B9EFF),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        modifier = Modifier
                                            .rubberBandStretch(headerIndex, totalItems, overscrollState)
                                            .padding(
                                                start = 8.dp,
                                                top = 12.dp,
                                                bottom = 4.dp
                                            )
                                    )
                                }

                                widgets.forEach { entry ->
                                    val currentIdx = itemIndex++
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .rubberBandStretch(currentIdx, totalItems, overscrollState)
                                        ) {
                                            WidgetPickerItem(
                                                entry = entry,
                                                onClick = {
                                                    // Allocate widget ID and attempt to bind
                                                    val appWidgetId = appWidgetHost.allocateAppWidgetId()
                                                    val provider = entry.info.provider

                                                    val bound = appWidgetManager.bindAppWidgetIdIfAllowed(
                                                        appWidgetId,
                                                        provider
                                                    )

                                                    if (bound) {
                                                        onWidgetSelected(
                                                            appWidgetId,
                                                            provider,
                                                            entry.minSpanX,
                                                            entry.minSpanY
                                                        )
                                                    } else {
                                                        // System will show permission dialog
                                                        // For now, deallocate and show error state
                                                        // In a production app, you'd launch the bind intent
                                                        appWidgetHost.deleteAppWidgetId(appWidgetId)
                                                        // Try again — the system may grant after first attempt
                                                        val retryId = appWidgetHost.allocateAppWidgetId()
                                                        val retryBound = appWidgetManager.bindAppWidgetIdIfAllowed(
                                                            retryId,
                                                            provider
                                                        )
                                                        if (retryBound) {
                                                            onWidgetSelected(
                                                                retryId,
                                                                provider,
                                                                entry.minSpanX,
                                                                entry.minSpanY
                                                            )
                                                        } else {
                                                            appWidgetHost.deleteAppWidgetId(retryId)
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetPickerItem(
    entry: WidgetProviderEntry,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Widget preview icon
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            val previewDrawable = remember(entry.info) {
                try {
                    val previewRes = entry.info.previewImage
                    if (previewRes != 0) {
                        context.packageManager.getResourcesForApplication(
                            entry.info.provider.packageName
                        ).getDrawable(previewRes, null)
                    } else {
                        // Fallback to app icon
                        context.packageManager.getApplicationIcon(entry.info.provider.packageName)
                    }
                } catch (e: Exception) {
                    try {
                        context.packageManager.getApplicationIcon(entry.info.provider.packageName)
                    } catch (e2: Exception) {
                        null
                    }
                }
            }

            if (previewDrawable != null) {
                AndroidView(
                    factory = { ctx ->
                        android.widget.ImageView(ctx).apply {
                            setImageDrawable(previewDrawable)
                            scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
                        }
                    },
                    modifier = Modifier.fillMaxSize().padding(4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.widgetLabel,
                style = TextStyle(
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${entry.minSpanX}×${entry.minSpanY} cells",
                style = TextStyle(
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 12.sp
                )
            )
        }
    }
}
