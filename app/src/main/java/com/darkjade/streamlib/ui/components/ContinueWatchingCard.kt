package com.darkjade.streamlib.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.darkjade.streamlib.data.db.entity.MediaItemEntity
import com.darkjade.streamlib.ui.theme.VaultColors
import com.darkjade.streamlib.ui.theme.VaultShapes
import com.darkjade.streamlib.ui.theme.VaultSpacing
import com.darkjade.streamlib.ui.util.PosterRotationCache

/**
 * Wide/landscape thumbnail with a centered play button, title, and
 * rating+runtime+genre metadata below — the "Last Watch" card treatment
 * from the reference, used specifically for Continue Watching (a portrait
 * poster doesn't read well at this width, and a landscape frame plus a big
 * play affordance signals "resume this" better than a plain poster does).
 */
@Composable
fun ContinueWatchingCard(
    item: MediaItemEntity,
    watchedFraction: Float?,
    onClick: () -> Unit,
) {
    Column(modifier = Modifier.width(200.dp).clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .width(200.dp)
                .height(112.dp)
                .clip(VaultShapes.card)
                .background(VaultColors.SurfaceVariant)
        ) {
            val imageModel = item.backdropUrl ?: PosterRotationCache.posterFor(item) ?: item.localFileUri
            if (imageModel != null) {
                SubcomposeAsyncImage(
                    model = imageModel,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = { FallbackPoster(title = item.title) },
                    error = { FallbackPoster(title = item.title) },
                )
            } else {
                FallbackPoster(title = item.title)
            }
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.15f)))
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Resume", tint = VaultColors.Orange, modifier = Modifier.size(22.dp))
            }
            if (watchedFraction != null && watchedFraction > 0f) {
                LinearProgressIndicator(
                    progress = watchedFraction.coerceIn(0f, 1f),
                    color = VaultColors.Orange,
                    trackColor = Color.Black.copy(alpha = 0.4f),
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp)
                )
            }
        }
        Text(
            item.title,
            style = MaterialTheme.typography.titleSmall,
            color = VaultColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = VaultSpacing.xxs)
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
            item.rating?.let {
                Icon(Icons.Filled.Star, contentDescription = null, tint = VaultColors.PremiumGold, modifier = Modifier.size(12.dp))
                Text(
                    " ${"%.1f".format(it)} ",
                    style = MaterialTheme.typography.labelSmall,
                    color = VaultColors.TextSecondary,
                )
            }
            val metaBits = buildList {
                formatRuntime(item.runtimeMinutes)?.let { add(it) }
                item.genres.split(",").firstOrNull()?.takeIf { it.isNotBlank() }?.let { add(it) }
            }
            if (metaBits.isNotEmpty()) {
                Text(
                    metaBits.joinToString("  \u2022  "),
                    style = MaterialTheme.typography.labelSmall,
                    color = VaultColors.TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
