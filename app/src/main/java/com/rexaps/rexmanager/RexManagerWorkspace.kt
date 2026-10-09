package com.rexaps.rexmanager

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rexaps.rexmanager.settings.RexManagerSettingsScreen
import com.rexaps.rexmanager.ui.RexManagerBottomBar
import com.rexaps.rexmanager.ui.RexManagerBreadcrumb
import com.rexaps.rexmanager.ui.RexManagerFileGrid
import com.rexaps.rexmanager.ui.RexManagerFileList
import com.rexaps.rexmanager.ui.RexManagerMessageState
import com.rexaps.rexmanager.ui.RexManagerProgressDialog
import com.rexaps.rexmanager.ui.RexManagerSearchBar
import com.rexaps.rexmanager.ui.RexManagerSelectionBar
import com.rexaps.rexmanager.ui.RexManagerStorageHeader
import com.rexaps.rexmanager.ui.RexManagerTopBar
import com.rexaps.rexmanager.ui.dialogs.CollisionDialog
import com.rexaps.rexmanager.ui.dialogs.CompressDialog
import com.rexaps.rexmanager.ui.dialogs.DeleteDialog
import com.rexaps.rexmanager.ui.dialogs.ExtractDialog
import com.rexaps.rexmanager.ui.dialogs.HideFileDialog
import com.rexaps.rexmanager.ui.dialogs.NewFolderDialog
import com.rexaps.rexmanager.ui.dialogs.PropertiesDialog
import com.rexaps.rexmanager.ui.dialogs.RenameDialog
import com.rexaps.rexmanager.utils.RexManagerIntents
import kotlinx.coroutines.launch
import java.io.File

/**
 * Entry point. Drop this into the existing RexAps navigation graph / feature registry:
 *
 * RexManagerScreen(
 *     onExit = { navController.popBackStack() }
 * )
 */
@Composable
internal fun RexManagerWorkspace(
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RexManagerViewModel = viewModel(
        factory = RexManagerViewModel.factory(LocalContext.current)
    )
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    /*
     * ============================================================
     * STORAGE PERMISSION
     * ============================================================
     *
     * Android 11+:
     *   MANAGE_EXTERNAL_STORAGE
     *
     * Android 10 and below:
     *   READ_EXTERNAL_STORAGE
     *   WRITE_EXTERNAL_STORAGE
     */

    fun hasAccess(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }

    /*
     * Runtime permission launcher for Android 10 and below.
     */
    val legacyLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.onRefresh()
    }

    /*
     * Open the correct permission screen.
     */
    fun requestAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } else {
            legacyLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    /*
     * Automatically refresh when returning from Android Settings.
     *
     * This is important because the user may enable
     * "Allow access to manage all files" and then return
     * to RexManager.
     */
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onRefresh()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    /*
     * Request storage access when the screen is first opened.
     */
    LaunchedEffect(Unit) {
        if (!hasAccess()) {
            requestAccess()
        }
    }

    /*
     * ViewModel events.
     */
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is RexManagerEvent.Message -> launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(event.text)
                }

                is RexManagerEvent.Share ->
                    RexManagerIntents
                        .share(context, event.paths)
                        ?.let { error ->
                            launch {
                                snackbarHostState.showSnackbar(error)
                            }
                        }

                is RexManagerEvent.OpenFile ->
                    RexManagerIntents
                        .open(
                            context,
                            event.path,
                            event.mimeType
                        )
                        ?.let { error ->
                            launch {
                                snackbarHostState.showSnackbar(error)
                            }
                        }
            }
        }
    }

    RexManagerContent(
        state = state,
        actions = viewModel,
        snackbarHostState = snackbarHostState,
        onExit = onExit,

        /*
         * Request access button / retry button.
         */
        onRequestAccess = {
            if (!hasAccess()) {
                requestAccess()
            } else {
                viewModel.onRefresh()
            }
        },

        modifier = modifier
    )
}

/**
 * Stateless screen content:
 * state in, callbacks out.
 */
@Composable
fun RexManagerContent(
    state: RexManagerState,
    actions: RexManagerActions,
    snackbarHostState: SnackbarHostState,
    onExit: () -> Unit,
    onRequestAccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        if (!actions.onBackPressed()) {
            onExit()
        }
    }

    /*
     * Settings screen.
     */
    if (state.showSettings) {
        RexManagerSettingsScreen(
            settings = state.settings,
            onChange = actions::onSettingsChange,
            onBack = actions::onCloseSettings,
            modifier = modifier
        )
        return
    }

    /*
     * Selected files.
     */
    val selectedFiles = remember(
        state.files,
        state.selectedPaths
    ) {
        state.files.filter {
            it.path in state.selectedPaths
        }
    }

    /*
     * Main screen.
     */
    Scaffold(
        modifier = modifier,

        /*
         * Top bar.
         */
        topBar = {
            if (state.isSelectionMode) {
                RexManagerSelectionBar(
                    selectedFiles = selectedFiles,
                    totalCount = state.files.size,
                    actions = actions
                )
            } else {
                RexManagerTopBar(
                    state = state,
                    actions = actions
                ) {
                    if (!actions.onBackPressed()) {
                        onExit()
                    }
                }
            }
        },

        /*
         * Clipboard bottom bar.
         */
        bottomBar = {
            state.clipboard?.let { clip ->
                RexManagerBottomBar(
                    clipboard = clip,
                    onPaste = actions::onPaste,
                    onCancel = actions::onCancelClipboard
                )
            }
        },

        /*
         * Snackbar.
         */
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            /*
             * Storage header.
             */
            RexManagerStorageHeader(
                root = state.currentRoot,
                roots = state.roots,
                currentPath = state.currentPath,
                storageInfo = state.storageInfo,
                onSelectRoot = actions::onSelectRoot
            )

            /*
             * Breadcrumb navigation.
             */
            RexManagerBreadcrumb(
                root = state.currentRoot,
                currentPath = state.currentPath,
                onSegmentClick = actions::onBreadcrumbClick
            )

            /*
             * Search.
             */
            RexManagerSearchBar(
                query = state.searchQuery,
                isSearching = state.isSearchRunning,
                onQueryChange = actions::onSearchQueryChange,
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 8.dp
                )
            )

            /*
             * Search truncation information.
             */
            if (
                state.isSearchActive &&
                state.searchTruncated
            ) {
                Text(
                    text = "Showing the first results only. Refine your search to narrow it down.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 4.dp
                    )
                )
            }

            /*
             * File content.
             */
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {

                when {

                    /*
                     * Initial loading.
                     */
                    state.isLoading &&
                        state.files.isEmpty() -> {

                        CircularProgressIndicator(
                            modifier = Modifier.align(
                                Alignment.Center
                            )
                        )
                    }

                    /*
                     * Error.
                     */
                    state.errorMessage != null -> {

                        RexManagerMessageState(
                            icon = Icons.Filled.ErrorOutline,
                            title = "Can't open this folder",
                            description = state.errorMessage,
                            actionLabel = "Retry",
                            onAction = onRequestAccess
                        )
                    }

                    /*
                     * Search with no results.
                     */
                    state.files.isEmpty() &&
                        state.isSearchActive -> {

                        if (!state.isSearchRunning) {
                            RexManagerMessageState(
                                icon = Icons.Filled.SearchOff,
                                title = "No results",
                                description =
                                    "Nothing matches \"${state.searchQuery.trim()}\" in this folder."
                            )
                        }
                    }

                    /*
                     * Empty folder.
                     */
                    state.files.isEmpty() -> {

                        RexManagerMessageState(
                            icon = Icons.Filled.FolderOpen,
                            title = "This folder is empty",
                            description =
                                if (state.showHiddenFiles) {
                                    null
                                } else {
                                    "Hidden files are not shown. You can turn them on in the menu."
                                },
                            actionLabel = "New folder",
                            onAction = {
                                actions.onRequestCreate(true)
                            }
                        )
                    }

                    /*
                     * List view.
                     */
                    state.viewMode == ViewMode.LIST -> {

                        RexManagerFileList(
                            files = state.files,
                            selectedPaths = state.selectedPaths,
                            showExtensions =
                                state.settings.showFileExtensions,
                            onClick = actions::onFileClick,
                            onLongClick = actions::onFileLongClick
                        )
                    }

                    /*
                     * Grid view.
                     */
                    else -> {

                        RexManagerFileGrid(
                            files = state.files,
                            selectedPaths = state.selectedPaths,
                            showExtensions =
                                state.settings.showFileExtensions,
                            onClick = actions::onFileClick,
                            onLongClick = actions::onFileLongClick
                        )
                    }
                }

                /*
                 * Loading indicator when files are already visible.
                 */
                if (
                    state.isLoading &&
                    state.files.isNotEmpty()
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                    )
                }
            }
        }
    }

    /*
     * Dialog host.
     */
    RexManagerDialogHost(
        state = state,
        actions = actions
    )

    /*
     * Operation progress.
     */
    state.operationProgress?.let { progress ->
        RexManagerProgressDialog(
            progress,
            actions::onCancelOperation
        )
    }
}

/**
 * Dialog host.
 */
@Composable
private fun RexManagerDialogHost(
    state: RexManagerState,
    actions: RexManagerActions
) {
    /*
     * Existing sibling names.
     *
     * During search we don't use search results for collision
     * checking because they may not represent all siblings.
     */
    val siblingNames = remember(
        state.files,
        state.isSearchActive
    ) {
        if (state.isSearchActive) {
            emptySet<String>()
        } else {
            state.files.mapTo(HashSet()) {
                it.name
            }
        }
    }

    when (val dialog = state.dialog) {

        /*
         * No dialog.
         */
        null -> Unit

        /*
         * Rename.
         */
        is RexDialog.Rename -> {
            RenameDialog(
                file = dialog.file,
                existingNames = siblingNames,
                onConfirm = {
                    actions.onConfirmRename(
                        dialog.file,
                        it
                    )
                },
                onDismiss = actions::onDismissDialog
            )
        }

        /*
         * Create folder / file.
         */
        is RexDialog.Create -> {
            NewFolderDialog(
                isFolder = dialog.isFolder,
                existingNames = siblingNames,
                onConfirm = {
                    actions.onConfirmCreate(
                        dialog.isFolder,
                        it
                    )
                },
                onDismiss = actions::onDismissDialog
            )
        }

        /*
         * Delete.
         */
        is RexDialog.Delete -> {
            DeleteDialog(
                files = dialog.files,
                onConfirm = {
                    actions.onConfirmDelete(
                        dialog.files
                    )
                },
                onDismiss = actions::onDismissDialog
            )
        }

        /*
         * Compress.
         */
        is RexDialog.Compress -> {
            CompressDialog(
                files = dialog.files,
                destinationPath = state.currentPath,
                existingNames = siblingNames,
                onConfirm = { name, format ->
                    actions.onConfirmCompress(
                        dialog.files,
                        name,
                        format
                    )
                },
                onDismiss = actions::onDismissDialog
            )
        }

        /*
         * Extract.
         */
        is RexDialog.Extract -> {
            ExtractDialog(
                archive = dialog.archive,
                parentPath = File(
                    dialog.archive.path
                ).parent.orEmpty(),
                onConfirm = { folder, policy ->
                    actions.onConfirmExtract(
                        dialog.archive,
                        folder,
                        policy
                    )
                },
                onDismiss = actions::onDismissDialog
            )
        }

        /*
         * Properties.
         */
        is RexDialog.Properties -> {
            PropertiesDialog(
                metadata = dialog.metadata,
                totalSize = dialog.totalSize,
                onDismiss = actions::onDismissDialog
            )
        }

        /*
         * Hide / unhide.
         */
        is RexDialog.Hide -> {
            HideFileDialog(
                files = dialog.files,
                hide = dialog.hide,
                conflicts = dialog.conflicts,
                onConfirm = {
                    actions.onConfirmHide(
                        dialog.files,
                        dialog.hide
                    )
                },
                onDismiss = actions::onDismissDialog
            )
        }

        /*
         * File collision.
         */
        is RexDialog.Collision -> {
            CollisionDialog(
                conflicts = dialog.conflicts,
                isMove = dialog.transfer.isMove,
                onResolve = {
                    actions.onResolveCollision(
                        dialog.transfer,
                        it
                    )
                },
                onDismiss = actions::onDismissDialog
            )
        }
    }
}
