@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.rexaps.rexgit

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
    val screen = screenIndex(state)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(
                hostState = host.snackbar,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = if (screen == 1) 84.dp else 0.dp)
            )
        },
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp)
    ) { padding ->

        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Crossfade(
                targetState = screen,
                animationSpec = tween(160)
            ) { target ->
                when (target) {
                    2 -> editor?.let {
                        EditorScreenPortrait(
                            state = state,
                            editor = it,
                            onBack = {
                                if (state.editorDirty) host.discardDialog.value = true
                                else viewModel.closeEditor()
                            },
                            onSave = viewModel::saveEditor
                        )
                    }

                    1 -> RepositoryScreenPortrait(
                        state = state,
                        host = host,
                        onBack = viewModel::backToRepositories,
                        onRefresh = viewModel::refreshStatus,
                        onRefreshHub = viewModel::refreshRepositoryHub,
                        onDownloadAsset = viewModel::downloadGithubAsset,
                        onOpen = viewModel::openFile,
                        onNavigate = { path ->
                            host.repoTab.value = 1
                            if (path == null) viewModel.openRoot()
                            else viewModel.openDirectory(path)
                        },
                        onPull = viewModel::pull
                    )

                    else -> HomeScreenPortrait(
                        state = state,
                        host = host,
                        onRefresh = viewModel::refresh,
                        onSelect = viewModel::selectRepository,
                        onClone = viewModel::cloneRepository,
                        onSwitch = viewModel::switchAccount,
                        onSearchGithub = viewModel::searchGithubRepositories
                    )
                }
            }
        }
    }
}

// ============================================================
// Home
// ============================================================

@Composable
private fun HomeScreenPortrait(
    state: RexGitUiState,
    host: RexGitScreenHost,
    onRefresh: () -> Unit,
    onSelect: (RexGitRepository) -> Unit,
    onClone: (RexGitGithubRepository) -> Unit,
    onSwitch: (Int) -> Unit,
    onSearchGithub: (String) -> Unit
) {
    var tab by host.homeTab
    val busy = state.isLoading || state.isRepoLoading
    val onGithub = { host.githubDialog.value = true }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        HomeHeader(state = state, onRefresh = onRefresh, onGithub = onGithub)

        ProgressSlot(busy)

        SegmentedTabs(
            labels = listOf("Local  ·  ${state.repositories.size}", "GitHub"),
            icons = listOf(Icons.Default.Storage, Icons.Default.Cloud),
            selected = tab,
            onSelect = { tab = it }
        )

        Spacer(Modifier.height(4.dp))

        if (tab == 0) {
            LocalTab(
                state = state,
                query = host.localQuery.value,
                onQueryChange = { host.localQuery.value = it },
                onConnect = onGithub,
                onSelect = onSelect
            )
        } else {
            GithubTab(
                state = state,
                query = host.githubQuery.value,
                onQueryChange = { host.githubQuery.value = it },
                onConnect = onGithub,
                onSwitch = onSwitch,
                onClone = onClone,
                onOpen = onSelect,
                onSearch = onSearchGithub
            )
        }
    }
}

// ============================================================
// Editor
// ============================================================

@Composable
private fun EditorScreenPortrait(
    state: RexGitUiState,
    editor: RexGitEditorState,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    val actions = rememberEditorActions(editor)

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
    ) {
        EditorHeader(
            state = state,
            editor = editor,
            onBack = onBack,
            onSave = onSave
        )

        ProgressSlot(state.isSaving)

        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            actions.forEach { EditorToolButton(it) }
        }

        EditorFontControls(
            editor = editor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        )

        Spacer(Modifier.height(4.dp))

        EditorTextArea(
            state = state,
            editor = editor,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        )

        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            EditorKeys.forEach { (label, snippet) ->
                EditorKeyChip(label = label, onClick = { editor.insert(snippet) })
            }
        }

        EditorStatusText(
            editor = editor,
            modifier = Modifier
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
        )
    }
}
