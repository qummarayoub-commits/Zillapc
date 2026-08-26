package com.darkjade.streamlib.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.darkjade.streamlib.ui.theme.VaultColors
import com.darkjade.streamlib.ui.theme.VaultSpacing

@Composable
fun MiniPlayerBar(
    state: MusicPlayerUiState,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onTap: () -> Unit,
) {
    val song = state.currentSong ?: return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .background(VaultColors.Surface)
    ) {
        val progress = if (state.durationMs > 0) (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f) else 0f
        LinearProgressIndicator(
            progress = progress,
            color = VaultColors.Orange,
            trackColor = VaultColors.SurfaceVariant,
            modifier = Modifier.fillMaxWidth().height(2.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = VaultSpacing.sm, vertical = VaultSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)).background(VaultColors.SurfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (song.artworkPath != null) {
                    SubcomposeAsyncImage(
                        model = song.artworkPath,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        loading = { Icon(Icons.Filled.MusicNote, contentDescription = null, tint = VaultColors.TextTertiary) },
                        error = { Icon(Icons.Filled.MusicNote, contentDescription = null, tint = VaultColors.TextTertiary) },
                    )
                } else {
                    Icon(Icons.Filled.MusicNote, contentDescription = null, tint = VaultColors.TextTertiary)
                }
            }
            Column(modifier = Modifier.padding(start = VaultSpacing.sm).weight(1f)) {
                Text(song.title, style = MaterialTheme.typography.bodyMedium, color = VaultColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artist, style = MaterialTheme.typography.labelSmall, color = VaultColors.TextTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onTogglePlayPause) {
                Icon(
                    if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    tint = VaultColors.TextPrimary,
                )
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = VaultColors.TextPrimary)
            }
        }
    }
}
