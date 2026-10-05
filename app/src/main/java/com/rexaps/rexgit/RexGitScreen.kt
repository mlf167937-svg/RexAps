
@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.rexaps.rexgit

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private enum class CreateKind { FILE, FOLDER }

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
    var commitMessage by rememberSaveable { mutableStateOf("") }
    var bumpVersion by rememberSaveable { mutableStateOf(true) }
    var commitType by rememberSaveable { mutableStateOf(RexGitCommitType.FIX.name) }
    var commitFolder by rememberSaveable { mutableStateOf("") }

    val detectedCommitFolders = remember(
        state.selectedRepository?.path,
        state.gitStatus.changes
    ) {
        RexGitCommitMeta.detectFolders(
            state.selectedRepository,
            state.gitStatus.changes
        )
    }

    LaunchedEffect(pushDialog) {
        if (pushDialog) {
            val suggested = RexGitCommitMeta.inferFolder(
                state.selectedRepository,
                state.gitStatus.changes
            )
            val suggestedType = RexGitCommitMeta.inferType(state.gitStatus.changes)
            commitFolder = suggested ?: detectedCommitFolders.firstOrNull().orEmpty()
            commitType = suggestedType.name
        }
    }

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
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = if (screen == 1) 84.dp else 0.dp)
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
                        onNavigate = { path ->
                            if (path == null) viewModel.openRoot()
                            else viewModel.openDirectory(path)
                        },
                        onPull = viewModel::pull,
                        onPush = { pushDialog = true },
                        onCreateFile = viewModel::createFile,
                        onCreateFolder = viewModel::createFolder,
                        onDelete = viewModel::deleteEntry
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
            type = RexGitCommitType.valueOf(commitType),
            onTypeChange = { commitType = it.name },
            folder = commitFolder,
            folders = detectedCommitFolders,
            onFolderChange = { commitFolder = it },
            bumpVersion = bumpVersion,
            onBumpChange = { bumpVersion = it },
            changeCount = state.gitStatus.changes.size,
            onDismiss = { pushDialog = false },
            onPush = {
                viewModel.push(
                    message = commitMessage.trim(),
                    bumpVersion = bumpVersion,
                    commitType = RexGitCommitType.valueOf(commitType),
                    commitFolder = commitFolder
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
            LocalTab(state = state, onConnect = onGithub, onSelect = onSelect)
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
private fun HomeHeader(
    state: RexGitUiState,
    onRefresh: () -> Unit,
    onGithub: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RexGitLogo()

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text("RexGit", style = MaterialTheme.typography.headlineSmall)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(
                            if (state.isGithubConnected) scheme.secondary else scheme.outline
                        )
                )

                Spacer(Modifier.width(6.dp))

                Text(
                    if (state.isGithubConnected) "@${state.githubUsername}"
                    else "GitHub not connected",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        HeaderIconButton(
            icon = Icons.Default.Refresh,
            description = "Refresh repositories",
            onClick = onRefresh
        )

        Spacer(Modifier.width(8.dp))

        HeaderIconButton(
            icon = Icons.Default.Cloud,
            description =
                if (state.isGithubConnected) "GitHub account, connected"
                else "Connect GitHub",
            onClick = onGithub,
            tint = if (state.isGithubConnected) scheme.primary else scheme.onSurfaceVariant
        )
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

    val gitCount = remember(state.repositories) {
        state.repositories.count { it.isGitRepository }
    }

    Column(Modifier.fillMaxSize()) {

        if (state.repositories.isNotEmpty()) {
            SearchField(
                value = query,
                onValueChange = { query = it },
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
private fun GithubTab(
    state: RexGitUiState,
    onConnect: () -> Unit,
    onClone: (RexGitGithubRepository) -> Unit,
    onOpen: (RexGitRepository) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }

    val repos = remember(state.githubRepositories, query) {
        state.githubRepositories.filter {
            it.fullName.contains(query.trim(), ignoreCase = true)
        }
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
                    "${state.githubRepositories.size} repositories",
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

        SearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = "Search GitHub repositories",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
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
    onNavigate: (String?) -> Unit,
    onPull: () -> Unit,
    onPush: () -> Unit,
    onCreateFile: (String) -> Unit,
    onCreateFolder: (String) -> Unit,
    onDelete: (RexGitFile) -> Unit
) {
    val repo = state.selectedRepository ?: return
    val scheme = MaterialTheme.colorScheme

    var showChanges by rememberSaveable { mutableStateOf(false) }
    var createMenu by remember { mutableStateOf(false) }
    var createKind by remember { mutableStateOf<CreateKind?>(null) }
    var actionTarget by remember { mutableStateOf<RexGitFile?>(null) }
    var deleteTarget by remember { mutableStateOf<RexGitFile?>(null) }

    val changes = state.gitStatus.changes
    val changeMap = remember(changes) { changes.associate { it.path to it.kind } }
    val busy = state.isPushing || state.isPulling || state.isRepoLoading

    val relative = state.currentDirectory
        ?.removePrefix(repo.path)
        ?.trim('/')
        ?: ""

    val segments = remember(relative) {
        relative.split('/').filter { it.isNotEmpty() }
    }

    val locationLabel =
        if (segments.isEmpty()) repo.name
        else repo.name + "/" + segments.joinToString("/")

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {

        // ----- header -----
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderIconButton(
                icon = Icons.Default.ArrowBack,
                description = "Back to repositories",
                onClick = onBack
            )

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    repo.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!repo.remoteUrl.isNullOrBlank()) {
                    Text(
                        repo.remoteUrl.removePrefix("https://"),
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            HeaderIconButton(
                icon = Icons.Default.Refresh,
                description = "Refresh status",
                onClick = onRefresh,
                enabled = !busy
            )
        }

        ProgressSlot(busy)

        // ----- status pills -----
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (repo.isGitRepository) {
                InfoPill(
                    icon = Icons.Default.CallSplit,
                    text = repo.branch,
                    container = scheme.secondaryContainer,
                    content = scheme.onSecondaryContainer
                )

                if (changes.isEmpty() && state.gitStatus.error == null) {
                    InfoPill(
                        icon = Icons.Default.CheckCircle,
                        text = "Clean",
                        container = scheme.primaryContainer,
                        content = scheme.onPrimaryContainer
                    )
                } else if (changes.isNotEmpty()) {
                    InfoPill(
                        icon = Icons.Default.Warning,
                        text = if (changes.size == 1) "1 change" else "${changes.size} changes",
                        container = scheme.tertiaryContainer,
                        content = scheme.onTertiaryContainer
                    )
                }
            } else {
                InfoPill(
                    icon = Icons.Default.Warning,
                    text = "Not a Git repository",
                    container = scheme.errorContainer,
                    content = scheme.onErrorContainer
                )
            }
        }

        // ----- breadcrumb -----
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val rootActive = segments.isEmpty()

            Crumb(
                text = repo.name,
                active = rootActive,
                onClick = { onNavigate(null) }
            )

            segments.forEachIndexed { index, segment ->
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = scheme.onSurfaceVariant
                )

                val target = repo.path + "/" + segments.take(index + 1).joinToString("/")

                Crumb(
                    text = segment,
                    active = index == segments.lastIndex,
                    onClick = { onNavigate(target) }
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // ----- files -----
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(22.dp),
                color = scheme.surface,
                border = BorderStroke(1.dp, scheme.outlineVariant)
            ) {
                if (state.files.isEmpty() && !state.isRepoLoading) {
                    EmptyState(
                        icon = Icons.Default.FolderOpen,
                        title = "This folder is empty",
                        body = "Create a file or folder to get started.",
                        modifier = Modifier.fillMaxSize(),
                        actionLabel = "Create new",
                        onAction = { createMenu = true }
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(top = 6.dp, bottom = 96.dp)
                    ) {
                        itemsIndexed(state.files, key = { _, f -> f.path }) { index, file ->
                            val rel = file.path.removePrefix(repo.path).trimStart('/')
                            val kind = if (file.isDirectory) null else changeMap[rel]
                            val dirChanged =
                                file.isDirectory && changeMap.keys.any { it.startsWith("$rel/") }

                            Column {
                                if (index > 0) {
                                    Box(
                                        Modifier
                                            .padding(start = 68.dp)
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(scheme.outlineVariant.copy(alpha = 0.6f))
                                    )
                                }

                                FileRow(
                                    file = file,
                                    change = kind,
                                    hasChangesInside = dirChanged,
                                    onClick = { onOpen(file) },
                                    onMore = { actionTarget = file }
                                )
                            }
                        }
                    }
                }
            }

            ExtendedFloatingActionButton(
                onClick = { createMenu = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New") },
                containerColor = scheme.primary,
                contentColor = scheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 28.dp, bottom = 16.dp)
            )
        }

        if (state.gitStatus.error != null) {
            Text(
                "Status unavailable: ${state.gitStatus.error}",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
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
            onPull = onPull,
            onPush = onPush
        )
    }

    // ----- sheets & dialogs -----

    if (createMenu) {
        CreateSheet(
            location = locationLabel,
            onDismiss = { createMenu = false },
            onPick = {
                createMenu = false
                createKind = it
            }
        )
    }

    createKind?.let { kind ->
        NameDialog(
            kind = kind,
            location = locationLabel,
            existing = remember(state.files) { state.files.map { it.name }.toSet() },
            onDismiss = { createKind = null },
            onConfirm = { name ->
                createKind = null
                if (kind == CreateKind.FILE) onCreateFile(name) else onCreateFolder(name)
            }
        )
    }

    actionTarget?.let { file ->
        ItemActionsSheet(
            file = file,
            onDismiss = { actionTarget = null },
            onOpen = {
                actionTarget = null
                onOpen(file)
            },
            onDelete = {
                actionTarget = null
                deleteTarget = file
            }
        )
    }

    deleteTarget?.let { file ->
        DeleteDialog(
            file = file,
            isGit = repo.isGitRepository,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                deleteTarget = null
                onDelete(file)
            }
        )
    }
}

@Composable
private fun Crumb(
    text: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = if (active) scheme.primary else scheme.onSurfaceVariant,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = !active, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp)
    )
}

@Composable
private fun ChangesPanel(
    changes: List<RexGitChange>,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        color = scheme.surface,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        border = BorderStroke(1.dp, scheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClickLabel = if (expanded) "Hide changes" else "Show changes",
                        onClick = onToggle
                    )
                    .heightIn(min = 52.dp)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Changes (${changes.size})",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    if (expanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                    contentDescription = null,
                    tint = scheme.onSurfaceVariant
                )
            }

            if (expanded) {
                LazyColumn(
                    Modifier.heightIn(max = 200.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(changes.take(300), key = { it.kind.name + it.path }) { change ->
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
                                color = scheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ActionBar(
    isGit: Boolean,
    busy: Boolean,
    pulling: Boolean,
    pushing: Boolean,
    changeCount: Int,
    onPull: () -> Unit,
    onPush: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Surface(color = scheme.surface) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(scheme.outlineVariant)
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onPull,
                    enabled = isGit && !busy,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, scheme.outline),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp)
                ) {
                    if (pulling) ButtonSpinner()
                    else Icon(Icons.Default.CloudDownload, contentDescription = null)

                    Spacer(Modifier.width(8.dp))
                    Text(if (pulling) "Pulling" else "Pull")
                }

                GradientButton(
                    text = when {
                        pushing -> "Pushing"
                        changeCount > 0 -> "Push ($changeCount)"
                        else -> "Push"
                    },
                    icon = Icons.Default.CloudUpload,
                    onClick = onPush,
                    enabled = isGit && !busy,
                    loading = pushing,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ============================================================
// Create / delete UI
// ============================================================

@Composable
private fun CreateSheet(
    location: String,
    onDismiss: () -> Unit,
    onPick: (CreateKind) -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = scheme.surface
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            Text("Create new", style = MaterialTheme.typography.titleLarge)

            Text(
                "in $location",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            SheetAction(
                icon = Icons.Default.NoteAdd,
                title = "New file",
                subtitle = "Create an empty file and open it in the editor",
                onClick = { onPick(CreateKind.FILE) }
            )

            SheetAction(
                icon = Icons.Default.CreateNewFolder,
                title = "New folder",
                subtitle = "Create an empty folder here",
                onClick = { onPick(CreateKind.FOLDER) }
            )
        }
    }
}

@Composable
private fun ItemActionsSheet(
    file: RexGitFile,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val style = fileStyle(file, scheme)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = scheme.surface
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            Row(
                Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconTile(style.icon, style.color)

                Spacer(Modifier.width(14.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        file.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        if (file.isDirectory) "Folder" else formatSize(file.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            SheetAction(
                icon = if (file.isDirectory) Icons.Default.FolderOpen else Icons.Default.Description,
                title = if (file.isDirectory) "Open folder" else "Edit file",
                subtitle = null,
                onClick = onOpen
            )

            SheetAction(
                icon = Icons.Default.Delete,
                title = if (file.isDirectory) "Delete folder" else "Delete file",
                subtitle = if (file.isDirectory) "Removes the folder and everything inside it"
                else "Permanently removes this file",
                destructive = true,
                onClick = onDelete
            )
        }
    }
}

@Composable
private fun SheetAction(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    destructive: Boolean = false
) {
    val scheme = MaterialTheme.colorScheme
    val tint = if (destructive) scheme.error else scheme.primary

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .heightIn(min = 60.dp)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconTile(icon, tint)

        Spacer(Modifier.width(14.dp))

        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = if (destructive) scheme.error else scheme.onSurface
            )

            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NameDialog(
    kind: CreateKind,
    location: String,
    existing: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }

    val error: String? = when {
        name.isBlank() -> null
        validateEntryName(name) != null -> validateEntryName(name)
        existing.any { it.equals(name.trim(), ignoreCase = true) } ->
            "\"${name.trim()}\" already exists in this folder"
        else -> null
    }

    val canCreate = name.isNotBlank() && error == null
    val isFile = kind == CreateKind.FILE

    LaunchedEffect(Unit) {
        runCatching { focus.requestFocus() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isFile) "New file" else "New folder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "in $location",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focus),
                    label = { Text(if (isFile) "File name" else "Folder name") },
                    placeholder = { Text(if (isFile) "MainActivity.kt" else "components") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = {
                        Text(error ?: if (isFile) "Include the extension, e.g. notes.md" else "Letters, numbers, dots and dashes")
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { if (canCreate) onConfirm(name) }
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name) },
                enabled = canCreate
            ) {
                Text(if (isFile) "Create file" else "Create folder")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun DeleteDialog(
    file: RexGitFile,
    isGit: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Delete, contentDescription = null, tint = scheme.error)
        },
        title = { Text(if (file.isDirectory) "Delete folder?" else "Delete file?") },
        text = {
            Text(
                if (file.isDirectory)
                    "\"${file.name}\" and everything inside it will be permanently deleted from this device." +
                        (if (isGit) " Uncommitted work inside it can't be recovered." else "")
                else
                    "\"${file.name}\" will be permanently deleted from this device." +
                        (if (isGit) " If it was never committed, it can't be recovered." else "")
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = scheme.error,
                    contentColor = scheme.onError
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ============================================================
// Editor
// ============================================================

private val EditorKeys = listOf(
    "Tab" to "    ",
    "{" to "{", "}" to "}",
    "(" to "(", ")" to ")",
    "[" to "[", "]" to "]",
    "<" to "<", ">" to ">",
    ";" to ";", ":" to ":",
    "\"" to "\"", "'" to "'",
    "=" to "=", "/" to "/", "#" to "#"
)

private const val EDITOR_MIN_FONT = 2
private const val EDITOR_MAX_FONT = 38
private const val EDITOR_DEFAULT_FONT = 14

@Composable
private fun EditorScreen(
    state: RexGitUiState,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onEdit: (String) -> Unit
) {
    val path = state.editorPath ?: return
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val focusRequester = remember { FocusRequester() }

    var field by remember(path) { mutableStateOf(TextFieldValue(state.editorContent)) }
    var fontSize by rememberSaveable { mutableStateOf(EDITOR_DEFAULT_FONT) }
    var searchOpen by rememberSaveable(path) { mutableStateOf(false) }
    var searchQuery by rememberSaveable(path) { mutableStateOf("") }
    var searchIndex by rememberSaveable(path) { mutableStateOf(0) }
    var goToLineOpen by rememberSaveable(path) { mutableStateOf(false) }
    var goToLineText by rememberSaveable(path) { mutableStateOf("1") }

    val repoPath = state.selectedRepository?.path ?: ""
    val name = path.substringAfterLast('/')
    val relative = path.removePrefix(repoPath).trimStart('/')
    val lines = remember(field.text) { field.text.count { it == '\n' } + 1 }
    val language = remember(path) { RexGitSyntax.detect(path) }

    val syntaxColors = RexGitSyntaxColors(
        keyword = scheme.primary,
        string = scheme.tertiary,
        number = scheme.secondary,
        comment = scheme.onSurfaceVariant.copy(alpha = 0.72f),
        type = scheme.primary.copy(alpha = 0.82f),
        property = scheme.secondary,
        tag = scheme.primary,
        punctuation = scheme.onSurfaceVariant
    )

    val highlighted = remember(
        field.text,
        language,
        syntaxColors
    ) {
        RexGitSyntax.highlight(field.text, language, syntaxColors)
    }

    val visualTransformation = remember(highlighted) {
        VisualTransformation { transformed ->
            TransformedText(
                highlighted,
                androidx.compose.ui.text.input.OffsetMapping.Identity
            )
        }
    }

    fun insert(snippet: String) {
        val sel = field.selection
        val updated = field.text.replaceRange(sel.min, sel.max, snippet)
        field = TextFieldValue(updated, TextRange(sel.min + snippet.length))
        onEdit(updated)
    }

    fun toast(text: String) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }

    fun searchMatches(query: String): List<Int> {
        if (query.isBlank()) return emptyList()
        val result = mutableListOf<Int>()
        var from = 0
        while (from < field.text.length) {
            val found = field.text.indexOf(query, from, ignoreCase = true)
            if (found < 0) break
            result += found
            from = found + maxOf(query.length, 1)
        }
        return result
    }

    fun selectSearchMatch(direction: Int) {
        val matches = searchMatches(searchQuery)
        if (matches.isEmpty()) {
            toast("No match")
            return
        }
        searchIndex = (searchIndex + direction).mod(matches.size)
        val start = matches[searchIndex]
        field = field.copy(selection = TextRange(start, start + searchQuery.length))
        focusRequester.requestFocus()
    }

    fun goToLine() {
        val requested = goToLineText.toIntOrNull() ?: 1
        val targetLine = requested.coerceIn(1, lines)
        var currentLine = 1
        var offset = 0
        while (currentLine < targetLine) {
            val next = field.text.indexOf('\n', offset)
            if (next < 0) break
            offset = next + 1
            currentLine++
        }
        field = field.copy(selection = TextRange(offset))
        focusRequester.requestFocus()
        goToLineOpen = false
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderIconButton(
                icon = Icons.Default.ArrowBack,
                description = "Close editor",
                onClick = onBack
            )

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (state.editorDirty) "Unsaved changes" else relative,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (state.editorDirty) scheme.tertiary else scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                RexGitSyntax.label(language),
                style = MaterialTheme.typography.labelMedium,
                color = scheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            HeaderIconButton(
                icon = Icons.Default.Search,
                description = "Search",
                onClick = { searchOpen = true }
            )

            Spacer(Modifier.width(6.dp))

            Button(
                onClick = onSave,
                enabled = state.editorDirty && !state.isSaving,
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier.heightIn(min = 44.dp)
            ) {
                Text(if (state.isSaving) "Saving" else "Save")
            }
        }

        ProgressSlot(state.isSaving)

        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(
                onClick = { searchOpen = true },
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.heightIn(min = 40.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Search")
            }

            FilledTonalButton(
                onClick = {
                    goToLineText = "${field.text.substring(0, field.selection.start).count { it == '\n' } + 1}"
                    goToLineOpen = true
                },
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.heightIn(min = 40.dp)
            ) {
                Text("Go to line")
            }

            FilledTonalButton(
                onClick = {
                    clipboard.setText(AnnotatedString(field.text))
                    toast("All text copied")
                },
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.heightIn(min = 40.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Copy all")
            }

            FilledTonalButton(
                onClick = {
                    val text = clipboard.getText()?.text.orEmpty()
                    if (text.isNotEmpty()) insert(text) else toast("Clipboard is empty")
                },
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.heightIn(min = 40.dp)
            ) {
                Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Paste")
            }

            FilledTonalButton(
                onClick = { field = field.copy(selection = TextRange(0, field.text.length)) },
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.heightIn(min = 40.dp)
            ) {
                Icon(Icons.Default.SelectAll, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Select all")
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = { fontSize = (fontSize - 1).coerceAtLeast(EDITOR_MIN_FONT) },
                enabled = fontSize > EDITOR_MIN_FONT,
                shape = RoundedCornerShape(10.dp),
                color = scheme.surfaceVariant
            ) {
                Text("A−", style = MaterialTheme.typography.labelLarge, modifier = Modifier.heightIn(min = 40.dp).padding(horizontal = 14.dp, vertical = 10.dp))
            }
            Slider(
                value = fontSize.toFloat(),
                onValueChange = { fontSize = it.toInt().coerceIn(EDITOR_MIN_FONT, EDITOR_MAX_FONT) },
                valueRange = EDITOR_MIN_FONT.toFloat()..EDITOR_MAX_FONT.toFloat(),
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            )
            Surface(
                onClick = { fontSize = (fontSize + 1).coerceAtMost(EDITOR_MAX_FONT) },
                enabled = fontSize < EDITOR_MAX_FONT,
                shape = RoundedCornerShape(10.dp),
                color = scheme.surfaceVariant
            ) {
                Text("A+", style = MaterialTheme.typography.labelLarge, modifier = Modifier.heightIn(min = 40.dp).padding(horizontal = 14.dp, vertical = 10.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text("${fontSize}px", style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant, modifier = Modifier.width(46.dp))
        }

        Spacer(Modifier.height(4.dp))

        Surface(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
            shape = RoundedCornerShape(18.dp),
            color = scheme.surface,
            border = BorderStroke(1.dp, scheme.outlineVariant)
        ) {
            BasicTextField(
                value = field,
                onValueChange = { new ->
                    val changed = new.text != field.text
                    field = new
                    if (changed) onEdit(new.text)
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    color = scheme.onSurface,
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * 1.4f).sp
                ),
                visualTransformation = visualTransformation,
                cursorBrush = SolidColor(scheme.primary),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    autoCorrect = false
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .focusRequester(focusRequester)
                    .padding(14.dp)
            )
        }

        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            EditorKeys.forEach { (label, snippet) ->
                Surface(
                    onClick = { insert(snippet) },
                    shape = RoundedCornerShape(10.dp),
                    color = scheme.surfaceVariant
                ) {
                    Text(label, style = MaterialTheme.typography.labelLarge, fontFamily = FontFamily.Monospace, modifier = Modifier.heightIn(min = 40.dp).padding(horizontal = 14.dp, vertical = 10.dp))
                }
            }
        }

        Text(
            "$lines lines  •  ${field.text.length} characters",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
        )
    }

    if (searchOpen) {
        AlertDialog(
            onDismissRequest = { searchOpen = false },
            title = { Text("Search in file") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            searchIndex = 0
                        },
                        singleLine = true,
                        label = { Text("Find") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                    )
                    val count = searchMatches(searchQuery).size
                    Text(
                        if (searchQuery.isBlank()) "Type text to search" else "$count match${if (count == 1) "" else "es"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { selectSearchMatch(-1) }, enabled = searchQuery.isNotBlank()) { Text("Previous") }
                        OutlinedButton(onClick = { selectSearchMatch(1) }, enabled = searchQuery.isNotBlank()) { Text("Next") }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { searchOpen = false }) { Text("Done") } }
        )
    }

    if (goToLineOpen) {
        AlertDialog(
            onDismissRequest = { goToLineOpen = false },
            title = { Text("Go to line") },
            text = {
                OutlinedTextField(
                    value = goToLineText,
                    onValueChange = { goToLineText = it.filter(Char::isDigit) },
                    singleLine = true,
                    label = { Text("Line 1–$lines") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            },
            confirmButton = { TextButton(onClick = ::goToLine) { Text("Go") } },
            dismissButton = { TextButton(onClick = { goToLineOpen = false }) { Text("Cancel") } }
        )
    }
}

// ============================================================
// Cards & rows
// ============================================================

@Composable
private fun WorkspaceCard(
    total: Int,
    gitCount: Int,
    path: String
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)

    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(scheme.primaryContainer, scheme.secondaryContainer)
                )
            )
            .border(1.dp, scheme.primary.copy(alpha = 0.25f), shape)
            .padding(20.dp)
    ) {
        Text(
            "Workspace",
            style = MaterialTheme.typography.labelLarge,
            color = scheme.onPrimaryContainer.copy(alpha = 0.7f)
        )

        Text(
            "$total",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = scheme.onPrimaryContainer
        )

        Text(
            if (total == 1) "repository" else "repositories",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onPrimaryContainer.copy(alpha = 0.8f)
        )

        Spacer(Modifier.height(14.dp))

        InfoPill(
            icon = Icons.Default.CallSplit,
            text = "$gitCount Git",
            container = scheme.surface.copy(alpha = 0.55f),
            content = scheme.onSurface
        )

        if (path.isNotBlank()) {
            Spacer(Modifier.height(12.dp))

            Text(
                path,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = scheme.onPrimaryContainer.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RepositoryCard(
    repo: RexGitRepository,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = scheme.surface,
        border = BorderStroke(1.dp, scheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconTile(
                icon = Icons.Default.Folder,
                tint = if (repo.isGitRepository) scheme.primary else scheme.onSurfaceVariant,
                size = 48.dp
            )

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    repo.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                InfoPill(
                    icon = if (repo.isGitRepository) Icons.Default.CallSplit else Icons.Default.Folder,
                    text = if (repo.isGitRepository) "Git repository" else "Plain folder",
                    container = scheme.surfaceVariant,
                    content = scheme.onSurfaceVariant,
                    compact = true
                )
            }

            Icon(
                Icons.Default.ArrowForward,
                contentDescription = null,
                tint = scheme.onSurfaceVariant
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
    val scheme = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = scheme.surface,
        border = BorderStroke(1.dp, scheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(Icons.Default.Code, scheme.secondary)

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        repo.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        repo.fullName,
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(8.dp))

                InfoPill(
                    icon = if (repo.private) Icons.Default.Lock else Icons.Default.Public,
                    text = if (repo.private) "Private" else "Public",
                    container = scheme.surfaceVariant,
                    content = scheme.onSurfaceVariant,
                    compact = true
                )
            }

            if (!repo.description.isNullOrBlank()) {
                Text(
                    repo.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            if (cloned) {
                OutlinedButton(
                    onClick = onOpen,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Icon(
                        Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Open")
                }
            } else {
                FilledTonalButton(
                    onClick = onClone,
                    enabled = !anyCloning,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    if (cloning) ButtonSpinner()
                    else Icon(
                        Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(Modifier.width(8.dp))
                    Text(if (cloning) "Cloning" else "Clone")
                }
            }
        }
    }
}

private data class FileStyle(val icon: ImageVector, val color: Color)

private fun fileStyle(file: RexGitFile, scheme: ColorScheme): FileStyle {
    if (file.isDirectory) return FileStyle(Icons.Default.Folder, scheme.primary)

    return when (file.name.substringAfterLast('.', "").lowercase()) {
        "kt", "kts", "java", "gradle" -> FileStyle(Icons.Default.Code, scheme.primary)
        "xml", "html", "css", "js", "ts", "json", "yml", "yaml", "toml" ->
            FileStyle(Icons.Default.Code, scheme.secondary)
        "md", "txt" -> FileStyle(Icons.Default.Description, scheme.tertiary)
        "png", "jpg", "jpeg", "webp", "gif", "svg" ->
            FileStyle(Icons.Default.Image, Color(0xFFFF8FB1))
        "properties", "pro", "gitignore" ->
            FileStyle(Icons.Default.Settings, scheme.onSurfaceVariant)
        else -> FileStyle(Icons.Default.InsertDriveFile, scheme.onSurfaceVariant)
    }
}

@Composable
private fun FileRow(
    file: RexGitFile,
    change: RexGitChangeKind?,
    hasChangesInside: Boolean,
    onClick: () -> Unit,
    onMore: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val style = fileStyle(file, scheme)

    Row(
        Modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = if (file.isDirectory) "Open folder" else "Open file",
                onClick = onClick
            )
            .padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconTile(style.icon, style.color)

        Spacer(Modifier.width(14.dp))

        Column(Modifier.weight(1f)) {
            Text(
                file.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                if (file.isDirectory) "Folder" else formatSize(file.size),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
        }

        if (change != null) {
            Spacer(Modifier.width(8.dp))
            ChangeBadge(change)
        } else if (hasChangesInside) {
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(scheme.tertiary)
                    .semantics { contentDescription = "Contains changes" }
            )
        }

        IconButton(onClick = onMore) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "Actions for ${file.name}",
                tint = scheme.onSurfaceVariant
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
    type: RexGitCommitType,
    onTypeChange: (RexGitCommitType) -> Unit,
    folder: String,
    folders: List<String>,
    onFolderChange: (String) -> Unit,
    bumpVersion: Boolean,
    onBumpChange: (Boolean) -> Unit,
    changeCount: Int,
    onDismiss: () -> Unit,
    onPush: () -> Unit
) {
    var typeMenuOpen by remember { mutableStateOf(false) }
    var folderMenuOpen by remember { mutableStateOf(false) }

    val canCommit = changeCount == 0 || (
        message.isNotBlank() &&
            folder.isNotBlank() &&
            folders.contains(folder)
        )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Commit & push") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (changeCount == 0) {
                        "No local changes. Existing commits will be pushed."
                    } else {
                        "$changeCount changed file(s) will be committed and pushed to origin."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )

                if (changeCount > 0) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(Modifier.weight(0.9f)) {
                            OutlinedButton(
                                onClick = { typeMenuOpen = true },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Text(type.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            DropdownMenu(
                                expanded = typeMenuOpen,
                                onDismissRequest = { typeMenuOpen = false }
                            ) {
                                RexGitCommitType.values().forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.label) },
                                        onClick = {
                                            onTypeChange(option)
                                            typeMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }

                        Box(Modifier.weight(1.1f)) {
                            OutlinedButton(
                                onClick = { folderMenuOpen = true },
                                enabled = folders.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Text(
                                    folder.ifBlank { "Folder" },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            DropdownMenu(
                                expanded = folderMenuOpen,
                                onDismissRequest = { folderMenuOpen = false }
                            ) {
                                folders.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            onFolderChange(option)
                                            folderMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (folders.isEmpty()) {
                        Text(
                            "No folder found under com/rexaps.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    OutlinedTextField(
                        value = message,
                        onValueChange = onMessageChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Description") },
                        placeholder = { Text("RexGit Screen Error") },
                        maxLines = 3,
                        singleLine = false
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
                                "Commit prefix uses the new version, e.g. [26.57] Fix: rexgit: ...",
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
                enabled = canCommit
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
    val scheme = MaterialTheme.colorScheme

    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(scheme.primary, scheme.secondary))),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Code,
            contentDescription = null,
            tint = scheme.onPrimary
        )
    }
}

@Composable
private fun IconTile(
    icon: ImageVector,
    tint: Color,
    size: Dp = 40.dp
) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

@Composable
private fun HeaderIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        color = scheme.surface,
        border = BorderStroke(1.dp, scheme.outlineVariant),
        modifier = Modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = description,
                tint = if (enabled) tint else tint.copy(alpha = 0.38f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun SegmentedTabs(
    labels: List<String>,
    icons: List<ImageVector>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Row(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        labels.forEachIndexed { index, label ->
            val isSelected = index == selected

            val bg by animateColorAsState(
                if (isSelected) scheme.surface else Color.Transparent
            )
            val fg by animateColorAsState(
                if (isSelected) scheme.primary else scheme.onSurfaceVariant
            )

            Row(
                Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg)
                    .selectable(
                        selected = isSelected,
                        role = Role.Tab,
                        onClick = { onSelect(index) }
                    ),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icons[index],
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(Modifier.width(8.dp))

                Text(label, color = fg, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** Reserves space for the progress bar so the layout never jumps. */
@Composable
private fun ProgressSlot(visible: Boolean) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(3.dp)
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
private fun GradientButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    val scheme = MaterialTheme.colorScheme

    val brush: Brush =
        if (enabled || loading) Brush.linearGradient(listOf(scheme.primary, scheme.secondary))
        else SolidColor(scheme.onSurface.copy(alpha = 0.12f))

    val content =
        if (enabled || loading) scheme.onPrimary
        else scheme.onSurface.copy(alpha = 0.38f)

    Row(
        modifier
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(brush)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = content
            )
        } else {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
        }

        Spacer(Modifier.width(8.dp))

        Text(text, color = content, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)

    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(scheme.surface)
            .border(1.dp, scheme.outlineVariant, shape)
            .padding(start = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            tint = scheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )

        Spacer(Modifier.width(10.dp))

        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurfaceVariant
                )
            }

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
                cursorBrush = SolidColor(scheme.primary),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (value.isNotEmpty()) {
            IconButton(onClick = { onValueChange("") }) {
                Icon(Icons.Default.Close, contentDescription = "Clear search")
            }
        } else {
            Spacer(Modifier.width(10.dp))
        }
    }
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
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(scheme.primaryContainer, scheme.secondaryContainer)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = scheme.onPrimaryContainer
            )
        }

        Spacer(Modifier.height(18.dp))

        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(6.dp))

        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))

            GradientButton(
                text = actionLabel,
                icon = Icons.Default.Add,
                onClick = onAction
            )
        }
    }
}

@Composable
private fun StorageNotice(
    path: String,
    onGrant: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = scheme.tertiaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = scheme.onTertiaryContainer
            )

            Spacer(Modifier.width(12.dp))

            Column {
                Text(
                    "Using private app storage",
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.onTertiaryContainer
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    "Downloads isn't writable, so repositories are stored in:\n$path\n\n" +
                        "Allow \"All files access\" to use /Download/RexAps instead.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onTertiaryContainer.copy(alpha = 0.85f)
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
    content: Color,
    compact: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = container,
        contentColor = content
    ) {
        Row(
            Modifier.padding(
                horizontal = if (compact) 8.dp else 10.dp,
                vertical = if (compact) 4.dp else 6.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(if (compact) 14.dp else 16.dp))

            Spacer(Modifier.width(6.dp))

            Text(
                text,
                style =
                    if (compact) MaterialTheme.typography.labelMedium
                    else MaterialTheme.typography.labelLarge,
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
            .clip(RoundedCornerShape(7.dp))
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


