
@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.rexaps.rexgit

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.res.Configuration
import androidx.compose.foundation.layout.only
import androidx.compose.ui.platform.LocalConfiguration


// ============================================================
// Root
//
// RexGitScreen only detects the phone orientation and calls the matching
// layout. The UI itself lives in:
//   RexGitScreenPotrait.kt   (portrait)
//   RexGitScreenLandscape.kt (landscape)
// Shared state, dialogs and building blocks live in
// RexGitScreenShared.kt, RexGitEditor.kt and RexGitComponents.kt.
// ============================================================

@Composable
fun RexGitScreen(
    viewModel: RexGitViewModel
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Created here, above the portrait / landscape switch, so rotating the
    // phone keeps dialogs, typing, search and the selected px.
    val host = rememberRexGitScreenHost()
    val editor = state.editorPath?.let { rememberRexGitEditorState(it, viewModel) }

    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        RexGitScreenLandscape(
            state = state,
            host = host,
            editor = editor,
            viewModel = viewModel
        )
    } else {
        RexGitScreenPortrait(
            state = state,
            host = host,
            editor = editor,
            viewModel = viewModel
        )
    }

    RexGitScreenController(
        state = state,
        host = host,
        editor = editor,
        viewModel = viewModel
    )
}
