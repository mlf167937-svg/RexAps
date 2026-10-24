// RexAppsScreenPortrait.kt
package com.rexaps.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun RexAppsScreenPortrait(shell: RexShell) {
    Box(Modifier.fillMaxSize()) {
        ProAmbientBackdrop(Modifier.fillMaxSize())

        Crossfade(
            targetState = shell.selectedTab,
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            animationSpec = tween(320),
            label = "tabPortrait"
        ) { tab ->
            // Ruang di bawah agar konten tidak tertutup navigasi mengambang.
            val bottomSafe = PaddingValues(bottom = 120.dp)
            when (tab) {
                RexTab.HOME -> ProPortraitHome(shell)

                RexTab.PROFILE -> ProPortraitProfile(
                    appCount = RexAppRegistry.modules.size,
                    themeLabel = shell.themeOption.label,
                    isActive = shell.selectedTab == RexTab.PROFILE
                )

                RexTab.THEME -> ThemeTabContent(
                    padding = bottomSafe,
                    selectedTheme = shell.themeOption,
                    onOpenTheme = shell.onOpenTheme
                )

                RexTab.SETTINGS -> RexSettingsScreen(
                    padding = bottomSafe,
                    selectedTheme = shell.themeOption,
                    onOpenTheme = shell.onOpenTheme,
                    onBackHome = { shell.onSelectTab(RexTab.HOME) }
                )

                else -> RexLocalAccountScreen(padding = bottomSafe)
            }
        }

        ProPortraitNav(
            selected = shell.selectedTab,
            onSelect = shell.onSelectTab,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/* ═════════════════════ Navigasi bawah (pill kaca) ═════════════════════ */
