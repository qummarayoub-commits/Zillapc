package com.darkjade.streamlib.ui.screens.news

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.darkjade.streamlib.data.db.entity.NewsCategory
import com.darkjade.streamlib.ui.components.EmptyState
import com.darkjade.streamlib.ui.theme.VaultColors
import com.darkjade.streamlib.ui.theme.VaultShapes
import com.darkjade.streamlib.ui.theme.VaultSpacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Keeps the user inside the app when they tap a news card, per the fix
 * request — shows whatever headline/summary/image the RSS source gave us,
 * then an explicit "Open Original Source" button for the full article
 * (never scrapes/reproduces the full copyrighted article body).
 */
@Composable
fun NewsArticleDetailsScreen(
    viewModel: NewsArticleDetailsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize().background(VaultColors.Background)) {
        when {
            state.isLoading -> CircularProgressIndicator(color = VaultColors.Orange, modifier = Modifier.align(Alignment.Center))
            state.article == null -> EmptyState(title = "Article not found", message = "This article is no longer available.")
            else -> {
                val article = state.article!!
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(VaultSpacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = VaultColors.TextPrimary)
                            }
                        }
                    }

                    if (article.imageUrl != null) {
                        item {
                            SubcomposeAsyncImage(
                                model = article.imageUrl,
                                contentDescription = article.headline,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .padding(horizontal = VaultSpacing.md),
                                loading = {},
                                error = {},
                            )
                        }
                    }

                    item {
                        Column(modifier = Modifier.padding(VaultSpacing.md)) {
                            Text(
                                article.headline,
                                style = MaterialTheme.typography.headlineSmall,
                                color = VaultColors.TextPrimary,
                            )
                            Text(
                                "${article.category.label()} \u2022 ${article.sourceName} \u2022 ${formatDate(article.publishedAt)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = VaultColors.Orange,
                                modifier = Modifier.padding(top = VaultSpacing.xs)
                            )
                            article.excerpt?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = VaultColors.TextSecondary,
                                    modifier = Modifier.padding(top = VaultSpacing.md)
                                )
                            }

                            Text(
                                "This is a summary from ${article.sourceName}. Read the full story at the original source.",
                                style = MaterialTheme.typography.labelSmall,
                                color = VaultColors.TextTertiary,
                                modifier = Modifier.padding(top = VaultSpacing.lg)
                            )

                            Button(
                                onClick = {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(article.articleUrl)))
                                    } catch (e: Exception) {
                                        // No browser available — fail silently rather than crash.
                                    }
                                },
                                shape = VaultShapes.button,
                                colors = ButtonDefaults.buttonColors(containerColor = VaultColors.Orange, contentColor = Color.White),
                                modifier = Modifier.fillMaxWidth().padding(top = VaultSpacing.md)
                            ) {
                                Icon(Icons.Filled.OpenInBrowser, contentDescription = null)
                                Text(" Open Original Source", modifier = Modifier.padding(start = 4.dp))
                            }
                        }
                    }
                }
            }
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

private fun formatDate(ms: Long): String = SimpleDateFormat("MMM d, yyyy \u00b7 h:mm a", Locale.US).format(Date(ms))
