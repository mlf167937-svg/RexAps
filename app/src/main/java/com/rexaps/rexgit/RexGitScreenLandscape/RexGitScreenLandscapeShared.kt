@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.rexaps.rexgit

import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp

// ============================================================
// LANDSCAPE layout. RexGitScreen.kt picks this one in landscape.
//
// Wide screens use a side pane (account / repository info / editor
// tools) next to the main content, so nothing is squeezed into the
// short height of a phone held sideways.
// ============================================================

internal val LandscapeRailWidth = 300.dp

internal val LandscapeRepoPaneWidth = 320.dp

internal val LandscapeEditorPanelWidth = 280.dp
