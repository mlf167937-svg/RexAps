
@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.rexaps.rexgit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue


// ============================================================
// PORTRAIT layout. RexGitScreen.kt picks this one in portrait.
// ============================================================

@Composable
internal fun RexGitScreenPortrait(
    state: RexGitUiState,
    host: RexGitScreenHost,
    editor: RexGitEditorState?,
    viewModel: RexGitViewModel
) {
        return __splitImpl_RexGitScreenPortrait(state, host, editor, viewModel)
    }

// ============================================================
// Home
// ============================================================

@Composable
internal fun HomeScreenPortrait(
    state: RexGitUiState,
    host: RexGitScreenHost,
    onRefresh: () -> Unit,
    onSelect: (RexGitRepository) -> Unit,
    onClone: (RexGitGithubRepository) -> Unit,
    onSwitch: (Int) -> Unit,
    onSearchGithub: (String) -> Unit
) {
        return __splitImpl_HomeScreenPortrait(state, host, onRefresh, onSelect, onClone, onSwitch, onSearchGithub)
    }


@Composable
internal fun LocalTab(
    state: RexGitUiState,
    query: String,
    onQueryChange: (String) -> Unit,
    onConnect: () -> Unit,
    onSelect: (RexGitRepository) -> Unit
) {
        return __splitImpl_LocalTab(state, query, onQueryChange, onConnect, onSelect)
    }

@Composable
internal fun GithubTab(
    state: RexGitUiState,
    query: String,
    onQueryChange: (String) -> Unit,
    onConnect: () -> Unit,
    onSwitch: (Int) -> Unit,
    onClone: (RexGitGithubRepository) -> Unit,
    onOpen: (RexGitRepository) -> Unit,
    onSearch: (String) -> Unit
) {
        return __splitImpl_GithubTab(state, query, onQueryChange, onConnect, onSwitch, onClone, onOpen, onSearch)
    }

// ============================================================
// Repository
// ============================================================

@Composable
internal fun RepositoryScreenPortrait(
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
        return __splitImpl_RepositoryScreenPortrait(state, host, onBack, onRefresh, onRefreshHub, onDownloadAsset, onOpen, onNavigate, onPull)
    }

// ============================================================
// Editor
// ============================================================

@Composable
internal fun EditorScreenPortrait(
    state: RexGitUiState,
    editor: RexGitEditorState,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
        return __splitImpl_EditorScreenPortrait(state, editor, onBack, onSave)
    }
