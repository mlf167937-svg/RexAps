package com.rexaps.rexgit

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File

data class RexGitNativeResult(
    val success: Boolean,
    val message: String
)

class RexGitNative {

    fun clone(
        url: String,
        destination: File
    ): RexGitNativeResult {
        return try {
            if (destination.exists()) {
                return RexGitNativeResult(
                    false,
                    "Folder ${destination.name} sudah ada"
                )
            }

            destination.parentFile?.mkdirs()

            Git.cloneRepository()
                .setURI(url)
                .setDirectory(destination)
                .call()
                .use { }

            RexGitNativeResult(
                true,
                "Repository berhasil di-clone"
            )
        } catch (e: Exception) {
            RexGitNativeResult(
                false,
                e.message ?: "Clone gagal"
            )
        }
    }

    fun status(
        repository: RexGitRepository
    ): RexGitStatus {
        val repo = File(repository.path)

        return try {
            Git.open(repo).use { git ->
                val status = git.status().call()

                val modified = buildList {
                    addAll(status.modified)
                    addAll(status.missing)
                    addAll(status.removed)
                }

                val staged = buildList {
                    addAll(status.added)
                    addAll(status.changed)
                }

                val hasChanges =
                    status.hasUncommittedChanges()

                RexGitStatus(
                    output = buildString {
                        status.modified.forEach {
                            appendLine(" M $it")
                        }
                        status.changed.forEach {
                            appendLine("M  $it")
                        }
                        status.added.forEach {
                            appendLine("A  $it")
                        }
                        status.removed.forEach {
                            appendLine(" D $it")
                        }
                        status.untracked.forEach {
                            appendLine("?? $it")
                        }
                    }.trim(),
                    modifiedFiles = modified,
                    stagedFiles = staged,
                    hasChanges = hasChanges
                )
            }
        } catch (e: Exception) {
            RexGitStatus(
                output = e.message ?: "Status gagal"
            )
        }
    }

    fun branch(
        repository: RexGitRepository
    ): String {
        return try {
            Git.open(File(repository.path)).use { git ->
                git.repository.branch
            }
        } catch (_: Exception) {
            "main"
        }
    }

    fun remote(
        repository: RexGitRepository
    ): String? {
        return try {
            Git.open(File(repository.path)).use { git ->
                git.repository.config
                    .getString("remote", "origin", "url")
            }
        } catch (_: Exception) {
            null
        }
    }

    fun addAll(
        repository: RexGitRepository
    ): RexGitNativeResult {
        return try {
            Git.open(File(repository.path)).use { git ->
                git.add()
                    .addFilepattern(".")
                    .call()
            }

            RexGitNativeResult(
                true,
                "Semua perubahan berhasil di-stage"
            )
        } catch (e: Exception) {
            RexGitNativeResult(
                false,
                e.message ?: "Stage gagal"
            )
        }
    }

    fun commit(
        repository: RexGitRepository,
        message: String
    ): RexGitNativeResult {
        return try {
            Git.open(File(repository.path)).use { git ->
                git.commit()
                    .setMessage(message)
                    .call()
            }

            RexGitNativeResult(
                true,
                "Commit berhasil"
            )
        } catch (e: Exception) {
            RexGitNativeResult(
                false,
                e.message ?: "Commit gagal"
            )
        }
    }

    fun push(
        repository: RexGitRepository,
        username: String? = null,
        token: String? = null
    ): RexGitNativeResult {
        return try {
            Git.open(File(repository.path)).use { git ->

                val command = git.push()

                if (
                    !username.isNullOrBlank() &&
                    !token.isNullOrBlank()
                ) {
                    command.setCredentialsProvider(
                        UsernamePasswordCredentialsProvider(
                            username,
                            token
                        )
                    )
                }

                command.call()
            }

            RexGitNativeResult(
                true,
                "Push berhasil"
            )
        } catch (e: Exception) {
            RexGitNativeResult(
                false,
                e.message ?: "Push gagal"
            )
        }
    }

    fun pull(
        repository: RexGitRepository,
        username: String? = null,
        token: String? = null
    ): RexGitNativeResult {
        return try {
            Git.open(File(repository.path)).use { git ->

                val command = git.pull()

                if (
                    !username.isNullOrBlank() &&
                    !token.isNullOrBlank()
                ) {
                    command.setCredentialsProvider(
                        UsernamePasswordCredentialsProvider(
                            username,
                            token
                        )
                    )
                }

                command.call()
            }

            RexGitNativeResult(
                true,
                "Pull berhasil"
            )
        } catch (e: Exception) {
            RexGitNativeResult(
                false,
                e.message ?: "Pull gagal"
            )
        }
    }
}
