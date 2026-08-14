package com.darkjade.streamlib.ui.screens.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.darkjade.streamlib.ui.theme.VaultColors
import com.darkjade.streamlib.ui.theme.VaultSpacing

@Composable
fun AccountScreen(
    viewModel: AccountViewModel,
    onOpenSettings: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

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

        SettingsRow(label = "Switch Profile")
        Divider(color = VaultColors.Divider)
        SettingsRow(label = "Content Restrictions", value = state.activeProfile?.contentRestriction ?: "None")
        Divider(color = VaultColors.Divider)
        SettingsRow(label = "Audio Language", value = state.activeProfile?.audioLanguage ?: "English")
        Divider(color = VaultColors.Divider)
        SettingsRow(label = "Subtitles/CC Language", value = state.activeProfile?.subtitleLanguage ?: "English")
        Divider(color = VaultColors.Divider)
        SettingsRow(label = "Library Sources & Scan", onClick = onOpenSettings)
        Divider(color = VaultColors.Divider)
        SettingsRow(label = "About")
    }
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
