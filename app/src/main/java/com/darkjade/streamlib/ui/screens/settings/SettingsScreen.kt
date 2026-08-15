package com.darkjade.streamlib.ui.screens.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.darkjade.streamlib.data.db.entity.ScanState
import com.darkjade.streamlib.ui.theme.VaultColors
import com.darkjade.streamlib.ui.theme.VaultShapes
import com.darkjade.streamlib.ui.theme.VaultSpacing

private fun videoPermission(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_VIDEO
    else Manifest.permission.READ_EXTERNAL_STORAGE

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var permissionDenied by remember { mutableStateOf(false) }
    var comicVineKeyField by remember(state.comicVineApiKey) { mutableStateOf(state.comicVineApiKey) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionDenied = false
            viewModel.scanDeviceForVideos()
        } else {
            permissionDenied = true
        }
    }

    fun requestScan() {
        val permission = videoPermission()
        val alreadyGranted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        if (alreadyGranted) {
            viewModel.scanDeviceForVideos()
        } else {
            permissionLauncher.launch(permission)
        }
    }

    // Optional: SAF folder picker, kept as a secondary way to scope the library
    // to a specific folder. Primary scanning uses MediaStore above, which is
    // far more reliable across OEM Android skins.
    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                // Non-fatal — the repository's own try/catch handles any
                // downstream scan failure without crashing the app.
            }
            val name = uri.lastPathSegment ?: "Folder"
            viewModel.onFolderSelected(uri, name)
        }
    }

    // Comics get their own folder picker — kept fully separate from the video
    // folder picker above so picking a comics folder never touches video scanning.
    val comicFolderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                // Non-fatal.
            }
            val name = uri.lastPathSegment ?: "Comics"
            viewModel.onComicFolderSelected(uri, name)
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
                        onClick = { requestScan() },
                        shape = VaultShapes.button,
                        colors = ButtonDefaults.buttonColors(containerColor = VaultColors.Orange, contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth().padding(vertical = VaultSpacing.sm)
                    ) {
                        Icon(Icons.Filled.VideoLibrary, contentDescription = null)
                        Text(" Scan Device for Videos", modifier = Modifier.padding(start = 4.dp))
                    }
                    if (status?.state == ScanState.COMPLETED) {
                        Text(
                            "Last scan found ${status.filesFound} video file(s).",
                            color = VaultColors.TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    status?.errorMessage?.let {
                        Text(it, color = VaultColors.Error, style = MaterialTheme.typography.bodySmall)
                    }
                    if (permissionDenied) {
                        Text(
                            "Video access permission was denied. Enable it from your phone's App Info > Permissions to scan your library.",
                            color = VaultColors.Error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = VaultSpacing.xxs)
                        )
                    }
                }
            }

            item {
                Divider(color = VaultColors.Divider, modifier = Modifier.padding(vertical = VaultSpacing.sm))
                SectionHeader("Library Sources (optional, folder-scoped)")
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
                OutlinedButton(
                    onClick = { folderPicker.launch(null) },
                    shape = VaultShapes.button,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = VaultColors.TextPrimary),
                    modifier = Modifier.fillMaxWidth().padding(vertical = VaultSpacing.sm)
                ) {
                    Icon(Icons.Filled.CreateNewFolder, contentDescription = null)
                    Text(" Add Specific Folder", modifier = Modifier.padding(start = 4.dp))
                }
                if (state.folderSources.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { viewModel.rescanAll() },
                        shape = VaultShapes.button,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = VaultColors.TextPrimary),
                        modifier = Modifier.fillMaxWidth().padding(top = VaultSpacing.xs)
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Text(" Rescan Folders", modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }

            item {
                Divider(color = VaultColors.Divider, modifier = Modifier.padding(vertical = VaultSpacing.sm))
                SectionHeader("Comics")
                Text(
                    "Comics are scanned completely separately from videos — only .cbz/.cbr/.cb7/.pdf files in the folder you pick below are added.",
                    color = VaultColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = VaultSpacing.sm)
                )
            }
            item {
                OutlinedTextField(
                    value = comicVineKeyField,
                    onValueChange = { comicVineKeyField = it },
                    label = { Text("Comic Vine API key") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VaultColors.Orange,
                        unfocusedBorderColor = VaultColors.Divider,
                        focusedTextColor = VaultColors.TextPrimary,
                        unfocusedTextColor = VaultColors.TextPrimary,
                        cursorColor = VaultColors.Orange,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { viewModel.setComicVineApiKey(comicVineKeyField.trim()) },
                    shape = VaultShapes.button,
                    colors = ButtonDefaults.buttonColors(containerColor = VaultColors.SurfaceVariant, contentColor = VaultColors.TextPrimary),
                    modifier = Modifier.fillMaxWidth().padding(top = VaultSpacing.xs, bottom = VaultSpacing.sm)
                ) {
                    Text("Save Comic Vine Key")
                }
            }
            items(state.comicFolderSources, key = { it.id }) { source ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = VaultSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.MenuBook, contentDescription = null, tint = VaultColors.Orange)
                    Column(modifier = Modifier.padding(start = VaultSpacing.sm).weight(1f)) {
                        Text(source.displayName, color = VaultColors.TextPrimary, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "${source.itemCount} comics",
                            color = VaultColors.TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            item {
                val comicStatus = state.comicScanStatus
                if (comicStatus != null && comicStatus.state == ScanState.SCANNING) {
                    Column(modifier = Modifier.padding(vertical = VaultSpacing.sm)) {
                        Text(
                            "Scanning comics… ${comicStatus.filesProcessed}/${comicStatus.filesFound.coerceAtLeast(1)}",
                            color = VaultColors.TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        LinearProgressIndicator(
                            progress = if (comicStatus.filesFound > 0) comicStatus.filesProcessed / comicStatus.filesFound.toFloat() else 0f,
                            color = VaultColors.Orange,
                            trackColor = VaultColors.SurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(top = VaultSpacing.xxs)
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = { comicFolderPicker.launch(null) },
                        shape = VaultShapes.button,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = VaultColors.TextPrimary),
                        modifier = Modifier.fillMaxWidth().padding(vertical = VaultSpacing.xs)
                    ) {
                        Icon(Icons.Filled.CreateNewFolder, contentDescription = null)
                        Text(" Add Comics Folder", modifier = Modifier.padding(start = 4.dp))
                    }
                    if (state.comicFolderSources.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { viewModel.rescanComics() },
                            shape = VaultShapes.button,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = VaultColors.TextPrimary),
                            modifier = Modifier.fillMaxWidth().padding(top = VaultSpacing.xs)
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null)
                            Text(" Rescan Comics", modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                    comicStatus?.errorMessage?.let {
                        Text(it, color = VaultColors.Error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = VaultSpacing.xxs))
                    }
                }
            }

            item {
                Divider(color = VaultColors.Divider, modifier = Modifier.padding(vertical = VaultSpacing.sm))
                SectionHeader("Default Player")
                Text(
                    "Choose an app to always open videos with, skipping the chooser every time.",
                    color = VaultColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = VaultSpacing.sm)
                )
            }
            item {
                PlayerOptionRow(
                    label = "Always ask",
                    selected = state.preferredPlayerPackage == null,
                    onClick = { viewModel.setPreferredPlayer(null) },
                )
            }
            items(state.installedPlayers, key = { it.packageName }) { app ->
                PlayerOptionRow(
                    label = app.label,
                    selected = state.preferredPlayerPackage == app.packageName,
                    onClick = { viewModel.setPreferredPlayer(app.packageName) },
                )
            }

            item {
                Divider(color = VaultColors.Divider, modifier = Modifier.padding(vertical = VaultSpacing.sm))
                SectionHeader("Metadata")
                val apiKeyConfigured = com.darkjade.streamlib.BuildConfig.TMDB_API_KEY.isNotBlank()
                if (apiKeyConfigured) {
                    Text(
                        "TMDB API key detected — posters, backdrops, overviews, and episode details will be fetched automatically after scanning.",
                        color = VaultColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = VaultSpacing.sm)
                    )
                } else {
                    Text(
                        "TMDB API key is NOT configured in this build — that's why posters/backdrops are missing. " +
                            "Add a TMDB_API_KEY repository secret on GitHub (Settings > Secrets and variables > Actions), " +
                            "then rebuild and rescan.",
                        color = VaultColors.Error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = VaultSpacing.sm)
                    )
                }
            }

            item {
                Divider(color = VaultColors.Divider, modifier = Modifier.padding(vertical = VaultSpacing.sm))
                SectionHeader("Playback")
                Text(
                    "Videos and comics always open in your installed external apps via Android's chooser (or your chosen default player above). This app never plays video or renders comics internally.",
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
private fun PlayerOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = VaultSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (selected) VaultColors.Orange else VaultColors.TextTertiary,
        )
        Text(label, color = VaultColors.TextPrimary, modifier = Modifier.padding(start = VaultSpacing.sm))
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
