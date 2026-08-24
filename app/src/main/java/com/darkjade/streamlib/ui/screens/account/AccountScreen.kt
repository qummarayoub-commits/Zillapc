package com.darkjade.streamlib.ui.screens.account

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.darkjade.streamlib.R
import com.darkjade.streamlib.ui.theme.VaultColors
import com.darkjade.streamlib.ui.theme.VaultSpacing

@Composable
fun AccountScreen(
    viewModel: AccountViewModel,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showBannerAdjustDialog by remember { mutableStateOf(false) }

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: SecurityException) { /* non-fatal */ }
            viewModel.setAvatar(uri.toString())
        }
    }

    val bannerPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: SecurityException) { /* non-fatal */ }
            viewModel.setBanner(uri.toString())
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultColors.Background)
    ) {
        item {
            // Cover/banner image — edit icon picks a new image, move icon
            // opens the pinch/drag adjustment view for the current one.
            Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                val bannerUri = state.activeProfile?.bannerRes
                val bannerScale = state.activeProfile?.bannerScale ?: 1f
                val bannerOffsetX = state.activeProfile?.bannerOffsetX ?: 0f
                val bannerOffsetY = state.activeProfile?.bannerOffsetY ?: 0f
                if (bannerUri != null) {
                    SubcomposeAsyncImage(
                        model = bannerUri,
                        contentDescription = "Profile banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = bannerScale,
                                scaleY = bannerScale,
                                translationX = bannerOffsetX,
                                translationY = bannerOffsetY,
                            ),
                        loading = { Box(Modifier.fillMaxSize().background(VaultColors.SurfaceVariant)) },
                        error = { Box(Modifier.fillMaxSize().background(VaultColors.SurfaceVariant)) },
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(
                            Brush.linearGradient(listOf(VaultColors.OrangeDim, VaultColors.Background))
                        )
                    )
                }
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(Color.Transparent, VaultColors.Background))
                    )
                )
                Row(modifier = Modifier.align(Alignment.TopEnd).padding(VaultSpacing.sm)) {
                    if (bannerUri != null) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .clickable { showBannerAdjustDialog = true },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.OpenWith, contentDescription = "Adjust banner", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(VaultSpacing.xs))
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable {
                                bannerPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = "Change banner", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
                Row(modifier = Modifier.align(Alignment.TopStart).padding(VaultSpacing.sm)) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.clickable(onClick = onBack)
                    )
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(start = VaultSpacing.md)
                            .clickable(onClick = onOpenSettings)
                    )
                }
            }
        }

        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = VaultSpacing.sm)) {
                Box(modifier = Modifier.size(88.dp)) {
                    val avatarUri = state.activeProfile?.avatarRes
                    if (avatarUri != null) {
                        SubcomposeAsyncImage(
                            model = avatarUri,
                            contentDescription = "Profile picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            loading = { DefaultAvatar() },
                            error = { DefaultAvatar() },
                        )
                    } else {
                        DefaultAvatar()
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(VaultColors.Orange)
                            .clickable {
                                avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = "Change picture", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = VaultSpacing.sm).clickable { showEditNameDialog = true }
                ) {
                    Text(
                        state.activeProfile?.name ?: "Profile",
                        style = MaterialTheme.typography.titleLarge,
                        color = VaultColors.TextPrimary,
                    )
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Edit name",
                        tint = VaultColors.TextTertiary,
                        modifier = Modifier.padding(start = VaultSpacing.xs).size(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(VaultSpacing.lg))
        }

        item {
            SettingsRow(
                label = "Content Restrictions",
                value = state.activeProfile?.contentRestriction ?: "None",
                onClick = { viewModel.cycleContentRestriction() }
            )
            Divider(color = VaultColors.Divider)
            SettingsRow(
                label = "Audio Language",
                value = state.activeProfile?.audioLanguage ?: "English",
                onClick = { viewModel.cycleAudioLanguage() }
            )
            Divider(color = VaultColors.Divider)
            SettingsRow(
                label = "Subtitles/CC Language",
                value = state.activeProfile?.subtitleLanguage ?: "English",
                onClick = { viewModel.cycleSubtitleLanguage() }
            )
            Divider(color = VaultColors.Divider)
            SettingsRow(label = "Library Sources & Scan", onClick = onOpenSettings)
            Divider(color = VaultColors.Divider)
            SettingsRow(label = "About")
        }
    }

    if (showEditNameDialog) {
        EditNameDialog(
            currentName = state.activeProfile?.name.orEmpty(),
            onSave = {
                viewModel.setUsername(it)
                showEditNameDialog = false
            },
            onDismiss = { showEditNameDialog = false },
        )
    }

    if (showBannerAdjustDialog && state.activeProfile?.bannerRes != null) {
        BannerAdjustDialog(
            bannerUri = state.activeProfile!!.bannerRes!!,
            initialScale = state.activeProfile!!.bannerScale,
            initialOffsetX = state.activeProfile!!.bannerOffsetX,
            initialOffsetY = state.activeProfile!!.bannerOffsetY,
            onSave = { scale, offsetX, offsetY ->
                viewModel.setBannerAdjustment(scale, offsetX, offsetY)
                showBannerAdjustDialog = false
            },
            onDismiss = { showBannerAdjustDialog = false },
        )
    }
}

/** Full-screen pinch-zoom / drag adjustment for the profile banner image. */
@Composable
private fun BannerAdjustDialog(
    bannerUri: String,
    initialScale: Float,
    initialOffsetX: Float,
    initialOffsetY: Float,
    onSave: (scale: Float, offsetX: Float, offsetY: Float) -> Unit,
    onDismiss: () -> Unit,
) {
    var scale by remember { mutableStateOf(initialScale) }
    var offsetX by remember { mutableStateOf(initialOffsetX) }
    var offsetY by remember { mutableStateOf(initialOffsetY) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .align(Alignment.Center)
                .pointerInput(Unit) {
                    // Same safe multi-touch-only gesture handling used in the
                    // comic reader — single-finger drags still pan the image
                    // here (no sibling pager to conflict with), pinch zooms.
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        do {
                            val event = awaitPointerEvent()
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            scale = (scale * zoomChange).coerceIn(1f, 3f)
                            offsetX += panChange.x
                            offsetY += panChange.y
                            event.changes.forEach { it.consume() }
                        } while (event.changes.any { it.pressed })
                    }
                }
        ) {
            SubcomposeAsyncImage(
                model = bannerUri,
                contentDescription = "Adjust banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offsetX, translationY = offsetY),
                loading = {},
                error = {},
            )
        }

        Text(
            "Pinch to zoom, drag to reposition",
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp)
        )

        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(VaultSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(VaultSpacing.sm),
        ) {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = VaultColors.TextSecondary)
            }
            Button(
                onClick = { onSave(scale, offsetX, offsetY) },
                colors = ButtonDefaults.buttonColors(containerColor = VaultColors.Orange, contentColor = VaultColors.Background)
            ) {
                Text("Save")
            }
        }
    }
}

@Composable
private fun EditNameDialog(currentName: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VaultColors.Surface,
        title = { Text("Edit name", color = VaultColors.TextPrimary) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VaultColors.Orange,
                    unfocusedBorderColor = VaultColors.Divider,
                    focusedTextColor = VaultColors.TextPrimary,
                    unfocusedTextColor = VaultColors.TextPrimary,
                    cursorColor = VaultColors.Orange,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name) }) { Text("Save", color = VaultColors.Orange) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = VaultColors.TextSecondary) }
        }
    )
}

/** Shown until the user picks their own profile picture. */
@Composable
private fun DefaultAvatar() {
    androidx.compose.foundation.Image(
        painter = painterResource(R.drawable.default_avatar),
        contentDescription = "Default profile picture",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize().clip(CircleShape)
    )
}

@Composable
private fun SettingsRow(label: String, value: String? = null, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = VaultSpacing.md, vertical = VaultSpacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = VaultColors.TextPrimary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            value?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = VaultColors.TextSecondary)
                Spacer(Modifier.width(VaultSpacing.xxs))
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = VaultColors.TextTertiary)
        }
    }
}
