@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.rexaps.rexgit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Storage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxHeight

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
    var tab by host.homeTab
    val busy = state.isLoading || state.isRepoLoading
    val onGithub = { host.githubDialog.value = true }

    val gitCount = remember(state.repositories) {
        state.repositories.count { it.isGitRepository }
    }

    Row(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .landscapeSideInsets()
    ) {

        // ----- left rail -----
        Column(
            Modifier
                .width(LandscapeRailWidth)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            HomeHeader(
                state = state,
                onRefresh = onRefresh,
                onGithub = onGithub,
                compact = true
            )

            ProgressSlot(busy)

            SegmentedTabs(
                labels = listOf("Local  ·  ${state.repositories.size}", "GitHub"),
                icons = listOf(Icons.Default.Storage, Icons.Default.Cloud),
                selected = tab,
                onSelect = { tab = it }
            )

            Spacer(Modifier.height(12.dp))

            if (tab == 0) {
                if (state.repositories.isNotEmpty()) {
                    Box(Modifier.padding(horizontal = 16.dp)) {
                        WorkspaceCard(
                            total = state.repositories.size,
                            gitCount = gitCount,
                            path = state.storagePath
                        )
                    }
                }
            } else if (state.isGithubConnected) {
                GithubProfileRow(state = state, onSettings = onGithub)

                if (state.accounts.size > 1) {
                    AccountSwitcher(
                        accounts = state.accounts,
                        active = state.activeAccount,
                        onSwitch = onSwitch,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
        }

        // ----- content -----
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            if (tab == 0) {
                LocalTabLandscape(
                    state = state,
                    query = host.localQuery.value,
                    onQueryChange = { host.localQuery.value = it },
                    onConnect = onGithub,
                    onSelect = onSelect
                )
            } else {
                GithubTabLandscape(
                    state = state,
                    query = host.githubQuery.value,
                    onQueryChange = { host.githubQuery.value = it },
                    onConnect = onGithub,
                    onClone = onClone,
                    onOpen = onSelect,
                    onSearch = onSearchGithub
                )
            }
        }
    }
}
