package com.darkjade.streamlib.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.darkjade.streamlib.data.db.entity.ComicEntity
import com.darkjade.streamlib.ui.theme.VaultColors
import com.darkjade.streamlib.ui.theme.VaultShapes
import com.darkjade.streamlib.ui.theme.VaultSizes
import com.darkjade.streamlib.ui.theme.VaultSpacing

@Composable
fun ComicCard(
    comic: ComicEntity,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .width(VaultSizes.posterWidth) // intrinsic height — same overflow fix as PosterCard
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(VaultSizes.posterWidth, VaultSizes.posterHeight)
                .clip(VaultShapes.card)
                .background(VaultColors.SurfaceVariant)
        ) {
            if (comic.coverUrl != null) {
                SubcomposeAsyncImage(
                    model = comic.coverUrl,
                    contentDescription = comic.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = { ComicFallbackCover(title = comic.title) },
                    error = { ComicFallbackCover(title = comic.title) },
                )
            } else {
                ComicFallbackCover(title = comic.title)
            }
            // Small circular "read" affordance — consistent with the play-icon
            // treatment on video posters, purely decorative.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .size(26.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(VaultColors.Background.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.MenuBook,
                    contentDescription = null,
                    tint = VaultColors.TextPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        if (showTitle) {
            Text(
                text = comic.title,
                style = MaterialTheme.typography.bodySmall,
                color = VaultColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = VaultSpacing.xxs)
            )
        }
    }
}

@Composable
fun ComicFallbackCover(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = Icons.Filled.MenuBook, contentDescription = null, tint = VaultColors.TextTertiary)
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = VaultColors.TextTertiary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(VaultSpacing.xs)
            )
        }
    }
}

@Composable
fun ComicRail(
    title: String,
    items: List<ComicEntity>,
    modifier: Modifier = Modifier,
    onItemClick: (ComicEntity) -> Unit,
) {
    if (items.isEmpty()) return
    Column(modifier = modifier.padding(top = 40.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = VaultColors.TextPrimary,
            modifier = Modifier.padding(horizontal = VaultSpacing.md, vertical = VaultSpacing.sm)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(VaultSpacing.sm),
            contentPadding = PaddingValues(horizontal = VaultSpacing.md)
        ) {
            items(items, key = { it.id }) { comic ->
                ComicCard(comic = comic, onClick = { onItemClick(comic) })
            }
        }
    }
}
