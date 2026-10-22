
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


internal enum class CreateKind { FILE, FOLDER }

/**
 * UI state that must survive switching between portrait and landscape.
 * It lives in RexGitScreen (above the switch), so both layouts share it.
 */
@Stable
internal class RexGitScreenHost(
    val snackbar: SnackbarHostState,

    // dialogs
    val githubDialog: MutableState<Boolean>,
    val pushDialog: MutableState<Boolean>,
    val discardDialog: MutableState<Boolean>,
    val gitDevDialog: MutableState<Boolean>,

    // push form
    val commitMessage: MutableState<String>,
    val bumpVersion: MutableState<Boolean>,
    val commitType: MutableState<String>,
    val commitFolder: MutableState<String>,
    val useGitDevOnPush: MutableState<Boolean>,

    // home
    val homeTab: MutableState<Int>,
    val localQuery: MutableState<String>,
    val githubQuery: MutableState<String>,

    // repository
    val showChanges: MutableState<Boolean>,
    val repoTab: MutableState<Int>,
    val createMenu: MutableState<Boolean>,
    val createKind: MutableState<CreateKind?>,
    val actionTarget: MutableState<RexGitFile?>,
    val deleteTarget: MutableState<RexGitFile?>
) {
    /** Normal Push is always available; GitDev is an optional metadata mode. */
    fun requestPush(state: RexGitUiState) {
        useGitDevOnPush.value = false
        pushDialog.value = true
    }
}

@Composable
internal fun rememberRexGitScreenHost(): RexGitScreenHost {
    val snackbar = remember { SnackbarHostState() }

    val githubDialog = rememberSaveable { mutableStateOf(false) }
    val pushDialog = rememberSaveable { mutableStateOf(false) }
    val discardDialog = rememberSaveable { mutableStateOf(false) }
    val gitDevDialog = rememberSaveable { mutableStateOf(false) }

    val commitMessage = rememberSaveable { mutableStateOf("") }
    val bumpVersion = rememberSaveable { mutableStateOf(true) }
    val commitType = rememberSaveable { mutableStateOf(RexGitCommitType.FIX.name) }
    val commitFolder = rememberSaveable { mutableStateOf("") }
    val useGitDevOnPush = rememberSaveable { mutableStateOf(false) }

    val homeTab = rememberSaveable { mutableStateOf(0) }
    val localQuery = rememberSaveable { mutableStateOf("") }
    val githubQuery = rememberSaveable { mutableStateOf("") }

    val showChanges = rememberSaveable { mutableStateOf(false) }
    val repoTab = rememberSaveable { mutableStateOf(0) }
    val createMenu = remember { mutableStateOf(false) }
    val createKind = remember { mutableStateOf<CreateKind?>(null) }
    val actionTarget = remember { mutableStateOf<RexGitFile?>(null) }
    val deleteTarget = remember { mutableStateOf<RexGitFile?>(null) }

    return remember {
        RexGitScreenHost(
            snackbar = snackbar,
            githubDialog = githubDialog,
            pushDialog = pushDialog,
            discardDialog = discardDialog,
            gitDevDialog = gitDevDialog,
            commitMessage = commitMessage,
            bumpVersion = bumpVersion,
            commitType = commitType,
            commitFolder = commitFolder,
            useGitDevOnPush = useGitDevOnPush,
            homeTab = homeTab,
            localQuery = localQuery,
            githubQuery = githubQuery,
            showChanges = showChanges,
            repoTab = repoTab,
            createMenu = createMenu,
            createKind = createKind,
            actionTarget = actionTarget,
            deleteTarget = deleteTarget
        )
    }
}

/** 0 = home, 1 = repository, 2 = editor. */
internal fun screenIndex(state: RexGitUiState): Int {
    return when {
        state.editorPath != null -> 2
        state.selectedRepository != null -> 1
        else -> 0
    }
}

/**
 * Everything that is the same in portrait and landscape: back button,
 * snackbar messages and all dialogs / sheets.
 */
@Composable
internal fun RexGitScreenController(
    state: RexGitUiState,
    host: RexGitScreenHost,
    editor: RexGitEditorState?,
    viewModel: RexGitViewModel
) {
    val gitDev = state.gitDev

    val detectedCommitFolders = remember(
        state.selectedRepository?.path,
        state.gitStatus.changes,
        gitDev
    ) {
        RexGitCommitMeta.detectFolders(
            state.selectedRepository,
            state.gitStatus.changes,
            gitDev
        )
    }

    LaunchedEffect(host.pushDialog.value) {
        if (host.pushDialog.value) {
            val suggested = RexGitCommitMeta.inferFolder(
                state.selectedRepository,
                state.gitStatus.changes,
                gitDev
            )
            val suggestedType = RexGitCommitMeta.inferType(state.gitStatus.changes)
            host.commitFolder.value = suggested ?: detectedCommitFolders.firstOrNull().orEmpty()
            host.commitType.value = suggestedType.name
            host.bumpVersion.value = suggestedType == RexGitCommitType.UPGRADE
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            host.snackbar.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    BackHandler(
        enabled = state.editorPath != null || state.selectedRepository != null
    ) {
        when {
            state.editorPath != null ->
                if (state.editorDirty) host.discardDialog.value = true
                else viewModel.closeEditor()

            state.currentDirectory != null -> viewModel.openParent()
            else -> viewModel.backToRepositories()
        }
    }

    LaunchedEffect(state.selectedRepository?.path) {
        host.repoTab.value = 0
    }

    val onRepositoryScreen = state.selectedRepository != null && state.editorPath == null

    // ----- repository sheets & dialogs -----

    if (onRepositoryScreen) {
        val repo = state.selectedRepository
        val label = locationLabel(state)

        if (host.createMenu.value) {
            CreateSheet(
                location = label,
                onDismiss = { host.createMenu.value = false },
                onPick = {
                    host.createMenu.value = false
                    host.createKind.value = it
                }
            )
        }

        host.createKind.value?.let { kind ->
            NameDialog(
                kind = kind,
                location = label,
                existing = remember(state.files) { state.files.map { it.name }.toSet() },
                onDismiss = { host.createKind.value = null },
                onConfirm = { name ->
                    host.createKind.value = null
                    if (kind == CreateKind.FILE) viewModel.createFile(name)
                    else viewModel.createFolder(name)
                }
            )
        }

        host.actionTarget.value?.let { file ->
            ItemActionsSheet(
                file = file,
                onDismiss = { host.actionTarget.value = null },
                onOpen = {
                    host.actionTarget.value = null
                    viewModel.openFile(file)
                },
                onDelete = {
                    host.actionTarget.value = null
                    host.deleteTarget.value = file
                }
            )
        }

        host.deleteTarget.value?.let { file ->
            DeleteDialog(
                file = file,
                isGit = repo?.isGitRepository == true,
                onDismiss = { host.deleteTarget.value = null },
                onConfirm = {
                    host.deleteTarget.value = null
                    viewModel.deleteEntry(file)
                }
            )
        }

        if (host.gitDevDialog.value && repo != null) {
            GitDevDialog(
                repo = repo,
                config = gitDev,
                onSave = viewModel::saveGitDev,
                onDismiss = { host.gitDevDialog.value = false }
            )
        }
    }

    // ----- editor dialogs -----

    if (editor != null && state.editorPath != null) {
        EditorDialogs(editor)
    }

    // ----- GitHub accounts -----

    if (host.githubDialog.value) {
        GithubDialog(
            state = state,
            onDismiss = { host.githubDialog.value = false },
            onLoadEnv = { viewModel.connectGithub("") },
            onAdd = viewModel::addAccount,
            onSwitch = viewModel::switchAccount,
            onRemove = viewModel::removeAccount,
            onDisconnect = {
                viewModel.disconnectGithub()
                host.githubDialog.value = false
            }
        )
    }

    // ----- push -----

    if (host.pushDialog.value) {
        if (state.isPushing || state.pushFinished) {
            RexPushProgressDialog(
                state = state,
                onDismiss = {
                    if (!state.isPushing) {
                        viewModel.clearPushState()
                        host.pushDialog.value = false
                    }
                }
            )
        } else {
            PushDialog(
                message = host.commitMessage.value,
                onMessageChange = { host.commitMessage.value = it },
                type = RexGitCommitType.valueOf(host.commitType.value),
                onTypeChange = {
                    host.commitType.value = it.name
                    host.bumpVersion.value = it == RexGitCommitType.UPGRADE
                },
                folder = host.commitFolder.value,
                folders = detectedCommitFolders,
                packagePath = gitDev.packagePath,
                useGitDev = host.useGitDevOnPush.value,
                gitDevAvailable = gitDev.isReady,
                onUseGitDevChange = { host.useGitDevOnPush.value = it },
                onFolderChange = { host.commitFolder.value = it },
                bumpVersion = host.bumpVersion.value,
                onBumpChange = { host.bumpVersion.value = it },
                changeCount = state.gitStatus.changes.size,
                onDismiss = { host.pushDialog.value = false },
                onPush = {
                    viewModel.push(
                        message = host.commitMessage.value.trim(),
                        bumpVersion = host.bumpVersion.value,
                        commitType = RexGitCommitType.valueOf(host.commitType.value),
                        commitFolder = host.commitFolder.value,
                        useGitDev = host.useGitDevOnPush.value
                    )
                }
            )
        }
    }

    if (host.discardDialog.value) {
        AlertDialog(
            onDismissRequest = { host.discardDialog.value = false },
            title = { Text("Discard changes?") },
            text = { Text("This file has unsaved changes that will be lost.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        host.discardDialog.value = false
                        viewModel.closeEditor()
                    }
                ) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { host.discardDialog.value = false }) { Text("Keep editing") }
            }
        )
    }

    state.error?.let { error ->
        ErrorDialog(text = error, onDismiss = viewModel::dismissError)
    }
}
