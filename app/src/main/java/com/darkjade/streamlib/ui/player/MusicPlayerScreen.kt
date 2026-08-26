package com.darkjade.streamlib.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.darkjade.streamlib.ui.theme.VaultColors
import com.darkjade.streamlib.ui.theme.VaultSpacing

@Composable
fun MusicPlayerScreen(
    viewModel: MusicPlayerViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val song = state.currentSong
    val context = androidx.compose.ui.platform.LocalContext.current

    // Dark tint pulled from the album art itself — same premium "ambient
    // background" treatment modern music players use, not a flat black.
    val tintColor by androidx.compose.runtime.produceState<androidx.compose.ui.graphics.Color?>(initialValue = null, song?.artworkPath) {
        value = com.darkjade.streamlib.ui.util.ArtworkTintExtractor.extractTint(context, song?.artworkPath)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    0f to (tintColor?.copy(alpha = 0.8f) ?: VaultColors.Background),
                    0.6f to VaultColors.Background,
                    1f to VaultColors.Background,
                )
            )
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(VaultSpacing.xs)) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = VaultColors.TextPrimary)
            }
        }

        if (song == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nothing playing", color = VaultColors.TextSecondary)
            }
            return@Column
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(VaultSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .size(300.dp)
                    .padding(VaultSpacing.xs)
                    .shadow(elevation = 12.dp, shape = RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(VaultColors.SurfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (song.artworkPath != null) {
                    SubcomposeAsyncImage(
                        model = song.artworkPath,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = { Icon(Icons.Filled.MusicNote, contentDescription = null, tint = VaultColors.TextTertiary, modifier = Modifier.size(64.dp)) },
                        error = { Icon(Icons.Filled.MusicNote, contentDescription = null, tint = VaultColors.TextTertiary, modifier = Modifier.size(64.dp)) },
                    )
                } else {
                    Icon(Icons.Filled.MusicNote, contentDescription = null, tint = VaultColors.TextTertiary, modifier = Modifier.size(64.dp))
                }
            }

            Text(
                song.title,
                style = MaterialTheme.typography.headlineSmall,
                color = VaultColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = VaultSpacing.lg)
            )
            Text(
                song.artist,
                style = MaterialTheme.typography.bodyLarge,
                color = VaultColors.TextSecondary,
                modifier = Modifier.padding(top = VaultSpacing.xxs)
            )

            Slider(
                value = state.positionMs.toFloat(),
                valueRange = 0f..(state.durationMs.toFloat().coerceAtLeast(1f)),
                onValueChange = { viewModel.seekTo(it.toLong()) },
                colors = SliderDefaults.colors(thumbColor = VaultColors.Orange, activeTrackColor = VaultColors.Orange, inactiveTrackColor = VaultColors.SurfaceVariant),
                modifier = Modifier.fillMaxWidth().padding(top = VaultSpacing.md)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatMs(state.positionMs), style = MaterialTheme.typography.labelSmall, color = VaultColors.TextTertiary)
                Text("-" + formatMs((state.durationMs - state.positionMs).coerceAtLeast(0)), style = MaterialTheme.typography.labelSmall, color = VaultColors.TextTertiary)
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = VaultSpacing.lg),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { viewModel.toggleShuffle() }) {
                    Icon(
                        Icons.Filled.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (state.shuffleEnabled) VaultColors.Orange else VaultColors.TextTertiary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(onClick = { viewModel.previous() }) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = VaultColors.TextPrimary, modifier = Modifier.size(36.dp))
                }
                IconButton(
                    onClick = { viewModel.togglePlayPause() },
                    modifier = Modifier.size(64.dp).clip(CircleShape).background(VaultColors.Orange)
                ) {
                    Icon(
                        if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        tint = VaultColors.Background,
                        modifier = Modifier.size(32.dp)
                    )
                }
                IconButton(onClick = { viewModel.next() }) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = VaultColors.TextPrimary, modifier = Modifier.size(36.dp))
                }
                IconButton(onClick = { viewModel.cycleRepeatMode() }) {
                    Icon(
                        if (state.repeatMode == androidx.media3.common.Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        contentDescription = "Repeat",
                        tint = if (state.repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF) VaultColors.Orange else VaultColors.TextTertiary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}
