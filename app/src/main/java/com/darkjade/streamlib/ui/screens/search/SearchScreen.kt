package com.darkjade.streamlib.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.darkjade.streamlib.ui.components.EmptyState
import com.darkjade.streamlib.ui.components.PosterCard
import com.darkjade.streamlib.ui.theme.VaultColors
import com.darkjade.streamlib.ui.theme.VaultSpacing

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onOpenDetails: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(VaultColors.Background).padding(VaultSpacing.md)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            placeholder = { Text("Search movies, series, anime, episodes") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = VaultColors.TextSecondary) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VaultColors.Orange,
                unfocusedBorderColor = VaultColors.Divider,
                focusedTextColor = VaultColors.TextPrimary,
                unfocusedTextColor = VaultColors.TextPrimary,
                cursorColor = VaultColors.Orange,
            ),
            modifier = Modifier.fillMaxWidth()
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
                    contentPadding = PaddingValues(vertical = VaultSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(VaultSpacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(VaultSpacing.sm),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.results, key = { it.id }) { item ->
                        PosterCard(item = item, onClick = { onOpenDetails(item.id) })
                    }
                }
            }
            else -> {
                // Idle state before any query is typed.
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        "Search your local library",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VaultColors.TextTertiary,
                    )
                }
            }
        }
    }
}
