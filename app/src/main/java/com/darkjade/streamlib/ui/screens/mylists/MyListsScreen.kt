package com.darkjade.streamlib.ui.screens.mylists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.darkjade.streamlib.data.db.entity.MediaItemEntity
import com.darkjade.streamlib.ui.components.EmptyState
import com.darkjade.streamlib.ui.components.FallbackPoster
import com.darkjade.streamlib.ui.theme.VaultColors
import com.darkjade.streamlib.ui.theme.VaultShapes
import com.darkjade.streamlib.ui.theme.VaultSpacing

@Composable
fun MyListsScreen(
    viewModel: MyListsViewModel,
    onOpenDetails: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(VaultColors.Background)) {
        Text(
            "My Lists",
            style = MaterialTheme.typography.headlineSmall,
            color = VaultColors.TextPrimary,
            modifier = Modifier.padding(horizontal = VaultSpacing.md, vertical = VaultSpacing.sm)
        )

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = VaultSpacing.md)) {
            MyListsTab.entries.forEach { tab ->
                Text(
                    text = tab.label(),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (state.tab == tab) VaultColors.Orange else VaultColors.TextTertiary,
                    modifier = Modifier
                        .padding(end = VaultSpacing.md, bottom = VaultSpacing.xs)
                        .clickable { viewModel.setTab(tab) }
                )
            }
        }

        val items = when (state.tab) {
            MyListsTab.WATCHLIST -> state.watchlist
            MyListsTab.CONTINUE_WATCHING -> state.continueWatching
            MyListsTab.HISTORY -> state.continueWatching // history reuses continue-watching feed (dedup by media)
        }

        if (!state.isLoading && items.isEmpty()) {
            EmptyState(
                title = when (state.tab) {
                    MyListsTab.WATCHLIST -> "Your watchlist is empty."
                    MyListsTab.HISTORY -> "Nothing watched yet."
                    MyListsTab.CONTINUE_WATCHING -> "Nothing in progress."
                },
                message = "Titles you add or watch will show up here.",
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(vertical = VaultSpacing.sm),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items, key = { it.id }) { item ->
                    ListRow(item = item, onClick = { onOpenDetails(item.id) })
                }
            }
        }
    }
}

@Composable
private fun ListRow(item: MediaItemEntity, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = VaultSpacing.md, vertical = VaultSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp, 92.dp)
                .clip(VaultShapes.card)
        ) {
            if (item.posterUrl != null) {
                AsyncImage(
                    model = item.posterUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                FallbackPoster(title = item.title)
            }
        }
        Column(modifier = Modifier.padding(start = VaultSpacing.sm)) {
            Text(item.title, style = MaterialTheme.typography.titleSmall, color = VaultColors.TextPrimary)
            Text(
                item.type.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelSmall,
                color = VaultColors.TextSecondary,
            )
        }
    }
}

private fun MyListsTab.label() = when (this) {
    MyListsTab.WATCHLIST -> "Watchlist"
    MyListsTab.HISTORY -> "History"
    MyListsTab.CONTINUE_WATCHING -> "Continue Watching"
}
