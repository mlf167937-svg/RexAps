package com.rexaps.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/* ═════════════════════ Model & shell ═════════════════════ */

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

internal const val THEME_COLUMNS = 3

/** State & aksi yang dibagikan ke shell portrait maupun landscape. */
internal class RexShell(
    val selectedTab: Int,
    val themeOption: RexThemeOption,
    val onSelectTab: (Int) -> Unit,
    val onAppClick: (RexModule) -> Unit,
    val onOpenTheme: () -> Unit
)

/* ═════════════════════ Profil app: sosial & avatar ═════════════════════ */

internal data class SocialLink(
    val title: String,
    val subtitle: String,
    val symbol: String,
    val color: Color,
    val url: String
)

internal val socialLinks = listOf(
    SocialLink(
        title = "YouTube",
        subtitle = "@RexRuinCraft",
        symbol = "▶",
        color = Color(0xFFFF0000),
        url = "https://www.youtube.com/@RexRuinCraft"
    ),
    SocialLink(
        title = "TikTok",
        subtitle = "@rexstudio4",
        symbol = "♪",
        color = Color(0xFF111111),
        url = "https://www.tiktok.com/@rexstudio4"
    ),
    SocialLink(
        title = "Saweria",
        subtitle = "Dukung lewat donasi",
        symbol = "☕",
        color = Color(0xFFF5A623),
        url = "https://saweria.co/rexruincraft"
    ),
    SocialLink(
        title = "Saluran WhatsApp",
        subtitle = "Gabung untuk update terbaru",
        symbol = "✆",
        color = Color(0xFF25D366),
        url = "https://whatsapp.com/channel/0029Vb8oqCTDOQIYrRuWic33"
    )
)
