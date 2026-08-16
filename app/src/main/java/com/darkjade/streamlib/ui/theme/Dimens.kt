package com.darkjade.streamlib.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object VaultSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
}

object VaultShapes {
    // Rounder cards — closer to the premium, glossy card look in the reference.
    val card = RoundedCornerShape(16.dp)
    val chip = RoundedCornerShape(50)
    val button = RoundedCornerShape(28.dp)
    val sheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
}

object VaultSizes {
    val posterWidth = 128.dp
    val posterHeight = 188.dp
    val posterWidthLarge = 160.dp
    val posterHeightLarge = 236.dp
    val heroHeight = 380.dp
    val episodeThumbWidth = 140.dp
    val episodeThumbHeight = 90.dp
    val bottomNavHeight = 64.dp
    val touchTarget = 48.dp
}
