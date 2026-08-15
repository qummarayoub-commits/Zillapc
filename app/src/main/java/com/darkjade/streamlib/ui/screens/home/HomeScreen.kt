package com.darkjade.streamlib.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import kotlinx.coroutines.delay

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

                    // Top: main auto-rotating carousel across everything (up to 5: movies, series, comics).
                    if (state.heroItems.isNotEmpty()) {
                        item {
                            MainHeroCarousel(
                                items = state.heroItems,
                                onWatch = { openHero(it, onOpenDetails, onOpenComicDetails) },
                                onOpenDetails = { openHero(it, onOpenDetails, onOpenComicDetails) },
                            )
                        }
                    }

                    item {
                        MediaRail("Continue Watching", state.continueWatching, onItemClick = { onOpenDetails(it.id) })
                    }
                    item {
                        MediaRail("Recently Added", state.recentlyAdded, onItemClick = { onOpenDetails(it.id) })
                    }

                    // Movies section, then an auto-rotating movie banner strip below it.
                    item {
                        MediaRail("Movies", state.movies, onItemClick = { onOpenDetails(it.id) })
                    }
                    if (state.movieBanners.isNotEmpty()) {
                        item {
                            SecondaryBannerCarousel(
                                items = state.movieBanners,
                                categoryLabel = "Movie",
                                imageUrl = { it.backdropUrl ?: it.posterUrl ?: it.localFileUri },
                                title = { it.title },
                                onClick = { onOpenDetails(it.id) },
                            )
                        }
                    }

                    // Series section, then an auto-rotating series banner strip.
                    item {
                        MediaRail("Series", state.series, onItemClick = { onOpenDetails(it.id) })
                    }
                    if (state.seriesBanners.isNotEmpty()) {
                        item {
                            SecondaryBannerCarousel(
                                items = state.seriesBanners,
                                categoryLabel = "Series",
                                imageUrl = { it.backdropUrl ?: it.posterUrl },
                                title = { it.title },
                                onClick = { onOpenDetails(it.id) },
                            )
                        }
                    }

                    item {
                        MediaRail("Anime", state.anime, onItemClick = { onOpenDetails(it.id) })
                    }

                    // Comics section, then an auto-rotating comics banner strip.
                    item {
                        ComicRail("Comics", state.comics, onItemClick = { onOpenComicDetails(it.id) })
                    }
                    if (state.comicsBanners.isNotEmpty()) {
                        item {
                            SecondaryBannerCarousel(
                                items = state.comicsBanners,
                                categoryLabel = "Comic",
                                imageUrl = { it.coverUrl },
                                title = { it.title },
                                onClick = { onOpenComicDetails(it.id) },
                            )
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

/** Main top-of-Home banner: up to 5 items, auto-advancing, with page dots — Crunchyroll-style. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MainHeroCarousel(
    items: List<HeroCandidate>,
    onWatch: (HeroCandidate) -> Unit,
    onOpenDetails: (HeroCandidate) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { items.size })

    LaunchedEffect(items.size) {
        if (items.size <= 1) return@LaunchedEffect
        while (true) {
            delay(5000)
            val next = (pagerState.currentPage + 1) % items.size
            pagerState.animateScrollToPage(next)
        }
    }

    Box(modifier = Modifier.fillMaxWidth().height(VaultSizes.heroHeight)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val hero = items[page]
            Box(modifier = Modifier.fillMaxSize()) {
                if (hero.backdropUrl != null) {
                    SubcomposeAsyncImage(
                        model = hero.backdropUrl,
                        contentDescription = hero.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = { Box(Modifier.fillMaxSize().background(VaultColors.SurfaceVariant)) },
                        error = { Box(Modifier.fillMaxSize().background(VaultColors.SurfaceVariant)) },
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
                        .padding(bottom = VaultSpacing.md)
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
                            onClick = { onWatch(hero) },
                            shape = VaultShapes.button,
                            colors = ButtonDefaults.buttonColors(containerColor = VaultColors.Orange, contentColor = Color.White),
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Text(text = if (hero is HeroCandidate.Comic) " Read" else " Watch", modifier = Modifier.padding(start = 2.dp))
                        }
                        OutlinedButton(
                            onClick = { onOpenDetails(hero) },
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
        if (items.size > 1) {
            DotIndicators(
                count = items.size,
                currentPage = pagerState.currentPage,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = VaultSpacing.xs)
            )
        }
    }
}

/**
 * Compact auto-rotating strip used between sections (below "Movies", below
 * "Series", below "Comics") — smaller than the main hero, with a small
 * category chip + title overlaid directly on the image (Netflix-style),
 * and its own page dots. Cycles through a small set of items on its own,
 * independent of the main hero carousel.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun <T> SecondaryBannerCarousel(
    items: List<T>,
    categoryLabel: String,
    imageUrl: (T) -> String?,
    title: (T) -> String,
    onClick: (T) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { items.size })

    LaunchedEffect(items.size) {
        if (items.size <= 1) return@LaunchedEffect
        while (true) {
            delay(4000)
            val next = (pagerState.currentPage + 1) % items.size
            pagerState.animateScrollToPage(next)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .padding(horizontal = VaultSpacing.md, vertical = VaultSpacing.xs)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .clip(VaultShapes.card)
        ) { page ->
            val item = items[page]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(VaultColors.SurfaceVariant)
                    .clickable { onClick(item) }
            ) {
                val model = imageUrl(item)
                if (model != null) {
                    SubcomposeAsyncImage(
                        model = model,
                        contentDescription = title(item),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = {},
                        error = {},
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, VaultColors.Background.copy(alpha = 0.9f)),
                                startY = 60f,
                            )
                        )
                )
                Column(modifier = Modifier.align(Alignment.BottomStart).padding(VaultSpacing.sm)) {
                    Box(
                        modifier = Modifier
                            .clip(VaultShapes.chip)
                            .background(VaultColors.Orange)
                            .padding(horizontal = VaultSpacing.xs, vertical = 2.dp)
                    ) {
                        Text(categoryLabel, style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }
                    Text(
                        text = title(item),
                        style = MaterialTheme.typography.titleMedium,
                        color = VaultColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = VaultSpacing.xxs)
                    )
                }
            }
        }
        if (items.size > 1) {
            DotIndicators(
                count = items.size,
                currentPage = pagerState.currentPage,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = VaultSpacing.xxs)
            )
        }
    }
}

@Composable
private fun DotIndicators(count: Int, currentPage: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(count) { index ->
            val selected = index == currentPage
            Box(
                modifier = Modifier
                    .size(if (selected) 7.dp else 5.dp)
                    .clip(CircleShape)
                    .background(if (selected) VaultColors.Orange else Color.White.copy(alpha = 0.4f))
            )
        }
    }
}
