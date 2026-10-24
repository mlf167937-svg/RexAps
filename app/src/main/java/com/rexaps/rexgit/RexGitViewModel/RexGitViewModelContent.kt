package com.rexaps.rexgit

import kotlinx.coroutines.flow.update

fun RexGitViewModel.__split_openRoot_13() {
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
