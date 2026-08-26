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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
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
import com.darkjade.streamlib.ui.components.ContinueWatchingCard
import com.darkjade.streamlib.ui.components.EmptyState
import com.darkjade.streamlib.ui.components.MediaRail
import com.darkjade.streamlib.ui.theme.VaultColors
import com.darkjade.streamlib.ui.theme.VaultShapes
import com.darkjade.streamlib.ui.theme.VaultSizes
import com.darkjade.streamlib.ui.theme.VaultSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenDetails: (Long) -> Unit,
    onOpenComicDetails: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenNews: () -> Unit,
    onOpenBrowse: () -> Unit,
    onOpenMusic: () -> Unit = {},
    onOpenAccount: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    var selectedCategory by remember { mutableStateOf<HomeCategoryFilter?>(null) }

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
                        HomeTopBar(onOpenSearch = onOpenSearch, onOpenSettings = onOpenSettings, onOpenNews = onOpenNews, onOpenAccount = onOpenAccount)
                    }
                    item {
                        HomeCategoryPills(
                            selected = selectedCategory,
                            onSelect = { selectedCategory = it },
                        )
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

                    if (state.continueWatching.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.padding(top = 32.dp)) {
                                Text(
                                    "Continue Watching",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = VaultColors.TextPrimary,
                                    modifier = Modifier.padding(horizontal = VaultSpacing.md, vertical = VaultSpacing.sm)
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(VaultSpacing.sm),
                                    contentPadding = PaddingValues(horizontal = VaultSpacing.md)
                                ) {
                                    items(state.continueWatching, key = { it.id }) { item ->
                                        ContinueWatchingCard(item = item, watchedFraction = null, onClick = { onOpenDetails(item.id) })
                                    }
                                }
                            }
                        }
                    }
                    item {
                        MediaRail("Recently Added", state.recentlyAdded, onItemClick = { onOpenDetails(it.id) }, onAddToList = viewModel::addToWatchlist, onRemoveFromLibrary = viewModel::removeFromLibrary)
                    }

                    // Small horizontal Music section — hidden entirely if there's no music.
                    if (state.recentSongs.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.padding(top = 40.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = VaultSpacing.md, vertical = VaultSpacing.sm),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text("Music", style = MaterialTheme.typography.titleMedium, color = VaultColors.TextPrimary)
                                    Text(
                                        "See All",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = VaultColors.Orange,
                                        modifier = Modifier.clickable(onClick = onOpenMusic)
                                    )
                                }
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(VaultSpacing.sm),
                                    contentPadding = PaddingValues(horizontal = VaultSpacing.md)
                                ) {
                                    items(state.recentSongs, key = { it.id }) { song ->
                                        Column(modifier = Modifier.width(100.dp).clickable { onOpenMusic() }) {
                                            Box(
                                                modifier = Modifier.size(100.dp).clip(RoundedCornerShape(6.dp)).background(VaultColors.SurfaceVariant),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                if (song.artworkPath != null) {
                                                    SubcomposeAsyncImage(
                                                        model = song.artworkPath,
                                                        contentDescription = song.title,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize(),
                                                        loading = { Icon(Icons.Filled.MusicNote, contentDescription = null, tint = VaultColors.TextTertiary) },
                                                        error = { Icon(Icons.Filled.MusicNote, contentDescription = null, tint = VaultColors.TextTertiary) },
                                                    )
                                                } else {
                                                    Icon(Icons.Filled.MusicNote, contentDescription = null, tint = VaultColors.TextTertiary)
                                                }
                                            }
                                            Text(song.title, style = MaterialTheme.typography.bodySmall, color = VaultColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = VaultSpacing.xxs))
                                            Text(song.artist, style = MaterialTheme.typography.labelSmall, color = VaultColors.TextTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Movies: banner ABOVE the content row, then the row itself.
                    val showMovies = selectedCategory == null || selectedCategory == HomeCategoryFilter.MOVIES
                    if (showMovies && state.movieBanners.isNotEmpty()) {
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
                    if (showMovies) {
                        item {
                            MediaRail("Movies", state.movies, onItemClick = { onOpenDetails(it.id) }, onAddToList = viewModel::addToWatchlist, onRemoveFromLibrary = viewModel::removeFromLibrary)
                        }
                    }

                    // Series: banner ABOVE the content row, then the row itself.
                    val showSeries = selectedCategory == null || selectedCategory == HomeCategoryFilter.SERIES
                    if (showSeries && state.seriesBanners.isNotEmpty()) {
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
                    if (showSeries) {
                        item {
                            MediaRail("Series", state.series, onItemClick = { onOpenDetails(it.id) }, onAddToList = viewModel::addToWatchlist, onRemoveFromLibrary = viewModel::removeFromLibrary)
                        }
                    }

                    // Anime: banner ABOVE the content row, then the row itself.
                    val showAnime = selectedCategory == null || selectedCategory == HomeCategoryFilter.ANIME
                    if (showAnime && state.animeBanners.isNotEmpty()) {
                        item {
                            SecondaryBannerCarousel(
                                items = state.animeBanners,
                                categoryLabel = "Anime",
                                imageUrl = { it.backdropUrl ?: it.posterUrl },
                                title = { it.title },
                                onClick = { onOpenDetails(it.id) },
                            )
                        }
                    }
                    if (showAnime) {
                        item {
                            MediaRail("Anime", state.anime, onItemClick = { onOpenDetails(it.id) }, onAddToList = viewModel::addToWatchlist, onRemoveFromLibrary = viewModel::removeFromLibrary)
                        }
                    }

                    // Comics: banner ABOVE the content row, then the row itself.
                    val showComics = selectedCategory == null || selectedCategory == HomeCategoryFilter.COMICS
                    if (showComics && state.comicsBanners.isNotEmpty()) {
                        item {
                            SecondaryBannerCarousel(
                                items = state.comicsBanners,
                                categoryLabel = "Comic",
                                imageUrl = { it.coverUrl },
                                title = { it.title },
                                onClick = { onOpenComicDetails(it.id) },
                                // Comic covers are portrait — cropping them into a wide
                                // landscape strip like movie/series backdrops butchered
                                // the artwork. Fit them centered on a blurred backdrop instead.
                                isPortraitContent = true,
                            )
                        }
                    }
                    if (showComics) {
                        item {
                            ComicRail("Comics", state.comics, onItemClick = { onOpenComicDetails(it.id) })
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

private enum class HomeCategoryFilter { MOVIES, SERIES, ANIME, COMICS }

/** Purple rounded-pill category selector under the header — filters which
 * category sections are shown below (Continue Watching/Recently Added stay
 * visible regardless, since they aren't category-specific). */
@Composable
private fun HomeCategoryPills(selected: HomeCategoryFilter?, onSelect: (HomeCategoryFilter?) -> Unit) {
    val categories = listOf(
        null to "All",
        HomeCategoryFilter.MOVIES to "Movies",
        HomeCategoryFilter.SERIES to "Series",
        HomeCategoryFilter.ANIME to "Anime",
        HomeCategoryFilter.COMICS to "Comics",
    )
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(VaultSpacing.xs),
        contentPadding = PaddingValues(horizontal = VaultSpacing.md, vertical = VaultSpacing.sm),
    ) {
        items(categories) { (value, label) ->
            val isSelected = selected == value
            Box(
                modifier = Modifier
                    .clip(VaultShapes.chip)
                    .background(if (isSelected) VaultColors.Orange else VaultColors.Surface)
                    .clickable { onSelect(value) }
                    .padding(horizontal = VaultSpacing.md, vertical = VaultSpacing.xs)
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) Color.White else VaultColors.TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun HomeTopBar(onOpenSearch: () -> Unit, onOpenSettings: () -> Unit, onOpenNews: () -> Unit, onOpenAccount: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = VaultSpacing.md, vertical = VaultSpacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = com.darkjade.streamlib.R.drawable.logo_velora),
            contentDescription = "App logo",
            modifier = Modifier.height(36.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onOpenNews) {
                Icon(Icons.Filled.Article, contentDescription = "News", tint = VaultColors.TextPrimary)
            }
            IconButton(onClick = onOpenSearch) {
                Icon(Icons.Filled.Search, contentDescription = "Search", tint = VaultColors.TextPrimary)
            }
            IconButton(onClick = onOpenAccount) {
                Icon(Icons.Filled.AccountCircle, contentDescription = "Account", tint = VaultColors.TextPrimary)
            }
        }
    }
}

/** Main top-of-Home banner: up to 6 items, auto-advancing, with page indicators — Crunchyroll-style. */
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
                hero.rating?.let { rating ->
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = VaultSpacing.xl, end = VaultSpacing.md)
                            .clip(VaultShapes.chip)
                            .background(Color.Black.copy(alpha = 0.45f))
                            .padding(horizontal = VaultSpacing.sm, vertical = VaultSpacing.xxs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = VaultColors.PremiumGold, modifier = Modifier.size(14.dp))
                        Text(
                            " ${"%.1f".format(rating)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = VaultColors.TextPrimary,
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(VaultSpacing.md)
                        .padding(bottom = VaultSpacing.lg)
                ) {
                    hero.metaLine?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelMedium,
                            color = VaultColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = hero.title,
                        style = MaterialTheme.typography.headlineLarge,
                        color = VaultColors.TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
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
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = VaultSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(
                            onClick = { onWatch(hero) },
                            shape = VaultShapes.button,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = VaultColors.Orange),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Text(
                                text = if (hero is HeroCandidate.Comic) " Read Now" else " Watch Now",
                                style = MaterialTheme.typography.titleSmall,
                                color = VaultColors.Orange,
                                modifier = Modifier.padding(start = 2.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .padding(start = VaultSpacing.sm)
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.35f))
                                .clickable { onOpenDetails(hero) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Save to list", tint = Color.White)
                        }
                    }
                }
            }
        }
        if (items.size > 1) {
            SegmentedPageIndicators(
                count = items.size,
                currentPage = pagerState.currentPage,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = VaultSpacing.xxs).padding(horizontal = VaultSpacing.md)
            )
        }
    }
}

/**
 * Two large banner cards side by side — a fresh random pair each time the
 * app opens (picked once per Home session by the ViewModel, not re-shuffled
 * on a timer). Applies the same treatment to Movies/Series/Anime/Comics.
 */
@Composable
private fun <T> SecondaryBannerCarousel(
    items: List<T>,
    categoryLabel: String,
    imageUrl: (T) -> String?,
    title: (T) -> String,
    onClick: (T) -> Unit,
    isPortraitContent: Boolean = false,
) {
    val pair = items.take(2)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = VaultSpacing.md)
            .padding(top = 40.dp),
        horizontalArrangement = Arrangement.spacedBy(VaultSpacing.sm),
    ) {
        pair.forEach { item ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(260.dp) // same size for Movies/Series/Anime/Comics banners
                    .clip(VaultShapes.card)
                    .background(VaultColors.SurfaceVariant)
                    .clickable { onClick(item) }
            ) {
                val model = imageUrl(item)
                if (model != null) {
                    if (isPortraitContent) {
                        SubcomposeAsyncImage(
                            model = model,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().blur(24.dp),
                            loading = {},
                            error = {},
                        )
                        SubcomposeAsyncImage(
                            model = model,
                            contentDescription = title(item),
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(vertical = VaultSpacing.xs),
                            loading = {},
                            error = {},
                        )
                    } else {
                        SubcomposeAsyncImage(
                            model = model,
                            contentDescription = title(item),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            loading = {},
                            error = {},
                        )
                    }
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
                        Text(categoryLabel, style = MaterialTheme.typography.labelSmall, color = VaultColors.Background)
                    }
                    Text(
                        text = title(item),
                        style = MaterialTheme.typography.titleSmall,
                        color = VaultColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = VaultSpacing.xxs)
                    )
                }
            }
        }
    }
}

/** Thin segmented capsule bars (not small dots) — matches the reference's page-position style. */
@Composable
private fun SegmentedPageIndicators(count: Int, currentPage: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(count) { index ->
            val selected = index == currentPage
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(VaultShapes.chip)
                    .background(if (selected) VaultColors.Orange else Color.White.copy(alpha = 0.3f))
            )
        }
    }
}
