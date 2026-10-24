package com.rexaps.rexgit

import kotlinx.coroutines.flow.update

fun RexGitViewModel.__split_createFolder_18(name: String) {
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
