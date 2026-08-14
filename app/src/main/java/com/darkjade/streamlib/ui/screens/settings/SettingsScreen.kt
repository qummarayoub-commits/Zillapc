package com.darkjade.streamlib.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.darkjade.streamlib.data.db.entity.ScanState
import com.darkjade.streamlib.ui.theme.VaultColors
import com.darkjade.streamlib.ui.theme.VaultShapes
import com.darkjade.streamlib.ui.theme.VaultSpacing

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            // Some OEM file pickers (e.g. certain ZTE/MyOS builds) don't actually
            // grant a persistable permission even though they return a URI —
            // calling takePersistableUriPermission then throws SecurityException,
            // which previously crashed the app right at folder selection. We still
            // have temporary read access from this activity result regardless, so
            // we proceed with the scan even if persisting the grant fails; the
            // user will just need to re-pick the folder after an app restart.
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                // Non-fatal — continue with the one-time scan below.
            }
            val name = uri.lastPathSegment ?: "Folder"
            viewModel.onFolderSelected(uri, name)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(VaultColors.Background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(VaultSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = VaultColors.TextPrimary)
            }
            Text("Settings", style = MaterialTheme.typography.headlineSmall, color = VaultColors.TextPrimary)
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = VaultSpacing.md)) {
            item {
                SectionHeader("Library Sources")
            }
            items(state.folderSources, key = { it.id }) { source ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = VaultSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Folder, contentDescription = null, tint = VaultColors.Orange)
                    Column(modifier = Modifier.padding(start = VaultSpacing.sm).weight(1f)) {
                        Text(source.displayName, color = VaultColors.TextPrimary, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "${source.itemCount} items",
                            color = VaultColors.TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            item {
                Button(
                    onClick = { folderPicker.launch(null) },
                    shape = VaultShapes.button,
                    colors = ButtonDefaults.buttonColors(containerColor = VaultColors.SurfaceVariant, contentColor = VaultColors.TextPrimary),
                    modifier = Modifier.fillMaxWidth().padding(vertical = VaultSpacing.sm)
                ) {
                    Icon(Icons.Filled.CreateNewFolder, contentDescription = null)
                    Text(" Add Folder", modifier = Modifier.padding(start = 4.dp))
                }
            }

            item {
                Divider(color = VaultColors.Divider, modifier = Modifier.padding(vertical = VaultSpacing.sm))
                SectionHeader("Scan Library")
            }
            item {
                val status = state.scanStatus
                if (status != null && status.state == ScanState.SCANNING) {
                    Column(modifier = Modifier.padding(vertical = VaultSpacing.sm)) {
                        Text(
                            "Scanning… ${status.filesProcessed}/${status.filesFound.coerceAtLeast(1)}",
                            color = VaultColors.TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        LinearProgressIndicator(
                            progress = if (status.filesFound > 0) status.filesProcessed / status.filesFound.toFloat() else 0f,
                            color = VaultColors.Orange,
                            trackColor = VaultColors.SurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(top = VaultSpacing.xxs)
                        )
                    }
                } else {
                    Button(
                        onClick = { viewModel.rescanAll() },
                        shape = VaultShapes.button,
                        colors = ButtonDefaults.buttonColors(containerColor = VaultColors.Orange, contentColor = androidx.compose.ui.graphics.Color.White),
                        modifier = Modifier.fillMaxWidth().padding(vertical = VaultSpacing.sm)
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Text(" Rescan Library", modifier = Modifier.padding(start = 4.dp))
                    }
                    status?.errorMessage?.let {
                        Text(it, color = VaultColors.Error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            item {
                Divider(color = VaultColors.Divider, modifier = Modifier.padding(vertical = VaultSpacing.sm))
                SectionHeader("Metadata")
                Text(
                    "TMDB metadata is enabled. Posters, backdrops, overviews, and episode details are fetched automatically after scanning.",
                    color = VaultColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = VaultSpacing.sm)
                )
            }

            item {
                Divider(color = VaultColors.Divider, modifier = Modifier.padding(vertical = VaultSpacing.sm))
                SectionHeader("Playback")
                Text(
                    "Videos always open in your installed external player via Android's chooser. This app never plays video internally.",
                    color = VaultColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = VaultSpacing.sm)
                )
            }

            item {
                Divider(color = VaultColors.Divider, modifier = Modifier.padding(vertical = VaultSpacing.sm))
                SectionHeader("About")
                Text(
                    "DarkVault — a local-only media library. Your files never leave your device.",
                    color = VaultColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = VaultSpacing.xl)
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = VaultColors.Orange,
        modifier = Modifier.padding(vertical = VaultSpacing.xs)
    )
}
