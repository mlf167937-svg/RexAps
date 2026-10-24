package com.rexaps.rexgit

import java.io.File
import kotlinx.coroutines.flow.update

internal fun RexGitViewModel.__splitImpl_createFile(name: String) {
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

internal fun RexGitViewModel.__splitImpl_createFolder(name: String) {
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
                val result = repositoryManager.createFolder(
                    repo,
                    dir,
                    clean
                )

                if (result.success) {
                    reloadRepository(repo)

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

internal fun RexGitViewModel.__splitImpl_openFile(file: RexGitFile) {
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

internal fun RexGitViewModel.__splitImpl_openDirectory(directory: String) {
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
