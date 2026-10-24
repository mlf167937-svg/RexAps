package com.rexaps.rexgit

import kotlinx.coroutines.flow.update
import java.io.File

// ---------- files ----------

internal fun RexGitViewModel.openDirectoryImpl(directory: String) {
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

internal fun RexGitViewModel.openRootImpl() {
    val repo = _state.value.selectedRepository
        ?: return

    launchIO {
        val files = repositoryManager.files(repo)

        _state.update {
            it.copy(
                files = files,
                currentDirectory = null
            )
        }
    }
}

internal fun RexGitViewModel.openParentImpl() {
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

internal fun RexGitViewModel.openFileImpl(file: RexGitFile) {
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

private fun RexGitViewModel.looksBinary(file: File): Boolean {
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

// ---------- create / delete ----------

internal fun RexGitViewModel.createFileImpl(name: String) {
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

internal fun RexGitViewModel.createFolderImpl(name: String) {
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

internal fun RexGitViewModel.deleteEntryImpl(file: RexGitFile) {
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
