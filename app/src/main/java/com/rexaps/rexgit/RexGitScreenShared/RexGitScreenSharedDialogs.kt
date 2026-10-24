@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.rexaps.rexgit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*

/**
 * Everything that is the same in portrait and landscape: back button,
 * snackbar messages and all dialogs / sheets.
 */
@Composable
internal fun __splitImpl_RexGitScreenController(
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
