@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.rexaps.rexgit

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// ============================================================
// Root
// ============================================================

@Composable
fun RexGitScreen(
    viewModel: RexGitViewModel
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    var githubDialog by rememberSaveable { mutableStateOf(false) }
    var pushDialog by rememberSaveable { mutableStateOf(false) }
    var discardDialog by rememberSaveable { mutableStateOf(false) }
    var commitMessage by rememberSaveable { mutableStateOf("Update from RexGit") }
    var bumpVersion by rememberSaveable { mutableStateOf(true) }

    // Show, then dismiss (dismissing first would cancel this effect).
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    BackHandler(
        enabled = state.editorPath != null || state.selectedRepository != null
    ) {
        when {
            state.editorPath != null ->
                if (state.editorDirty) discardDialog = true
                else viewModel.closeEditor()

            state.currentDirectory != null -> viewModel.openParent()
            else -> viewModel.backToRepositories()
        }
    }

    val screen = when {
        state.editorPath != null -> 2
        state.selectedRepository != null -> 1
        else -> 0
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp)
    ) { padding ->

        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Crossfade(
                targetState = screen,
                animationSpec = tween(150)
            ) { target ->
                when (target) {
                    2 -> EditorScreen(
                        state = state,
                        onBack = {
                            if (state.editorDirty) discardDialog = true
                            else viewModel.closeEditor()
                        },
                        onSave = viewModel::saveEditor,
                        onEdit = viewModel::updateEditor
                    )

                    1 -> RepositoryScreen(
                        state = state,
                        onBack = viewModel::backToRepositories,
                        onRefresh = viewModel::refreshStatus,
                        onOpen = viewModel::openFile,
                        onUp = viewModel::openParent,
                        onPull = viewModel::pull,
                        onPush = { pushDialog = true }
                    )

                    else -> HomeScreen(
                        state = state,
                        onRefresh = viewModel::refresh,
                        onGithub = { githubDialog = true },
                        onSelect = viewModel::selectRepository,
                        onClone = viewModel::cloneRepository
                    )
                }
            }
        }
    }

    if (githubDialog) {
        GithubDialog(
            state = state,
            onDismiss = { githubDialog = false },
            onConnect = viewModel::connectGithub,
            onDisconnect = {
                viewModel.disconnectGithub()
                githubDialog = false
            }
        )
    }

    if (pushDialog) {
        PushDialog(
            message = commitMessage,
            onMessageChange = { commitMessage = it },
            bumpVersion = bumpVersion,
            onBumpChange = { bumpVersion = it },
            changeCount = state.gitStatus.changes.size,
            onDismiss = { pushDialog = false },
            onPush = {
                viewModel.push(
                    commitMessage.trim().ifBlank { "Update from RexGit" },
                    bumpVersion
                )
                pushDialog = false
            }
        )
    }

    if (discardDialog) {
        AlertDialog(
            onDismissRequest = { discardDialog = false },
            title = { Text("Discard changes?") },
            text = { Text("This file has unsaved changes that will be lost.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        discardDialog = false
                        viewModel.closeEditor()
                    }
                ) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { discardDialog = false }) { Text("Keep editing") }
            }
        )
    }

    state.error?.let { error ->
        ErrorDialog(text = error, onDismiss = viewModel::dismissError)
    }
}

// ============================================================
// Home
// ============================================================

@Composable
private fun HomeScreen(
    state: RexGitUiState,
    onRefresh: () -> Unit,
    onGithub: () -> Unit,
    onSelect: (RexGitRepository) -> Unit,
    onClone: (RexGitGithubRepository) -> Unit
) {
    var tab by rememberSaveable { mutableStateOf(0) }
    val busy = state.isLoading || state.isRepoLoading

    Column(Modifier.fillMaxSize()) {

        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RexGitLogo()

                    Spacer(Modifier.width(12.dp))

                    Column {
                        Text("RexGit", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Git workspace",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            actions = {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh repositories")
                }

                IconButton(onClick = onGithub) {
                    Icon(
                        Icons.Default.Cloud,
                        contentDescription =
                            if (state.isGithubConnected) "GitHub account, connected"
                            else "Connect GitHub",
                        tint =
                            if (state.isGithubConnected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )

        ProgressSlot(busy)

        TabRow(selectedTabIndex = tab) {
            Tab(
                selected = tab == 0,
                onClick = { tab = 0 },
                text = { Text("Local (${state.repositories.size})") },
                icon = { Icon(Icons.Default.Storage, contentDescription = null) }
            )

            Tab(
                selected = tab == 1,
                onClick = { tab = 1 },
                text = { Text("GitHub") },
                icon = { Icon(Icons.Default.Cloud, contentDescription = null) }
            )
        }

        if (tab == 0) {
            LocalTab(
                state = state,
                onConnect = onGithub,
                onSelect = onSelect
            )
        } else {
            GithubTab(
                state = state,
                onConnect = onGithub,
                onClone = onClone,
                onOpen = onSelect
            )
        }
    }
}

@Composable
private fun LocalTab(
    state: RexGitUiState,
    onConnect: () -> Unit,
    onSelect: (RexGitRepository) -> Unit
) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }

    val repos = remember(state.repositories, query) {
        state.repositories.filter {
            it.name.contains(query.trim(), ignoreCase = true)
        }
    }

    Column(Modifier.fillMaxSize()) {

        if (state.repositories.isNotEmpty()) {
            SearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search repositories",
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            if (state.storageFallback) {
                item(key = "storage-notice") {
                    StorageNotice(
                        path = state.storagePath,
                        onGrant = { openAllFilesAccess(context) }
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

            if (state.storagePath.isNotBlank()) {
                item(key = "path") {
                    Text(
                        state.storagePath,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GithubTab(
    state: RexGitUiState,
    onConnect: () -> Unit,
    onClone: (RexGitGithubRepository) -> Unit,
    onOpen: (RexGitRepository) -> Unit
) {
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

    var query by rememberSaveable { mutableStateOf("") }

    val repos = remember(state.githubRepositories, query) {
        state.githubRepositories.filter {
            it.fullName.contains(query.trim(), ignoreCase = true)
        }
    }

    Column(Modifier.fillMaxSize()) {

        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    (state.githubUsername ?: "?").take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text("@${state.githubUsername}", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${state.githubRepositories.size} repositories",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onConnect) {
                Icon(Icons.Default.Settings, contentDescription = "GitHub account settings")
            }
        }

        SearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = "Search GitHub repositories",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
        )

        ProgressSlot(state.isGithubLoading)

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (repos.isEmpty() && !state.isGithubLoading) {
                item(key = "empty") {
                    EmptyState(
                        icon = Icons.Default.Search,
                        title = if (query.isBlank()) "No repositories found" else "No matches",
                        body = if (query.isBlank()) "Your account has no repositories yet."
                        else "Nothing matches \"${query.trim()}\"."
                    )
                }
            }

            items(repos, key = { it.fullName }) { repo ->
                val local = state.repositories.firstOrNull { it.name == repo.name }

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
// Repository
// ============================================================

@Composable
private fun RepositoryScreen(
    state: RexGitUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpen: (RexGitFile) -> Unit,
    onUp: () -> Unit,
    onPull: () -> Unit,
    onPush: () -> Unit
) {
    val repo = state.selectedRepository ?: return

    var showChanges by rememberSaveable { mutableStateOf(false) }

    val changes = state.gitStatus.changes
    val changeMap = remember(changes) { changes.associate { it.path to it.kind } }
    val busy = state.isPushing || state.isPulling || state.isRepoLoading
    val relative = state.currentDirectory
        ?.removePrefix(repo.path)
        ?.trim('/')
        ?: ""

    Column(Modifier.fillMaxSize()) {

        TopAppBar(
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back to repositories")
                }
            },
            title = {
                Text(
                    repo.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            actions = {
                IconButton(onClick = onRefresh, enabled = !busy) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh status")
                }
            }
        )

        ProgressSlot(busy)

        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (repo.isGitRepository) {
                    InfoPill(
                        icon = Icons.Default.CallSplit,
                        text = repo.branch,
                        container = MaterialTheme.colorScheme.secondaryContainer,
                        content = MaterialTheme.colorScheme.onSecondaryContainer
                    )

                    if (changes.isEmpty() && state.gitStatus.error == null) {
                        InfoPill(
                            icon = Icons.Default.CheckCircle,
                            text = "Clean",
                            container = MaterialTheme.colorScheme.primaryContainer,
                            content = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else if (changes.isNotEmpty()) {
                        InfoPill(
                            icon = Icons.Default.Warning,
                            text = if (changes.size == 1) "1 change" else "${changes.size} changes",
                            container = MaterialTheme.colorScheme.tertiaryContainer,
                            content = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                } else {
                    InfoPill(
                        icon = Icons.Default.Warning,
                        text = "Not a Git repository",
                        container = MaterialTheme.colorScheme.errorContainer,
                        content = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            if (!repo.remoteUrl.isNullOrBlank()) {
                Text(
                    repo.remoteUrl,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (relative.isNotEmpty()) {
                IconButton(onClick = onUp) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Go to parent folder")
                }
            } else {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                if (relative.isEmpty()) "/" else "/$relative",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(Modifier.weight(1f)) {
            if (state.files.isEmpty() && !state.isRepoLoading) {
                EmptyState(
                    icon = Icons.Default.FolderOpen,
                    title = "This folder is empty",
                    body = "Files you add will show up here."
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    items(state.files, key = { it.path }) { file ->
                        val rel = file.path.removePrefix(repo.path).trimStart('/')

                        val kind =
                            if (file.isDirectory) null else changeMap[rel]

                        val dirChanged =
                            file.isDirectory && changeMap.keys.any { it.startsWith("$rel/") }

                        FileRow(
                            file = file,
                            change = kind,
                            hasChangesInside = dirChanged,
                            onClick = { onOpen(file) }
                        )
                    }
                }
            }
        }

        if (state.gitStatus.error != null) {
            Text(
                "Status unavailable: ${state.gitStatus.error}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        if (repo.isGitRepository && changes.isNotEmpty()) {
            Surface(
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                Column {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(
                                onClickLabel =
                                    if (showChanges) "Hide changes" else "Show changes"
                            ) { showChanges = !showChanges }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Changes (${changes.size})",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f)
                        )

                        Icon(
                            if (showChanges) Icons.Default.ExpandMore
                            else Icons.Default.ExpandLess,
                            contentDescription = null
                        )
                    }

                    if (showChanges) {
                        LazyColumn(
                            Modifier.heightIn(max = 200.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(
                                changes.take(300),
                                key = { it.kind.name + it.path }
                            ) { change ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    ChangeBadge(change.kind)

                                    Spacer(Modifier.width(10.dp))

                                    Text(
                                        change.path,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (changes.size > 300) {
                                item(key = "more") {
                                    Text(
                                        "+ ${changes.size - 300} more",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Surface(tonalElevation = 3.dp) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onPull,
                    enabled = repo.isGitRepository && !busy,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    if (state.isPulling) ButtonSpinner()
                    else Icon(Icons.Default.CloudDownload, contentDescription = null)

                    Spacer(Modifier.width(8.dp))
                    Text(if (state.isPulling) "Pulling" else "Pull")
                }

                Button(
                    onClick = onPush,
                    enabled = repo.isGitRepository && !busy,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    if (state.isPushing) ButtonSpinner()
                    else Icon(Icons.Default.CloudUpload, contentDescription = null)

                    Spacer(Modifier.width(8.dp))
                    Text(
                        when {
                            state.isPushing -> "Pushing"
                            changes.isNotEmpty() -> "Push (${changes.size})"
                            else -> "Push"
                        }
                    )
                }
            }
        }
    }
}

// ============================================================
// Editor
// ============================================================

@Composable
private fun EditorScreen(
    state: RexGitUiState,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onEdit: (String) -> Unit
) {
    val path = state.editorPath ?: return

    // Local text state keeps the cursor stable while typing.
    var text by remember(path) { mutableStateOf(state.editorContent) }

    val repoPath = state.selectedRepository?.path ?: ""
    val name = path.substringAfterLast('/')
    val relative = path.removePrefix(repoPath).trimStart('/')
    val lines = remember(text) { text.count { it == '\n' } + 1 }

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Close editor")
                }
            },
            title = {
                Column {
                    Text(
                        name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        if (state.editorDirty) "Unsaved changes" else relative,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelMedium,
                        color =
                            if (state.editorDirty) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = onSave,
                    enabled = state.editorDirty && !state.isSaving
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save file")
                }
            }
        )

        ProgressSlot(state.isSaving)

        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 1.dp
        ) {
            BasicTextField(
                value = text,
                onValueChange = {
                    text = it
                    onEdit(it)
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    autoCorrect = false
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp)
            )
        }

        Text(
            "$lines lines  •  ${text.length} characters",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

// ============================================================
// Cards & rows
// ============================================================

@Composable
private fun RepositoryCard(
    repo: RexGitRepository,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    repo.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    if (repo.isGitRepository) "Git repository" else "Plain folder",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.Default.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GithubRepositoryCard(
    repo: RexGitGithubRepository,
    cloned: Boolean,
    cloning: Boolean,
    anyCloning: Boolean,
    onClone: () -> Unit,
    onOpen: () -> Unit
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    repo.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        if (repo.private) Icons.Default.Lock else Icons.Default.Public,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.width(4.dp))

                    Text(
                        (if (repo.private) "Private" else "Public") + "  •  " + repo.fullName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!repo.description.isNullOrBlank()) {
                    Text(
                        repo.description,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            if (cloned) {
                OutlinedButton(onClick = onOpen) { Text("Open") }
            } else {
                FilledTonalButton(
                    onClick = onClone,
                    enabled = !anyCloning
                ) {
                    if (cloning) ButtonSpinner()
                    else Icon(
                        Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(Modifier.width(6.dp))
                    Text(if (cloning) "Cloning" else "Clone")
                }
            }
        }
    }
}

@Composable
private fun FileRow(
    file: RexGitFile,
    change: RexGitChangeKind?,
    hasChangesInside: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                onClickLabel = if (file.isDirectory) "Open folder" else "Open file",
                onClick = onClick
            )
            .heightIn(min = 52.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (file.isDirectory) Icons.Default.Folder else Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint =
                if (file.isDirectory) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.width(14.dp))

        Column(Modifier.weight(1f)) {
            Text(
                file.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!file.isDirectory) {
                Text(
                    formatSize(file.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (change != null) {
            ChangeBadge(change)
        } else if (hasChangesInside) {
            Text(
                "changed",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

// ============================================================
// Dialogs
// ============================================================

@Composable
private fun GithubDialog(
    state: RexGitUiState,
    onDismiss: () -> Unit,
    onConnect: (String) -> Unit,
    onDisconnect: () -> Unit
) {
    var token by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    val wasConnected = remember { state.isGithubConnected }

    // Close automatically once the connection succeeds.
    LaunchedEffect(state.isGithubConnected) {
        if (state.isGithubConnected && !wasConnected) onDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("GitHub account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (state.isGithubConnected) {
                    Text(
                        "Connected as @${state.githubUsername}",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        "Your token is stored encrypted with the Android Keystore.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    OutlinedTextField(
                        value = token,
                        onValueChange = { token = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Personal access token") },
                        singleLine = true,
                        enabled = !state.isGithubLoading,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation =
                            if (visible) VisualTransformation.None
                            else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { visible = !visible }) {
                                Icon(
                                    if (visible) Icons.Default.VisibilityOff
                                    else Icons.Default.Visibility,
                                    contentDescription =
                                        if (visible) "Hide token" else "Show token"
                                )
                            }
                        }
                    )

                    Text(
                        "Create a token at github.com/settings/tokens with the \"repo\" scope. " +
                            "Never share it in chat or commit it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            if (state.isGithubConnected) {
                TextButton(onClick = onDisconnect) { Text("Disconnect") }
            } else {
                Button(
                    onClick = { onConnect(token) },
                    enabled = !state.isGithubLoading && token.isNotBlank()
                ) {
                    if (state.isGithubLoading) {
                        ButtonSpinner()
                        Spacer(Modifier.width(8.dp))
                    }

                    Text(if (state.isGithubLoading) "Connecting" else "Connect")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun PushDialog(
    message: String,
    onMessageChange: (String) -> Unit,
    bumpVersion: Boolean,
    onBumpChange: (Boolean) -> Unit,
    changeCount: Int,
    onDismiss: () -> Unit,
    onPush: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Commit & push") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (changeCount == 0) "No local changes. Existing commits will be pushed."
                    else "$changeCount changed file(s) will be committed and pushed to origin.",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (changeCount > 0) {
                    OutlinedTextField(
                        value = message,
                        onValueChange = onMessageChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Commit message") },
                        maxLines = 4
                    )

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = bumpVersion,
                                role = Role.Switch,
                                onValueChange = onBumpChange
                            )
                            .heightIn(min = 48.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Bump version", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Updates versionName and versionCode in build.gradle",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Switch(checked = bumpVersion, onCheckedChange = null)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onPush,
                enabled = changeCount == 0 || message.isNotBlank()
            ) {
                Icon(
                    Icons.Default.CloudUpload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(Modifier.width(8.dp))
                Text("Push")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ErrorDialog(
    text: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Something went wrong") },
        text = {
            Box(
                Modifier
                    .heightIn(max = 240.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                SelectionContainer {
                    Text(text, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    )
}

// ============================================================
// Shared building blocks
// ============================================================

@Composable
private fun RexGitLogo() {
    Box(
        Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Code,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

/** Reserves space for the progress bar so the layout never jumps. */
@Composable
private fun ProgressSlot(visible: Boolean) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(4.dp)
    ) {
        if (visible) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ButtonSpinner() {
    CircularProgressIndicator(
        modifier = Modifier.size(18.dp),
        strokeWidth = 2.dp
    )
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                }
            }
        }
    )
}

@Composable
private fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(6.dp))

        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
private fun StorageNotice(
    path: String,
    onGrant: () -> Unit
) {
    ElevatedCard(
        Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary
            )

            Spacer(Modifier.width(12.dp))

            Column {
                Text("Using private app storage", style = MaterialTheme.typography.titleSmall)

                Spacer(Modifier.height(4.dp))

                Text(
                    "Downloads isn't writable, so repositories are stored in:\n$path\n\n" +
                        "Allow \"All files access\" to use /Download/RexAps instead.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TextButton(onClick = onGrant) { Text("Grant access") }
            }
        }
    }
}

@Composable
private fun InfoPill(
    icon: ImageVector,
    text: String,
    container: Color,
    content: Color
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = container,
        contentColor = content
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))

            Spacer(Modifier.width(6.dp))

            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ChangeBadge(kind: RexGitChangeKind) {
    val scheme = MaterialTheme.colorScheme

    val (bg, fg) = when (kind) {
        RexGitChangeKind.ADDED -> scheme.primaryContainer to scheme.onPrimaryContainer
        RexGitChangeKind.MODIFIED -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        RexGitChangeKind.DELETED -> scheme.errorContainer to scheme.onErrorContainer
        RexGitChangeKind.CONFLICT -> scheme.errorContainer to scheme.onErrorContainer
        RexGitChangeKind.UNTRACKED -> scheme.secondaryContainer to scheme.onSecondaryContainer
    }

    Box(
        Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .semantics { contentDescription = kind.label },
        contentAlignment = Alignment.Center
    ) {
        Text(
            kind.symbol,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = fg
        )
    }
}

// ============================================================
// Utilities
// ============================================================

private fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "%.1f MB".format(bytes / 1048576.0)
    }
}

private fun openAllFilesAccess(context: Context) {
    val uri = Uri.parse("package:${context.packageName}")

    try {
        val intent =
            if (Build.VERSION.SDK_INT >= 30) {
                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri)
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri)
            }

        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Throwable) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Throwable) {
        }
    }
}
