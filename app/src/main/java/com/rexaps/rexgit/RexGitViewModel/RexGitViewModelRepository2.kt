package com.rexaps.rexgit

import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.update

/**
     * Loads the account(s) from token.env. The parameter is ignored; it stays
     * so older callers keep compiling. To log in with a typed username and
     * token use [addAccount].
     */
    internal fun RexGitViewModel.__splitImpl_connectGithub(
        @Suppress("UNUSED_PARAMETER")
        tokenFromUi: String
    ) {
        launchIO {
            _state.update {
                it.copy(
                    isGithubLoading = true,
                    error = null,
                    message = null
                )
            }

            try {
                val auth = externalGithubAuth(reload = true)
                    ?: throw missingAuthError()

                _state.update {
                    it.copy(
                        githubUsername = auth.username,
                        isGithubConnected = true,
                        message = "Connected as @${auth.username}"
                    )
                }

                fetchGithubRepositories(auth)
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                fail(
                    "Could not connect to GitHub:\n" +
                        t.readableMessage(
                            "Invalid or expired token"
                        )
                )
            } finally {
                _state.update {
                    it.copy(isGithubLoading = false)
                }
            }
        }
    }

internal fun RexGitViewModel.__splitImpl_reloadRepository(repo: RexGitRepository) {
        val dir = _state.value.currentDirectory
            ?.takeIf { File(it).isDirectory }

        val status =
            if (repo.isGitRepository) {
                repositoryManager.status(repo)
            } else {
                RexGitStatus()
            }

        val branch =
            if (repo.isGitRepository) {
                repositoryManager.currentBranch(repo)
            } else {
                repo.branch
            }

        val files = repositoryManager.files(repo, dir)

        _state.update { cur ->
            val selected = cur.selectedRepository

            if (selected == null || selected.path != repo.path) {
                cur
            } else {
                cur.copy(
                    gitStatus = status,
                    files = files,
                    readmeContent = readReadme(repo.path),
                    currentDirectory = dir,
                    selectedRepository = selected.copy(
                        branch = branch
                    )
                )
            }
        }
    }

internal fun RexGitViewModel.__splitImpl_backToRepositories() {
        editorBuffer = ""

        _state.update {
            it.copy(
                selectedRepository = null,
                files = emptyList(),
                currentDirectory = null,
                gitStatus = RexGitStatus(),
                gitDev = RexGitGitDevConfig(),
                readmeContent = "",
                workflows = emptyList(),
                workflowRuns = emptyList(),
                workflowArtifacts = emptyList(),
                releases = emptyList(),
                repositoryMetadata = null,
                repositoryTags = emptyList(),
                isRepoHubLoading = false,
                repoHubError = null,
                actionsError = null,
                releasesError = null,
                downloadError = null,
                downloadedBytes = 0L,
                downloadTotalBytes = null,
                downloadingArtifact = null,
                editorPath = null,
                editorContent = "",
                editorDirty = false
            )
        }
    }

/**
     * Saves GitDev mode for the selected repository. Turning it ON needs a
     * package path that really contains folders (e.g. com/rexaps).
     */
    internal fun RexGitViewModel.__splitImpl_saveGitDev(config: RexGitGitDevConfig) {
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

internal fun RexGitViewModel.__splitImpl_reloadGithub() {
        launchIO {
            try {
                val auth = externalGithubAuth(reload = true)

                if (auth == null) {
                    fail(missingAuthError().message ?: "No GitHub account connected.")
                    return@launchIO
                }

                _state.update {
                    it.copy(
                        githubUsername = auth.username,
                        isGithubConnected = true
                    )
                }

                fetchGithubRepositories(auth)
            } catch (t: Throwable) {
                if (t is CancellationException) {
                    throw t
                }

                fail(
                    "Could not load GitHub repositories:\n" +
                        t.readableMessage("Network error")
                )
            }
        }
    }

internal fun RexGitViewModel.__splitImpl_fetchGithubRepositories(
        auth: RexGitAuth
    ) {
        _state.update {
            it.copy(isGithubLoading = true)
        }

        try {
            val repos = github.listRepositories(
                auth.token
            )

            _state.update {
                it.copy(
                    githubRepositories = repos
                )
            }
        } catch (t: Throwable) {
            if (t is CancellationException) {
                throw t
            }

            fail(
                "Could not load GitHub repositories:\n" +
                    t.readableMessage("Network error")
            )
        } finally {
            _state.update {
                it.copy(isGithubLoading = false)
            }
        }
    }

internal fun RexGitViewModel.__splitImpl_disconnectGithub() {
        /*
         * IMPORTANT:
         *
         * We do NOT delete token.env.
         *
         * The token is managed externally.
         */
        accounts = emptyList()

        _state.update {
            it.copy(
                githubUsername = null,
                isGithubConnected = false,
                accounts = emptyList(),
                activeAccount = 0,
                githubRepositories = emptyList(),
                message = "GitHub disconnected"
            )
        }
    }

internal fun RexGitViewModel.__splitImpl_repositorySlug(repo: RexGitRepository): Pair<String, String>? {
        val remote = repo.remoteUrl?.trim().orEmpty()
        val match = Regex("""(?:github\.com[:/])([^/]+)/([^/]+?)(?:\.git)?/?$""", RegexOption.IGNORE_CASE).find(remote)
        if (match != null) return match.groupValues[1] to match.groupValues[2]
        val globalOwner = repo.globalOwner
        if (!globalOwner.isNullOrBlank()) return globalOwner to repo.name
        val account = repo.accountNickname
        if (!account.isNullOrBlank()) return account to repo.name
        return null
    }

/**
     * Returns the account currently in use.
     * [reload] re-reads token.env (used by refresh / connect).
     */
    internal suspend fun RexGitViewModel.__splitImpl_externalGithubAuth(reload: Boolean = false): RexGitAuth? {
        if (reload || accounts.isEmpty()) {
            val skipped = reloadAccounts()

            if (skipped.isNotEmpty()) {
                _state.update {
                    it.copy(message = "Skipped invalid token: ${skipped.joinToString()}")
                }
            }
        }

        return accounts.getOrNull(activeIndex)
    }
