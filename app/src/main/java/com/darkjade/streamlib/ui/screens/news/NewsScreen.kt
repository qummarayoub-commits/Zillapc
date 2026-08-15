package com.darkjade.streamlib.ui.screens.news

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.darkjade.streamlib.data.db.entity.NewsArticleEntity
import com.darkjade.streamlib.data.db.entity.NewsCategory
import com.darkjade.streamlib.ui.components.EmptyState
import com.darkjade.streamlib.ui.theme.VaultColors
import com.darkjade.streamlib.ui.theme.VaultShapes
import com.darkjade.streamlib.ui.theme.VaultSpacing
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun NewsScreen(
    viewModel: NewsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().background(VaultColors.Background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(VaultSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = VaultColors.TextPrimary)
            }
            Text("News", style = MaterialTheme.typography.headlineSmall, color = VaultColors.TextPrimary)
        }

        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            placeholder = { Text("Search a movie, series, anime, comic…") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = VaultColors.TextSecondary) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VaultColors.Orange,
                unfocusedBorderColor = VaultColors.Divider,
                focusedTextColor = VaultColors.TextPrimary,
                unfocusedTextColor = VaultColors.TextPrimary,
                cursorColor = VaultColors.Orange,
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = VaultSpacing.md)
        )

        if (state.searchResults == null) {
            // Category filter chips — only relevant for the browse feed, not search results.
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(VaultSpacing.xs),
                contentPadding = PaddingValues(horizontal = VaultSpacing.md, vertical = VaultSpacing.sm),
            ) {
                item {
                    CategoryChip("All", state.selectedCategory == null) { viewModel.setCategory(null) }
                }
                items(NewsCategory.entries.toList()) { category ->
                    CategoryChip(category.label(), state.selectedCategory == category) { viewModel.setCategory(category) }
                }
            }
        }

        val displayedArticles = state.searchResults ?: run {
            val cat = state.selectedCategory
            if (cat == null) state.allArticles else state.allArticles.filter { it.category == cat }
        }

        when {
            state.isLoading -> CircularProgressIndicator(color = VaultColors.Orange, modifier = Modifier.padding(VaultSpacing.xl))
            state.isOffline && displayedArticles.isEmpty() -> EmptyState(
                title = "You're offline",
                message = "Connect to the internet to load the latest news.",
                actionLabel = "Retry",
                onAction = { viewModel.refresh() },
            )
            state.searchResults != null && displayedArticles.isEmpty() -> EmptyState(
                title = "No results found.",
                message = "Try a different title or keyword.",
            )
            displayedArticles.isEmpty() -> EmptyState(
                title = "No news yet",
                message = "Pull to refresh, or check back soon.",
                actionLabel = "Refresh",
                onAction = { viewModel.refresh() },
            )
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = VaultSpacing.xl)
                ) {
                    items(displayedArticles, key = { it.id }) { article ->
                        NewsArticleCard(article = article, onClick = {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(article.articleUrl)))
                            } catch (e: Exception) {
                                // No browser available — fail silently rather than crash.
                            }
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = VaultColors.Orange,
            selectedLabelColor = androidx.compose.ui.graphics.Color.White,
            containerColor = VaultColors.SurfaceVariant,
            labelColor = VaultColors.TextSecondary,
        )
    )
}

@Composable
private fun NewsArticleCard(article: NewsArticleEntity, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = VaultSpacing.md, vertical = VaultSpacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(96.dp, 72.dp)
                .clip(VaultShapes.card)
                .background(VaultColors.SurfaceVariant)
        ) {
            if (article.imageUrl != null) {
                SubcomposeAsyncImage(
                    model = article.imageUrl,
                    contentDescription = article.headline,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = {},
                    error = {},
                )
            } else {
                Icon(
                    Icons.Filled.Article,
                    contentDescription = null,
                    tint = VaultColors.TextTertiary,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
        Column(modifier = Modifier.padding(start = VaultSpacing.sm).weight(1f)) {
            Text(
                article.headline,
                style = MaterialTheme.typography.titleSmall,
                color = VaultColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            article.excerpt?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = VaultColors.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Text(
                "${article.category.label()} • ${article.sourceName} • ${relativeTime(article.publishedAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = VaultColors.Orange,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

private fun NewsCategory.label() = when (this) {
    NewsCategory.MOVIES -> "Movies"
    NewsCategory.SERIES -> "Series"
    NewsCategory.ANIME -> "Anime"
    NewsCategory.ANIMATION -> "Animation"
    NewsCategory.COMICS -> "Comics"
}

private fun relativeTime(publishedAtMs: Long): String {
    val diffMs = System.currentTimeMillis() - publishedAtMs
    if (diffMs < 0) return "just now"
    val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
    return when {
        hours < 1 -> "${TimeUnit.MILLISECONDS.toMinutes(diffMs).coerceAtLeast(1)}m ago"
        hours < 24 -> "${hours}h ago"
        hours < 24 * 7 -> "${hours / 24}d ago"
        else -> SimpleDateFormat("MMM d", Locale.US).format(java.util.Date(publishedAtMs))
    }
}
