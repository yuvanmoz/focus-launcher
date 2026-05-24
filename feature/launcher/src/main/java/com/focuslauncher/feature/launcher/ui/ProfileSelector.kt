package com.focuslauncher.feature.launcher.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuslauncher.core.common.theme.GlassOverlayDark
import com.focuslauncher.core.common.theme.GlassOverlayLight
import com.focuslauncher.core.common.theme.PersonalAccent
import com.focuslauncher.core.common.theme.StudyAccent
import com.focuslauncher.core.common.theme.TradingAccent
import com.focuslauncher.core.common.theme.WorkAccent
import com.focuslauncher.core.model.FocusProfile

@Composable
fun ProfileSwitcherDock(
    profiles: List<FocusProfile>,
    activeProfileId: Long,
    onProfileSelected: (Long) -> Unit,
    darkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (darkTheme) GlassOverlayDark else GlassOverlayLight
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            items(profiles, key = { it.id }) { profile ->
                val isActive = profile.id == activeProfileId
                val accentColor = when (profile.name) {
                    "Work" -> WorkAccent
                    "Trading" -> TradingAccent
                    "Study" -> StudyAccent
                    "Personal" -> PersonalAccent
                    else -> MaterialTheme.colorScheme.primary
                }

                // Dynamic background & text color animations
                val itemBgColor by animateColorAsState(
                    targetValue = if (isActive) accentColor else Color.Transparent,
                    label = "ItemBgColor"
                )
                val itemTextColor by animateColorAsState(
                    targetValue = if (isActive) Color.White else MaterialTheme.colorScheme.onBackground,
                    label = "ItemTextColor"
                )
                val dynamicPadding by animateDpAsState(
                    targetValue = if (isActive) 16.dp else 12.dp,
                    label = "ItemPadding"
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(itemBgColor)
                        .clickable { onProfileSelected(profile.id) }
                        .padding(horizontal = dynamicPadding, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profile.name,
                        color = itemTextColor,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
