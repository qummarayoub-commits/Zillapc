package com.darkjade.streamlib.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.darkjade.streamlib.ui.navigation.bottomNavItems
import com.darkjade.streamlib.ui.theme.VaultColors
import com.darkjade.streamlib.ui.theme.VaultSizes
import com.darkjade.streamlib.ui.theme.VaultSpacing

/** Floating, rounded, elevated nav bar — closer to the "premium" reference
 * style than a flat full-bleed bar touching the screen edges. */
@Composable
fun VaultBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    NavigationBar(
        containerColor = VaultColors.Surface,
        contentColor = VaultColors.TextSecondary,
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = VaultSpacing.sm, vertical = VaultSpacing.xs)
            .clip(RoundedCornerShape(24.dp))
            .height(VaultSizes.bottomNavHeight)
    ) {
        bottomNavItems.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        imageVector = if (selected) item.filledIcon else item.outlineIcon,
                        contentDescription = item.label,
                    )
                },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = VaultColors.Orange,
                    selectedTextColor = VaultColors.Orange,
                    unselectedIconColor = VaultColors.TextSecondary,
                    unselectedTextColor = VaultColors.TextSecondary,
                    indicatorColor = Color.Transparent,
                )
            )
        }
    }
}
