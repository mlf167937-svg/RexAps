package com.rexaps.rexgit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun RexGitScreen(
    viewModel: RexGitViewModel
) {

    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.selectedRepository == null) {

        RexGitRepositoryList(
            state = state,
            onRefresh = viewModel::refresh,
            onSelect = viewModel::selectRepository
        )

    } else {

        RexGitRepositoryScreen(
            state = state,
            onBack = viewModel::backToRepositories,
            onRefresh = viewModel::refreshStatus,
            onPush = viewModel::push
        )
    }
}

@Composable
private fun RexGitRepositoryList(
    state: RexGitUiState,
    onRefresh: () -> Unit,
    onSelect: (RexGitRepository) -> Unit
) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("RexGit")
                },
                actions = {
                    TextButton(onClick = onRefresh) {
                        Text("Refresh")
                    }
                }
            )
        }
    ) { padding ->

        if (state.isLoading) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }

        } else if (state.repositories.isEmpty()) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                Text(
                    text = "Belum ada repository",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(
                    Modifier.height(8.dp)
                )

                Text(
                    text = RexGitStorage.githubRoot.absolutePath
                )

                Spacer(
                    Modifier.height(16.dp)
                )

                Button(
                    onClick = onRefresh
                ) {
                    Text("Scan Repository")
                }
            }

        } else {

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                item {
                    Text(
                        text = "Local Repositories",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                items(
                    state.repositories,
                    key = { it.path }
                ) { repository ->

                    RepositoryCard(
                        repository = repository,
                        onClick = {
                            onSelect(repository)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RepositoryCard(
    repository: RexGitRepository,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = "📦 ${repository.name}",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(
                Modifier.height(6.dp)
            )

            Text(
                text = repository.path,
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(
                Modifier.height(8.dp)
            )

            Text(
                text = if (repository.isGitRepository) {
                    "Git repository"
                } else {
                    "Folder"
                },
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun RexGitRepositoryScreen(
    state: RexGitUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onPush: (String, Boolean) -> Unit
) {

    var showPushDialog by remember {
        mutableStateOf(false)
    }

    Scaffold(
        topBar = {

            TopAppBar(
                title = {
                    Column {
                        Text(
                            state.selectedRepository?.name
                                ?: "Repository"
                        )

                        Text(
                            state.selectedRepository?.branch
                                ?: "main",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                navigationIcon = {
                    TextButton(
                        onClick = onBack
                    ) {
                        Text("Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = onRefresh
                    ) {
                        Text("Refresh")
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            state.selectedRepository?.remoteUrl?.let {
                Text(
                    text = it,
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            HorizontalDivider()

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    16.dp
                )
            ) {

                item {

                    Text(
                        text = "Files",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(
                        Modifier.height(8.dp)
                    )
                }

                items(
                    state.files,
                    key = { it.path }
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {

                        Text(
                            text = if (it.isDirectory) {
                                "📁 ${it.name}"
                            } else {
                                "📄 ${it.name}"
                            }
                        )
                    }
                }

                item {

                    Spacer(
                        Modifier.height(16.dp)
                    )

                    Text(
                        text = "Git Status",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(
                        Modifier.height(8.dp)
                    )

                    if (!state.gitStatus.hasChanges) {
                        Text("Working tree clean")
                    } else {
                        Text(
                            "${state.gitStatus.modifiedFiles.size} modified file(s)"
                        )
                    }

                    Spacer(
                        Modifier.height(16.dp)
                    )

                    state.message?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(
                            Modifier.height(8.dp)
                        )
                    }

                    state.error?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onRefresh
                ) {
                    Text("Status")
                }

                Button(
                    modifier = Modifier.weight(1f),
                    enabled = !state.isPushing &&
                        state.gitStatus.hasChanges,
                    onClick = {
                        showPushDialog = true
                    }
                ) {
                    if (state.isPushing) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Push")
                    }
                }
            }
        }
    }

    if (showPushDialog) {

        RexGitPushDialog(
            onDismiss = {
                showPushDialog = false
            },
            onPush = { message, bump ->

                showPushDialog = false

                onPush(
                    message,
                    bump
                )
            }
        )
    }
}

@Composable
private fun RexGitPushDialog(
    onDismiss: () -> Unit,
    onPush: (String, Boolean) -> Unit
) {

    var message by remember {
        mutableStateOf("")
    }

    var bumpVersion by remember {
        mutableStateOf(true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text("Push to GitHub")
        },

        text = {

            Column {

                OutlinedTextField(
                    value = message,
                    onValueChange = {
                        message = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Commit message")
                    },
                    placeholder = {
                        Text("Update RexMusic")
                    }
                )

                Spacer(
                    Modifier.height(12.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Checkbox(
                        checked = bumpVersion,
                        onCheckedChange = {
                            bumpVersion = it
                        }
                    )

                    Text(
                        "Bump version automatically"
                    )
                }
            }
        },

        confirmButton = {

            TextButton(
                enabled = message.isNotBlank(),
                onClick = {
                    onPush(
                        message.trim(),
                        bumpVersion
                    )
                }
            ) {
                Text("PUSH")
            }
        },

        dismissButton = {

            TextButton(
                onClick = onDismiss
            ) {
                Text("CANCEL")
            }
        }
    )
}
