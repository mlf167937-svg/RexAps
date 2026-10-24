// RexApsApp.kt
package com.rexaps.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

internal data class BottomTab(val label: String, val icon: ImageVector)

internal val bottomTabs = listOf(
    BottomTab("Home", Icons.Default.Home),
    BottomTab("Profile", Icons.Default.Person),
    BottomTab("Theme", Icons.Default.ColorLens),
    BottomTab("Settings", Icons.Default.Settings),
    BottomTab("Account", Icons.Default.AccountCircle)
)

internal object RexTab {
    const val HOME = 0
    const val PROFILE = 1
    const val THEME = 2
    const val SETTINGS = 3
    const val ACCOUNT = 4
}
