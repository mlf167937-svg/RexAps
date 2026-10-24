package com.rexaps.rexgit

import java.io.File
import kotlinx.coroutines.flow.update

internal fun RexGitViewModel.__splitImpl_push(
        message: String,
        bumpVersion: Boolean,
        commitType: RexGitCommitType = RexGitCommitType.FIX,
        commitFolder: String = "",
        useGitDev: Boolean = false
    ) {
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

internal fun RexGitViewModel.__splitImpl_pull() {
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

internal fun RexGitViewModel.__splitImpl_loadInitial() {
        launchIO {
            try {
                RexGitStorage.ensureDirectories()

                val auth = try {
                    externalGithubAuth()
                } catch (_: Throwable) {
                    null
                }

                val repos = repositoryManager.scan()
                val (path, fallback) = storageInfo()

                _state.update {
                    it.copy(
                        repositories = repos,
                        githubUsername = auth?.username,
                        isGithubConnected = auth != null,
                        storagePath = path,
                        storageFallback = fallback
                    )
                }

                if (auth != null) {
                    fetchGithubRepositories(auth)
                }
            } finally {
                _state.update {
                    it.copy(isLoading = false)
                }
            }
        }
    }

internal fun RexGitViewModel.__splitImpl_readReadme(repositoryPath: String): String {
        return try {
            val root = File(repositoryPath)
            val readme = root.listFiles()?.firstOrNull { it.isFile && it.name.equals("README.md", true) }
                ?: return ""
            if (readme.length() > RexGitViewModel.MAX_EDITOR_BYTES) return "README is too large to preview in RexGit. Open the file from Code to inspect it."
            readme.readText(Charsets.UTF_8)
        } catch (_: Throwable) { "" }
    }

internal fun RexGitViewModel.__splitImpl_openParent() {
        val repo = _state.value.selectedRepository
            ?: return

        val current = _state.value.currentDirectory
            ?: return

        val parent = File(current).parentFile

        if (parent == null || parent.absolutePath == repo.path) {
            openRoot()
        } else {
            openDirectory(parent.absolutePath)
        }
    }

internal fun RexGitViewModel.__splitImpl_looksBinary(file: File): Boolean {
        return file.inputStream().use { stream ->
            val buffer = ByteArray(8000)
            val read = stream.read(buffer)

            if (read <= 0) {
                false
            } else {
                (0 until read).any {
                    buffer[it] == 0.toByte()
                }
            }
        }
    }
