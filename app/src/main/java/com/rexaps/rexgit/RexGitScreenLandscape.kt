
@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.rexaps.rexgit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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

/** Keeps content clear of the notch / side navigation bar in landscape. */
@Composable
internal fun Modifier.landscapeSideInsets(): Modifier {
        return this.__splitImpl_landscapeSideInsets()
    }

@Composable
internal fun RexGitScreenLandscape(
    state: RexGitUiState,
    host: RexGitScreenHost,
    editor: RexGitEditorState?,
    viewModel: RexGitViewModel
) {
        return __splitImpl_RexGitScreenLandscape(state, host, editor, viewModel)
    }

// ============================================================
// Home (landscape)
// ============================================================

@Composable
internal fun HomeScreenLandscape(
    state: RexGitUiState,
    host: RexGitScreenHost,
    onRefresh: () -> Unit,
    onSelect: (RexGitRepository) -> Unit,
    onClone: (RexGitGithubRepository) -> Unit,
    onSwitch: (Int) -> Unit,
    onSearchGithub: (String) -> Unit
) {
        return __splitImpl_HomeScreenLandscape(state, host, onRefresh, onSelect, onClone, onSwitch, onSearchGithub)
    }

@Composable
internal fun GithubProfileRow(
    state: RexGitUiState,
    onSettings: () -> Unit
) {
        return __splitImpl_GithubProfileRow(state, onSettings)
    }

@Composable
internal fun LocalTabLandscape(
    state: RexGitUiState,
    query: String,
    onQueryChange: (String) -> Unit,
    onConnect: () -> Unit,
    onSelect: (RexGitRepository) -> Unit
) {
        return __splitImpl_LocalTabLandscape(state, query, onQueryChange, onConnect, onSelect)
    }

@Composable
internal fun GithubTabLandscape(
    state: RexGitUiState,
    query: String,
    onQueryChange: (String) -> Unit,
    onConnect: () -> Unit,
    onClone: (RexGitGithubRepository) -> Unit,
    onOpen: (RexGitRepository) -> Unit,
    onSearch: (String) -> Unit
) {
        return __splitImpl_GithubTabLandscape(state, query, onQueryChange, onConnect, onClone, onOpen, onSearch)
    }

// ============================================================
// Repository (landscape)
// ============================================================

@Composable
internal fun RepositoryScreenLandscape(
    state: RexGitUiState,
    host: RexGitScreenHost,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onRefreshHub: () -> Unit,
    onDownloadAsset: (String, String) -> Unit,
    onOpen: (RexGitFile) -> Unit,
    onNavigate: (String?) -> Unit,
    onPull: () -> Unit
) {
        return __splitImpl_RepositoryScreenLandscape(state, host, onBack, onRefresh, onRefreshHub, onDownloadAsset, onOpen, onNavigate, onPull)
    }

// ============================================================
// Editor (landscape): text on the left, tools on the right
// ============================================================

@Composable
internal fun EditorScreenLandscape(
    state: RexGitUiState,
    editor: RexGitEditorState,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
        return __splitImpl_EditorScreenLandscape(state, editor, onBack, onSave)
    }
