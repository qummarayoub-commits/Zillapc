package com.darkjade.streamlib.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.darkjade.streamlib.ui.navigation.Routes
import com.darkjade.streamlib.ui.navigation.bottomNavItems
import com.darkjade.streamlib.ui.theme.VaultColors
import com.darkjade.streamlib.ui.theme.VaultSizes
import com.darkjade.streamlib.ui.theme.VaultSpacing

/** Floating, rounded, elevated nav bar with a raised red circular Search
 * button in the middle — matches the reference's iconic center FAB style,
 * while keeping every existing destination and its behavior unchanged. */
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

            if (item.route == Routes.SEARCH) {
                // The one distinctive raised circular button, floating above the bar.
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .offset(y = (-14).dp)
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(VaultColors.Orange)
                            .clickable { onNavigate(item.route) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(item.filledIcon, contentDescription = item.label, tint = Color.White)
                    }
                }
            } else {
                NavigationBarItem(
                    selected = selected,
                    onClick = { onNavigate(item.route) },
                    icon = {
                        Icon(
                            imageVector = if (selected) item.filledIcon else item.outlineIcon,
                            contentDescription = item.label,
                        )
                    },
                    label = { androidx.compose.material3.Text(item.label) },
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
}
