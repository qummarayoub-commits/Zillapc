package com.darkjade.streamlib.ui.theme

import androidx.compose.ui.graphics.Color

// Core palette — centralized so no screen hardcodes raw colors.
object VaultColors {
    val Background = Color(0xFF000000)
    val Surface = Color(0xFF121212)
    val SurfaceElevated = Color(0xFF1C1C1C)
    val SurfaceVariant = Color(0xFF242424)

    // "Accent" (kept the identifier generic since the color itself now
    // changes per theme direction) — currently a vivid purple/violet,
    // matching the reference mockup's cinematic streaming-app look.
    val Orange = Color(0xFF8B5CF6)
    val OrangeBright = Color(0xFFA78BFA)
    val OrangeDim = Color(0xFF4C2E8F)

    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFAAAAAA)
    val TextTertiary = Color(0xFF6E6E6E)

    val Divider = Color(0xFF2A2A2A)
    val Success = Color(0xFF2ECC71)
    val Error = Color(0xFFE74C3C)
    val PremiumGold = Color(0xFFE8A93A)

    val GradientTop = Color(0x00000000)
    val GradientBottom = Color(0xF0000000)
}
