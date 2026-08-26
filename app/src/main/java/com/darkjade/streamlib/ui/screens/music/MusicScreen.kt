package com.darkjade.streamlib.ui.screens.music

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.darkjade.streamlib.data.db.dao.AlbumSummary
import com.darkjade.streamlib.data.db.dao.PlaylistSummary
import com.darkjade.streamlib.data.db.entity.SongEntity
import com.darkjade.streamlib.ui.components.EmptyState
import com.darkjade.streamlib.ui.theme.VaultColors
import com.darkjade.streamlib.ui.theme.VaultShapes
import com.darkjade.streamlib.ui.theme.VaultSpacing

@Composable
fun MusicScreen(
    viewModel: MusicViewModel,
    onOpenAlbum: (String, String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onPlaySong: (SongEntity) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var addToPlaylistSong by remember { mutableStateOf<SongEntity?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(VaultColors.Background)) {
        when {
            state.isLoading -> CircularProgressIndicator(color = VaultColors.Orange, modifier = Modifier.align(Alignment.Center))
            state.allSongs.isEmpty() -> EmptyState(
                title = "No music yet",
                message = "Add a music folder in Sources to scan for songs.",
            )
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = VaultSpacing.xl)) {
                    item {
                        Text(
                            "Music",
                            style = MaterialTheme.typography.headlineSmall,
                            color = VaultColors.TextPrimary,
                            modifier = Modifier.padding(horizontal = VaultSpacing.md, vertical = VaultSpacing.md)
                        )
                    }

                    item {
                        SectionHeader("Playlists")
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(VaultSpacing.sm),
                            contentPadding = PaddingValues(horizontal = VaultSpacing.md)
                        ) {
                            items(state.playlists, key = { it.id }) { playlist ->
                                PlaylistCard(playlist = playlist, onClick = { onOpenPlaylist(playlist.id) })
                            }
                            item {
                                Column(
                                    modifier = Modifier.width(110.dp).clickable { showCreatePlaylistDialog = true },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Box(
                                        modifier = Modifier.size(110.dp).clip(RoundedCornerShape(6.dp)).background(VaultColors.SurfaceVariant),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(Icons.Filled.Add, contentDescription = "New playlist", tint = VaultColors.Orange)
                                    }
                                    Text("New Playlist", style = MaterialTheme.typography.bodySmall, color = VaultColors.TextSecondary, maxLines = 1, modifier = Modifier.padding(top = VaultSpacing.xxs))
                                }
                            }
                        }
                    }

                    if (state.recentlyAdded.isNotEmpty()) {
                        item {
                            SectionHeader("Recently Added")
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(VaultSpacing.sm),
                                contentPadding = PaddingValues(horizontal = VaultSpacing.md)
                            ) {
                                items(state.recentlyAdded, key = { it.id }) { song ->
                                    SongArtCard(song = song, onClick = { onPlaySong(song) })
                                }
                            }
                        }
                    }

                    if (state.albums.isNotEmpty()) {
                        item {
                            SectionHeader("Albums")
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(VaultSpacing.sm),
                                contentPadding = PaddingValues(horizontal = VaultSpacing.md)
                            ) {
                                items(state.albums, key = { it.album + it.artist }) { album ->
                                    AlbumCard(album = album, onClick = { onOpenAlbum(album.album, album.artist) })
                                }
                            }
                        }
                    }

                    if (state.artists.isNotEmpty()) {
                        item { SectionHeader("Artists") }
                        items(state.artists, key = { it.artist }) { artist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenArtist(artist.artist) }
                                    .padding(horizontal = VaultSpacing.md, vertical = VaultSpacing.sm),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier.size(44.dp).clip(CircleShape).background(VaultColors.SurfaceVariant),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Filled.Person, contentDescription = null, tint = VaultColors.TextTertiary)
                                }
                                Column(modifier = Modifier.padding(start = VaultSpacing.sm)) {
                                    Text(artist.artist, style = MaterialTheme.typography.bodyLarge, color = VaultColors.TextPrimary)
                                    Text("${artist.trackCount} songs", style = MaterialTheme.typography.labelSmall, color = VaultColors.TextTertiary)
                                }
                            }
                        }
                    }

                    item { SectionHeader("Songs") }
                    items(state.allSongs, key = { it.id }) { song ->
                        SongRow(song = song, onClick = { onPlaySong(song) }, onAddToPlaylist = { addToPlaylistSong = song })
                    }
                }
            }
        }
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onCreate = { name -> viewModel.createPlaylist(name); showCreatePlaylistDialog = false },
            onDismiss = { showCreatePlaylistDialog = false },
        )
    }

    addToPlaylistSong?.let { song ->
        AddToPlaylistDialog(
            playlists = state.playlists,
            onPick = { playlistId -> viewModel.addSongToPlaylist(playlistId, song.id); addToPlaylistSong = null },
            onCreateNew = { addToPlaylistSong = null; showCreatePlaylistDialog = true },
            onDismiss = { addToPlaylistSong = null },
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = VaultColors.TextPrimary,
        modifier = Modifier.padding(horizontal = VaultSpacing.md, vertical = VaultSpacing.sm)
    )
}

@Composable
private fun ArtworkBox(path: String?, fallbackIcon: ImageVector, size: Dp) {
    Box(
        modifier = Modifier.size(size).clip(RoundedCornerShape(6.dp)).background(VaultColors.SurfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (path != null) {
            SubcomposeAsyncImage(
                model = path,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { Icon(fallbackIcon, contentDescription = null, tint = VaultColors.TextTertiary) },
                error = { Icon(fallbackIcon, contentDescription = null, tint = VaultColors.TextTertiary) },
            )
        } else {
            Icon(fallbackIcon, contentDescription = null, tint = VaultColors.TextTertiary)
        }
    }
}

@Composable
private fun PlaylistCard(playlist: PlaylistSummary, onClick: () -> Unit) {
    Column(modifier = Modifier.width(110.dp).clickable(onClick = onClick)) {
        ArtworkBox(playlist.artworkPath, Icons.Filled.QueueMusic, 110.dp)
        Text(playlist.name, style = MaterialTheme.typography.bodySmall, color = VaultColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = VaultSpacing.xxs))
        Text("${playlist.songCount} songs", style = MaterialTheme.typography.labelSmall, color = VaultColors.TextTertiary, maxLines = 1)
    }
}

@Composable
private fun SongArtCard(song: SongEntity, onClick: () -> Unit) {
    Column(modifier = Modifier.width(110.dp).clickable(onClick = onClick)) {
        ArtworkBox(song.artworkPath, Icons.Filled.MusicNote, 110.dp)
        Text(song.title, style = MaterialTheme.typography.bodySmall, color = VaultColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = VaultSpacing.xxs))
        Text(song.artist, style = MaterialTheme.typography.labelSmall, color = VaultColors.TextTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun AlbumCard(album: AlbumSummary, onClick: () -> Unit) {
    Column(modifier = Modifier.width(110.dp).clickable(onClick = onClick)) {
        ArtworkBox(album.artworkPath, Icons.Filled.Album, 110.dp)
        Text(album.album, style = MaterialTheme.typography.bodySmall, color = VaultColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = VaultSpacing.xxs))
        Text(album.artist, style = MaterialTheme.typography.labelSmall, color = VaultColors.TextTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Artwork | Song Title | Artist | Duration + a "More" menu with Add to Playlist. */
@Composable
private fun SongRow(song: SongEntity, onClick: () -> Unit, onAddToPlaylist: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = VaultSpacing.md, vertical = VaultSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtworkBox(song.artworkPath, Icons.Filled.MusicNote, 44.dp)
        Column(modifier = Modifier.padding(start = VaultSpacing.sm).weight(1f)) {
            Text(song.title, style = MaterialTheme.typography.bodyMedium, color = VaultColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artist, style = MaterialTheme.typography.labelSmall, color = VaultColors.TextTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(formatDuration(song.durationMs), style = MaterialTheme.typography.labelSmall, color = VaultColors.TextTertiary, modifier = Modifier.padding(end = VaultSpacing.xs))
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More options", tint = VaultColors.TextTertiary)
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Add to Playlist") },
                    onClick = { showMenu = false; onAddToPlaylist() }
                )
            }
        }
    }
}

@Composable
private fun CreatePlaylistDialog(onCreate: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VaultColors.Surface,
        title = { Text("New Playlist", color = VaultColors.TextPrimary) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Playlist name") },
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
            TextButton(onClick = { onCreate(name) }) { Text("Create", color = VaultColors.Orange) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = VaultColors.TextSecondary) }
        }
    )
}

@Composable
private fun AddToPlaylistDialog(playlists: List<PlaylistSummary>, onPick: (Long) -> Unit, onCreateNew: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VaultColors.Surface,
        title = { Text("Add to Playlist", color = VaultColors.TextPrimary) },
        text = {
            Column {
                playlists.forEach { playlist ->
                    Text(
                        playlist.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = VaultColors.TextPrimary,
                        modifier = Modifier.fillMaxWidth().clickable { onPick(playlist.id) }.padding(vertical = VaultSpacing.sm)
                    )
                }
                Text(
                    "+ Create New Playlist",
                    style = MaterialTheme.typography.bodyLarge,
                    color = VaultColors.Orange,
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onCreateNew).padding(vertical = VaultSpacing.sm)
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = VaultColors.TextSecondary) }
        }
    )
}

private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}
