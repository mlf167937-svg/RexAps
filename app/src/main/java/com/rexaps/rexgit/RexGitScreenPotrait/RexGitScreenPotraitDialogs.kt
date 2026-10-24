@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.rexaps.rexgit

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Storage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun __splitImpl_HomeScreenPortrait(
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
