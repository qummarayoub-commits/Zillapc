package com.darkjade.streamlib.ui.screens.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.OutlinedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.darkjade.streamlib.data.db.entity.MediaItemEntity
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
                    message = "Add a folder to start building your library.",
                    actionLabel = "Add Folder",
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
                    state.hero?.let { hero ->
                        item {
                            HeroSection(item = hero, onWatch = { onOpenDetails(hero.id) }, onOpenDetails = { onOpenDetails(hero.id) })
                        }
                    }
                    item {
                        MediaRail("Continue Watching", state.continueWatching, onItemClick = { onOpenDetails(it.id) })
                    }
                    item {
                        MediaRail("Recently Added", state.recentlyAdded, onItemClick = { onOpenDetails(it.id) })
                    }
                    item {
                        MediaRail("Movies", state.movies, onItemClick = { onOpenDetails(it.id) })
                    }
                    item {
                        MediaRail("Series", state.series, onItemClick = { onOpenDetails(it.id) })
                    }
                    item {
                        MediaRail("Anime", state.anime, onItemClick = { onOpenDetails(it.id) })
                    }
                }
            }
        }
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
    item: MediaItemEntity,
    onWatch: () -> Unit,
    onOpenDetails: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(VaultSizes.heroHeight)
    ) {
        if (item.backdropUrl != null) {
            AsyncImage(
                model = item.backdropUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
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
                text = item.title,
                style = MaterialTheme.typography.headlineLarge,
                color = VaultColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            item.overview?.let {
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
                    Text(text = " Watch", modifier = Modifier.padding(start = 2.dp))
                }
                OutlinedButton(
                    onClick = onOpenDetails,
                    shape = VaultShapes.button,
                    colors = OutlinedButtonDefaults.outlinedButtonColors(contentColor = VaultColors.TextPrimary),
                    modifier = Modifier.padding(start = VaultSpacing.sm)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text(text = " My List", modifier = Modifier.padding(start = 2.dp))
                }
            }
        }
    }
}
