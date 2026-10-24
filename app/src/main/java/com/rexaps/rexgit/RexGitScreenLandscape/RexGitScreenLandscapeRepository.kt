
@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)
package com.rexaps.rexgit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items

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
    if (!state.isGithubConnected) {
        EmptyState(
            icon = Icons.Default.Cloud,
            title = "Connect your GitHub account",
            body = "Log in with your username and token to browse and clone your repositories.",
            modifier = Modifier.fillMaxSize(),
            actionLabel = "Connect GitHub",
            onAction = onConnect
        )
        return
    }

    val activeSearch = query.isNotBlank() && state.githubSearchMode && state.githubSearchQuery.equals(query.trim(), ignoreCase = true)
    val repoSource = if (activeSearch) state.githubSearchResults else state.githubRepositories
    val repos = remember(repoSource, query, activeSearch) {
        if (activeSearch) repoSource else repoSource.filter { it.fullName.contains(query.trim(), ignoreCase = true) }
    }

    Column(Modifier.fillMaxSize()) {

        SearchField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = "Search repositories (tap icon for global search)",
            modifier = Modifier.padding(start = 8.dp, end = 16.dp, top = 8.dp),
            onSearch = { onSearch(query) }
        )

        ProgressSlot(state.isGithubLoading)

        LazyVerticalGrid(
            columns = GridCells.Adaptive(300.dp),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 8.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (repos.isEmpty() && !state.isGithubLoading) {
                item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(
                        icon = Icons.Default.Search,
                        title = if (query.isBlank()) "No repositories found" else "No matches",
                        body = if (query.isBlank()) "Your account has no repositories yet."
                        else "Nothing matches \"${query.trim()}\"."
                    )
                }
            }

            items(repos, key = { it.fullName }) { repo ->
                val owner = repo.fullName.substringBefore('/')
                val local = state.repositories.firstOrNull {
                    it.name.equals(repo.name, true) && if (it.globalClone) it.globalOwner.equals(owner, true) else it.accountNickname.equals(owner, true)
                }

                GithubRepositoryCard(
                    repo = repo,
                    cloned = local != null,
                    cloning = state.cloningName == repo.name,
                    anyCloning = state.isCloning,
                    onClone = { onClone(repo) },
                    onOpen = { local?.let(onOpen) }
                )
            }
        }
    }
}

// ============================================================
// Repository (landscape)
// ============================================================
