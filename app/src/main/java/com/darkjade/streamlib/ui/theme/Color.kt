package com.darkjade.streamlib.ui.theme

import androidx.compose.ui.graphics.Color

// Core palette — centralized so no screen hardcodes raw colors.
object VaultColors {
    // Ebony Clay background — surfaces are lighter/darker shades of the
    // same navy for card differentiation.
    val Background = Color(0xFF2A428C)
    val Surface = Color(0xFF243972)
    val SurfaceElevated = Color(0xFF2E4A9E)
    val SurfaceVariant = Color(0xFF345296)

    // Sandy Yellow — the single accent, used for buttons, selected states,
    // progress, and important icons.
    val Orange = Color(0xFFFFEF4D)
    val OrangeBright = Color(0xFFFFF48A)
    val OrangeDim = Color(0xFFB3A730)

    // Secondary accent — kept for sparing use where a second highlight is needed.
    val Jade = Color(0xFF2DD4A7)

    // Sandy Yellow for primary text (per the reference); secondary/tertiary
    // text uses paler, less saturated tones of the same navy/yellow family
    // so hierarchy stays readable against the Ebony Clay background.
    val TextPrimary = Color(0xFFFFEF4D)
    val TextSecondary = Color(0xFFC7CEE8)
    val TextTertiary = Color(0xFF8F9BC7)

    val Divider = Color(0xFF3E5AA8)
    val Success = Color(0xFF2ECC71)
    val Error = Color(0xFFFF6B6B)
    val PremiumGold = Color(0xFFE8A93A)

    val GradientTop = Color(0x002A428C)
    val GradientBottom = Color(0xF02A428C)
}
