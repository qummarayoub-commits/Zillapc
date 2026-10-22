package com.darkjade.streamlib.ui.theme

import androidx.compose.ui.graphics.Color

// Core palette — centralized so no screen hardcodes raw colors.
object VaultColors {
    // Black (#171717) background, per the reference swatches.
    val Background = Color(0xFF171717)
    val Surface = Color(0xFF232323)
    val SurfaceElevated = Color(0xFF4D4D4D) // "dark gray" swatch — elevated/hover surfaces
    val SurfaceVariant = Color(0xFF2E2E2E)

    // Orange (#F25623) — the single accent, used for buttons, selected
    // states, progress, and important icons.
    val Orange = Color(0xFFF25623)
    val OrangeBright = Color(0xFFFF7A4D)
    val OrangeDim = Color(0xFF8A2F13)

    // Secondary accent — kept for sparing use where a second highlight is needed.
    val Jade = Color(0xFF2DD4A7)

    // Light gray (#DEDEDE) for primary text; muted grays for hierarchy.
    val TextPrimary = Color(0xFFDEDEDE)
    val TextSecondary = Color(0xFFA8A8A8)
    val TextTertiary = Color(0xFF777777)

    val Divider = Color(0xFF3A3A3A)
    val Success = Color(0xFF2ECC71)
    val Error = Color(0xFFFF6B6B)
    val PremiumGold = Color(0xFFE8A93A)

    val GradientTop = Color(0x00171717)
    val GradientBottom = Color(0xF0171717)
}
