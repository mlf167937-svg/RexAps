package com.rexaps.rexgit

import kotlinx.coroutines.flow.update
import java.io.File

// ---------- pull / push ----------

internal fun RexGitViewModel.pullImpl() {
    val repo = _state.value.selectedRepository
        ?: return

    val current = _state.value

    if (
        !repo.isGitRepository ||
        current.isPulling ||
        current.isPushing
    ) {
        return
    }

    launchIO {
        _state.update {
            it.copy(
                isPulling = true,
                error = null,
                message = null
            )
        }

        try {
            val auth = externalGithubAuth()
                ?: throw missingAuthError()

            val result = repositoryManager.pull(
                repo,
                auth.username,
                auth.token
            )

            reloadRepository(repo)

            if (result.success) {
                _state.update {
                    it.copy(message = result.message)
                }
            } else {
                _state.update {
                    it.copy(error = result.message)
                }
            }
        } finally {
            _state.update {
                it.copy(isPulling = false)
            }
        }
    }
}

internal fun RexGitViewModel.pushImpl(message: String,
    bumpVersion: Boolean,
    commitType: RexGitCommitType = RexGitCommitType.FIX,
    commitFolder: String = "",
    useGitDev: Boolean = false) {
    val repo = _state.value.selectedRepository
        ?: return

    val current = _state.value

    if (
        !repo.isGitRepository ||
        current.isPushing ||
        current.isPulling
    ) {
        return
    }

    launchIO {
        _state.update {
            it.copy(
                isPushing = true,
                pushStage = "Checking Files...",
                pushDetail = "Preparing RexPush",
                pushLog = emptyList(),
                pushFinished = false,
                pushSuccess = false,
                pushError = null,
                pushCommitMessage = null,
                pushVersion = null,
                error = null,
                message = null
            )
        }

        try {
            val auth = externalGithubAuth()
                ?: throw missingAuthError()

            val result = pushManager.push(
                repository = repo,
                commitMessage = message,
                bumpVersion = bumpVersion,
                commitType = commitType,
                commitFolder = commitFolder,
                gitDev = current.gitDev,
                useGitDev = useGitDev,
                username = auth.username,
                token = auth.token,
                onProgress = { stage, detail, logLine ->
                    _state.update { cur ->
                        cur.copy(
                            pushStage = stage,
                            pushDetail = detail,
                            pushLog = if (logLine.isNullOrBlank()) {
                                cur.pushLog
                            } else {
                                (cur.pushLog + logLine).takeLast(80)
                            }
                        )
                    }
                }
            )

            _state.update {
                it.copy(
                    pushStage = if (result.success) "RexPush" else "Push failed",
                    pushDetail = if (result.success) "Successfully pushed" else result.message,
                    pushFinished = true,
                    pushSuccess = result.success,
                    pushError = result.message.takeIf { !result.success },
                    pushVersion = result.version,
                    pushCommitMessage = if (result.success && message.isNotBlank()) {
                        if (useGitDev) RexGitCommitMeta.buildMessage(
                            version = result.version,
                            type = commitType,
                            folder = commitFolder,
                            description = message
                        ) else RexGitCommitMeta.buildStandardMessage(commitType, message)
                    } else null
                )
            }

            if (result.success) {
                _state.update {
                    it.copy(pushStage = "Refreshing", pushDetail = "Updating repository status")
                }
                reloadRepository(repo)
                _state.update {
                    it.copy(
                        pushStage = "RexPush",
                        pushDetail = "Successfully pushed",
                        pushFinished = true,
                        pushSuccess = true
                    )
                }
            }
        } catch (t: Throwable) {
            _state.update {
                it.copy(
                    pushStage = "Push failed",
                    pushDetail = t.readableMessage("Push failed"),
                    pushFinished = true,
                    pushSuccess = false,
                    pushError = t.readableMessage("Push failed")
                )
            }
        } finally {
            _state.update {
                it.copy(isPushing = false)
            }
        }
    }
}

// ---------- GitDev ----------

/**
 * Saves GitDev mode for the selected repository. Turning it ON needs a
 * package path that really contains folders (e.g. com/rexaps).
 */
internal fun RexGitViewModel.saveGitDevImpl(config: RexGitGitDevConfig) {
    val repo = _state.value.selectedRepository
        ?: return

    val clean = RexGitGitDev.normalize(config)

    if (clean.enabled) {
        if (clean.packagePath.isBlank()) {
            fail("Fill in the package path first, for example: com/rexaps")
            return
        }

        val found = RexGitGitDev.detectFolders(File(repo.path), clean)

        if (found.isEmpty()) {
            fail(
                "No folders found under ${clean.packagePath}.\n" +
                    "Check the package path (and module) in the GitDev guide."
            )
            return
        }
    }

    gitDevStore.save(repo.path, clean)

    _state.update {
        it.copy(
            gitDev = clean,
            message = if (clean.enabled) "GitDev metadata is ready for this repository."
            else "GitDev is off. Standard Push remains available."
        )
    }
}

internal fun RexGitViewModel.clearPushStateImpl() {
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
