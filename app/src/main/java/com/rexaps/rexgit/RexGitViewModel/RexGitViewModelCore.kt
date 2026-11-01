package com.rexaps.rexgit

import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.update

/** Refreshes local repositories and the connected GitHub account without replacing the ViewModel. */
internal fun RexGitViewModel.__splitImpl_refresh() {
    if (_state.value.isLoading) return

    launchIO {
        _state.update { it.copy(isLoading = true, error = null) }
        try {
            val repositories = repositoryManager.scan()
            val (storagePath, storageFallback) = storageInfo()

            _state.update {
                it.copy(
                    repositories = repositories,
                    storagePath = storagePath,
                    storageFallback = storageFallback
                )
            }

            try {
                val auth = externalGithubAuth(reload = true)
                if (auth != null) {
                    _state.update {
                        it.copy(githubUsername = auth.username, isGithubConnected = true)
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
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                fail(
                    "Could not connect to GitHub:\n" +
                        error.readableMessage("Invalid or expired token")
                )
            }
        } finally {
            _state.update { it.copy(isLoading = false) }
        }
    }
}

/** Refreshes the selected repository's files and Git status. */
internal fun RexGitViewModel.__splitImpl_refreshStatus() {
    val repository = _state.value.selectedRepository ?: return
    if (_state.value.isRepoLoading) return

    launchIO {
        _state.update { it.copy(isRepoLoading = true, error = null) }
        try {
            reloadRepository(repository)
        } finally {
            _state.update { it.copy(isRepoLoading = false) }
        }
    }
}

/** Deletes only an entry accepted by RexGitRepositoryManager's repository-boundary checks. */
internal fun RexGitViewModel.__splitImpl_deleteEntry(file: RexGitFile) {
    val repository = _state.value.selectedRepository ?: return
    if (_state.value.isRepoLoading) return

    launchIO {
        _state.update { it.copy(isRepoLoading = true, error = null) }
        try {
            val result = repositoryManager.delete(repository, file.path)
            reloadRepository(repository)
            if (result.success) {
                _state.update { it.copy(message = result.message, error = null) }
            } else {
                fail(result.message)
            }
        } finally {
            _state.update { it.copy(isRepoLoading = false) }
        }
    }
}

/** Saves the editor snapshot and keeps the dirty flag if the user typed during the write. */
internal fun RexGitViewModel.__splitImpl_saveEditor() {
    val path = _state.value.editorPath ?: return
    if (_state.value.isSaving) return

    val contentSnapshot = editorBuffer
    launchIO {
        _state.update { it.copy(isSaving = true, error = null) }
        try {
            File(path).writeText(contentSnapshot)
            val repository = _state.value.selectedRepository
            val status = if (repository != null && repository.isGitRepository) {
                repositoryManager.status(repository)
            } else {
                null
            }

            _state.update { current ->
                current.copy(
                    editorDirty = editorBuffer != contentSnapshot,
                    gitStatus = status ?: current.gitStatus,
                    readmeContent = if (File(path).name.equals("README.md", ignoreCase = true)) {
                        contentSnapshot
                    } else {
                        current.readmeContent
                    },
                    message = "File saved",
                    error = null
                )
            }
        } finally {
            _state.update { it.copy(isSaving = false) }
        }
    }
}
