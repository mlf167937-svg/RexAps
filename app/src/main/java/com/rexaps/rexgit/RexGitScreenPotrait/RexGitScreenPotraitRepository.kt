
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
    val repo = state.selectedRepository ?: return

    var showChanges by host.showChanges

    val changes = state.gitStatus.changes
    val busy = state.isPushing || state.isPulling || state.isRepoLoading

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {

        RepositoryHeader(
            repo = repo,
            busy = busy,
            onBack = onBack,
            onRefresh = onRefresh
        )

        ProgressSlot(busy)

        // ----- status pills -----
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RepositoryStatusPills(state = state, repo = repo, changes = changes)

            RepositoryToolPills(
                state = state,
                repo = repo,
                onGitDev = { host.gitDevDialog.value = true },
                onAccount = { host.githubDialog.value = true }
            )
        }

        RepositoryTabBar(
            selected = host.repoTab.value,
            onSelect = { host.repoTab.value = it }
        )

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (host.repoTab.value) {
                0 -> RexGitReadmePreview(
                        repo = repo,
                        content = state.readmeContent,
                        metadata = state.repositoryMetadata,
                        tags = state.repositoryTags,
                        onFileLink = { link -> openReadmeFileLink(repo, link, onOpen) },
                        modifier = Modifier.fillMaxSize()
                    )
                1 -> RepositoryFiles(
                    state = state,
                    repo = repo,
                    onOpen = onOpen,
                    onMore = { host.actionTarget.value = it },
                    onNavigate = onNavigate,
                    onNew = { host.createMenu.value = true },
                    modifier = Modifier.fillMaxSize()
                )
                2 -> RexGitActionsPanel(
                    state = state,
                    onRefresh = onRefreshHub,
                    onDownload = onDownloadAsset,
                    onChooseAccount = { host.githubDialog.value = true },
                    modifier = Modifier.fillMaxSize()
                )
                else -> RexGitReleasePanel(
                    state = state,
                    onRefresh = onRefreshHub,
                    onDownload = onDownloadAsset,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (host.repoTab.value == 1 && repo.isGitRepository && changes.isNotEmpty()) {
            ChangesPanel(
                changes = changes,
                expanded = showChanges,
                onToggle = { showChanges = !showChanges }
            )
        }

        ActionBar(
            isGit = repo.isGitRepository,
            busy = busy,
            pulling = state.isPulling,
            pushing = state.isPushing,
            changeCount = changes.size,
            gitDevReady = state.gitDev.isReady,
            onPull = onPull,
            onPush = { host.requestPush(state) },
            onUnlockPush = { host.gitDevDialog.value = true }
        )
    }
}

// ============================================================
// Editor
// ============================================================
