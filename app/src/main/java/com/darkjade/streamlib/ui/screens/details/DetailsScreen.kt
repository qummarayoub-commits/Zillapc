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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.darkjade.streamlib.data.db.entity.EpisodeEntity
import com.darkjade.streamlib.data.db.entity.MediaItemEntity
import com.darkjade.streamlib.data.metadata.isSeriesLike
import com.darkjade.streamlib.ui.components.EmptyState
import com.darkjade.streamlib.ui.components.FallbackPoster
import com.darkjade.streamlib.ui.theme.VaultColors
import com.darkjade.streamlib.ui.theme.VaultShapes
import com.darkjade.streamlib.ui.theme.VaultSizes
import com.darkjade.streamlib.ui.theme.VaultSpacing
import com.darkjade.streamlib.ui.util.ArtworkTintExtractor

private data class ParsedCastMember(val name: String, val character: String?, val photoUrl: String?)

private fun parseCastMembers(raw: String): List<ParsedCastMember> {
    if (raw.isBlank()) return emptyList()
    return raw.split(";;").mapNotNull { entry ->
        val parts = entry.split("|")
        val name = parts.getOrNull(0)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        ParsedCastMember(
            name = name,
            character = parts.getOrNull(1)?.takeIf { it.isNotBlank() },
            photoUrl = parts.getOrNull(2)?.takeIf { it.isNotBlank() },
        )
    }
}

@Composable
fun DetailsScreen(
    viewModel: DetailsViewModel,
    onBack: () -> Unit,
    onPlay: (fileUriString: String, episodeId: Long?) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showRemoveDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showTrailer by remember { mutableStateOf(false) }

    // Dark/subtle tint pulled from this title's own artwork — cached per
    // image URL, recomputed only when the artwork actually changes.
    val tintColor by produceState<Color?>(initialValue = null, state.media?.backdropUrl, state.media?.posterUrl) {
        value = ArtworkTintExtractor.extractTint(context, state.media?.backdropUrl ?: state.media?.posterUrl)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to (tintColor?.copy(alpha = 0.6f) ?: VaultColors.Background),
                    0.45f to VaultColors.Background,
                    1f to VaultColors.Background,
                )
            )
    ) {
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
                val castMembers = remember(media.castMembers) { parseCastMembers(media.castMembers) }

                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = VaultSpacing.xl)) {
                    // Large cinematic hero — substantially bigger than a thumbnail strip,
                    // fading naturally into the tinted page background below it.
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
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
                                    Brush.verticalGradient(0.35f to Color.Transparent, 1f to VaultColors.Background)
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

                    // Poster on the left, title + play actions on the right.
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = VaultSpacing.md)
                                .padding(top = VaultSpacing.xxs),
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

                    // Breathing room below the poster, then Duration / IMDb / Rotten Tomatoes —
                    // only the ratings that actually exist; never invented.
                    item {
                        Column(modifier = Modifier.padding(horizontal = VaultSpacing.md).padding(top = VaultSpacing.lg)) {
                            RatingsBlock(media)

                            if (media.genres.isNotBlank()) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(VaultSpacing.xs),
                                    modifier = Modifier.padding(top = VaultSpacing.sm)
                                ) {
                                    items(media.genres.split(",").map { it.trim() }.filter { it.isNotBlank() }) { genre ->
                                        Box(
                                            modifier = Modifier
                                                .clip(VaultShapes.chip)
                                                .background(VaultColors.SurfaceVariant)
                                                .padding(horizontal = VaultSpacing.sm, vertical = VaultSpacing.xxs)
                                        ) {
                                            Text(genre, style = MaterialTheme.typography.labelMedium, color = VaultColors.TextSecondary)
                                        }
                                    }
                                }
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
                        }
                    }

                    // Trailer — plays INSIDE this screen via an embedded player, never
                    // an external browser/YouTube app. Nothing shown if none is available.
                    if (!media.trailerYoutubeKey.isNullOrBlank()) {
                        item {
                            Column(modifier = Modifier.padding(horizontal = VaultSpacing.md).padding(top = VaultSpacing.lg)) {
                                Text("Trailer", style = MaterialTheme.typography.titleMedium, color = VaultColors.TextPrimary)
                                Spacer(Modifier.height(VaultSpacing.xs))
                                if (showTrailer) {
                                    EmbeddedYoutubePlayer(
                                        youtubeKey = media.trailerYoutubeKey,
                                        modifier = Modifier.fillMaxWidth().height(200.dp).clip(VaultShapes.card)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                            .clip(VaultShapes.card)
                                            .background(VaultColors.SurfaceVariant)
                                            .clickable { showTrailer = true },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (media.backdropUrl != null) {
                                            AsyncImage(
                                                model = media.backdropUrl,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))
                                        }
                                        Icon(
                                            Icons.Filled.PlayCircle,
                                            contentDescription = "Play trailer",
                                            tint = Color.White,
                                            modifier = Modifier.size(56.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Cast — main cast only, horizontally scrollable, circular photos.
                    if (castMembers.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.padding(top = VaultSpacing.lg)) {
                                Text(
                                    "Cast",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = VaultColors.TextPrimary,
                                    modifier = Modifier.padding(horizontal = VaultSpacing.md)
                                )
                                Spacer(Modifier.height(VaultSpacing.xs))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(VaultSpacing.sm),
                                    contentPadding = PaddingValues(horizontal = VaultSpacing.md),
                                ) {
                                    items(castMembers) { member -> CastMemberCard(member) }
                                }
                            }
                        }
                    }

                    // Detailed information section.
                    item {
                        InfoSection(media, state.seasons.size)
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

/** "Duration: 2h 14m / IMDb: 8.1 / Rotten Tomatoes: 92%" — only fields that actually exist, never invented. */
@Composable
private fun RatingsBlock(media: MediaItemEntity) {
    val lines = buildList {
        formatRuntimeLong(media.runtimeMinutes)?.let { add("Duration: $it") }
        media.imdbRating?.let { add("IMDb: ${"%.1f".format(it)}") }
        media.rottenTomatoesPercent?.let { add("Rotten Tomatoes: $it%") }
        // TMDB's own score is a different thing from IMDb — labeled distinctly, only shown if IMDb wasn't found.
        if (media.imdbRating == null) {
            media.rating?.let { add("TMDB Rating: ${"%.1f".format(it)}") }
        }
    }
    if (lines.isEmpty()) return
    Column {
        lines.forEach {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = VaultColors.TextSecondary)
        }
    }
}

private fun formatRuntimeLong(minutes: Int?): String? {
    if (minutes == null || minutes <= 0) return null
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

@Composable
private fun CastMemberCard(member: ParsedCastMember) {
    Column(
        modifier = Modifier.width(80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(VaultColors.SurfaceVariant)
        ) {
            if (member.photoUrl != null) {
                SubcomposeAsyncImage(
                    model = member.photoUrl,
                    contentDescription = member.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = { CastFallbackIcon() },
                    error = { CastFallbackIcon() },
                )
            } else {
                CastFallbackIcon()
            }
        }
        Text(
            member.name,
            style = MaterialTheme.typography.labelSmall,
            color = VaultColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = VaultSpacing.xxs)
        )
        member.character?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                color = VaultColors.TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CastFallbackIcon() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.Person, contentDescription = null, tint = VaultColors.TextTertiary)
    }
}

/** Movies vs Series/Anime get different fields — only ones with real data are shown. */
@Composable
private fun InfoSection(media: MediaItemEntity, seasonCountFromDb: Int) {
    Column(modifier = Modifier.padding(horizontal = VaultSpacing.md).padding(top = VaultSpacing.lg)) {
        Text("Information", style = MaterialTheme.typography.titleMedium, color = VaultColors.TextPrimary)
        Spacer(Modifier.height(VaultSpacing.xs))

        InfoRow("Title", media.title)
        media.year?.let { InfoRow("Year", it.toString()) }
        if (media.genres.isNotBlank()) InfoRow("Genres", media.genres.replace(",", ", "))

        if (media.type.isSeriesLike()) {
            (media.seasonCount ?: seasonCountFromDb.takeIf { it > 0 })?.let { InfoRow("Seasons", it.toString()) }
            media.episodeCount?.let { InfoRow("Episodes", it.toString()) }
            media.status?.let { InfoRow("Status", it) }
        } else {
            formatRuntimeLong(media.runtimeMinutes)?.let { InfoRow("Duration", it) }
        }

        media.imdbRating?.let { InfoRow("IMDb Rating", "%.1f / 10".format(it)) }
        media.rottenTomatoesPercent?.let { InfoRow("Rotten Tomatoes", "$it%") }
        if (media.imdbRating == null) media.rating?.let { InfoRow("TMDB Rating", "%.1f / 10".format(it)) }

        if (!media.director.isNullOrBlank()) InfoRow("Director", media.director)
        if (media.cast.isNotBlank()) InfoRow("Main Cast", media.cast.replace(",", ", "))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = VaultColors.TextTertiary,
            modifier = Modifier.width(120.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = VaultColors.TextSecondary,
            modifier = Modifier.weight(1f)
        )
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
