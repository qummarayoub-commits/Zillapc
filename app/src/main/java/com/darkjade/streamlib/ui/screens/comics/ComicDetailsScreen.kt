package com.darkjade.streamlib.ui.screens.comics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.darkjade.streamlib.ui.components.EmptyState
import com.darkjade.streamlib.ui.theme.VaultColors
import com.darkjade.streamlib.ui.theme.VaultShapes
import com.darkjade.streamlib.ui.theme.VaultSpacing

@Composable
fun ComicDetailsScreen(
    viewModel: ComicDetailsViewModel,
    onBack: () -> Unit,
    onOpen: (fileUriString: String) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(VaultColors.Background)) {
        when {
            state.isLoading -> CircularProgressIndicator(
                color = VaultColors.Orange,
                modifier = Modifier.align(Alignment.Center)
            )
            state.comic == null -> EmptyState(
                title = "Not found",
                message = "This comic is no longer in your library.",
            )
            else -> {
                val comic = state.comic!!
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = VaultSpacing.xl)) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(320.dp)) {
                            if (comic.coverUrl != null) {
                                // Blurred cover fills the frame, real cover stays undistorted —
                                // portrait comic covers looked stretched/oddly cropped when
                                // forced into this wide box with plain Crop.
                                SubcomposeAsyncImage(
                                    model = comic.coverUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().blur(30.dp),
                                    loading = { Box(Modifier.fillMaxSize().background(VaultColors.SurfaceVariant)) },
                                    error = { Box(Modifier.fillMaxSize().background(VaultColors.SurfaceVariant)) },
                                )
                                SubcomposeAsyncImage(
                                    model = comic.coverUrl,
                                    contentDescription = comic.title,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize().padding(vertical = VaultSpacing.md),
                                    loading = {},
                                    error = {},
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize().background(VaultColors.SurfaceVariant)) {
                                    Icon(
                                        Icons.Filled.MenuBook,
                                        contentDescription = null,
                                        tint = VaultColors.TextTertiary,
                                        modifier = Modifier.align(Alignment.Center).height(64.dp)
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier.fillMaxSize().background(
                                    Brush.verticalGradient(listOf(Color.Transparent, VaultColors.Background))
                                )
                            )
                            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(VaultSpacing.xs)) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                            Box(modifier = Modifier.align(Alignment.TopEnd).padding(VaultSpacing.xs)) {
                                IconButton(onClick = { showOverflowMenu = true }) {
                                    Icon(Icons.Filled.MoreVert, contentDescription = "More options", tint = Color.White)
                                }
                                DropdownMenu(expanded = showOverflowMenu, onDismissRequest = { showOverflowMenu = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Remove from library") },
                                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                                        onClick = {
                                            showOverflowMenu = false
                                            showRemoveDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Column(modifier = Modifier.padding(horizontal = VaultSpacing.md, vertical = VaultSpacing.md)) {
                            Text(comic.title, style = MaterialTheme.typography.headlineMedium, color = VaultColors.TextPrimary)

                            val metaParts = buildList {
                                add(comic.seriesName)
                                comic.issueNumber?.let { add("Issue #$it") }
                                comic.publisher?.let { add(it) }
                                comic.releaseDate?.let { add(it) }
                            }
                            if (metaParts.isNotEmpty()) {
                                Text(
                                    metaParts.joinToString("  •  "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VaultColors.TextSecondary,
                                    modifier = Modifier.padding(top = VaultSpacing.xxs)
                                )
                            }

                            if (comic.metadataMissing) {
                                Text(
                                    "Metadata unavailable — showing local file info",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VaultColors.TextTertiary,
                                    modifier = Modifier.padding(top = VaultSpacing.xxs)
                                )
                            }

                            comic.overview?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = VaultColors.TextSecondary,
                                    modifier = Modifier.padding(top = VaultSpacing.sm)
                                )
                            }

                            Button(
                                onClick = { onOpen(comic.localFileUri) },
                                shape = VaultShapes.button,
                                colors = ButtonDefaults.buttonColors(containerColor = VaultColors.Orange, contentColor = Color.White),
                                modifier = Modifier.fillMaxWidth().padding(top = VaultSpacing.lg)
                            ) {
                                Icon(Icons.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(20.dp))
                                Text(" Read", modifier = Modifier.padding(start = 4.dp))
                            }
                        }
                    }
                }

                if (showRemoveDialog) {
                    AlertDialog(
                        onDismissRequest = { showRemoveDialog = false },
                        containerColor = VaultColors.Surface,
                        title = { Text("Remove from library?", color = VaultColors.TextPrimary) },
                        text = {
                            Text(
                                "This removes \"${comic.title}\" from DarkVault. Your actual file is not deleted.",
                                color = VaultColors.TextSecondary,
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                showRemoveDialog = false
                                viewModel.remove(onRemoved = onBack)
                            }) {
                                Text("Remove", color = VaultColors.Error)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showRemoveDialog = false }) {
                                Text("Cancel", color = VaultColors.TextSecondary)
                            }
                        }
                    )
                }
            }
        }
    }
}
