package com.focuslauncher.feature.settings.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.CandlestickChart
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuslauncher.core.common.rememberFrictionFlingBehavior
import com.focuslauncher.core.common.rememberRubberBandNestedScrollConnection
import com.focuslauncher.core.common.rememberRubberBandOverscrollState
import com.focuslauncher.core.common.rubberBandStretch
import com.focuslauncher.core.common.theme.ExerciseAccent
import com.focuslauncher.core.common.theme.GamesAccent
import com.focuslauncher.core.common.theme.PersonalAccent
import com.focuslauncher.core.common.theme.StudyAccent
import com.focuslauncher.core.common.theme.TradingAccent
import com.focuslauncher.core.common.theme.WorkAccent
import com.focuslauncher.core.model.FocusProfile
import com.focuslauncher.feature.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToEditProfile: (Long) -> Unit,
    onNavigateToGeneralSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profiles by viewModel.profiles.collectAsState()
    val activeProfileId by viewModel.activeProfileId.collectAsState()

    val overscrollState = rememberRubberBandOverscrollState()
    val nestedScrollConnection = rememberRubberBandNestedScrollConnection(overscrollState)
    val flingBehavior = rememberFrictionFlingBehavior()

    val totalItems = profiles.size + 1
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    @OptIn(ExperimentalFoundationApi::class)
    CompositionLocalProvider(
        LocalOverscrollConfiguration provides null
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "Modes",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            ),
                            color = if (isDark) Color.White else Color.Black
                        )
                    },
                    actions = {
                        IconButton(onClick = onNavigateToGeneralSettings) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "Settings",
                                tint = if (isDark) Color.White else Color.Black
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = if (isDark) Color.Black else Color.White
                    )
                )
            },
            containerColor = if (isDark) Color.Black else Color.White,
            modifier = modifier.nestedScroll(nestedScrollConnection)
        ) { padding ->
            LazyColumn(
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 24.dp,
                    bottom = padding.calculateBottomPadding() + 24.dp,
                    start = 20.dp,
                    end = 20.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                flingBehavior = flingBehavior,
                modifier = Modifier.fillMaxSize()
            ) {
                // Focus Profiles list
                itemsIndexed(profiles, key = { _, it -> it.id }) { index, profile ->
                    val isActive = profile.id == activeProfileId
                    Box(
                        modifier = Modifier.rubberBandStretch(index, totalItems, overscrollState)
                    ) {
                        ProfileRowItem(
                            profile = profile,
                            isActive = isActive,
                            onClick = {
                                viewModel.startEditingProfile(profile.id)
                                onNavigateToEditProfile(profile.id)
                            }
                        )
                    }
                }

                // Add Profile CTA Card
                item {
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(if (isPressed) 0.97f else 1f, label = "scaleAdd")

                    Box(
                        modifier = Modifier
                            .rubberBandStretch(profiles.size, totalItems, overscrollState)
                            .fillMaxWidth()
                            .height(90.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .clip(RoundedCornerShape(24.dp))
                            .dashedBorder(
                                color = if (isDark) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.2f),
                                strokeWidth = 1.5.dp,
                                cornerRadius = 24.dp
                            )
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = {
                                    viewModel.startEditingProfile(-1L)
                                    onNavigateToEditProfile(-1L)
                                }
                            )
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(
                                        color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Profile",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Add Focus Profile",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Create a custom focus space",
                                    color = if (isDark) Color.White.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.4f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class ProfileTypeConfig(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val subtitle: String,
    val accentColor: Color
)

@Composable
private fun getProfileConfig(name: String): ProfileTypeConfig {
    return when (name) {
        "Work" -> ProfileTypeConfig(
            icon = Icons.Outlined.Work,
            subtitle = "Stay focused and productive",
            accentColor = WorkAccent
        )
        "Personal" -> ProfileTypeConfig(
            icon = Icons.Outlined.Person,
            subtitle = "Balance life and personal time",
            accentColor = PersonalAccent
        )
        "Trading" -> ProfileTypeConfig(
            icon = Icons.Outlined.CandlestickChart,
            subtitle = "Market focus and analysis",
            accentColor = TradingAccent
        )
        "Study" -> ProfileTypeConfig(
            icon = Icons.AutoMirrored.Outlined.MenuBook,
            subtitle = "Learn and grow",
            accentColor = StudyAccent
        )
        "Games" -> ProfileTypeConfig(
            icon = Icons.Outlined.SportsEsports,
            subtitle = "Play and unwind",
            accentColor = GamesAccent
        )
        "Exercise" -> ProfileTypeConfig(
            icon = Icons.Outlined.FitnessCenter,
            subtitle = "Move and energize",
            accentColor = ExerciseAccent
        )
        else -> ProfileTypeConfig(
            icon = Icons.Outlined.Star,
            subtitle = "Custom focus space",
            accentColor = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun ProfileRowItem(
    profile: FocusProfile,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val config = getProfileConfig(profile.name)
    val accentColor = config.accentColor
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(if (isPressed) 0.97f else 1f, label = "scale")
    val rotation by animateFloatAsState(if (isPressed) 15f else 0f, label = "rotation")

    val cardBgColor = if (isDark) Color(0xFF121212) else Color(0xFFF8F8F8)

    val gradientBrush = Brush.horizontalGradient(
        0.0f to accentColor.copy(alpha = 0.18f),
        0.25f to accentColor.copy(alpha = 0.04f),
        0.5f to Color.Transparent
    )

    val cardModifier = Modifier
        .fillMaxWidth()
        .height(96.dp)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(
            if (isActive) {
                Modifier
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(24.dp),
                        clip = false,
                        ambientColor = accentColor.copy(alpha = 0.5f),
                        spotColor = accentColor
                    )
                    .border(1.5.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
            } else {
                Modifier.border(
                    width = 1.dp,
                    color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(24.dp)
                )
            }
        )
        .clip(RoundedCornerShape(24.dp))
        .background(cardBgColor)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )

    Box(modifier = cardModifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush)
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(accentColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = config.icon,
                        contentDescription = profile.name,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = profile.name,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp,
                            color = if (isDark) Color.White else Color.Black
                        )
                        if (isActive) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(accentColor.copy(alpha = 0.18f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Active",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = config.subtitle,
                        fontSize = 13.sp,
                        color = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Edit Profile",
                    tint = if (isDark) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer { rotationZ = rotation }
                )
            }
        }
    }
}

fun Modifier.dashedBorder(color: Color, strokeWidth: Dp, cornerRadius: Dp): Modifier = this.drawWithContent {
    drawContent()
    val strokeWidthPx = strokeWidth.toPx()
    val cornerRadiusPx = cornerRadius.toPx()
    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)
    drawRoundRect(
        color = color,
        style = Stroke(width = strokeWidthPx, pathEffect = pathEffect),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx, cornerRadiusPx)
    )
}
