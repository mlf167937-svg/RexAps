@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.rexaps.rexgit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GitHub
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun RexGitScreen(
    viewModel: RexGitViewModel
) {

    val state by viewModel.state.collectAsStateWithLifecycle()

    var githubDialog by remember {
        mutableStateOf(false)
    }

    var pushDialog by remember {
        mutableStateOf(false)
    }

    var commitMessage by remember {
        mutableStateOf("Update from RexGit")
    }

    var bumpVersion by remember {
        mutableStateOf(true)
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = {
                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Code,
                            contentDescription = null
                        )

                        Spacer(
                            Modifier.width(10.dp)
                        )

                        Text(
                            "RexGit",
                            style =
                                MaterialTheme.typography.titleLarge
                        )
                    }
                },
                actions = {

                    if (state.isGithubConnected) {
                        IconButton(
                            onClick = {
                                viewModel.reloadGithub()
                            }
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription =
                                    "Refresh GitHub"
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            githubDialog = true
                        }
                    ) {
                        Icon(
                            Icons.Default.GitHub,
                            contentDescription =
                                "GitHub"
                        )
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

            if (state.isLoading) {
                LinearProgressIndicator(
                    Modifier.fillMaxWidth()
                )
            }

            if (
                state.message != null ||
                state.error != null
            ) {

                val text =
                    state.error
                        ?: state.message
                        ?: ""

                Surface(
                    tonalElevation = 3.dp,
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier =
                            Modifier.padding(
                                12.dp
                            ),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Text(
                            text,
                            modifier =
                                Modifier.weight(1f),
                            style =
                                MaterialTheme.typography.bodySmall
                        )

                        TextButton(
                            onClick = {
                                viewModel.clearMessage()
                            }
                        ) {
                            Text("OK")
                        }
                    }
                }
            }

            if (state.selectedRepository == null) {

                RepositoryHome(
                    state = state,
                    onConnectGithub = {
                        githubDialog = true
                    },
                    onSelect = {
                        viewModel.selectRepository(it)
                    },
                    onClone = {
                        viewModel.cloneRepository(it)
                    }
                )

            } else {

                RepositoryEditor(
                    state = state,
                    onBack = {
                        viewModel.backToRepositories()
                    },
                    onRefresh = {
                        viewModel.refreshStatus()
                    },
                    onOpen = {
                        viewModel.openFile(it)
                    },
                    onRoot = {
                        viewModel.openRoot()
                    },
                    onSave = {
                        viewModel.saveEditor()
                    },
                    onEdit = {
                        viewModel.updateEditor(it)
                    },
                    onCloseEditor = {
                        viewModel.closeEditor()
                    },
                    onPull = {
                        viewModel.pull()
                    },
                    onPush = {
                        pushDialog = true
                    }
                )
            }
        }
    }

    if (githubDialog) {

        GithubDialog(
            state = state,
            onDismiss = {
                githubDialog = false
            },
            onConnect = { username, token ->
                viewModel.connectGithub(
                    username,
                    token
                )
            },
            onDisconnect = {
                viewModel.disconnectGithub()
            },
            onClone = {
                viewModel.cloneRepository(it)
            }
        )
    }

    if (pushDialog) {

        AlertDialog(
            onDismissRequest = {
                if (!state.isPushing) {
                    pushDialog = false
                }
            },
            title = {
                Text("Push ke GitHub")
            },
            text = {

                Column {

                    OutlinedTextField(
                        value = commitMessage,
                        onValueChange = {
                            commitMessage = it
                        },
                        label = {
                            Text("Commit message")
                        },
                        modifier =
                            Modifier.fillMaxWidth(),
                        enabled =
                            !state.isPushing
                    )

                    Spacer(
                        Modifier.height(12.dp)
                    )

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Switch(
                            checked =
                                bumpVersion,
                            onCheckedChange = {
                                bumpVersion = it
                            },
                            enabled =
                                !state.isPushing
                        )

                        Spacer(
                            Modifier.width(8.dp)
                        )

                        Text(
                            "Bump version otomatis"
                        )
                    }
                }
            },
            confirmButton = {

                Button(
                    onClick = {
                        viewModel.push(
                            commitMessage,
                            bumpVersion
                        )
                        pushDialog = false
                    },
                    enabled =
                        !state.isPushing &&
                            commitMessage.isNotBlank()
                ) {
                    Text("Push")
                }
            },
            dismissButton = {

                TextButton(
                    onClick = {
                        pushDialog = false
                    },
                    enabled =
                        !state.isPushing
                ) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun RepositoryHome(
    state: RexGitUiState,
    onConnectGithub: () -> Unit,
    onSelect: (RexGitRepository) -> Unit,
    onClone: (RexGitGithubRepository) -> Unit
) {

    var tab by remember {
        mutableStateOf(0)
    }

    Column(
        Modifier.fillMaxSize()
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 12.dp,
                        vertical = 8.dp
                    ),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            FilterChip(
                selected = tab == 0,
                onClick = {
                    tab = 0
                },
                label = {
                    Text("Local")
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Storage,
                        null
                    )
                }
            )

            FilterChip(
                selected = tab == 1,
                onClick = {
                    tab = 1
                },
                label = {
                    Text("GitHub")
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.GitHub,
                        null
                    )
                }
            )
        }

        if (tab == 0) {

            LocalRepositories(
                state = state,
                onConnectGithub = onConnectGithub,
                onSelect = onSelect
            )

        } else {

            GithubRepositories(
                state = state,
                onConnect = onConnectGithub,
                onClone = onClone
            )
        }
    }
}

@Composable
private fun LocalRepositories(
    state: RexGitUiState,
    onConnectGithub: () -> Unit,
    onSelect: (RexGitRepository) -> Unit
) {

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            "Workspace",
            style =
                MaterialTheme.typography.headlineSmall
        )

        Spacer(
            Modifier.height(4.dp)
        )

        Text(
            "/sdcard/Download/RexAps/Github",
            style =
                MaterialTheme.typography.bodySmall,
            color =
                MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(
            Modifier.height(16.dp)
        )

        if (state.repositories.isEmpty()) {

            ElevatedCard(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Column(
                    Modifier.padding(20.dp)
                ) {

                    Icon(
                        Icons.Default.FolderOpen,
                        null,
                        Modifier.size(42.dp)
                    )

                    Spacer(
                        Modifier.height(12.dp)
                    )

                    Text(
                        "Belum ada repository",
                        style =
                            MaterialTheme.typography.titleMedium
                    )

                    Text(
                        "Hubungkan GitHub lalu clone repository kamu.",
                        style =
                            MaterialTheme.typography.bodySmall
                    )

                    Spacer(
                        Modifier.height(14.dp)
                    )

                    Button(
                        onClick =
                            onConnectGithub
                    ) {
                        Icon(
                            Icons.Default.GitHub,
                            null
                        )

                        Spacer(
                            Modifier.width(8.dp)
                        )

                        Text("Connect GitHub")
                    }
                }
            }

        } else {

            LazyColumn(
                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                items(
                    state.repositories,
                    key = {
                        it.path
                    }
                ) { repo ->

                    RepositoryCard(
                        repo = repo,
                        onClick = {
                            onSelect(repo)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RepositoryCard(
    repo: RexGitRepository,
    onClick: () -> Unit
) {

    ElevatedCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = onClick
                )
    ) {

        Row(
            Modifier.padding(16.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .background(
                            MaterialTheme
                                .colorScheme
                                .primaryContainer,
                            RoundedCornerShape(14.dp)
                        ),
                contentAlignment =
                    Alignment.Center
            ) {
                Icon(
                    Icons.Default.Folder,
                    null
                )
            }

            Spacer(
                Modifier.width(14.dp)
            )

            Column(
                Modifier.weight(1f)
            ) {

                Text(
                    repo.name,
                    style =
                        MaterialTheme.typography.titleMedium
                )

                Text(
                    if (repo.isGitRepository)
                        "Git repository"
                    else
                        "Folder",
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }

            Text(
                "›",
                style =
                    MaterialTheme.typography.headlineMedium
            )
        }
    }
}

@Composable
private fun GithubRepositories(
    state: RexGitUiState,
    onConnect: () -> Unit,
    onClone: (RexGitGithubRepository) -> Unit
) {

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        if (!state.isGithubConnected) {

            ElevatedCard(
                Modifier.fillMaxWidth()
            ) {

                Column(
                    Modifier.padding(20.dp)
                ) {

                    Icon(
                        Icons.Default.GitHub,
                        null,
                        Modifier.size(46.dp)
                    )

                    Spacer(
                        Modifier.height(10.dp)
                    )

                    Text(
                        "Connect GitHub",
                        style =
                            MaterialTheme
                                .typography
                                .headlineSmall
                    )

                    Text(
                        "Setelah terhubung, repository GitHub kamu muncul di sini.",
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )

                    Spacer(
                        Modifier.height(14.dp)
                    )

                    Button(
                        onClick = onConnect
                    ) {
                        Text("Connect")
                    }
                }
            }

            return
        }

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Column(
                Modifier.weight(1f)
            ) {

                Text(
                    "@${state.githubUsername}",
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium
                )

                Text(
                    "${state.githubRepositories.size} repositories",
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )
            }

            OutlinedButton(
                onClick = onConnect
            ) {
                Text("Account")
            }
        }

        Spacer(
            Modifier.height(12.dp)
        )

        if (state.isGithubLoading) {
            LinearProgressIndicator(
                Modifier.fillMaxWidth()
            )
        }

        LazyColumn(
            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            items(
                state.githubRepositories,
                key = {
                    it.fullName
                }
            ) { repo ->

                ElevatedCard(
                    Modifier.fillMaxWidth()
                ) {

                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(
                            Icons.Default.Code,
                            null,
                            Modifier.size(34.dp)
                        )

                        Spacer(
                            Modifier.width(12.dp)
                        )

                        Column(
                            Modifier.weight(1f)
                        ) {

                            Text(
                                repo.name,
                                style =
                                    MaterialTheme
                                        .typography
                                        .titleMedium
                            )

                            Text(
                                repo.fullName,
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodySmall
                            )

                            if (repo.private) {
                                Text(
                                    "Private",
                                    style =
                                        MaterialTheme
                                            .typography
                                            .labelSmall
                                )
                            }
                        }

                        Button(
                            onClick = {
                                onClone(repo)
                            },
                            enabled =
                                !state.isCloning
                        ) {
                            Icon(
                                Icons.Default.CloudDownload,
                                null
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GithubDialog(
    state: RexGitUiState,
    onDismiss: () -> Unit,
    onConnect: (String, String) -> Unit,
    onDisconnect: () -> Unit,
    onClone: (RexGitGithubRepository) -> Unit
) {

    var username by remember {
        mutableStateOf(
            state.githubUsername ?: ""
        )
    }

    var token by remember {
        mutableStateOf("")
    }

    var visible by remember {
        mutableStateOf(false)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("GitHub")
        },
        text = {

            Column {

                if (state.isGithubConnected) {

                    Text(
                        "Connected as @${state.githubUsername}"
                    )

                    Spacer(
                        Modifier.height(12.dp)
                    )

                    Text(
                        "Token tersimpan terenkripsi di Android Keystore."
                    )

                } else {

                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                        },
                        label = {
                            Text("GitHub username")
                        },
                        singleLine = true,
                        modifier =
                            Modifier.fillMaxWidth()
                    )

                    Spacer(
                        Modifier.height(10.dp)
                    )

                    OutlinedTextField(
                        value = token,
                        onValueChange = {
                            token = it
                        },
                        label = {
                            Text("Personal Access Token")
                        },
                        singleLine = true,
                        modifier =
                            Modifier.fillMaxWidth(),
                        visualTransformation =
                            if (visible)
                                VisualTransformation.None
                            else
                                PasswordVisualTransformation(),
                        trailingIcon = {

                            IconButton(
                                onClick = {
                                    visible = !visible
                                }
                            ) {

                                Icon(
                                    if (visible)
                                        Icons.Default.VisibilityOff
                                    else
                                        Icons.Default.Visibility,
                                    null
                                )
                            }
                        }
                    )

                    Spacer(
                        Modifier.height(8.dp)
                    )

                    Text(
                        "Token hanya dimasukkan ke aplikasi dan disimpan terenkripsi. Jangan kirim token ke chat.",
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )
                }
            }
        },
        confirmButton = {

            if (state.isGithubConnected) {

                TextButton(
                    onClick = {
                        onDisconnect()
                        onDismiss()
                    }
                ) {
                    Text("Disconnect")
                }

            } else {

                Button(
                    onClick = {
                        onConnect(
                            username,
                            token
                        )
                    },
                    enabled =
                        !state.isGithubLoading &&
                            username.isNotBlank() &&
                            token.isNotBlank()
                ) {
                    Text(
                        if (state.isGithubLoading)
                            "Connecting..."
                        else
                            "Connect"
                    )
                }
            }
        },
        dismissButton = {

            TextButton(
                onClick = onDismiss
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun RepositoryEditor(
    state: RexGitUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpen: (RexGitFile) -> Unit,
    onRoot: () -> Unit,
    onSave: () -> Unit,
    onEdit: (String) -> Unit,
    onCloseEditor: () -> Unit,
    onPull: () -> Unit,
    onPush: () -> Unit
) {

    if (state.editorPath != null) {

        CodeEditor(
            state = state,
            onBack = onCloseEditor,
            onSave = onSave,
            onEdit = onEdit
        )

        return
    }

    Column(
        Modifier.fillMaxSize()
    ) {

        Row(
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 8.dp,
                    vertical = 6.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onBack
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    null
                )
            }

            Column(
                Modifier.weight(1f)
            ) {

                Text(
                    state.selectedRepository
                        ?.name
                        ?: "Repository",
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium
                )

                Row {

                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                state.selectedRepository
                                    ?.branch
                                    ?: "main"
                            )
                        }
                    )

                    Spacer(
                        Modifier.width(6.dp)
                    )

                    Text(
                        if (state.gitStatus.hasChanges)
                            "Changes"
                        else
                            "Clean",
                        style =
                            MaterialTheme
                                .typography
                                .labelSmall
                    )
                }
            }

            IconButton(
                onClick = onRefresh
            ) {
                Icon(
                    Icons.Default.Refresh,
                    null
                )
            }

            IconButton(
                onClick = onPull
            ) {
                Icon(
                    Icons.Default.CloudDownload,
                    null
                )
            }

            IconButton(
                onClick = onPush
            ) {
                Icon(
                    Icons.Default.CloudUpload,
                    null
                )
            }
        }

        Divider()

        Row(
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp,
                    vertical = 8.dp
                ),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            OutlinedButton(
                onClick = onRoot
            ) {
                Icon(
                    Icons.Default.FolderOpen,
                    null
                )

                Spacer(
                    Modifier.width(6.dp)
                )

                Text("Root")
            }

            Text(
                state.currentDirectory
                    ?: "/",
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(
                            top = 10.dp
                        ),
                style =
                    MaterialTheme
                        .typography
                        .bodySmall
            )
        }

        LazyColumn(
            Modifier.weight(1f),
            contentPadding =
                androidx.compose.foundation.layout
                    .PaddingValues(12.dp),
            verticalArrangement =
                Arrangement.spacedBy(4.dp)
        ) {

            items(
                state.files,
                key = {
                    it.path
                }
            ) { file ->

                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            onOpen(file)
                        }
                        .padding(12.dp),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Icon(
                        if (file.isDirectory)
                            Icons.Default.Folder
                        else
                            Icons.Default.Code,
                        null
                    )

                    Spacer(
                        Modifier.width(12.dp)
                    )

                    Text(
                        file.name,
                        style =
                            MaterialTheme
                                .typography
                                .bodyLarge
                    )
                }
            }
        }

        if (state.gitStatus.output.isNotBlank()) {

            Surface(
                tonalElevation = 4.dp,
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Column(
                    Modifier.padding(12.dp)
                ) {

                    Text(
                        "Git Status",
                        style =
                            MaterialTheme
                                .typography
                                .titleSmall
                    )

                    Spacer(
                        Modifier.height(5.dp)
                    )

                    Text(
                        state.gitStatus.output,
                        fontFamily =
                            FontFamily.Monospace,
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun CodeEditor(
    state: RexGitUiState,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onEdit: (String) -> Unit
) {

    Scaffold(
        topBar = {

            SmallTopAppBar(
                navigationIcon = {

                    IconButton(
                        onClick = onBack
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            null
                        )
                    }
                },
                title = {

                    Text(
                        state.editorPath
                            ?.let {
                                FileName(it)
                            }
                            ?: "Editor"
                    )
                },
                actions = {

                    IconButton(
                        onClick = onSave,
                        enabled =
                            state.editorDirty &&
                                !state.isSaving
                    ) {
                        Icon(
                            Icons.Default.Save,
                            null
                        )
                    }
                }
            )
        }
    ) { padding ->

        OutlinedTextField(
            value =
                state.editorContent,
            onValueChange = onEdit,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(8.dp),
            textStyle =
                MaterialTheme
                    .typography
                    .bodyMedium
                    .copy(
                        fontFamily =
                            FontFamily.Monospace
                    ),
            singleLine = false
        )
    }
}

private fun FileName(
    path: String
): String {
    return path
        .substringAfterLast("/")
        .ifBlank {
            "Editor"
        }
}
