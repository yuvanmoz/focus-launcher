package com.focuslauncher.feature.settings.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuslauncher.core.common.theme.PersonalAccent
import com.focuslauncher.core.common.theme.StudyAccent
import com.focuslauncher.core.common.theme.TradingAccent
import com.focuslauncher.core.common.theme.WorkAccent
import com.focuslauncher.feature.settings.SettingsViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    viewModel: SettingsViewModel,
    onNavigateToAppSelector: () -> Unit,
    onBack: () -> Unit,
    onRequestSetDefaultLauncher: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val draftProfile by viewModel.editProfile.collectAsState()
    val activeProfileId by viewModel.activeProfileId.collectAsState()
    val allowedApps by viewModel.allowedApps.collectAsState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.selectWallpaperImage(uri)
            }
        }
    )

    if (draftProfile == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Loading profile...")
        }
        return
    }

    val profile = draftProfile!!
    
    // Local state for the text field to prevent lag during typing
    var profileName by remember(profile.id) { mutableStateOf(profile.name) }

    val accentColor = when (profile.name) {
        "Work" -> WorkAccent
        "Trading" -> TradingAccent
        "Study" -> StudyAccent
        "Personal" -> PersonalAccent
        else -> MaterialTheme.colorScheme.primary
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {}, // Clean top bar without text title
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black
                )
            )
        },
        containerColor = Color.Black, // Dark/black background matching image
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Recreated Top Look card with overlapping circular icon badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                // 1. Card Container (Offset downwards to make space for the overlapping icon)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 42.dp) // half of icon size (84.dp / 2 = 42.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFF1C1C1E)) // Dark grey background matching image
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(48.dp)) // Spacer to push text below the overlapping icon
                    
                    Text(
                        text = profileName.ifEmpty { "New Mode" },
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 24.sp,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val isCurrentlyActive = activeProfileId == profile.id
                    Button(
                        onClick = {
                            if (isCurrentlyActive) {
                                viewModel.selectProfile(-99L) // Switch back to No Mode
                            } else {
                                viewModel.selectProfile(profile.id) // Turn on this mode
                                onRequestSetDefaultLauncher() // Prompt user to set Focus Launcher as default
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCurrentlyActive) Color.White else Color(0xFF4C80F1), // Bright blue pill button matching image
                            contentColor = if (isCurrentlyActive) Color.Black else Color.White
                        ),
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp),
                        modifier = Modifier.height(46.dp)
                    ) {
                        Text(
                            text = if (isCurrentlyActive) "Turn off" else "Turn on",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                // 2. Overlapping Circular Icon Badge
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .background(Color(0xFF2C2C2E), CircleShape)
                        .align(Alignment.TopCenter),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getProfileIcon(profile.name),
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Name Input Section
            Text(
                text = "Profile Name",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = profileName,
                onValueChange = {
                    profileName = it
                    viewModel.updateProfileDraft { p -> p.copy(name = it) }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Allowed Apps Section
            Text(
                text = "Allowed Apps",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                    .clickable(onClick = onNavigateToAppSelector)
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Select Focus Apps",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${allowedApps.size} apps whitelisted for this mode",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Edit Apps",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Wallpaper Selector Section
            Text(
                text = "Profile Wallpaper",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Card 1: Default (System Wallpaper)
                val isDefault = profile.wallpaperPath == null
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(110.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDefault) accentColor.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                        .border(
                            width = if (isDefault) 2.dp else 1.dp,
                            color = if (isDefault) accentColor else Color.Transparent,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { viewModel.clearCustomWallpaper() }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Home,
                            contentDescription = "System Wallpaper",
                            tint = if (isDefault) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "System Default",
                            color = if (isDefault) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                // Card 2: Custom Gallery Image
                val isCustom = profile.wallpaperPath != null
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(110.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isCustom) accentColor.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                        .border(
                            width = if (isCustom) 2.dp else 1.dp,
                            color = if (isCustom) accentColor else Color.Transparent,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCustom && profile.wallpaperPath != null) {
                        val bitmap = remember(profile.wallpaperPath) {
                            try {
                                BitmapFactory.decodeFile(profile.wallpaperPath)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Wallpaper Thumbnail",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Star,
                                contentDescription = "Choose Image",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Custom Image",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Delete Profile Button
            if (profile.id != 0L) {
                Button(
                    onClick = {
                        viewModel.deleteProfile(profile)
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Delete Focus Profile",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(padding.calculateBottomPadding() + 24.dp))
        }
    }
}

@Composable
fun getProfileIcon(name: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (name) {
        "Work" -> Icons.Outlined.Business
        "Trading" -> Icons.Outlined.TrendingUp
        "Study" -> Icons.Outlined.School
        "Personal" -> Icons.Outlined.Person
        else -> Icons.Outlined.Star
    }
}
