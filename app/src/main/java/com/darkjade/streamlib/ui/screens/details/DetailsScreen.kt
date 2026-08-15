package com.darkjade.streamlib.ui.screens.details

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.darkjade.streamlib.data.db.entity.EpisodeEntity
import com.darkjade.streamlib.data.metadata.isSeriesLike
import com.darkjade.streamlib.ui.components.EmptyState
import com.darkjade.streamlib.ui.components.FallbackPoster
import com.darkjade.streamlib.ui.theme.VaultColors
import com.darkjade.streamlib.ui.theme.VaultShapes
import com.darkjade.streamlib.ui.theme.VaultSizes
import com.darkjade.streamlib.ui.theme.VaultSpacing

@Composable
fun DetailsScreen(
    viewModel: DetailsViewModel,
    onBack: () -> Unit,
    onPlay: (fileUriString: String, episodeId: Long?) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    var showRemoveDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(VaultColors.Background)) {
        when {
            state.isLoading -> CircularProgressIndicator(
                color = VaultColors.Orange,
                modifier = Modifier.align(Alignment.Center)
            )
            state.media == null -> EmptyState(
                title = "Not found",
                message = "This title is no longer in your library.",
            )
            else -> {
                val media = state.media!!
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = VaultSpacing.xl)) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                            if (media.backdropUrl != null) {
                                AsyncImage(
                                    model = media.backdropUrl,
                                    contentDescription = media.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize().background(VaultColors.SurfaceVariant))
                            }
                            Box(
                                modifier = Modifier.fillMaxSize().background(
                                    // Concentrate the fade near the bottom edge only — spreading it
                                    // across the whole backdrop made a large chunk look like dead
                                    // black space above the poster row instead of visible artwork.
                                    Brush.verticalGradient(0.55f to Color.Transparent, 1f to VaultColors.Background)
                                )
                            )
                            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(VaultSpacing.xs)) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                            Box(modifier = Modifier.align(Alignment.TopEnd).padding(VaultSpacing.xs)) {
                                IconButton(onClick = { showOverflowMenu = true }) {
                                    Icon(Icons.Filled.MoreVert, contentDescription = "More options", tint = Color.White)
                                }
                                DropdownMenu(expanded = showOverflowMenu, onDismissRequest = { showOverflowMenu = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Remove from library") },
                                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                                        onClick = {
                                            showOverflowMenu = false
                                            showRemoveDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Poster on the left, title + play actions on the right — matches
                    // the layout requested over the previous centered-overlap style.
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = VaultSpacing.md)
                                .padding(top = VaultSpacing.sm),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(110.dp, 165.dp)
                                    .clip(VaultShapes.card)
                                    .background(VaultColors.SurfaceVariant)
                            ) {
                                val posterModel = com.darkjade.streamlib.ui.util.PosterRotationCache.posterFor(media) ?: media.localFileUri
                                if (posterModel != null) {
                                    SubcomposeAsyncImage(
                                        model = posterModel,
                                        contentDescription = media.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                        loading = { FallbackPoster(title = media.title) },
                                        error = { FallbackPoster(title = media.title) },
                                    )
                                } else {
                                    FallbackPoster(title = media.title)
                                }
                            }

                            Column(modifier = Modifier.padding(start = VaultSpacing.sm).weight(1f)) {
                                Text(media.title, style = MaterialTheme.typography.titleLarge, color = VaultColors.TextPrimary, maxLines = 3)

                                Spacer(Modifier.height(VaultSpacing.xs))

                                if (state.nextUpLabel != null && state.nextUpUri != null) {
                                    if (state.hasResumeProgress) {
                                        // Two explicit choices: start over, or pick up where you left off.
                                        Button(
                                            onClick = {
                                                viewModel.recordOpened(state.nextUpEpisodeId)
                                                onPlay(state.nextUpUri.toString(), state.nextUpEpisodeId)
                                            },
                                            shape = VaultShapes.button,
                                            colors = ButtonDefaults.buttonColors(containerColor = VaultColors.Orange, contentColor = Color.White),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                            Text(" Resume", modifier = Modifier.padding(start = 4.dp))
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.recordOpened(state.nextUpEpisodeId)
                                                viewModel.playFromBeginning(state.nextUpEpisodeId) {
                                                    onPlay(state.nextUpUri.toString(), state.nextUpEpisodeId)
                                                }
                                            },
                                            shape = VaultShapes.button,
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = VaultColors.TextPrimary),
                                            modifier = Modifier.fillMaxWidth().padding(top = VaultSpacing.xxs)
                                        ) {
                                            Icon(Icons.Filled.Replay, contentDescription = null)
                                            Text(" From Beginning", modifier = Modifier.padding(start = 4.dp))
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                viewModel.recordOpened(state.nextUpEpisodeId)
                                                onPlay(state.nextUpUri.toString(), state.nextUpEpisodeId)
                                            },
                                            shape = VaultShapes.button,
                                            colors = ButtonDefaults.buttonColors(containerColor = VaultColors.Orange, contentColor = Color.White),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                            Text(" ${state.nextUpLabel}", modifier = Modifier.padding(start = 4.dp))
                                        }
                                    }
                                }

                                if (state.hasResumeProgress && !media.type.isSeriesLike()) {
                                    Text(
                                        formatWatchedProgress(state.resumePositionMs, media.runtimeMinutes),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = VaultColors.Orange,
                                        modifier = Modifier.padding(top = VaultSpacing.xxs)
                                    )
                                }

                                IconButton(onClick = { viewModel.toggleWatchlist() }, modifier = Modifier.padding(top = VaultSpacing.xxs)) {
                                    Icon(
                                        imageVector = if (state.isInWatchlist) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                        contentDescription = "Watchlist",
                                        tint = VaultColors.Orange,
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Column(modifier = Modifier.padding(horizontal = VaultSpacing.md)) {
                            val metaParts = buildList {
                                media.ageRating?.let { add(it) }
                                media.year?.let { add(it.toString()) }
                                media.runtimeMinutes?.let { add("${it}m") }
                                if (media.genres.isNotBlank()) add(media.genres.replace(",", ", "))
                            }
                            if (metaParts.isNotEmpty()) {
                                Text(
                                    metaParts.joinToString("  •  "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VaultColors.TextSecondary,
                                )
                            }

                            media.rating?.let {
                                Text(
                                    "IMDb/TMDB Rating: ${"%.1f".format(it)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VaultColors.TextSecondary,
                                    modifier = Modifier.padding(top = VaultSpacing.xxs)
                                )
                            }

                            if (media.metadataMissing) {
                                Text(
                                    "Metadata unavailable — showing local file info",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VaultColors.TextTertiary,
                                    modifier = Modifier.padding(top = VaultSpacing.xxs)
                                )
                            }

                            media.overview?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = VaultColors.TextSecondary,
                                    modifier = Modifier.padding(top = VaultSpacing.sm)
                                )
                            }

                            if (!media.director.isNullOrBlank()) {
                                Text(
                                    "Director: ${media.director}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VaultColors.TextSecondary,
                                    modifier = Modifier.padding(top = VaultSpacing.sm)
                                )
                            }
                            if (media.cast.isNotBlank()) {
                                Text(
                                    "Cast: ${media.cast.replace(",", ", ")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VaultColors.TextSecondary,
                                    modifier = Modifier.padding(top = VaultSpacing.xxs)
                                )
                            }
                        }
                    }

                    if (state.seasons.isNotEmpty()) {
                        item {
                            LazyRow(
                                modifier = Modifier.padding(vertical = VaultSpacing.md),
                                horizontalArrangement = Arrangement.spacedBy(VaultSpacing.xs),
                                contentPadding = PaddingValues(horizontal = VaultSpacing.md),
                            ) {
                                items(state.seasons, key = { it.id }) { season ->
                                    FilterChip(
                                        selected = season.id == state.selectedSeasonId,
                                        onClick = { viewModel.selectSeason(season.id) },
                                        label = { Text("Season ${season.seasonNumber}") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = VaultColors.Orange,
                                            selectedLabelColor = Color.White,
                                            containerColor = VaultColors.SurfaceVariant,
                                            labelColor = VaultColors.TextSecondary,
                                        )
                                    )
                                }
                            }
                        }

                        items(state.episodes, key = { it.id }) { episode ->
                            EpisodeRow(
                                episode = episode,
                                watchedMs = state.episodeProgress[episode.id]?.positionMs,
                                onClick = {
                                    viewModel.recordOpened(episode.id)
                                    onPlay(episode.localFileUri, episode.id)
                                },
                                onToggleWatched = { viewModel.toggleEpisodeWatched(episode) },
                                onRemove = { viewModel.removeEpisode(episode.id) },
                            )
                        }

                        if (state.episodes.isEmpty()) {
                            item {
                                EmptyState(
                                    title = "No episodes found",
                                    message = "This season has no scanned episode files yet.",
                                    modifier = Modifier.height(160.dp)
                                )
                            }
                        }
                    } else {
                        item { Spacer(Modifier.height(VaultSpacing.xl)) }
                    }
                }

                if (showRemoveDialog) {
                    AlertDialog(
                        onDismissRequest = { showRemoveDialog = false },
                        containerColor = VaultColors.Surface,
                        title = { Text("Remove from library?", color = VaultColors.TextPrimary) },
                        text = {
                            Text(
                                "This removes \"${media.title}\" from DarkVault. Your actual video file is not deleted.",
                                color = VaultColors.TextSecondary,
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                showRemoveDialog = false
                                viewModel.removeMediaItem(onRemoved = onBack)
                            }) {
                                Text("Remove", color = VaultColors.Error)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showRemoveDialog = false }) {
                                Text("Cancel", color = VaultColors.TextSecondary)
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun formatWatchedTime(ms: Long): String {
    val totalMinutes = (ms / 60000).toInt()
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return if (h > 0) "${h}h ${m}m watched" else "${m}m watched"
}

private fun formatWatchedProgress(watchedMs: Long, totalMinutes: Int?): String {
    val watchedLabel = formatWatchedTime(watchedMs)
    if (totalMinutes == null || totalMinutes <= 0) return watchedLabel
    val totalMs = totalMinutes.toLong() * 60000
    val pct = ((watchedMs.toFloat() / totalMs.toFloat()) * 100).toInt().coerceIn(0, 100)
    return "$watchedLabel of ${totalMinutes}m ($pct%)"
}

@Composable
private fun EpisodeRow(
    episode: EpisodeEntity,
    watchedMs: Long?,
    onClick: () -> Unit,
    onToggleWatched: () -> Unit,
    onRemove: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = VaultSpacing.md, vertical = VaultSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(VaultSizes.episodeThumbWidth, VaultSizes.episodeThumbHeight)
                .clip(VaultShapes.card)
                .background(VaultColors.SurfaceVariant)
        ) {
            // Prefer TMDB episode thumbnail; fall back to a decoded frame from
            // the episode's own local video file (always available, unlike
            // series posters), so episode rows are never left blank.
            SubcomposeAsyncImage(
                model = episode.thumbnailUrl ?: episode.localFileUri,
                contentDescription = episode.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { FallbackPoster(title = "E${episode.episodeNumber}") },
                error = { FallbackPoster(title = "E${episode.episodeNumber}") },
            )
            Icon(
                imageVector = if (episode.watched) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = if (episode.watched) "Watched" else "Not watched",
                tint = if (episode.watched) VaultColors.Orange else Color.White.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(18.dp)
                    .clickable(onClick = onToggleWatched)
            )
            // Thin progress bar along the bottom of the thumbnail when there's
            // real playback progress for this episode — the "5m watched" fix.
            if (watchedMs != null && watchedMs > 0 && episode.durationMinutes != null) {
                val totalMs = episode.durationMinutes.toLong() * 60000
                if (totalMs > 0) {
                    LinearProgressIndicator(
                        progress = (watchedMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f),
                        color = VaultColors.Orange,
                        trackColor = Color.Black.copy(alpha = 0.4f),
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp)
                    )
                }
            }
        }
        Column(modifier = Modifier.padding(start = VaultSpacing.sm).weight(1f)) {
            Text(
                "${episode.episodeNumber}. ${episode.title ?: "Episode ${episode.episodeNumber}"}",
                style = MaterialTheme.typography.titleSmall,
                color = VaultColors.TextPrimary,
                maxLines = 2,
            )
            val meta = buildList {
                episode.durationMinutes?.let { add("${it}m") }
                episode.quality?.let { add(it) }
                if (episode.fileMissing) add("File missing")
            }
            if (meta.isNotEmpty()) {
                Text(
                    meta.joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (episode.fileMissing) VaultColors.Error else VaultColors.TextTertiary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (watchedMs != null && watchedMs > 0) {
                Text(
                    formatWatchedTime(watchedMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = VaultColors.Orange,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = VaultColors.Orange)
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More options", tint = VaultColors.TextSecondary)
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text(if (episode.watched) "Mark as unwatched" else "Mark as watched") },
                    onClick = { showMenu = false; onToggleWatched() }
                )
                DropdownMenuItem(
                    text = { Text("Remove from here") },
                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                    onClick = { showMenu = false; onRemove() }
                )
            }
        }
    }
}
