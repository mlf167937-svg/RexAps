package com.rexaps.rexgit

import kotlinx.coroutines.flow.update
import java.io.File

fun RexGitViewModel.__split_selectRepository_8(repository: RexGitRepository) {
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

internal fun RexGitViewModel.__split_reloadRepository_10(repo: RexGitRepository) {
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

fun RexGitViewModel.__split_openDirectory_12(directory: String) {
        val repo = _state.value.selectedRepository
            ?: return

        launchIO {
            val files = repositoryManager.files(
                repo,
                directory
            )

            _state.update {
                it.copy(
                    files = files,
                    currentDirectory = directory
                )
            }
        }
    }

fun RexGitViewModel.__split_openFile_15(file: RexGitFile) {
        if (file.isDirectory) {
            openDirectory(file.path)
            return
        }

        launchIO {
            val target = File(file.path)

            if (target.length() > MAX_EDITOR_BYTES) {
                fail(
                    "This file is too large to edit on a phone " +
                        "(limit 400 KB)."
                )
                return@launchIO
            }

            if (looksBinary(target)) {
                fail("Binary files can't be opened in the editor.")
                return@launchIO
            }

            val content = target.readText()

            editorBuffer = content

            _state.update {
                it.copy(
                    editorPath = file.path,
                    editorContent = content,
                    editorDirty = false,
                    error = null
                )
            }
        }
    }

fun RexGitViewModel.__split_createFile_17(name: String) {
        val repo = _state.value.selectedRepository
            ?: return

        val dir = _state.value.currentDirectory
        val clean = name.trim()

        launchIO {
            _state.update {
                it.copy(
                    isRepoLoading = true,
                    error = null
                )
            }

            try {
                val result = repositoryManager.createFile(
                    repo,
                    dir,
                    clean
                )

                if (result.success) {
                    reloadRepository(repo)

                    _state.update {
                        it.copy(message = result.message)
                    }

                    openFile(
                        RexGitFile(
                            name = clean,
                            path = File(
                                dir ?: repo.path,
                                clean
                            ).absolutePath,
                            isDirectory = false
                        )
                    )
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

fun RexGitViewModel.__split_updateEditor_20(content: String) {
        editorBuffer = content

        _state.update {
            if (it.editorDirty) {
                it
            } else {
                it.copy(editorDirty = true)
            }
        }
    }

fun RexGitViewModel.__split_closeEditor_21() {
        editorBuffer = ""

        _state.update {
            it.copy(
                editorPath = null,
                editorContent = "",
                editorDirty = false
            )
        }
    }

fun RexGitViewModel.__split_saveEditor_22() {
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
