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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.darkjade.streamlib.data.repository.NewReleaseEntry
import com.darkjade.streamlib.ui.theme.VaultColors
import com.darkjade.streamlib.ui.theme.VaultSizes
import com.darkjade.streamlib.ui.theme.VaultSpacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Horizontal rail of TMDB releases. Titles that exist on this device carry the Velora V badge. */
@Composable
fun NewReleasesRail(
    title: String,
    entries: List<NewReleaseEntry>,
    modifier: Modifier = Modifier,
    onEntryClick: (NewReleaseEntry) -> Unit,
) {
    if (entries.isEmpty()) return
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
            items(entries, key = { it.release.key }) { entry ->
                ReleasePosterCard(entry = entry, onClick = { onEntryClick(entry) })
            }
        }
    }
}

@Composable
private fun ReleasePosterCard(entry: NewReleaseEntry, onClick: () -> Unit) {
    val release = entry.release
    Column(
        modifier = Modifier
            .width(VaultSizes.posterWidth)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(VaultSizes.posterWidth, VaultSizes.posterHeight)
                .clip(RoundedCornerShape(4.dp))
                .background(VaultColors.SurfaceVariant)
        ) {
            if (release.posterUrl != null) {
                SubcomposeAsyncImage(
                    model = release.posterUrl,
                    contentDescription = release.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = { FallbackPoster(title = release.title) },
                    error = { FallbackPoster(title = release.title) },
                )
            } else {
                FallbackPoster(title = release.title)
            }
            if (entry.isAvailable) {
                VeloraAvailableBadge(
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                )
            }
        }
        Text(
            text = release.title,
            style = MaterialTheme.typography.bodySmall,
            color = VaultColors.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = VaultSpacing.xxs),
        )
        Text(
            text = releaseSubtitle(entry),
            style = MaterialTheme.typography.labelSmall,
            color = VaultColors.TextTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** "Season 2 · Mar 2026", "New Series · Jan 2026", or just "Oct 2025" for movies. */
fun releaseSubtitle(entry: NewReleaseEntry): String {
    val r = entry.release
    val date = formatReleaseDate(r.releaseDate)
    return listOfNotNull(r.label, date).joinToString(" · ")
}

fun formatReleaseDate(iso: String?): String? {
    if (iso.isNullOrBlank()) return null
    return try {
        LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault()))
    } catch (e: Exception) {
        iso.take(4).takeIf { it.length == 4 }
    }
}
