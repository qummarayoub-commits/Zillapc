package com.darkjade.streamlib.ui.screens.newreleases

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.darkjade.streamlib.data.repository.NewReleaseEntry
import com.darkjade.streamlib.ui.components.MediaRail
import com.darkjade.streamlib.ui.components.NewReleasesRail
import com.darkjade.streamlib.ui.components.ReleaseInfoDialog
import com.darkjade.streamlib.ui.theme.VaultColors
import com.darkjade.streamlib.ui.theme.VaultSpacing
import java.time.LocalDate

/**
 * The "New" page: everything the Home page only previews. Titles that are on this
 * device carry the Velora "V" circle; "Available for Stream" lists just those.
 */
@Composable
fun NewReleasesScreen(
    viewModel: NewReleasesViewModel,
    onOpenDetails: (Long) -> Unit,
) {
    val board by viewModel.board.collectAsState()
    val refreshing by viewModel.refreshing.collectAsState()
    var selectedRelease by remember { mutableStateOf<NewReleaseEntry?>(null) }

    val onEntryClick: (NewReleaseEntry) -> Unit = { entry ->
        val local = entry.localItem
        if (local != null) onOpenDetails(local.id) else selectedRelease = entry
    }

    selectedRelease?.let { entry -> ReleaseInfoDialog(entry) { selectedRelease = null } }

    Box(modifier = Modifier.fillMaxSize().background(VaultColors.Background)) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = VaultSpacing.md, vertical = VaultSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "New",
                        style = MaterialTheme.typography.headlineSmall,
                        color = VaultColors.TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    if (refreshing) {
                        CircularProgressIndicator(
                            color = VaultColors.Orange,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp),
                        )
                    } else {
                        IconButton(onClick = { viewModel.refresh(force = true) }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = VaultColors.TextSecondary)
                        }
                    }
                }
            }

            if (board.availableForStream.isNotEmpty()) {
                item {
                    MediaRail(
                        "Available for Stream",
                        board.availableForStream,
                        modifier = Modifier,
                        onItemClick = { onOpenDetails(it.id) },
                        showAvailableBadge = true,
                    )
                }
            }

            items(board.rows, key = { it.id }) { row ->
                NewReleasesRail(title = row.title, entries = row.entries, onEntryClick = onEntryClick)
            }

            if (board.all.isNotEmpty()) {
                item {
                    val thisYear = LocalDate.now().year
                    NewReleasesRail(
                        title = "All New Releases · ${thisYear - 1}–$thisYear",
                        entries = board.all,
                        onEntryClick = onEntryClick,
                    )
                }
            }

            if (!board.isLoading && !refreshing && board.all.isEmpty() && board.rows.isEmpty()) {
                item {
                    Text(
                        "Couldn't load new releases. Check your internet connection and the TMDB key, then tap refresh.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VaultColors.TextSecondary,
                        modifier = Modifier.padding(VaultSpacing.md),
                    )
                }
            }

            item { Box(modifier = Modifier.size(24.dp)) }
        }
    }
}
