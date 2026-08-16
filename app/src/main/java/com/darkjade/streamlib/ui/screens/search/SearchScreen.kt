package com.darkjade.streamlib.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.darkjade.streamlib.ui.components.EmptyState
import com.darkjade.streamlib.ui.components.MediaRail
import com.darkjade.streamlib.ui.components.PosterCard
import com.darkjade.streamlib.ui.theme.VaultColors
import com.darkjade.streamlib.ui.theme.VaultShapes
import com.darkjade.streamlib.ui.theme.VaultSpacing

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onOpenDetails: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(VaultColors.Background)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            placeholder = { Text("Search movies, series, anime, episodes") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = VaultColors.TextSecondary) },
            singleLine = true,
            shape = VaultShapes.button, // fully rounded pill search field, matching the reference
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VaultColors.Orange,
                unfocusedBorderColor = VaultColors.Divider,
                focusedContainerColor = VaultColors.Surface,
                unfocusedContainerColor = VaultColors.Surface,
                focusedTextColor = VaultColors.TextPrimary,
                unfocusedTextColor = VaultColors.TextPrimary,
                cursorColor = VaultColors.Orange,
            ),
            modifier = Modifier.fillMaxWidth().padding(VaultSpacing.md)
        )

        when {
            state.isSearching -> CircularProgressIndicator(
                color = VaultColors.Orange,
                modifier = Modifier.padding(VaultSpacing.xl)
            )
            state.hasSearched && state.results.isEmpty() -> EmptyState(
                title = "No results found.",
                message = "Try a different title or keyword.",
            )
            state.results.isNotEmpty() -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 110.dp),
                    contentPadding = PaddingValues(horizontal = VaultSpacing.md, vertical = VaultSpacing.md),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(VaultSpacing.sm),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(VaultSpacing.sm),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.results, key = { it.id }) { item ->
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
            state.todaysTopPicks.isEmpty() && state.weeklyPicks.isEmpty() && state.recentlyAdded.isEmpty() -> {
                EmptyState(
                    title = "Search your local library",
                    message = "Movies, series, anime, and episodes will show up here once scanned.",
                )
            }
            else -> {
                // Idle state before any query is typed — browse-style default content
                // instead of a blank screen.
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = VaultSpacing.xxl)) {
                    item {
                        MediaRail("Today's Top Picks", state.todaysTopPicks, onItemClick = { onOpenDetails(it.id) }, onAddToList = viewModel::addToWatchlist, onRemoveFromLibrary = viewModel::removeFromLibrary)
                    }
                    item {
                        MediaRail("Weekly Picks", state.weeklyPicks, onItemClick = { onOpenDetails(it.id) }, onAddToList = viewModel::addToWatchlist, onRemoveFromLibrary = viewModel::removeFromLibrary)
                    }
                    item {
                        MediaRail("Recently Added", state.recentlyAdded, onItemClick = { onOpenDetails(it.id) }, onAddToList = viewModel::addToWatchlist, onRemoveFromLibrary = viewModel::removeFromLibrary)
                    }
                }
            }
        }
    }
}
