package com.rexaps.rexgit

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/** Runs on IO and converts ANY throwable (including Errors) into a UI error instead of a crash. */
    internal fun RexGitViewModel.__split_launchIO_1(block: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                fail(t.readableMessage("Unexpected error"))
            }
        }
    }

fun RexGitViewModel.__split_backToRepositories_9() {
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

fun RexGitViewModel.__split_openParent_14() {
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

internal fun RexGitViewModel.__split_looksBinary_16(file: File): Boolean {
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

fun RexGitViewModel.__split_deleteEntry_19(file: RexGitFile) {
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
