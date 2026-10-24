@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.rexaps.rexgit

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
internal fun __splitImpl_rememberRexGitScreenHost(): RexGitScreenHost {
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
