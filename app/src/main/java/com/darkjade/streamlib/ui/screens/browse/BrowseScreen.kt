package com.darkjade.streamlib.ui.screens.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.darkjade.streamlib.ui.components.EmptyState
import com.darkjade.streamlib.ui.components.PosterCard
import com.darkjade.streamlib.ui.theme.VaultColors
import com.darkjade.streamlib.ui.theme.VaultSpacing

@Composable
fun BrowseScreen(
    viewModel: BrowseViewModel,
    onOpenDetails: (Long) -> Unit,
    onOpenComicDetails: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(VaultColors.Background)) {
        Text(
            "Browse",
            style = MaterialTheme.typography.headlineSmall,
            color = VaultColors.TextPrimary,
            modifier = Modifier.padding(horizontal = VaultSpacing.md, vertical = VaultSpacing.sm)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(VaultSpacing.xs),
            contentPadding = PaddingValues(horizontal = VaultSpacing.md),
        ) {
            items(BrowseCategory.entries.toList()) { category ->
                FilterChip(
                    selected = state.category == category,
                    onClick = { viewModel.setCategory(category) },
                    label = { Text(category.label()) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VaultColors.Orange,
                        selectedLabelColor = Color.White,
                        containerColor = VaultColors.SurfaceVariant,
                        labelColor = VaultColors.TextSecondary,
                    )
                )
            }
        }

        if (state.genres.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(VaultSpacing.xs),
                contentPadding = PaddingValues(horizontal = VaultSpacing.md, vertical = VaultSpacing.xs),
            ) {
                items(state.genres) { genre ->
                    FilterChip(
                        selected = state.selectedGenre == genre,
                        onClick = { viewModel.setGenre(if (state.selectedGenre == genre) null else genre) },
                        label = { Text(genre) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VaultColors.OrangeDim,
                            selectedLabelColor = Color.White,
                            containerColor = VaultColors.SurfaceVariant,
                            labelColor = VaultColors.TextSecondary,
                        )
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = VaultSpacing.md, vertical = VaultSpacing.xs),
            horizontalArrangement = Arrangement.End,
        ) {
            SortOrder.entries.forEach { order ->
                Text(
                    text = order.label(),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (state.sortOrder == order) VaultColors.Orange else VaultColors.TextTertiary,
                    modifier = Modifier
                        .padding(start = VaultSpacing.sm)
                        .clickable { viewModel.setSortOrder(order) }
                )
            }
        }

        when {
            state.isLoading -> CircularProgressIndicator(
                color = VaultColors.Orange,
                modifier = Modifier.padding(VaultSpacing.xl)
            )
            state.category == BrowseCategory.COMICS -> {
                if (state.comics.isEmpty()) {
                    EmptyState(
                        title = "No comics found",
                        message = "Add a comics folder from Settings to get started.",
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 110.dp),
                        contentPadding = PaddingValues(VaultSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(VaultSpacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(VaultSpacing.sm),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        gridItems(state.comics, key = { it.id }) { comic ->
                            com.darkjade.streamlib.ui.components.ComicCard(comic = comic, onClick = { onOpenComicDetails(comic.id) })
                        }
                    }
                }
            }
            state.displayedItems.isEmpty() -> EmptyState(
                title = "No results found",
                message = "Try a different category or filter.",
            )
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 110.dp),
                    contentPadding = PaddingValues(VaultSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(VaultSpacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(VaultSpacing.sm),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    gridItems(state.displayedItems, key = { it.id }) { item ->
                        PosterCard(
                            item = item,
                            onClick = { onOpenDetails(item.id) },
                            showMenu = true,
                            onAddToList = { viewModel.addToWatchlist(item) },
                            onRemoveFromLibrary = { viewModel.removeFromLibrary(item) },
                        )
                    }
                }
            }
        }
    }
}

private fun BrowseCategory.label() = when (this) {
    BrowseCategory.ALL -> "All"
    BrowseCategory.MOVIES -> "Movies"
    BrowseCategory.SERIES -> "Series"
    BrowseCategory.ANIME -> "Anime"
    BrowseCategory.COMICS -> "Comics"
}

private fun SortOrder.label() = when (this) {
    SortOrder.RECENTLY_ADDED -> "Recent"
    SortOrder.A_Z -> "A-Z"
    SortOrder.YEAR -> "Year"
}
