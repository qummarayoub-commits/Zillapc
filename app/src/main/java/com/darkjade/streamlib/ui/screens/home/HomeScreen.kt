package com.darkjade.streamlib.ui.screens.home

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.darkjade.streamlib.data.db.entity.ComicEntity
import com.darkjade.streamlib.data.db.entity.MediaItemEntity
import com.darkjade.streamlib.ui.components.ComicRail
import com.darkjade.streamlib.ui.components.EmptyState
import com.darkjade.streamlib.ui.components.MediaRail
import com.darkjade.streamlib.ui.theme.VaultColors
import com.darkjade.streamlib.ui.theme.VaultShapes
import com.darkjade.streamlib.ui.theme.VaultSizes
import com.darkjade.streamlib.ui.theme.VaultSpacing

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenDetails: (Long) -> Unit,
    onOpenComicDetails: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultColors.Background)
    ) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(
                    color = VaultColors.Orange,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            state.libraryEmpty -> {
                EmptyState(
                    title = "Your library is empty",
                    message = "Scan your device to start building your library.",
                    actionLabel = "Scan for Videos",
                    onAction = onOpenSettings,
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = VaultSpacing.xxl)
                ) {
                    item {
                        HomeTopBar(onOpenSearch = onOpenSearch, onOpenSettings = onOpenSettings)
                    }
                    // Top: main rotating banner across everything (movies, series, comics).
                    state.hero?.let { hero ->
                        item {
                            HeroSection(
                                hero = hero,
                                height = VaultSizes.heroHeight,
                                onWatch = { openHero(hero, onOpenDetails, onOpenComicDetails) },
                                onOpenDetails = { openHero(hero, onOpenDetails, onOpenComicDetails) },
                            )
                        }
                    }
                    item {
                        MediaRail("Continue Watching", state.continueWatching, onItemClick = { onOpenDetails(it.id) })
                    }
                    item {
                        MediaRail("Recently Added", state.recentlyAdded, onItemClick = { onOpenDetails(it.id) })
                    }

                    // Movies section, then a dedicated movie banner right below it.
                    item {
                        MediaRail("Movies", state.movies, onItemClick = { onOpenDetails(it.id) })
                    }
                    state.movieBanner?.let { movie ->
                        item {
                            SecondaryMediaBanner(item = movie, onClick = { onOpenDetails(movie.id) })
                        }
                    }

                    // Series section, then a dedicated series banner.
                    item {
                        MediaRail("Series", state.series, onItemClick = { onOpenDetails(it.id) })
                    }
                    state.seriesBanner?.let { series ->
                        item {
                            SecondaryMediaBanner(item = series, onClick = { onOpenDetails(series.id) })
                        }
                    }

                    item {
                        MediaRail("Anime", state.anime, onItemClick = { onOpenDetails(it.id) })
                    }

                    // Comics section, then a dedicated comics banner.
                    item {
                        ComicRail("Comics", state.comics, onItemClick = { onOpenComicDetails(it.id) })
                    }
                    state.comicsBanner?.let { comic ->
                        item {
                            SecondaryComicBanner(comic = comic, onClick = { onOpenComicDetails(comic.id) })
                        }
                    }
                }
            }
        }
    }
}

private fun openHero(hero: HeroCandidate, onOpenDetails: (Long) -> Unit, onOpenComicDetails: (Long) -> Unit) {
    when (hero) {
        is HeroCandidate.Media -> onOpenDetails(hero.id)
        is HeroCandidate.Comic -> onOpenComicDetails(hero.id)
    }
}

@Composable
private fun HomeTopBar(onOpenSearch: () -> Unit, onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = VaultSpacing.md, vertical = VaultSpacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "DarkVault",
            style = MaterialTheme.typography.headlineSmall,
            color = VaultColors.Orange,
        )
        Row {
            IconButton(onClick = onOpenSearch) {
                Icon(Icons.Filled.Search, contentDescription = "Search", tint = VaultColors.TextPrimary)
            }
        }
    }
}

@Composable
private fun HeroSection(
    hero: HeroCandidate,
    height: androidx.compose.ui.unit.Dp,
    onWatch: () -> Unit,
    onOpenDetails: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
    ) {
        if (hero.backdropUrl != null) {
            SubcomposeAsyncImage(
                model = hero.backdropUrl,
                contentDescription = hero.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { Box(modifier = Modifier.fillMaxSize().background(VaultColors.SurfaceVariant)) },
                error = { Box(modifier = Modifier.fillMaxSize().background(VaultColors.SurfaceVariant)) },
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(VaultColors.SurfaceVariant))
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, VaultColors.Background),
                        startY = 0f,
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(VaultSpacing.md)
        ) {
            Text(
                text = hero.title,
                style = MaterialTheme.typography.headlineLarge,
                color = VaultColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            hero.overview?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = VaultColors.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = VaultSpacing.xxs)
                )
            }
            Row(modifier = Modifier.padding(top = VaultSpacing.sm)) {
                Button(
                    onClick = onWatch,
                    shape = VaultShapes.button,
                    colors = ButtonDefaults.buttonColors(containerColor = VaultColors.Orange, contentColor = Color.White),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Text(text = if (hero is HeroCandidate.Comic) " Read" else " Watch", modifier = Modifier.padding(start = 2.dp))
                }
                OutlinedButton(
                    onClick = onOpenDetails,
                    shape = VaultShapes.button,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = VaultColors.TextPrimary),
                    modifier = Modifier.padding(start = VaultSpacing.sm)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text(text = " My List", modifier = Modifier.padding(start = 2.dp))
                }
            }
        }
    }
}

/**
 * Smaller, compact banner used between sections (e.g. below "Movies", below
 * "Series") — deliberately lower height than the main hero and with no
 * buttons, so the page reads as one movie/series highlighted per section
 * rather than looking cluttered with repeated full-size heroes.
 */
@Composable
private fun SecondaryMediaBanner(item: MediaItemEntity, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .padding(horizontal = VaultSpacing.md, vertical = VaultSpacing.xs)
            .clip(VaultShapes.card)
            .background(VaultColors.SurfaceVariant)
            .clickable(onClick = onClick)
    ) {
        if (item.backdropUrl != null || item.posterUrl != null) {
            SubcomposeAsyncImage(
                model = item.backdropUrl ?: item.posterUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {},
                error = {},
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(listOf(VaultColors.Background, Color.Transparent)))
        )
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium,
            color = VaultColors.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(VaultSpacing.sm)
        )
    }
}

@Composable
private fun SecondaryComicBanner(comic: ComicEntity, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .padding(horizontal = VaultSpacing.md, vertical = VaultSpacing.xs)
            .clip(VaultShapes.card)
            .background(VaultColors.SurfaceVariant)
            .clickable(onClick = onClick)
    ) {
        if (comic.coverUrl != null) {
            SubcomposeAsyncImage(
                model = comic.coverUrl,
                contentDescription = comic.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {},
                error = {},
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(listOf(VaultColors.Background, Color.Transparent)))
        )
        Text(
            text = comic.title,
            style = MaterialTheme.typography.titleMedium,
            color = VaultColors.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(VaultSpacing.sm)
        )
    }
}

