package com.darkjade.streamlib.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.darkjade.streamlib.data.db.entity.MediaItemEntity
import com.darkjade.streamlib.ui.theme.VaultColors
import com.darkjade.streamlib.ui.theme.VaultShapes
import com.darkjade.streamlib.ui.theme.VaultSizes
import com.darkjade.streamlib.ui.theme.VaultSpacing

/** Formats runtime like "1h 42m" / "46m" — same style used in Details. */
fun formatRuntime(minutes: Int?): String? {
    if (minutes == null || minutes <= 0) return null
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

@Composable
fun PosterCard(
    item: MediaItemEntity,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
    showMenu: Boolean = false,
    onClick: () -> Unit,
    onAddToList: (() -> Unit)? = null,
    onRemoveFromLibrary: (() -> Unit)? = null,
) {
    var showDropdown by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .width(VaultSizes.posterWidth) // height is intrinsic now — see note below
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(VaultSizes.posterWidth, VaultSizes.posterHeight)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp)) // sharp/minimal corners, Netflix-style
                .background(VaultColors.SurfaceVariant)
        ) {
            // Prefer TMDB poster (rotated per app-session across TMDB's multiple
            // available posters for this title); fall back to decoding a frame
            // directly from the local video file (movies only) so a title never
            // looks completely blank just because it lacks metadata.
            val imageModel = com.darkjade.streamlib.ui.util.PosterRotationCache.posterFor(item) ?: item.localFileUri
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
            // Small circular play affordance in the corner — same idea as the
            // reference image's poster styling, purely decorative (the whole
            // card is already clickable, this doesn't add new behavior).
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
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = VaultColors.TextPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        if (showTitle) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = VaultSpacing.xxs),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = VaultColors.TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    formatRuntime(item.runtimeMinutes)?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = VaultColors.TextTertiary,
                        )
                    }
                }
                if (showMenu && (onAddToList != null || onRemoveFromLibrary != null)) {
                    Box {
                        IconButton(onClick = { showDropdown = true }, modifier = Modifier.size(20.dp)) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = "More options",
                                tint = VaultColors.TextTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        DropdownMenu(expanded = showDropdown, onDismissRequest = { showDropdown = false }) {
                            onAddToList?.let { action ->
                                DropdownMenuItem(
                                    text = { Text("Add to List") },
                                    leadingIcon = { Icon(Icons.Filled.Bookmark, contentDescription = null) },
                                    onClick = {
                                        showDropdown = false
                                        action()
                                    }
                                )
                            }
                            onRemoveFromLibrary?.let { action ->
                                DropdownMenuItem(
                                    text = { Text("Remove from Library") },
                                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                                    onClick = {
                                        showDropdown = false
                                        action()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Clean fallback shown when metadata couldn't be fetched — never a blank/broken image. */
@Composable
fun FallbackPoster(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.Movie,
                contentDescription = null,
                tint = VaultColors.TextTertiary,
            )
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
