@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.rexaps.rexgit

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable

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

/** 0 = home, 1 = repository, 2 = editor. */
internal fun screenIndex(state: RexGitUiState): Int {
    return when {
        state.editorPath != null -> 2
        state.selectedRepository != null -> 1
        else -> 0
    }
}
