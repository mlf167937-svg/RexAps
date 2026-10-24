@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.rexaps.rexgit

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun __splitImpl_RepositoryScreenLandscape(
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

    Row(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .landscapeSideInsets()
    ) {

        // ----- left pane: info + Pull / Push -----
        Column(
            Modifier
                .width(LandscapeRepoPaneWidth)
                .fillMaxHeight()
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                RepositoryHeader(
                    repo = repo,
                    busy = busy,
                    onBack = onBack,
                    onRefresh = onRefresh,
                    compact = true
                )

                ProgressSlot(busy)

                Column(
                    Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RepositoryStatusPills(state = state, repo = repo, changes = changes)
                    }

                    if (repo.isGitRepository) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RepositoryToolPills(
                                state = state,
                                repo = repo,
                                onGitDev = { host.gitDevDialog.value = true },
                                onAccount = { host.githubDialog.value = true }
                            )
                        }
                    }
                }
            }

            if (repo.isGitRepository && changes.isNotEmpty()) {
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

        // ----- right pane: README / Code / Actions / Release -----
        Column(
            Modifier.weight(1f).fillMaxHeight().padding(top = 8.dp)
        ) {
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
                    2 -> RexGitActionsPanel(state = state, onRefresh = onRefreshHub, onDownload = onDownloadAsset, onChooseAccount = { host.githubDialog.value = true }, modifier = Modifier.fillMaxSize())
                    else -> RexGitReleasePanel(state = state, onRefresh = onRefreshHub, onDownload = onDownloadAsset, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
internal fun __splitImpl_RexGitScreenLandscape(
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
                    .padding(start = if (screen == 1) LandscapeRepoPaneWidth else 0.dp)
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
                        EditorScreenLandscape(
                            state = state,
                            editor = it,
                            onBack = {
                                if (state.editorDirty) host.discardDialog.value = true
                                else viewModel.closeEditor()
                            },
                            onSave = viewModel::saveEditor
                        )
                    }

                    1 -> RepositoryScreenLandscape(
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

                    else -> HomeScreenLandscape(
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
