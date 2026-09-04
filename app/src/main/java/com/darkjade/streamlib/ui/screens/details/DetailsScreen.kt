package com.darkjade.streamlib.ui.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.PhotoLibrary
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
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

/** "Add Info" — search TMDB manually and pick the correct match, for
 * titles that auto-matched wrong (or not at all, e.g. a censor-certificate
 * image instead of a real poster). */
/** Real Rotten Tomatoes tomato mark (bundled logo asset). */
@Composable
private fun RottenTomatoIcon(size: androidx.compose.ui.unit.Dp = 14.dp) {
    androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(id = com.darkjade.streamlib.R.drawable.logo_rotten_tomatoes),
        contentDescription = "Rotten Tomatoes",
        modifier = Modifier.size(size),
    )
}

/** Real IMDb logo mark (bundled logo asset), height-constrained so it sits
 * inline with the score text at the right rating-row size. */
@Composable
private fun ImdbIcon(height: androidx.compose.ui.unit.Dp = 16.dp) {
    androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(id = com.darkjade.streamlib.R.drawable.logo_imdb),
        contentDescription = "IMDb",
        modifier = Modifier.height(height),
    )
}

/** Real Metacritic "M" mark (bundled logo asset). */
@Composable
private fun MetacriticIcon(size: androidx.compose.ui.unit.Dp = 16.dp) {
    androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(id = com.darkjade.streamlib.R.drawable.logo_metacritic),
        contentDescription = "Metacritic",
        modifier = Modifier.size(size),
    )
}

/** Real TMDB logo mark (bundled logo asset), height-constrained so it sits
 * inline with the score text at the right rating-row size. */
@Composable
private fun TmdbIcon(height: androidx.compose.ui.unit.Dp = 14.dp) {
    androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(id = com.darkjade.streamlib.R.drawable.logo_tmdb),
        contentDescription = "TMDB",
        modifier = Modifier.height(height),
    )
}

@Composable
private fun ActionIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = VaultColors.TextPrimary,
    labelColor: Color = VaultColors.TextSecondary,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick).widthIn(max = 76.dp)) {
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = tint)
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun RateDialog(currentRating: Int, onRate: (Int) -> Unit, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(VaultShapes.card)
                .background(VaultColors.Surface)
                .padding(VaultSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Rate this title", style = MaterialTheme.typography.titleMedium, color = VaultColors.TextPrimary)
            Row(modifier = Modifier.padding(top = VaultSpacing.md)) {
                for (i in 1..5) {
                    Icon(
                        imageVector = if (i <= currentRating) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = "$i star",
                        tint = VaultColors.Orange,
                        modifier = Modifier.size(36.dp).clickable { onRate(i) }.padding(2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddInfoDialog(viewModel: DetailsViewModel, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val results by viewModel.searchResults.collectAsState()
    val loading by viewModel.searchInProgress.collectAsState()
    val isSeries by viewModel.searchIsSeries.collectAsState()

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 480.dp)
                .clip(VaultShapes.card)
                .background(VaultColors.Surface)
                .padding(VaultSpacing.md)
        ) {
            Text("Add Info — search TMDB", style = MaterialTheme.typography.titleMedium, color = VaultColors.TextPrimary)
            Spacer(Modifier.height(VaultSpacing.sm))
            // Movie/Series toggle — defaults to whatever this item's own
            // type already is, but overridable: a title can be mis-typed
            // in the library (a series scanned as a movie or vice versa),
            // so without this the search could never reach the right TMDB
            // catalog at all.
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(VaultSpacing.xs)) {
                listOf(false to "Movie", true to "Series").forEach { (seriesValue, label) ->
                    val selected = isSeries == seriesValue
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) VaultColors.Background else VaultColors.TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) VaultColors.Orange else VaultColors.SurfaceVariant)
                            .clickable { viewModel.setSearchIsSeries(seriesValue) }
                            .padding(vertical = VaultSpacing.xs)
                    )
                }
            }
            Spacer(Modifier.height(VaultSpacing.sm))
            androidx.compose.material3.OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(if (isSeries) "Series title" else "Movie title") },
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { viewModel.searchTmdb(query) }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search", tint = VaultColors.Orange)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VaultColors.Orange,
                    unfocusedBorderColor = VaultColors.Divider,
                    focusedTextColor = VaultColors.TextPrimary,
                    unfocusedTextColor = VaultColors.TextPrimary,
                    cursorColor = VaultColors.Orange,
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { viewModel.searchTmdb(query) }),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
            )
            Spacer(Modifier.height(VaultSpacing.sm))

            if (loading) {
                Box(modifier = Modifier.fillMaxWidth().padding(VaultSpacing.lg), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = VaultColors.Orange)
                }
            } else if (results.isEmpty()) {
                Text(
                    "Search for the correct title above.",
                    style = MaterialTheme.typography.bodySmall,
                    color = VaultColors.TextTertiary,
                    modifier = Modifier.padding(vertical = VaultSpacing.md)
                )
            } else {
                LazyColumn {
                    items(results, key = { it.remoteId }) { candidate ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.applyManualMatch(candidate.remoteId, onApplied = onDismiss) }
                                .padding(vertical = VaultSpacing.xs),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp, 66.dp)
                                    .clip(VaultShapes.card)
                                    .background(VaultColors.SurfaceVariant)
                            ) {
                                if (candidate.posterUrl != null) {
                                    SubcomposeAsyncImage(
                                        model = candidate.posterUrl,
                                        contentDescription = candidate.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                        loading = {},
                                        error = {},
                                    )
                                }
                            }
                            Column(modifier = Modifier.padding(start = VaultSpacing.sm)) {
                                Text(candidate.title, style = MaterialTheme.typography.bodyMedium, color = VaultColors.TextPrimary)
                                candidate.year?.let {
                                    Text(it.toString(), style = MaterialTheme.typography.labelSmall, color = VaultColors.TextTertiary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** "Change Poster / Backdrop" — pick a specific image from the TMDB
 * gallery already fetched with this title, instead of the one TMDB
 * auto-assigned. Two tabs (Poster/Backdrop), each a grid of thumbnails;
 * tapping one applies it immediately and closes the dialog. */
@Composable
private fun ChangeImageDialog(viewModel: DetailsViewModel, media: MediaItemEntity, onDismiss: () -> Unit) {
    var showingBackdrops by remember { mutableStateOf(false) }

    val posterUrls = remember(media.alternatePosterUrls, media.posterUrl) {
        val alts = media.alternatePosterUrls.split(",").filter { it.isNotBlank() }
        (listOfNotNull(media.posterUrl) + alts).distinct()
    }
    val backdropUrls = remember(media.alternateBackdropUrls, media.backdropUrl) {
        val alts = media.alternateBackdropUrls.split(",").filter { it.isNotBlank() }
        (listOfNotNull(media.backdropUrl) + alts).distinct()
    }
    val urls = if (showingBackdrops) backdropUrls else posterUrls
    val currentUrl = if (showingBackdrops) media.backdropUrl else media.posterUrl

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 480.dp)
                .clip(VaultShapes.card)
                .background(VaultColors.Surface)
                .padding(VaultSpacing.md)
        ) {
            Text("Change Poster / Backdrop", style = MaterialTheme.typography.titleMedium, color = VaultColors.TextPrimary)
            Spacer(Modifier.height(VaultSpacing.sm))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(VaultSpacing.xs)) {
                listOf(false to "Poster", true to "Backdrop").forEach { (backdrop, label) ->
                    val selected = showingBackdrops == backdrop
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) VaultColors.Background else VaultColors.TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) VaultColors.Orange else VaultColors.SurfaceVariant)
                            .clickable { showingBackdrops = backdrop }
                            .padding(vertical = VaultSpacing.xs)
                    )
                }
            }
            Spacer(Modifier.height(VaultSpacing.md))

            if (urls.isEmpty()) {
                Text(
                    if (showingBackdrops) "No alternate backdrops available from TMDB for this title." else "No alternate posters available from TMDB for this title.",
                    style = MaterialTheme.typography.bodySmall,
                    color = VaultColors.TextTertiary,
                    modifier = Modifier.padding(vertical = VaultSpacing.lg)
                )
            } else {
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(if (showingBackdrops) 2 else 3),
                    horizontalArrangement = Arrangement.spacedBy(VaultSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(VaultSpacing.xs),
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    items(urls, key = { it }) { url ->
                        val isCurrent = url == currentUrl
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(if (showingBackdrops) 16f / 9f else 2f / 3f)
                                .clip(VaultShapes.card)
                                .background(VaultColors.SurfaceVariant)
                                .then(
                                    if (isCurrent) Modifier.border(2.dp, VaultColors.Orange, VaultShapes.card)
                                    else Modifier
                                )
                                .clickable {
                                    if (showingBackdrops) viewModel.setBackdrop(url) else viewModel.setPoster(url)
                                    onDismiss()
                                },
                        ) {
                            SubcomposeAsyncImage(
                                model = url,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                                loading = {},
                                error = {},
                            )
                            if (isCurrent) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = "Current",
                                    tint = VaultColors.Orange,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

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
    // Full-phone-size cinematic backdrop — computed from actual screen
    // height (not a fixed dp) so it scales correctly across device sizes,
    // matching the reference's near-full-screen hero exactly.
    // Full-phone-size cinematic backdrop — computed from actual screen
    // height (not a fixed dp) so it scales correctly across device sizes.
    // Generous enough (0.85) that the overlaid poster/title/duration/
    // overview/genres/button content never overflows past the backdrop's
    // own bounds (a Box's aligned children aren't auto-clipped in Compose,
    // so if the content was taller than this, it would visibly "leak" into
    // plain black below the image instead of staying on the artwork).
    val heroHeightDp = (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp * 0.78f).dp
    val context = LocalContext.current
    var showRemoveDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showTrailer by remember { mutableStateOf(false) }
    var showAddInfoDialog by remember { mutableStateOf(false) }
    var showChangeImageDialog by remember { mutableStateOf(false) }
    var showRateDialog by remember { mutableStateOf(false) }

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
                    0f to (tintColor?.copy(alpha = 0.75f) ?: VaultColors.Background),
                    0.75f to VaultColors.Background,
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
                    // ===== Step 1-6: everything below is overlaid on ONE
                    // full-phone-size backdrop image, exactly like the
                    // reference — not separate stacked sections. =====
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(heroHeightDp)) {
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
                            // Gradient dark enough at the bottom for the poster/title/button block to stay readable.
                            Box(
                                modifier = Modifier.fillMaxSize().background(
                                    Brush.verticalGradient(0.15f to Color.Transparent, 0.55f to VaultColors.Background.copy(alpha = 0.75f), 1f to VaultColors.Background)
                                )
                            )

                            // Step 2: back/menu icons, top corners.
                            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(VaultSpacing.xs)) {
                                Box(
                                    modifier = Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.4f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.padding(6.dp))
                                }
                            }
                            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                                DropdownMenu(expanded = showOverflowMenu, onDismissRequest = { showOverflowMenu = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Add Info") },
                                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                                        onClick = {
                                            showOverflowMenu = false
                                            showAddInfoDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Change Poster / Backdrop") },
                                        leadingIcon = { Icon(Icons.Filled.PhotoLibrary, contentDescription = null) },
                                        onClick = {
                                            showOverflowMenu = false
                                            showChangeImageDialog = true
                                        }
                                    )
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

                            // Step 2: reserved leftover space above the content block
                            // (weight-based) — kept empty now that the center play
                            // button has been removed, so the content below still
                            // never overlaps/collides regardless of screen size.
                            Column(modifier = Modifier.fillMaxSize()) {
                                Box(modifier = Modifier.weight(1f).fillMaxWidth())

                                // Large title directly on the backdrop — no small poster
                                // thumbnail block, matching the reference exactly.
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(VaultSpacing.md),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                if (!media.titleLogoUrl.isNullOrBlank()) {
                                    // The movie's actual stylized title-logo artwork — not
                                    // manually rendered text — matching what appears on the
                                    // poster/marketing art itself.
                                    SubcomposeAsyncImage(
                                        model = media.titleLogoUrl,
                                        contentDescription = media.title,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxWidth().heightIn(max = 90.dp),
                                        loading = {
                                            Text(media.title, style = MaterialTheme.typography.headlineLarge, color = VaultColors.TextPrimary, maxLines = 2)
                                        },
                                        error = {
                                            Text(media.title, style = MaterialTheme.typography.headlineLarge, color = VaultColors.TextPrimary, maxLines = 2)
                                        },
                                    )
                                } else {
                                    Text(
                                        media.title,
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = VaultColors.TextPrimary,
                                        maxLines = 2,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }

                                // Year + Duration • Genres, age-cert badge — all on one
                                // clean metadata line together, sitting with real breathing
                                // room below the title (not cramped directly underneath).
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = VaultSpacing.lg)) {
                                    val infoBits = buildList {
                                        media.year?.let { add(it.toString()) }
                                        formatRuntimeLong(media.runtimeMinutes)?.let { add(it) }
                                        media.genres.split(",").map { it.trim() }.filter { it.isNotBlank() }.take(2).let {
                                            if (it.isNotEmpty()) add(it.joinToString(", "))
                                        }
                                    }
                                    if (infoBits.isNotEmpty()) {
                                        Text(
                                            infoBits.joinToString("   "),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = VaultColors.TextSecondary,
                                        )
                                    }
                                    if (!media.ageRating.isNullOrBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .padding(start = VaultSpacing.sm)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(Color.Black.copy(alpha = 0.5f))
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text(media.ageRating, style = MaterialTheme.typography.labelSmall, color = VaultColors.TextPrimary)
                                        }
                                    }
                                }

                                // Ratings row — its own clean, aligned row underneath. Only
                                // real, fetched data; nothing invented. More breathing room
                                // between each logo+score group, and more space below the row
                                // so the capsule row underneath doesn't feel cramped against it.
                                if (media.imdbRating != null || media.rottenTomatoesPercent != null || media.rating != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = VaultSpacing.sm)) {
                                        media.imdbRating?.let {
                                            ImdbIcon(height = 15.dp)
                                            Text(" ${"%.1f".format(it)}", style = MaterialTheme.typography.labelMedium, color = VaultColors.TextPrimary, modifier = Modifier.padding(start = 4.dp, end = VaultSpacing.md))
                                        }
                                        media.rottenTomatoesPercent?.let {
                                            RottenTomatoIcon()
                                            Text(" $it%", style = MaterialTheme.typography.labelMedium, color = VaultColors.TextPrimary, modifier = Modifier.padding(start = 3.dp, end = VaultSpacing.md))
                                        }
                                        media.rating?.let {
                                            TmdbIcon()
                                            Text(" ${"%.1f".format(it)}", style = MaterialTheme.typography.labelMedium, color = VaultColors.TextPrimary, modifier = Modifier.padding(start = 4.dp, end = VaultSpacing.md))
                                        }
                                        media.metacriticScore?.let {
                                            MetacriticIcon(size = 16.dp)
                                            Text(" $it", style = MaterialTheme.typography.labelMedium, color = VaultColors.TextPrimary, modifier = Modifier.padding(start = 4.dp))
                                        }
                                    }
                                }

                                // Watch in Velora capsule (left) + Add to List (right) — same
                                // size/design as the Home banner's capsule (52dp tall, 38dp
                                // icon-circle, titleSmall Bold text) instead of a smaller
                                // Details-only sizing, and Add to List is now the same
                                // orange-ring bookmark icon button used on the banner instead
                                // of the old white text pill.
                                Row(modifier = Modifier.fillMaxWidth().padding(top = VaultSpacing.lg), verticalAlignment = Alignment.CenterVertically) {

                                    // Velora play capsule — the V-mark icon attached to a pill
                                    // reading "Watch in Velora", or the honest "No Streaming
                                    // Availability" state when nothing is actually playable.
                                    if (state.nextUpUri != null) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(52.dp)
                                                .clip(RoundedCornerShape(percent = 50))
                                                .background(VaultColors.Orange)
                                                .clickable {
                                                    viewModel.recordOpened(state.nextUpEpisodeId)
                                                    onPlay(state.nextUpUri.toString(), state.nextUpEpisodeId)
                                                }
                                                .padding(start = 8.dp, end = 20.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier.size(38.dp).clip(CircleShape).background(VaultColors.Background),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                androidx.compose.foundation.Image(
                                                    painter = androidx.compose.ui.res.painterResource(id = com.darkjade.streamlib.R.drawable.logo_v_mark),
                                                    contentDescription = "Velora",
                                                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(VaultColors.Orange),
                                                    modifier = Modifier.size(20.dp),
                                                )
                                            }
                                            Text(
                                                "Watch in Velora",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = VaultColors.Background,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(start = 10.dp)
                                            )
                                        }
                                    } else {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(52.dp)
                                                .clip(RoundedCornerShape(percent = 50))
                                                .background(Color.White.copy(alpha = 0.08f))
                                                .padding(start = 8.dp, end = 20.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier.size(38.dp).clip(CircleShape).background(VaultColors.SurfaceVariant),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(Icons.Filled.CloudOff, contentDescription = null, tint = VaultColors.TextTertiary, modifier = Modifier.size(18.dp))
                                            }
                                            Text(
                                                "No Streaming Availability",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = VaultColors.TextTertiary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(start = 10.dp)
                                            )
                                        }
                                    }

                                    // Add to List — same orange-ring circular bookmark button as
                                    // the Home banner (was a white "Add to List" text pill before).
                                    Box(
                                        modifier = Modifier
                                            .padding(start = VaultSpacing.sm)
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.35f))
                                            .border(1.5.dp, VaultColors.Orange, CircleShape)
                                            .clickable { viewModel.toggleWatchlist() },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (state.isInWatchlist) {
                                            Icon(Icons.Filled.Bookmark, contentDescription = "Added to List", tint = VaultColors.Orange)
                                        } else {
                                            Icon(Icons.Filled.BookmarkBorder, contentDescription = "Add to list", tint = VaultColors.Orange)
                                        }
                                    }
                                }

                                // Action row — Resume/Watch From Beginning on the left,
                                // Watch Trailer + More grouped together on the right.
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = VaultSpacing.md),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(VaultSpacing.lg)) {
                                        // Always shown once something is playable - not hidden
                                        // until the title has been watched. Normal color when
                                        // there's no resume position yet; the same Velora orange
                                        // once real progress exists, so the color itself signals
                                        // "you have somewhere to resume to".
                                        if (state.nextUpUri != null) {
                                            ActionIconButton(
                                                Icons.Filled.PlayCircle,
                                                "Resume Where You Left Off",
                                                tint = if (state.hasResumeProgress) VaultColors.Orange else VaultColors.TextPrimary,
                                                labelColor = if (state.hasResumeProgress) VaultColors.Orange else VaultColors.TextSecondary,
                                            ) {
                                                viewModel.recordOpened(state.nextUpEpisodeId)
                                                onPlay(state.nextUpUri.toString(), state.nextUpEpisodeId)
                                            }
                                        }
                                        if (state.nextUpUri != null) {
                                            ActionIconButton(Icons.Filled.RestartAlt, "Watch From Beginning") {
                                                viewModel.playFromBeginning(state.nextUpEpisodeId) {
                                                    onPlay(state.nextUpUri.toString(), state.nextUpEpisodeId)
                                                }
                                            }
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(VaultSpacing.lg)) {
                                        if (!media.trailerYoutubeKey.isNullOrBlank()) {
                                            ActionIconButton(Icons.Filled.PlayCircle, "Watch Trailer") { showTrailer = true }
                                        }
                                        ActionIconButton(Icons.Filled.MoreHoriz, "More") { showOverflowMenu = true }
                                    }
                                }

                                // Description, then "Directed by X" — matching the reference's placement.
                                media.overview?.let {
                                    Text(
                                        it,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = VaultColors.TextSecondary,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = VaultSpacing.md)
                                    )
                                }
                                if (!media.director.isNullOrBlank()) {
                                    Text(
                                        "Directed by ${media.director}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = VaultColors.TextTertiary,
                                        modifier = Modifier.padding(top = VaultSpacing.xs)
                                    )
                                }

                                if (state.hasResumeProgress && !media.type.isSeriesLike()) {
                                    Text(
                                        formatWatchedProgress(state.resumePositionMs, media.runtimeMinutes),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = VaultColors.Orange,
                                        modifier = Modifier.padding(top = VaultSpacing.xs)
                                    )
                                }
                                }
                            }
                        }
                    }

                    // Embedded trailer player — appears here once "Watch Trailer" is tapped above.
                    if (!media.trailerYoutubeKey.isNullOrBlank() && showTrailer) {
                        item {
                            EmbeddedYoutubePlayer(
                                youtubeKey = media.trailerYoutubeKey,
                                modifier = Modifier.fillMaxWidth().height(200.dp)
                            )
                        }
                    }

                    // Cast — main cast only, 3-column grid with circular
                    // photos (matches the reference's "Top Cast" grid layout).
                    if (castMembers.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.padding(top = VaultSpacing.lg).padding(horizontal = VaultSpacing.md)) {
                                Text(
                                    "Cast & Crew",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = VaultColors.TextPrimary,
                                )
                                Spacer(Modifier.height(VaultSpacing.sm))
                                castMembers.chunked(3).forEach { rowMembers ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = VaultSpacing.sm),
                                        horizontalArrangement = Arrangement.spacedBy(VaultSpacing.sm),
                                    ) {
                                        rowMembers.forEach { member ->
                                            Box(modifier = Modifier.weight(1f)) { CastMemberCard(member) }
                                        }
                                        // Pad the last row so cards stay left-aligned instead of stretching.
                                        repeat(3 - rowMembers.size) { Box(modifier = Modifier.weight(1f)) }
                                    }
                                }
                            }
                        }
                    }

                    // Same full Information card for both movies and series.
                    item {
                        InfoSection(media)
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

                if (showAddInfoDialog) {
                    AddInfoDialog(
                        viewModel = viewModel,
                        onDismiss = { showAddInfoDialog = false },
                    )
                }
                if (showChangeImageDialog) {
                    ChangeImageDialog(
                        viewModel = viewModel,
                        media = media,
                        onDismiss = { showChangeImageDialog = false },
                    )
                }
                if (showRateDialog) {
                    RateDialog(
                        currentRating = media.userRating ?: 0,
                        onRate = { stars -> viewModel.rateMedia(stars); showRateDialog = false },
                        onDismiss = { showRateDialog = false },
                    )
                }
            }
        }
    }
}

/** "Duration: 2h 14m / IMDb: 8.1 / Rotten Tomatoes: 92%" — only fields that actually exist, never invented. */
@Composable
private fun formatRuntimeLong(minutes: Int?): String? {
    if (minutes == null || minutes <= 0) return null
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

@Composable
private fun CastMemberCard(member: ParsedCastMember) {
    Column(
        modifier = Modifier.fillMaxWidth(),
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
/** Full details layout for movies — RATING (IMDb + Rotten Tomatoes), GENRES,
 * RUNTIME, DIRECTORS, matching the reference's label-caps + value style. */
@Composable
private fun InfoSection(media: MediaItemEntity) {
    Column(
        modifier = Modifier
            .padding(horizontal = VaultSpacing.md)
            .padding(top = VaultSpacing.lg)
            .fillMaxWidth()
            .clip(VaultShapes.card)
            .background(VaultColors.Surface)
            .padding(VaultSpacing.md)
    ) {
        if (media.imdbRating != null || media.rottenTomatoesPercent != null || media.rating != null || media.metacriticScore != null) {
            CapsInfoRow("RATING") {
                Column {
                    media.imdbRating?.let {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ImdbIcon(height = 18.dp)
                            Text(" ${"%.1f".format(it)}", style = MaterialTheme.typography.bodyMedium, color = VaultColors.TextPrimary, modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                    media.rottenTomatoesPercent?.let {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            RottenTomatoIcon(size = 18.dp)
                            Text(" $it%", style = MaterialTheme.typography.bodyMedium, color = VaultColors.TextPrimary, modifier = Modifier.padding(start = 5.dp))
                        }
                    }
                    media.rating?.let {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            TmdbIcon(height = 16.dp)
                            Text(" ${"%.1f".format(it)}", style = MaterialTheme.typography.bodyMedium, color = VaultColors.TextPrimary, modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                    media.metacriticScore?.let {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            MetacriticIcon(size = 18.dp)
                            Text(" $it", style = MaterialTheme.typography.bodyMedium, color = VaultColors.TextPrimary, modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                }
            }
        }

        if (media.genres.isNotBlank()) CapsInfoRow("GENRES") { Text(media.genres.replace(",", ", "), style = MaterialTheme.typography.bodyMedium, color = VaultColors.TextPrimary) }
        formatRuntimeLong(media.runtimeMinutes)?.let { CapsInfoRow("RUNTIME") { Text(it, style = MaterialTheme.typography.bodyMedium, color = VaultColors.TextPrimary) } }
        if (!media.productionCountry.isNullOrBlank()) {
            CapsInfoRow("PRODUCTION COUNTRY") { Text(media.productionCountry, style = MaterialTheme.typography.bodyMedium, color = VaultColors.TextPrimary) }
        }
        if (!media.director.isNullOrBlank()) {
            CapsInfoRow("DIRECTORS") {
                Text(media.director.replace(",", ", "), style = MaterialTheme.typography.bodyMedium, color = VaultColors.Orange)
            }
        }
    }

    // Only what's genuinely known — real file path and format from the
    // actual extension — never fabricated size/resolution/audio-track data
    // we don't reliably have stored for this item.
    if (media.localFilePath != null || media.localFileUri != null) {
        LocalFileInfoSection(media)
    }
}

@Composable
private fun CapsInfoRow(label: String, content: @Composable () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = VaultSpacing.sm)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = VaultColors.TextTertiary,
            modifier = Modifier.width(110.dp)
        )
        Box(modifier = Modifier.weight(1f)) { content() }
    }
}

@Composable
private fun LocalFileInfoSection(media: MediaItemEntity) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.padding(horizontal = VaultSpacing.md).padding(top = VaultSpacing.md)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(VaultShapes.card)
                .background(VaultColors.Surface)
                .clickable { expanded = !expanded }
                .padding(VaultSpacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Local File Information", style = MaterialTheme.typography.titleSmall, color = VaultColors.TextPrimary)
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = VaultColors.TextSecondary,
            )
        }
        if (expanded) {
            Column(modifier = Modifier.padding(top = VaultSpacing.xs)) {
                val rawPath = media.localFilePath ?: media.localFileUri.orEmpty()
                InfoRow("Path", rawPath)
                val ext = rawPath.substringAfterLast('.', "").uppercase()
                if (ext.isNotBlank() && ext.length <= 5) InfoRow("Format", ext)
            }
        }
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
