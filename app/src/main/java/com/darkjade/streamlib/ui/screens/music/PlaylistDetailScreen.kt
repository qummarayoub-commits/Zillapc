package com.darkjade.streamlib.ui.screens.music

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.darkjade.streamlib.data.db.entity.SongEntity
import com.darkjade.streamlib.ui.components.EmptyState
import com.darkjade.streamlib.ui.theme.VaultColors
import com.darkjade.streamlib.ui.theme.VaultShapes
import com.darkjade.streamlib.ui.theme.VaultSpacing

@Composable
private fun CollageCell(artworkPath: String?, modifier: Modifier) {
    Box(modifier = modifier.background(VaultColors.Background.copy(alpha = 0.3f))) {
        if (artworkPath != null) {
            SubcomposeAsyncImage(
                model = artworkPath,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {},
                error = {},
            )
        }
    }
}

@Composable
fun PlaylistDetailScreen(
    viewModel: PlaylistDetailViewModel,
    onBack: () -> Unit,
    onPlaySong: (SongEntity, List<SongEntity>) -> Unit,
    onPlayShuffled: (List<SongEntity>) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(VaultColors.Background)) {
        Row(modifier = Modifier.fillMaxWidth().padding(VaultSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = VaultColors.TextPrimary)
            }
            Text(
                state.playlist?.name ?: "",
                style = MaterialTheme.typography.titleMedium,
                color = VaultColors.TextPrimary,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete playlist", tint = VaultColors.TextSecondary)
            }
        }

        if (state.songs.isEmpty()) {
            EmptyState(title = "No songs yet", message = "Add songs from the Music tab via \u201cAdd to Playlist\u201d.")
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(VaultSpacing.lg)) {
                        val distinctArtworks = state.songs.mapNotNull { it.artworkPath }.distinct().take(4)
                        Box(
                            modifier = Modifier.size(180.dp).clip(RoundedCornerShape(12.dp)).background(VaultColors.SurfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            when {
                                distinctArtworks.size >= 2 -> {
                                    // Simple collage — up to 4 artworks in a 2x2 grid.
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                            CollageCell(distinctArtworks.getOrNull(0), Modifier.weight(1f).fillMaxHeight())
                                            CollageCell(distinctArtworks.getOrNull(1), Modifier.weight(1f).fillMaxHeight())
                                        }
                                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                            CollageCell(distinctArtworks.getOrNull(2), Modifier.weight(1f).fillMaxHeight())
                                            CollageCell(distinctArtworks.getOrNull(3), Modifier.weight(1f).fillMaxHeight())
                                        }
                                    }
                                }
                                distinctArtworks.size == 1 -> {
                                    SubcomposeAsyncImage(
                                        model = distinctArtworks[0],
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                        loading = { Icon(Icons.Filled.QueueMusic, contentDescription = null, tint = VaultColors.TextTertiary, modifier = Modifier.size(48.dp)) },
                                        error = { Icon(Icons.Filled.QueueMusic, contentDescription = null, tint = VaultColors.TextTertiary, modifier = Modifier.size(48.dp)) },
                                    )
                                }
                                else -> {
                                    Icon(Icons.Filled.QueueMusic, contentDescription = null, tint = VaultColors.TextTertiary, modifier = Modifier.size(48.dp))
                                }
                            }
                        }
                        Text(
                            "${state.songs.size} songs",
                            style = MaterialTheme.typography.bodyMedium,
                            color = VaultColors.TextSecondary,
                            modifier = Modifier.padding(top = VaultSpacing.sm)
                        )
                        Row(modifier = Modifier.padding(top = VaultSpacing.md)) {
                            Button(
                                onClick = { onPlaySong(state.songs.first(), state.songs) },
                                shape = VaultShapes.button,
                                colors = ButtonDefaults.buttonColors(containerColor = VaultColors.Orange, contentColor = VaultColors.Background),
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text(" Play", modifier = Modifier.padding(start = 4.dp))
                            }
                            OutlinedButton(
                                onClick = { onPlayShuffled(state.songs) },
                                shape = VaultShapes.button,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = VaultColors.TextPrimary),
                                modifier = Modifier.padding(start = VaultSpacing.sm)
                            ) {
                                Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text(" Shuffle", modifier = Modifier.padding(start = 4.dp))
                            }
                        }
                    }
                }

                items(state.songs, key = { it.id }) { song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlaySong(song, state.songs) }
                            .padding(horizontal = VaultSpacing.md, vertical = VaultSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)).background(VaultColors.SurfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (song.artworkPath != null) {
                                SubcomposeAsyncImage(
                                    model = song.artworkPath,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
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
                        IconButton(onClick = { viewModel.removeSong(song.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Remove from playlist", tint = VaultColors.TextTertiary)
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = VaultColors.Surface,
            title = { Text("Delete playlist?", color = VaultColors.TextPrimary) },
            text = { Text("This removes \"${state.playlist?.name}\". Your songs are not deleted.", color = VaultColors.TextSecondary) },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; viewModel.deletePlaylist(onDeleted = onBack) }) {
                    Text("Delete", color = VaultColors.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel", color = VaultColors.TextSecondary) }
            }
        )
    }
}
