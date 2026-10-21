
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
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.window.Dialog
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
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
import kotlin.math.cos
import kotlin.math.sin
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.res.Configuration
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import java.io.File


// ============================================================
// LANDSCAPE layout. RexGitScreen.kt picks this one in landscape.
//
// Wide screens use a side pane (account / repository info / editor
// tools) next to the main content, so nothing is squeezed into the
// short height of a phone held sideways.
// ============================================================

private val LandscapeRailWidth = 300.dp
private val LandscapeRepoPaneWidth = 320.dp
private val LandscapeEditorPanelWidth = 280.dp

/** Keeps content clear of the notch / side navigation bar in landscape. */
@Composable
private fun Modifier.landscapeSideInsets(): Modifier {
    return this.windowInsetsPadding(
        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
    )
}

@Composable
internal fun RexGitScreenLandscape(
    state: RexGitUiState,
    host: RexGitScreenHost,
    editor: RexGitEditorState?,
    viewModel: RexGitViewModel
) {
    val screen = screenIndex(state)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(
                hostState = host.snackbar,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(start = if (screen == 1) LandscapeRepoPaneWidth else 0.dp)
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
                    2 -> editor?.let {
                        EditorScreenLandscape(
                            state = state,
                            editor = it,
                            onBack = {
                                if (state.editorDirty) host.discardDialog.value = true
                                else viewModel.closeEditor()
                            },
                            onSave = viewModel::saveEditor
                        )
                    }

                    1 -> RepositoryScreenLandscape(
                        state = state,
                        host = host,
                        onBack = viewModel::backToRepositories,
                        onRefresh = viewModel::refreshStatus,
                        onOpen = viewModel::openFile,
                        onNavigate = { path ->
                            if (path == null) viewModel.openRoot()
                            else viewModel.openDirectory(path)
                        },
                        onPull = viewModel::pull
                    )

                    else -> HomeScreenLandscape(
                        state = state,
                        host = host,
                        onRefresh = viewModel::refresh,
                        onSelect = viewModel::selectRepository,
                        onClone = viewModel::cloneRepository,
                        onSwitch = viewModel::switchAccount
                    )
                }
            }
        }
    }
}

// ============================================================
// Home (landscape)
// ============================================================

@Composable
private fun HomeScreenLandscape(
    state: RexGitUiState,
    host: RexGitScreenHost,
    onRefresh: () -> Unit,
    onSelect: (RexGitRepository) -> Unit,
    onClone: (RexGitGithubRepository) -> Unit,
    onSwitch: (Int) -> Unit
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
                    onOpen = onSelect
                )
            }
        }
    }
}

@Composable
private fun GithubProfileRow(
    state: RexGitUiState,
    onSettings: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 12.dp),
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
            Text(
                "@${state.githubUsername}",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${state.githubRepositories.size} repositories",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
        }

        HeaderIconButton(
            icon = Icons.Default.Settings,
            description = "GitHub account settings",
            onClick = onSettings
        )
    }
}

@Composable
private fun LocalTabLandscape(
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

    Column(Modifier.fillMaxSize()) {

        if (state.repositories.isNotEmpty()) {
            SearchField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = "Search repositories",
                modifier = Modifier.padding(start = 8.dp, end = 16.dp, top = 8.dp)
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(300.dp),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 8.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            if (state.storageFallback) {
                item(key = "storage-notice", span = { GridItemSpan(maxLineSpan) }) {
                    StorageNotice(
                        path = state.storagePath,
                        onGrant = { openAllFilesAccess(context) }
                    )
                }
            }

            if (state.repositories.isEmpty()) {
                item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(
                        icon = Icons.Default.FolderOpen,
                        title = "No repositories yet",
                        body = "Connect GitHub and clone a repository into your workspace.",
                        actionLabel = "Connect GitHub",
                        onAction = onConnect
                    )
                }
            } else if (repos.isEmpty()) {
                item(key = "no-match", span = { GridItemSpan(maxLineSpan) }) {
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
private fun GithubTabLandscape(
    state: RexGitUiState,
    query: String,
    onQueryChange: (String) -> Unit,
    onConnect: () -> Unit,
    onClone: (RexGitGithubRepository) -> Unit,
    onOpen: (RexGitRepository) -> Unit
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

    val repos = remember(state.githubRepositories, query) {
        state.githubRepositories.filter {
            it.fullName.contains(query.trim(), ignoreCase = true)
        }
    }

    Column(Modifier.fillMaxSize()) {

        SearchField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = "Search GitHub repositories",
            modifier = Modifier.padding(start = 8.dp, end = 16.dp, top = 8.dp)
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
// Repository (landscape)
// ============================================================

@Composable
private fun RepositoryScreenLandscape(
    state: RexGitUiState,
    host: RexGitScreenHost,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpen: (RexGitFile) -> Unit,
    onNavigate: (String?) -> Unit,
    onPull: () -> Unit
) {
    val repo = state.selectedRepository ?: return

    var showChanges by host.showChanges

    val changes = state.gitStatus.changes
    val busy = state.isPushing || state.isPulling || state.isRepoLoading

    Row(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .landscapeSideInsets()
    ) {

        // ----- left pane: info + Pull / Push -----
        Column(
            Modifier
                .width(LandscapeRepoPaneWidth)
                .fillMaxHeight()
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                RepositoryHeader(
                    repo = repo,
                    busy = busy,
                    onBack = onBack,
                    onRefresh = onRefresh,
                    compact = true
                )

                ProgressSlot(busy)

                Column(
                    Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RepositoryStatusPills(state = state, repo = repo, changes = changes)
                    }

                    if (repo.isGitRepository) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RepositoryToolPills(
                                state = state,
                                repo = repo,
                                onGitDev = { host.gitDevDialog.value = true },
                                onAccount = { host.githubDialog.value = true }
                            )
                        }
                    }
                }
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
                gitDevReady = state.gitDev.isReady,
                onPull = onPull,
                onPush = { host.requestPush(state) },
                onUnlockPush = { host.gitDevDialog.value = true }
            )
        }

        // ----- right pane: files -----
        RepositoryFiles(
            state = state,
            repo = repo,
            onOpen = onOpen,
            onMore = { host.actionTarget.value = it },
            onNavigate = onNavigate,
            onNew = { host.createMenu.value = true },
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(top = 8.dp)
        )
    }
}

// ============================================================
// Editor (landscape): text on the left, tools on the right
// ============================================================

@Composable
private fun EditorScreenLandscape(
    state: RexGitUiState,
    editor: RexGitEditorState,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val actions = rememberEditorActions(editor)

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .landscapeSideInsets()
            .imePadding()
    ) {
        EditorHeader(
            state = state,
            editor = editor,
            onBack = onBack,
            onSave = onSave,
            compact = true
        )

        ProgressSlot(state.isSaving)

        Row(
            Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {

            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 12.dp)
            ) {
                EditorTextArea(
                    state = state,
                    editor = editor,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )

                EditorStatusText(
                    editor = editor,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Column(
                Modifier
                    .width(LandscapeEditorPanelWidth)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Tools",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant
                )

                actions.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { action ->
                            EditorToolButton(action, Modifier.weight(1f))
                        }

                        if (pair.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }

                Text(
                    "Text size",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant
                )

                EditorFontControls(
                    editor = editor,
                    modifier = Modifier.fillMaxWidth(),
                    stacked = true
                )

                Text(
                    "Symbols",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant
                )

                EditorKeys.chunked(4).forEach { rowKeys ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        rowKeys.forEach { (label, snippet) ->
                            EditorKeyChip(
                                label = label,
                                onClick = { editor.insert(snippet) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        repeat(4 - rowKeys.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
