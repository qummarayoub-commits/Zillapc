package com.darkjade.streamlib.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val VaultDarkColorScheme = darkColorScheme(
    primary = VaultColors.Orange,
    onPrimary = VaultColors.TextPrimary,
    secondary = VaultColors.OrangeBright,
    background = VaultColors.Background,
    onBackground = VaultColors.TextPrimary,
    surface = VaultColors.Surface,
    onSurface = VaultColors.TextPrimary,
    surfaceVariant = VaultColors.SurfaceVariant,
    onSurfaceVariant = VaultColors.TextSecondary,
    error = VaultColors.Error,
    outline = VaultColors.Divider,
)

@Composable
fun StreamLibTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // App is dark-only by design (Crunchyroll-inspired premium dark theme).
    MaterialTheme(
        colorScheme = VaultDarkColorScheme,
        typography = VaultTypography,
        content = content
    )
}
