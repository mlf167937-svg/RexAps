package com.rexaps.rexmusic

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// ───────────────────────── Root ─────────────────────────
/**
 * Landscape shell futuristik: floating navigation dock dengan indikator animasi,
 * ambient backdrop, content stage sinematik, dan floating audio dock.
 * Seluruh warna/typography berasal dari MaterialTheme; state & playback tetap milik parent.
 */

@Composable
internal fun RexMusicScreenLandscape(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    miniPlayer: @Composable () -> Unit,
    snackbarHost: @Composable () -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    RexMusicScreenLandscapeContent(
        selectedTab = selectedTab,
        onTabSelected = onTabSelected,
        miniPlayer = miniPlayer,
        snackbarHost = snackbarHost,
        content = content
    )
}
