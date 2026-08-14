package com.darkjade.streamlib.ui.screens.account

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.darkjade.streamlib.data.db.entity.ProfileEntity
import com.darkjade.streamlib.ui.theme.VaultColors
import com.darkjade.streamlib.ui.theme.VaultSpacing

@Composable
fun AccountScreen(
    viewModel: AccountViewModel,
    onOpenSettings: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    var showSwitchDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultColors.Background)
            .padding(VaultSpacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Account", style = MaterialTheme.typography.headlineSmall, color = VaultColors.TextPrimary)
            Icon(
                Icons.Filled.Settings,
                contentDescription = "Settings",
                tint = VaultColors.TextSecondary,
                modifier = Modifier.clickable(onClick = onOpenSettings)
            )
        }

        Spacer(Modifier.height(VaultSpacing.lg))

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Icon(
                Icons.Filled.AccountCircle,
                contentDescription = null,
                tint = VaultColors.Orange,
                modifier = Modifier.size(88.dp).clip(CircleShape)
            )
            Text(
                state.activeProfile?.name ?: "Profile",
                style = MaterialTheme.typography.titleLarge,
                color = VaultColors.TextPrimary,
                modifier = Modifier.padding(top = VaultSpacing.sm)
            )
        }

        Spacer(Modifier.height(VaultSpacing.lg))

        SettingsRow(label = "Switch Profile", onClick = { showSwitchDialog = true })
        Divider(color = VaultColors.Divider)
        SettingsRow(
            label = "Content Restrictions",
            value = state.activeProfile?.contentRestriction ?: "None",
            onClick = { viewModel.cycleContentRestriction() }
        )
        Divider(color = VaultColors.Divider)
        SettingsRow(
            label = "Audio Language",
            value = state.activeProfile?.audioLanguage ?: "English",
            onClick = { viewModel.cycleAudioLanguage() }
        )
        Divider(color = VaultColors.Divider)
        SettingsRow(
            label = "Subtitles/CC Language",
            value = state.activeProfile?.subtitleLanguage ?: "English",
            onClick = { viewModel.cycleSubtitleLanguage() }
        )
        Divider(color = VaultColors.Divider)
        SettingsRow(label = "Library Sources & Scan", onClick = onOpenSettings)
        Divider(color = VaultColors.Divider)
        SettingsRow(label = "About")
    }

    if (showSwitchDialog) {
        SwitchProfileDialog(
            profiles = state.allProfiles,
            activeProfileId = state.activeProfile?.id,
            onSelect = {
                viewModel.switchProfile(it)
                showSwitchDialog = false
            },
            onAddProfile = { name -> viewModel.addProfile(name) },
            onDismiss = { showSwitchDialog = false },
        )
    }
}

@Composable
private fun SwitchProfileDialog(
    profiles: List<ProfileEntity>,
    activeProfileId: Long?,
    onSelect: (ProfileEntity) -> Unit,
    onAddProfile: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var newProfileName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VaultColors.Surface,
        title = { Text("Switch Profile", color = VaultColors.TextPrimary) },
        text = {
            Column {
                profiles.forEach { profile ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(profile) }
                            .padding(vertical = VaultSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (profile.id == activeProfileId) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (profile.id == activeProfileId) VaultColors.Orange else VaultColors.TextTertiary,
                        )
                        Text(
                            profile.name,
                            color = VaultColors.TextPrimary,
                            modifier = Modifier.padding(start = VaultSpacing.sm)
                        )
                    }
                }
                Spacer(Modifier.height(VaultSpacing.sm))
                Divider(color = VaultColors.Divider)
                Spacer(Modifier.height(VaultSpacing.sm))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newProfileName,
                        onValueChange = { newProfileName = it },
                        placeholder = { Text("New profile name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VaultColors.Orange,
                            unfocusedBorderColor = VaultColors.Divider,
                            focusedTextColor = VaultColors.TextPrimary,
                            unfocusedTextColor = VaultColors.TextPrimary,
                            cursorColor = VaultColors.Orange,
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Add profile",
                        tint = VaultColors.Orange,
                        modifier = Modifier
                            .padding(start = VaultSpacing.xs)
                            .clickable {
                                if (newProfileName.isNotBlank()) {
                                    onAddProfile(newProfileName.trim())
                                    newProfileName = ""
                                }
                            }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = VaultColors.Orange)
            }
        }
    )
}

@Composable
private fun SettingsRow(label: String, value: String? = null, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = VaultSpacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = VaultColors.TextPrimary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            value?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = VaultColors.TextSecondary)
                Spacer(Modifier.width(VaultSpacing.xxs))
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = VaultColors.TextTertiary)
        }
    }
}
