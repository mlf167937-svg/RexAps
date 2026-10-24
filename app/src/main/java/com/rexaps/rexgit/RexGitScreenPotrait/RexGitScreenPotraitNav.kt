@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.rexaps.rexgit

import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.grid.items

@Composable
internal fun LocalTab(
    state: RexGitUiState,
    query: String,
    onQueryChange: (String) -> Unit,
    onConnect: () -> Unit,
    onSelect: (RexGitRepository) -> Unit
) {
    val context = LocalContext.current

    val repos = remember(state.repositories, query) {
        state.repositories.filter {
            it.name.contains(query.trim(), ignoreCase = true)
        }
    }

    val gitCount = remember(state.repositories) {
        state.repositories.count { it.isGitRepository }
    }

    Column(Modifier.fillMaxSize()) {

        if (state.repositories.isNotEmpty()) {
            SearchField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = "Search repositories",
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            if (state.storageFallback) {
                item(key = "storage-notice") {
                    StorageNotice(
                        path = state.storagePath,
                        onGrant = { openAllFilesAccess(context) }
                    )
                }
            }

            if (state.repositories.isNotEmpty()) {
                item(key = "workspace") {
                    WorkspaceCard(
                        total = state.repositories.size,
                        gitCount = gitCount,
                        path = state.storagePath
                    )
                }
            }

            if (state.repositories.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        icon = Icons.Default.FolderOpen,
                        title = "No repositories yet",
                        body = "Connect GitHub and clone a repository into your workspace.",
                        actionLabel = "Connect GitHub",
                        onAction = onConnect
                    )
                }
            } else if (repos.isEmpty()) {
                item(key = "no-match") {
                    EmptyState(
                        icon = Icons.Default.Search,
                        title = "No matches",
                        body = "No repository matches \"${query.trim()}\"."
                    )
                }
            } else {
                items(repos, key = { it.path }) { repo ->
                    RepositoryCard(repo = repo, onClick = { onSelect(repo) })
                }
            }
        }
    }
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

    val activeSearch = query.isNotBlank() && state.githubSearchMode && state.githubSearchQuery.equals(query.trim(), ignoreCase = true)
    val repoSource = if (activeSearch) state.githubSearchResults else state.githubRepositories
    val repos = remember(repoSource, query, activeSearch) {
        if (activeSearch) repoSource else repoSource.filter { it.fullName.contains(query.trim(), ignoreCase = true) }
    }

    if (!state.isGithubConnected) {
        EmptyState(
            icon = Icons.Default.Cloud,
            title = "Connect your GitHub account",
            body = "Add a personal access token to browse and clone your repositories.",
            modifier = Modifier.fillMaxSize(),
            actionLabel = "Connect GitHub",
            onAction = onConnect
        )
        return
    }

    val scheme = MaterialTheme.colorScheme

    Column(Modifier.fillMaxSize()) {

        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 12.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(scheme.primary, scheme.secondary))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    (state.githubUsername ?: "?").take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onPrimary
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text("@${state.githubUsername}", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (activeSearch) "${state.githubSearchResults.size} search results" else "${state.githubRepositories.size} repositories",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }

            HeaderIconButton(
                icon = Icons.Default.Settings,
                description = "GitHub account settings",
                onClick = onConnect
            )
        }

        if (state.accounts.size > 1) {
            AccountSwitcher(
                accounts = state.accounts,
                active = state.activeAccount,
                onSwitch = onSwitch,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
            )
        }

        SearchField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = "Search repositories (press search for GitHub-wide results)",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
            onSearch = { onSearch(query) }
        )

        ProgressSlot(state.isGithubLoading)

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (repos.isEmpty() && !state.isGithubLoading) {
                item(key = "empty") {
                    EmptyState(
                        icon = Icons.Default.Search,
                        title = if (query.isBlank()) "No repositories found" else "No matches",
                        body = if (query.isBlank()) "Your account has no repositories yet."
                        else if (activeSearch) "No GitHub repositories matched \"${query.trim()}\"."
                        else "Type a keyword and tap the search icon to search GitHub-wide."
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
