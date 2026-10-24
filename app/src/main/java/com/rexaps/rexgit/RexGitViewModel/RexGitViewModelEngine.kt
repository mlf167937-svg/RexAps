package com.rexaps.rexgit

import kotlinx.coroutines.flow.update

internal fun RexGitViewModel.__splitImpl_clearPushState() {
        _state.update {
            it.copy(
                pushStage = "Checking Files...",
                pushDetail = "",
                pushLog = emptyList(),
                pushFinished = false,
                pushSuccess = false,
                pushError = null,
                pushCommitMessage = null,
                pushVersion = null
            )
        }
    }
