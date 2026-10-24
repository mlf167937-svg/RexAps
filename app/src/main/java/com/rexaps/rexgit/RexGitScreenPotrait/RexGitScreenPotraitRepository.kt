@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.rexaps.rexgit

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
