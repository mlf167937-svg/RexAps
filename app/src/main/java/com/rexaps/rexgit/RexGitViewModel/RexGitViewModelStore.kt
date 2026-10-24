package com.rexaps.rexgit

import kotlinx.coroutines.flow.update
import java.io.File

/**
     * Saves GitDev mode for the selected repository. Turning it ON needs a
     * package path that really contains folders (e.g. com/rexaps).
     */
    fun RexGitViewModel.__split_saveGitDev_39(config: RexGitGitDevConfig) {
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
