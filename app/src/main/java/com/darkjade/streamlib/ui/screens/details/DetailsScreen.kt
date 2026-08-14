package com.darkjade.streamlib.ui.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.darkjade.streamlib.data.db.entity.EpisodeEntity
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
                        Box(modifier = Modifier.fillMaxWidth().height(320.dp)) {
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
                                    Brush.verticalGradient(listOf(Color.Transparent, VaultColors.Background))
                                )
                            )
                            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(VaultSpacing.xs)) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                        }
                    }

                    item {
                        Column(modifier = Modifier.padding(horizontal = VaultSpacing.md)) {
                            Text(media.title, style = MaterialTheme.typography.headlineMedium, color = VaultColors.TextPrimary)

                            val metaParts = buildList {
                                media.ageRating?.let { add(it) }
                                media.year?.let { add(it.toString()) }
                                if (media.genres.isNotBlank()) add(media.genres.replace(",", ", "))
                            }
                            if (metaParts.isNotEmpty()) {
                                Text(
                                    metaParts.joinToString("  •  "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VaultColors.TextSecondary,
                                    modifier = Modifier.padding(top = VaultSpacing.xxs)
                                )
                            }

                            media.rating?.let {
                                Text(
                                    "Rating: ${"%.1f".format(it)}",
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

                            Row(modifier = Modifier.padding(top = VaultSpacing.sm)) {
                                IconButton(onClick = { viewModel.toggleWatchlist() }) {
                                    Icon(
                                        imageVector = if (state.isInWatchlist) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                        contentDescription = "Watchlist",
                                        tint = VaultColors.Orange,
                                    )
                                }
                            }

                            media.overview?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = VaultColors.TextSecondary,
                                    modifier = Modifier.padding(top = VaultSpacing.xs)
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
                                onClick = {
                                    viewModel.recordOpened(episode.id)
                                    onPlay(episode.localFileUri, episode.id)
                                }
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
                        item { androidx.compose.foundation.layout.Spacer(Modifier.height(VaultSpacing.xl)) }
                    }
                }

                // Bottom action bar: Continue/Start Watching + Play button
                state.nextUpLabel?.let { label ->
                    Box(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(VaultSpacing.md)) {
                        Button(
                            onClick = {
                                viewModel.recordOpened(state.nextUpEpisodeId)
                                state.nextUpUri?.let { onPlay(it.toString(), state.nextUpEpisodeId) }
                            },
                            enabled = state.nextUpUri != null,
                            shape = VaultShapes.button,
                            colors = ButtonDefaults.buttonColors(containerColor = VaultColors.Orange, contentColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Text(text = " $label", modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeRow(episode: EpisodeEntity, onClick: () -> Unit) {
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
            if (episode.thumbnailUrl != null) {
                AsyncImage(
                    model = episode.thumbnailUrl,
                    contentDescription = episode.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                FallbackPoster(title = "E${episode.episodeNumber}")
            }
            if (episode.watched) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Watched",
                    tint = VaultColors.Orange,
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(18.dp)
                )
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
        }
        Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = VaultColors.Orange)
    }
}
