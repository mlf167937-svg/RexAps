package com.rexaps.rexgit

import java.io.File

data class GitCommandResult(
    val exitCode: Int,
    val stdout: String = "",
    val stderr: String = ""
) {
    val success: Boolean
        get() = exitCode == 0
}

/*
 * Compatibility wrapper.
 *
 * RexGit sekarang memakai JGit melalui RexGitNative.
 * Tidak ada ProcessBuilder("git", ...) lagi.
 */
object GitCommandRunner {

    private val native =
        RexGitNative()

    fun status(
        repo: File
    ): GitCommandResult {

        return try {

            val repository =
                RexGitRepository(
                    name = repo.name,
                    path = repo.absolutePath,
                    isGitRepository =
                        File(repo, ".git").exists()
                )

            val result =
                native.status(repository)

            GitCommandResult(
                exitCode =
                    if (result.output.isEmpty() &&
                        result.hasChanges.not()
                    ) 0 else 0,
                stdout = result.output,
                stderr = ""
            )

        } catch (e: Exception) {

            GitCommandResult(
                1,
                stderr =
                    e.message ?: "Git status gagal"
            )
        }
    }

    fun addAll(
        repo: File
    ): GitCommandResult {

        return try {

            val repository =
                RexGitRepository(
                    name = repo.name,
                    path = repo.absolutePath,
                    isGitRepository = true
                )

            val result =
                native.addAll(repository)

            GitCommandResult(
                if (result.success) 0 else 1,
                stdout = result.message,
                stderr =
                    if (result.success)
                        ""
                    else
                        result.message
            )

        } catch (e: Exception) {

            GitCommandResult(
                1,
                stderr =
                    e.message ?: "git add gagal"
            )
        }
    }

    fun commit(
        repo: File,
        message: String
    ): GitCommandResult {

        return try {

            val repository =
                RexGitRepository(
                    name = repo.name,
                    path = repo.absolutePath,
                    isGitRepository = true
                )

            val result =
                native.commit(
                    repository,
                    message
                )

            GitCommandResult(
                if (result.success) 0 else 1,
                stdout = result.message,
                stderr =
                    if (result.success)
                        ""
                    else
                        result.message
            )

        } catch (e: Exception) {

            GitCommandResult(
                1,
                stderr =
                    e.message ?: "git commit gagal"
            )
        }
    }

    fun branch(
        repo: File
    ): String {

        val repository =
            RexGitRepository(
                name = repo.name,
                path = repo.absolutePath,
                isGitRepository = true
            )

        return native.branch(repository)
    }

    fun remote(
        repo: File
    ): String? {

        val repository =
            RexGitRepository(
                name = repo.name,
                path = repo.absolutePath,
                isGitRepository = true
            )

        return native.remote(repository)
    }

    fun pull(
        repo: File
    ): GitCommandResult {

        return try {

            val repository =
                RexGitRepository(
                    name = repo.name,
                    path = repo.absolutePath,
                    isGitRepository = true
                )

            val result =
                native.pull(repository)

            GitCommandResult(
                if (result.success) 0 else 1,
                stdout = result.message,
                stderr =
                    if (result.success)
                        ""
                    else
                        result.message
            )

        } catch (e: Exception) {

            GitCommandResult(
                1,
                stderr =
                    e.message ?: "git pull gagal"
            )
        }
    }
}
