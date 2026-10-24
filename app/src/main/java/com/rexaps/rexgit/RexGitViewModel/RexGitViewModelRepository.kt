package com.rexaps.rexgit

import kotlinx.coroutines.flow.update
import java.io.File

// ---------- home ----------

internal fun RexGitViewModel.loadInitial() {
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

internal fun RexGitViewModel.refreshImpl() {
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

// ---------- repository ----------

internal fun RexGitViewModel.selectRepositoryImpl(repository: RexGitRepository) {
    if (_state.value.isRepoLoading) return

    launchIO {
        _state.update {
            it.copy(
                isRepoLoading = true,
                error = null
            )
        }

        try {
            val savedAccount = accountPrefs.getString(REPO_ACCOUNT_PREFIX + repository.path, null)
            val desiredAccount = savedAccount ?: repository.accountNickname
            val desiredIndex = accounts.indexOfFirst { it.username.equals(desiredAccount, ignoreCase = true) }
            if (desiredIndex >= 0 && desiredIndex != activeIndex) {
                activeIndex = desiredIndex
                accountPrefs.edit().putString(ACTIVE_KEY, accounts[desiredIndex].username).apply()
                publishAccounts()
            }
            val selectedAccountName = accounts.getOrNull(activeIndex)?.username
            if (selectedAccountName != null) {
                accountPrefs.edit().putString(REPO_ACCOUNT_PREFIX + repository.path, selectedAccountName).apply()
            }

            val isGit = File(repository.path, ".git").exists()

            val branch =
                if (isGit) {
                    repositoryManager.currentBranch(repository)
                } else {
                    ""
                }

            val remote =
                if (isGit) {
                    repositoryManager.remote(repository)
                } else {
                    null
                }

            val files = repositoryManager.files(repository)

            val status =
                if (isGit) {
                    repositoryManager.status(repository)
                } else {
                    RexGitStatus()
                }

            editorBuffer = ""

            _state.update {
                it.copy(
                    selectedRepository = repository.copy(
                        branch = branch,
                        remoteUrl = remote,
                        isGitRepository = isGit
                    ),
                    files = files,
                    currentDirectory = null,
                    gitStatus = status,
                    editorPath = null,
                    editorContent = "",
                    editorDirty = false,
                    gitDev = gitDevStore.load(repository.path),
                    readmeContent = readReadme(repository.path),
                    workflows = emptyList(),
                    workflowRuns = emptyList(),
                    workflowArtifacts = emptyList(),
                    releases = emptyList(),
                    repositoryMetadata = null,
                    repositoryTags = emptyList(),
                    isRepoHubLoading = true,
                    repoHubError = null,
                    actionsError = null,
                    releasesError = null,
                    downloadError = null,
                    downloadedBytes = 0L,
                    downloadTotalBytes = null,
                    message = null
                )
            }
            loadRepositoryGithubData(repository.copy(branch = branch, remoteUrl = remote, isGitRepository = isGit))
        } finally {
            _state.update {
                it.copy(isRepoLoading = false)
            }
        }
    }
}

internal fun RexGitViewModel.backToRepositoriesImpl() {
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

internal fun RexGitViewModel.reloadRepository(repo: RexGitRepository) {
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

internal fun RexGitViewModel.refreshStatusImpl() {
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

private fun RexGitViewModel.readReadme(repositoryPath: String): String {
    return try {
        val root = File(repositoryPath)
        val readme = root.listFiles()?.firstOrNull { it.isFile && it.name.equals("README.md", true) }
            ?: return ""
        if (readme.length() > MAX_EDITOR_BYTES) return "README is too large to preview in RexGit. Open the file from Code to inspect it."
        readme.readText(Charsets.UTF_8)
    } catch (_: Throwable) { "" }
}
