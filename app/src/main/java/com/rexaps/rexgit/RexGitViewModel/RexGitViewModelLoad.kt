package com.rexaps.rexgit

import kotlinx.coroutines.flow.update

internal fun RexGitViewModel.__split_loadInitial_6() {
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

fun RexGitViewModel.__split_refresh_7() {
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

fun RexGitViewModel.__split_refreshStatus_11() {
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
