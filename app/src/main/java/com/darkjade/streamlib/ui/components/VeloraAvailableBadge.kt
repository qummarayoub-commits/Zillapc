package com.darkjade.streamlib.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.darkjade.streamlib.R
import com.darkjade.streamlib.ui.theme.VaultColors

/**
 * The Velora "V" in a small circle — marks a title that is available to
 * stream from THIS device's library. Place it in a corner of a poster.
 */
@Composable
fun VeloraAvailableBadge(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(VaultColors.Background.copy(alpha = 0.88f))
            .border(1.5.dp, VaultColors.Orange, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_v_mark),
            contentDescription = "Available on your device",
            contentScale = ContentScale.Fit,
            modifier = Modifier.padding(size * 0.22f),
        )
    }
}
