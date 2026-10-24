package com.rexaps.rexmanager

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

@Composable
internal fun __splitImpl_drawerItemColors() = androidx.compose.material3.NavigationDrawerItemDefaults.colors(
    selectedContainerColor = Rex.primary.copy(alpha = 0.18f),
    selectedIconColor = Color(0xFF83A5FF),
    selectedTextColor = Rex.onSurface,
    unselectedContainerColor = Color.Transparent,
    unselectedIconColor = Rex.muted,
    unselectedTextColor = Rex.onSurface
)
