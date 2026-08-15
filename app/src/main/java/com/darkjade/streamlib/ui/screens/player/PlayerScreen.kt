package com.darkjade.streamlib.ui.screens.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.Build
import android.util.Rational
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.ui.PlayerView
import com.darkjade.streamlib.ui.theme.VaultColors
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val view = LocalView.current

    var isLocked by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showAudioMenu by remember { mutableStateOf(false) }
    var showVolumeSlider by remember { mutableStateOf(false) }
    var showBrightnessSlider by remember { mutableStateOf(false) }

    // Every control (top bar + bottom bar) shows/hides together, driven only
    // by tapping the video — fully owned here instead of depending on
    // PlayerView's built-in controller, which is what caused the previous
    // bugs (seek bar/rewind/forward not responding, controls stuck visible).
    var controlsVisible by remember { mutableStateOf(true) }
    var unlockButtonVisible by remember { mutableStateOf(false) }

    // Auto-hide controls after a few seconds while playing, same as any
    // standard video player. Any tap toggles this back on/off immediately.
    LaunchedEffect(controlsVisible, state.isPlaying) {
        if (controlsVisible && state.isPlaying) {
            delay(3500)
            controlsVisible = false
        }
    }

    LaunchedEffect(unlockButtonVisible) {
        if (unlockButtonVisible) {
            delay(3000)
            unlockButtonVisible = false
        }
    }

    val audioManager = remember { context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var volumeLevel by remember { mutableStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume) }
    var brightnessLevel by remember {
        mutableStateOf(activity?.window?.attributes?.screenBrightness?.takeIf { it in 0f..1f } ?: 0.5f)
    }

    // While the user is actively dragging the seek bar, show the drag
    // position instead of fighting with the live position updates.
    var isDraggingSeek by remember { mutableStateOf(false) }
    var dragPositionMs by remember { mutableStateOf(0L) }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.saveProgressNow()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            restoreImmersive(view, restore = true)
            activity?.window?.let { window ->
                val attrs = window.attributes
                attrs.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = attrs
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = viewModel.player
                    useController = false // fully custom controls below
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isLocked) {
                    detectTapGestures {
                        if (isLocked) {
                            unlockButtonVisible = !unlockButtonVisible
                        } else {
                            controlsVisible = !controlsVisible
                        }
                    }
                }
        )

        when {
            state.isLoading -> CircularProgressIndicator(
                color = VaultColors.Orange,
                modifier = Modifier.align(Alignment.Center)
            )
            state.errorMessage != null -> Column(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Playback error", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text(
                    state.errorMessage.orEmpty(),
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    "Go back",
                    color = VaultColors.Orange,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 16.dp).clickable(onClick = onBack)
                )
            }
        }

        if (isLocked) {
            if (unlockButtonVisible) {
                IconButton(
                    onClick = { isLocked = false; unlockButtonVisible = false },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .size(44.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Filled.LockOpen, contentDescription = "Unlock", tint = Color.White)
                }
            }
        } else if (controlsVisible && state.errorMessage == null) {
            // Top overlay: back, title, lock, audio, speed, volume, brightness, fullscreen, PiP.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text(
                        state.title,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                }

                IconButton(onClick = { isLocked = true; controlsVisible = false }) {
                    Icon(Icons.Filled.Lock, contentDescription = "Lock", tint = Color.White)
                }

                if (state.audioTracks.size > 1) {
                    Box {
                        IconButton(onClick = { showAudioMenu = true }) {
                            Icon(Icons.Filled.GraphicEq, contentDescription = "Audio track", tint = Color.White)
                        }
                        DropdownMenu(expanded = showAudioMenu, onDismissRequest = { showAudioMenu = false }) {
                            state.audioTracks.forEach { track ->
                                DropdownMenuItem(
                                    text = { Text(if (track.isSelected) "${track.label} \u2713" else track.label) },
                                    onClick = {
                                        viewModel.selectAudioTrack(track)
                                        showAudioMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                Box {
                    IconButton(onClick = { showSpeedMenu = true }) {
                        Icon(Icons.Filled.Speed, contentDescription = "Playback speed", tint = Color.White)
                    }
                    DropdownMenu(expanded = showSpeedMenu, onDismissRequest = { showSpeedMenu = false }) {
                        listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { speed ->
                            DropdownMenuItem(
                                text = { Text("${speed}x") },
                                onClick = {
                                    viewModel.setPlaybackSpeed(speed)
                                    showSpeedMenu = false
                                }
                            )
                        }
                    }
                }

                IconButton(onClick = {
                    showVolumeSlider = !showVolumeSlider
                    showBrightnessSlider = false
                }) {
                    Icon(Icons.Filled.VolumeUp, contentDescription = "Volume", tint = Color.White)
                }

                IconButton(onClick = {
                    showBrightnessSlider = !showBrightnessSlider
                    showVolumeSlider = false
                }) {
                    Icon(Icons.Filled.Brightness6, contentDescription = "Brightness", tint = Color.White)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
                    IconButton(onClick = { enterPip(activity) }) {
                        Icon(Icons.Filled.PictureInPicture, contentDescription = "Picture in picture", tint = Color.White)
                    }
                }

                IconButton(onClick = {
                    isFullscreen = !isFullscreen
                    activity?.requestedOrientation = if (isFullscreen)
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    else
                        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    restoreImmersive(view, restore = !isFullscreen)
                }) {
                    Icon(
                        if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                        contentDescription = "Fullscreen",
                        tint = Color.White
                    )
                }
            }

            if (showVolumeSlider) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 56.dp, end = 16.dp)
                        .width(160.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text("Volume", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = volumeLevel,
                        onValueChange = {
                            volumeLevel = it
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (it * maxVolume).toInt(), 0)
                        },
                        colors = SliderDefaults.colors(thumbColor = VaultColors.Orange, activeTrackColor = VaultColors.Orange)
                    )
                }
            }

            if (showBrightnessSlider) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 56.dp, end = 16.dp)
                        .width(160.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text("Brightness", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = brightnessLevel,
                        onValueChange = { level ->
                            brightnessLevel = level
                            activity?.window?.let { window ->
                                val attrs = window.attributes
                                attrs.screenBrightness = level.coerceIn(0.01f, 1f)
                                window.attributes = attrs
                            }
                        },
                        colors = SliderDefaults.colors(thumbColor = VaultColors.Orange, activeTrackColor = VaultColors.Orange)
                    )
                }
            }

            // Bottom: play/pause, rewind/forward 10s, seek bar, time.
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { viewModel.seekBy(-10_000) }) {
                        Icon(Icons.Filled.Replay10, contentDescription = "Rewind 10 seconds", tint = Color.White)
                    }
                    IconButton(
                        onClick = { viewModel.togglePlayPause() },
                        modifier = Modifier.padding(horizontal = 24.dp).size(56.dp)
                    ) {
                        Icon(
                            if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    IconButton(onClick = { viewModel.seekBy(10_000) }) {
                        Icon(Icons.Filled.Forward10, contentDescription = "Forward 10 seconds", tint = Color.White)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    val displayPosition = if (isDraggingSeek) dragPositionMs else state.positionMs
                    Text(
                        formatTime(displayPosition),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Slider(
                        value = displayPosition.toFloat().coerceIn(0f, state.durationMs.toFloat().coerceAtLeast(1f)),
                        valueRange = 0f..state.durationMs.toFloat().coerceAtLeast(1f),
                        enabled = state.durationMs > 0,
                        onValueChange = {
                            isDraggingSeek = true
                            dragPositionMs = it.toLong()
                        },
                        onValueChangeFinished = {
                            viewModel.seekTo(dragPositionMs)
                            isDraggingSeek = false
                        },
                        colors = SliderDefaults.colors(thumbColor = VaultColors.Orange, activeTrackColor = VaultColors.Orange),
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                    Text(
                        formatTime(state.durationMs),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%d:%02d", m, s)
}

private fun enterPip(activity: Activity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        try {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            activity.enterPictureInPictureMode(params)
        } catch (e: Exception) {
            // Device/manufacturer doesn't support PiP here — fail silently rather than crash.
        }
    }
}

private fun restoreImmersive(view: android.view.View, restore: Boolean) {
    val window = (view.context as? Activity)?.window ?: return
    WindowCompat.setDecorFitsSystemWindows(window, restore)
    val controller = WindowInsetsControllerCompat(window, view)
    if (restore) {
        controller.show(WindowInsetsCompat.Type.systemBars())
    } else {
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}
