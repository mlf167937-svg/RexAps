// RexApsApp.kt
package com.rexaps.ui

import android.app.Activity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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

/* ═════════════════════ ROOT ═════════════════════ */

@Composable
fun RexApsApp(activity: Activity) {
        return __splitImpl_RexApsApp(activity)
    }

/* ═════════════════════ Animasi & util ═════════════════════ */

@Composable
internal fun Modifier.entrance(index: Int): Modifier {
        return this.__splitImpl_entrance(index)
    }

@Composable
internal fun Modifier.pressable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
        return this.__splitImpl_pressable(enabled, onClick)
    }

@Composable
internal fun accentBrush(): Brush = __splitImpl_accentBrush()

/* ═════════════════════ Komponen bersama app ═════════════════════ */

@Composable
internal fun AppIcon(letter: String, size: Dp, modifier: Modifier = Modifier) {
        return __splitImpl_AppIcon(letter, size, modifier)
    }

@Composable
internal fun SectionHeader(title: String, subtitle: String) {
        return __splitImpl_SectionHeader(title, subtitle)
    }

@Composable
internal fun StatusPill(available: Boolean) {
        return __splitImpl_StatusPill(available)
    }

@Composable
internal fun HomeHeader(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
        return __splitImpl_HomeHeader(onOpenSettings, modifier)
    }

@Composable
internal fun HeroCard(
    appCount: Int,
    availableCount: Int,
    themeLabel: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
        return __splitImpl_HeroCard(appCount, availableCount, themeLabel, isActive, modifier)
    }

@Composable
internal fun HeroStatChip(label: String, value: String, onAccent: Color) {
        return __splitImpl_HeroStatChip(label, value, onAccent)
    }

@Composable
internal fun FeaturedCard(
    app: RexModule,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
        return __splitImpl_FeaturedCard(app, isActive, onClick, modifier)
    }

@Composable
internal fun AppCard(
    app: RexModule,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
        return __splitImpl_AppCard(app, onClick, modifier)
    }

@Composable
internal fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
        return __splitImpl_StatCard(label, value, modifier)
    }

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

@Composable
internal fun SocialLinkCard(link: SocialLink, modifier: Modifier = Modifier) {
        return __splitImpl_SocialLinkCard(link, modifier)
    }

/** Avatar berputar dengan ring warna tema. */
@Composable
internal fun SpinningAvatar(isActive: Boolean, size: Dp = 132.dp) {
        return __splitImpl_SpinningAvatar(isActive, size)
    }

/* ═════════════════════ Tema ═════════════════════ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ThemeSheet(
    selected: RexThemeOption,
    onSelect: (RexThemeOption) -> Unit,
    onDismiss: () -> Unit
) {
        return __splitImpl_ThemeSheet(selected, onSelect, onDismiss)
    }

@Composable
internal fun ThemePicker(
    selected: RexThemeOption,
    onSelect: (RexThemeOption) -> Unit,
    modifier: Modifier = Modifier
) {
        return __splitImpl_ThemePicker(selected, onSelect, modifier)
    }

@Composable
internal fun ThemeTile(
    option: RexThemeOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
        return __splitImpl_ThemeTile(option, isSelected, onClick, modifier)
    }

@Composable
internal fun ThemeTabContent(
    padding: PaddingValues,
    selectedTheme: RexThemeOption,
    onOpenTheme: () -> Unit
) {
        return __splitImpl_ThemeTabContent(padding, selectedTheme, onOpenTheme)
    }
