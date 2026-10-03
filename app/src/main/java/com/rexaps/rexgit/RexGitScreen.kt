@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.rexaps.rexgit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File

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
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        RexGitLogo()

                        Spacer(
                            Modifier.width(10.dp)
                        )

                        Column {
                            Text(
                                "RexGit",
                                style =
                                    MaterialTheme.typography.titleLarge
                            )

                            Text(
                                "GitHub workspace",
                                style =
                                    MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                },
                actions = {

                    IconButton(
                        onClick = {
                            viewModel.refresh()
                        }
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }

                    IconButton(
                        onClick = {
                            githubDialog = true
                        }
                    ) {
                        Icon(
                            Icons.Default.Cloud,
                            contentDescription = "GitHub"
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors()
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

            StatusBanner(
                state = state,
                onDismiss = {
                    viewModel.clearMessage()
                }
            )

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
            }
        )
    }

    if (pushDialog) {
        PushDialog(
            message = commitMessage,
            onMessageChange = {
                commitMessage = it
            },
            bumpVersion = bumpVersion,
            onBumpChange = {
                bumpVersion = it
            },
            pushing = state.isPushing,
            onDismiss = {
                if (!state.isPushing) {
                    pushDialog = false
                }
            },
            onPush = {
                viewModel.push(
                    commitMessage,
                    bumpVersion
                )
                pushDialog = false
            }
        )
    }
}

@Composable
private fun RexGitLogo() {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(
                RoundedCornerShape(12.dp)
            )
            .background(
                MaterialTheme.colorScheme.primaryContainer
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Code,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun StatusBanner(
    state: RexGitUiState,
    onDismiss: () -> Unit
) {
    val text =
        state.error ?: state.message

    if (text == null) {
        return
    }

    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall
            )

            TextButton(
                onClick = onDismiss
            ) {
                Text("OK")
            }
        }
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 10.dp
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
                        contentDescription = null
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
                        Icons.Default.Cloud,
                        contentDescription = null
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

            EmptyRepositoryCard(
                onConnectGithub = onConnectGithub
            )

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
private fun EmptyRepositoryCard(
    onConnectGithub: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(22.dp)
        ) {

            RexGitLogo()

            Spacer(
                Modifier.height(14.dp)
            )

            Text(
                "Belum ada repository",
                style =
                    MaterialTheme.typography.titleLarge
            )

            Spacer(
                Modifier.height(5.dp)
            )

            Text(
                "Hubungkan GitHub untuk mengambil repository langsung ke workspace RexGit.",
                style =
                    MaterialTheme.typography.bodyMedium
            )

            Spacer(
                Modifier.height(16.dp)
            )

            Button(
                onClick = onConnectGithub
            ) {
                Icon(
                    Icons.Default.Cloud,
                    contentDescription = null
                )

                Spacer(
                    Modifier.width(8.dp)
                )

                Text("Connect GitHub")
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
        modifier = Modifier
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
                modifier = Modifier
                    .size(48.dp)
                    .clip(
                        RoundedCornerShape(14.dp)
                    )
                    .background(
                        MaterialTheme
                            .colorScheme
                            .primaryContainer
                    ),
                contentAlignment =
                    Alignment.Center
            ) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null
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

                Spacer(
                    Modifier.height(2.dp)
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

                if (!repo.remoteUrl.isNullOrBlank()) {
                    Text(
                        repo.remoteUrl!!,
                        maxLines = 1,
                        style =
                            MaterialTheme.typography.labelSmall
                    )
                }
            }

            Icon(
                Icons.Default.ArrowForward,
                contentDescription = null
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
                    Modifier.padding(22.dp)
                ) {

                    RexGitLogo()

                    Spacer(
                        Modifier.height(14.dp)
                    )

                    Text(
                        "GitHub belum terhubung",
                        style =
                            MaterialTheme.typography.headlineSmall
                    )

                    Spacer(
                        Modifier.height(6.dp)
                    )

                    Text(
                        "Connect akun GitHub untuk melihat dan clone repository.",
                        style =
                            MaterialTheme.typography.bodyMedium
                    )

                    Spacer(
                        Modifier.height(16.dp)
                    )

                    Button(
                        onClick = onConnect
                    ) {
                        Text("Connect GitHub")
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
                        MaterialTheme.typography.titleMedium
                )

                Text(
                    "${state.githubRepositories.size} repositories",
                    style =
                        MaterialTheme.typography.bodySmall
                )
            }

            OutlinedButton(
                onClick = onConnect
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null
                )

                Spacer(
                    Modifier.width(6.dp)
                )

                Text("Account")
            }
        }

        Spacer(
            Modifier.height(12.dp)
        )

        if (state.isGithubLoading ||
            state.isCloning
        ) {
            LinearProgressIndicator(
                Modifier.fillMaxWidth()
            )

            Spacer(
                Modifier.height(8.dp)
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

                GithubRepositoryCard(
                    repo = repo,
                    enabled = !state.isCloning,
                    onClone = {
                        onClone(repo)
                    }
                )
            }
        }
    }
}

@Composable
private fun GithubRepositoryCard(
    repo: RexGitGithubRepository,
    enabled: Boolean,
    onClone: () -> Unit
) {
    ElevatedCard(
        Modifier.fillMaxWidth()
    ) {

        Row(
            Modifier.padding(16.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(
                        RoundedCornerShape(14.dp)
                    )
                    .background(
                        MaterialTheme
                            .colorScheme
                            .secondaryContainer
                    ),
                contentAlignment =
                    Alignment.Center
            ) {
                Icon(
                    Icons.Default.Code,
                    contentDescription = null
                )
            }

            Spacer(
                Modifier.width(12.dp)
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
                    repo.fullName,
                    style =
                        MaterialTheme.typography.bodySmall
                )

                Text(
                    if (repo.private)
                        "Private"
                    else
                        "Public",
                    style =
                        MaterialTheme.typography.labelSmall
                )
            }

            Button(
                onClick = onClone,
                enabled = enabled
            ) {
                Icon(
                    Icons.Default.CloudDownload,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
private fun GithubDialog(
    state: RexGitUiState,
    onDismiss: () -> Unit,
    onConnect: (String, String) -> Unit,
    onDisconnect: () -> Unit
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
            Text("GitHub Account")
        },
        text = {

            Column {

                if (state.isGithubConnected) {

                    Text(
                        "Connected as @${state.githubUsername}",
                        style =
                            MaterialTheme.typography.titleMedium
                    )

                    Spacer(
                        Modifier.height(10.dp)
                    )

                    Text(
                        "Token tersimpan menggunakan Android Keystore.",
                        style =
                            MaterialTheme.typography.bodySmall
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
                                    contentDescription = null
                                )
                            }
                        }
                    )

                    Spacer(
                        Modifier.height(10.dp)
                    )

                    Text(
                        "Jangan kirim token GitHub ke chat atau commit ke repository.",
                        style =
                            MaterialTheme.typography.labelSmall
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
private fun PushDialog(
    message: String,
    onMessageChange: (String) -> Unit,
    bumpVersion: Boolean,
    onBumpChange: (Boolean) -> Unit,
    pushing: Boolean,
    onDismiss: () -> Unit,
    onPush: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Push ke GitHub")
        },
        text = {

            Column {

                OutlinedTextField(
                    value = message,
                    onValueChange = onMessageChange,
                    label = {
                        Text("Commit message")
                    },
                    modifier =
                        Modifier.fillMaxWidth(),
                    enabled = !pushing
                )

                Spacer(
                    Modifier.height(12.dp)
                )

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Switch(
                        checked = bumpVersion,
                        onCheckedChange =
                            onBumpChange,
                        enabled = !pushing
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
                onClick = onPush,
                enabled =
                    !pushing &&
                        message.isNotBlank()
            ) {
                Icon(
                    Icons.Default.CloudUpload,
                    contentDescription = null
                )

                Spacer(
                    Modifier.width(6.dp)
                )

                Text(
                    if (pushing)
                        "Pushing..."
                    else
                        "Push"
                )
            }
        },
        dismissButton = {

            TextButton(
                onClick = onDismiss,
                enabled = !pushing
            ) {
                Text("Batal")
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
                    vertical = 5.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onBack
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }

            Column(
                Modifier.weight(1f)
            ) {

                Text(
                    state.selectedRepository?.name
                        ?: "Repository",
                    style =
                        MaterialTheme.typography.titleMedium
                )

                Text(
                    "${state.selectedRepository?.branch ?: "main"}  •  " +
                        if (state.gitStatus.hasChanges)
                            "Changes"
                        else
                            "Clean",
                    style =
                        MaterialTheme.typography.labelSmall
                )
            }

            IconButton(
                onClick = onRefresh
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Refresh"
                )
            }

            IconButton(
                onClick = onPull
            ) {
                Icon(
                    Icons.Default.CloudDownload,
                    contentDescription = "Pull"
                )
            }

            IconButton(
                onClick = onPush
            ) {
                Icon(
                    Icons.Default.CloudUpload,
                    contentDescription = "Push"
                )
            }
        }

        Divider()

        Row(
            Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            OutlinedButton(
                onClick = onRoot
            ) {
                Icon(
                    Icons.Default.FolderOpen,
                    contentDescription = null
                )

                Spacer(
                    Modifier.width(6.dp)
                )

                Text("Root")
            }

            Spacer(
                Modifier.width(10.dp)
            )

            Text(
                state.currentDirectory ?: "/",
                modifier =
                    Modifier.weight(1f),
                maxLines = 1,
                style =
                    MaterialTheme.typography.bodySmall
            )
        }

        LazyColumn(
            Modifier.weight(1f),
            contentPadding =
                PaddingValues(
                    horizontal = 12.dp,
                    vertical = 4.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(2.dp)
        ) {

            items(
                state.files,
                key = {
                    it.path
                }
            ) { file ->

                FileRow(
                    file = file,
                    onClick = {
                        onOpen(file)
                    }
                )
            }
        }

        if (state.gitStatus.output.isNotBlank()) {

            Surface(
                tonalElevation = 5.dp,
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Column(
                    Modifier.padding(12.dp)
                ) {

                    Text(
                        "Git Status",
                        style =
                            MaterialTheme.typography.titleSmall
                    )

                    Spacer(
                        Modifier.height(5.dp)
                    )

                    Text(
                        state.gitStatus.output,
                        fontFamily =
                            FontFamily.Monospace,
                        style =
                            MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun FileRow(
    file: RexGitFile,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick
            )
            .padding(
                horizontal = 10.dp,
                vertical = 10.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Icon(
            if (file.isDirectory)
                Icons.Default.Folder
            else
                Icons.Default.Code,
            contentDescription = null,
            modifier = Modifier.size(22.dp)
        )

        Spacer(
            Modifier.width(12.dp)
        )

        Text(
            file.name,
            style =
                MaterialTheme.typography.bodyLarge
        )
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

            TopAppBar(
                navigationIcon = {

                    IconButton(
                        onClick = onBack
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                title = {

                    Column {

                        Text(
                            state.editorPath
                                ?.substringAfterLast("/")
                                ?: "Editor"
                        )

                        if (state.editorDirty) {
                            Text(
                                "Unsaved changes",
                                style =
                                    MaterialTheme.typography.labelSmall
                            )
                        }
                    }
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
                            contentDescription = "Save"
                        )
                    }
                }
            )
        }
    ) { padding ->

        CodeTextEditor(
            text = state.editorContent,
            onTextChange = onEdit,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(8.dp)
        )
    }
}

@Composable
private fun CodeTextEditor(
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = text,
        onValueChange = onTextChange,
        modifier = modifier,
        textStyle =
            MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace
            ),
        decorationBox = { innerTextField ->

            Surface(
                modifier = Modifier.fillMaxSize(),
                tonalElevation = 1.dp,
                shape =
                    RoundedCornerShape(10.dp)
            ) {

                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {

                    if (text.isEmpty()) {
                        Text(
                            "Start writing...",
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant,
                            fontFamily =
                                FontFamily.Monospace
                        )
                    }

                    innerTextField()
                }
            }
        }
    )
}

private fun FileName(
    path: String
): String {
    return File(path).name
}
