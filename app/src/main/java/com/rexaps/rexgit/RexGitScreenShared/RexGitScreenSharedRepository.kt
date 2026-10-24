
@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)
package com.rexaps.rexgit

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable

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
