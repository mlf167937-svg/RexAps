package com.rexaps.rexgit

import java.io.File
import kotlinx.coroutines.flow.update

internal fun RexGitViewModel.__splitImpl_refresh() {
        launchIO {
            _state.update {
                it.copy(isLoading = true)
            }

            try {
                val repos = repositoryManager.scan()
                val (path, fallback) = storageInfo()

                _state.update {
                    it.copy(
                        repositories = repos,
                        storagePath = path,
                        storageFallback = fallback
                    )
                }

                try {
                    val auth = externalGithubAuth(reload = true)

                    if (auth != null) {
                        _state.update {
                            it.copy(
                                githubUsername = auth.username,
                                isGithubConnected = true
                            )
                        }

                        fetchGithubRepositories(auth)
                    } else {
                        _state.update {
                            it.copy(
                                githubUsername = null,
                                isGithubConnected = false,
                                accounts = emptyList(),
                                githubRepositories = emptyList()
                            )
                        }
                    }
                } catch (t: Throwable) {
                    fail(
                        "Could not connect to GitHub:\n" +
                            t.readableMessage("Invalid or expired token")
                    )
                }
            } finally {
                _state.update {
                    it.copy(isLoading = false)
                }
            }
        }
    }

internal fun RexGitViewModel.__splitImpl_saveEditor() {
        val path = _state.value.editorPath
            ?: return

        if (_state.value.isSaving) return

        val content = editorBuffer

        launchIO {
            _state.update {
                it.copy(isSaving = true)
            }

            try {
                File(path).writeText(content)

                val repo = _state.value.selectedRepository

                val status =
                    if (repo != null && repo.isGitRepository) {
                        repositoryManager.status(repo)
                    } else {
                        null
                    }

                _state.update {
                    it.copy(
                        editorDirty = editorBuffer != content,
                        gitStatus = status ?: it.gitStatus,
                        readmeContent = if (File(path).name.equals("README.md", true)) content else it.readmeContent,
                        message = "File saved"
                    )
                }
            } finally {
                _state.update {
                    it.copy(isSaving = false)
                }
            }
        }
    }

internal fun RexGitViewModel.__splitImpl_deleteEntry(file: RexGitFile) {
        val repo = _state.value.selectedRepository
            ?: return

        launchIO {
            _state.update {
                it.copy(
                    isRepoLoading = true,
                    error = null
                )
            }

            try {
                val result = repositoryManager.delete(
                    repo,
                    file.path
                )

                reloadRepository(repo)

                if (result.success) {
                    _state.update {
                        it.copy(message = result.message)
                    }
                } else {
                    fail(result.message)
                }
            } finally {
                _state.update {
                    it.copy(isRepoLoading = false)
                }
            }
        }
    }

internal fun RexGitViewModel.__splitImpl_refreshStatus() {
        val repo = _state.value.selectedRepository
            ?: return

        launchIO {
            _state.update {
                it.copy(isRepoLoading = true)
            }

            try {
                reloadRepository(repo)
            } finally {
                _state.update {
                    it.copy(isRepoLoading = false)
                }
            }
        }
    }
