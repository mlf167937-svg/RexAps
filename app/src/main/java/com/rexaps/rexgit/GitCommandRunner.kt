package com.rexaps.rexgit

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.Status
import java.io.File

data class GitCommandResult(
    val exitCode: Int,
    val stdout: String = "",
    val stderr: String = ""
) {
    val success: Boolean
        get() = exitCode == 0
}

object GitCommandRunner {

    fun status(repo: File): GitCommandResult {
        return try {
            Git.open(repo).use { git ->
                val status = git.status().call()

                val output = buildString {
                    status.modified.forEach { appendLine(" M $it") }
                    status.changed.forEach { appendLine("M  $it") }
                    status.added.forEach { appendLine("A  $it") }
                    status.removed.forEach { appendLine(" D $it") }
                    status.missing.forEach { appendLine(" D $it") }
                    status.untracked.forEach { appendLine("?? $it") }
                }.trim()

                GitCommandResult(
                    exitCode = 0,
                    stdout = output
                )
            }
        } catch (e: Exception) {
            GitCommandResult(
                exitCode = 1,
                stderr = e.message ?: "Git status gagal"
            )
        }
    }

    fun addAll(repo: File): GitCommandResult {
        return try {
            Git.open(repo).use { git ->
                git.add()
                    .addFilepattern(".")
                    .call()
            }

            GitCommandResult(0, "Files staged")
        } catch (e: Exception) {
            GitCommandResult(
                1,
                stderr = e.message ?: "git add gagal"
            )
        }
    }

    fun commit(
        repo: File,
        message: String
    ): GitCommandResult {
        return try {
            Git.open(repo).use { git ->
                val commit = git.commit()
                    .setMessage(message)
                    .call()

                GitCommandResult(
                    0,
                    commit.name
                )
            }
        } catch (e: Exception) {
            GitCommandResult(
                1,
                stderr = e.message ?: "git commit gagal"
            )
        }
    }

    fun branch(repo: File): String {
        return try {
            Git.open(repo).use { git ->
                git.repository.branch
            }
        } catch (_: Exception) {
            "main"
        }
    }

    fun remote(repo: File): String? {
        return try {
            Git.open(repo).use { git ->
                git.repository.config
                    .getString("remote", "origin", "url")
            }
        } catch (_: Exception) {
            null
        }
    }

    fun pull(repo: File): GitCommandResult {
        return try {
            Git.open(repo).use { git ->
                git.pull().call()
            }

            GitCommandResult(
                0,
                "Pull berhasil"
            )
        } catch (e: Exception) {
            GitCommandResult(
                1,
                stderr = e.message ?: "git pull gagal"
            )
        }
    }
}
