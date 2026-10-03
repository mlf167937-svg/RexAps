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

/** Compatibility wrapper. RexGit uses JGit through RexGitNative (no ProcessBuilder). */
object GitCommandRunner {

    private val native = RexGitNative()

    private fun toRepo(dir: File) = RexGitRepository(
        name = dir.name,
        path = dir.absolutePath,
        isGitRepository = File(dir, ".git").exists()
    )

    private fun RexGitNativeResult.toCommand() =
        GitCommandResult(
            exitCode = if (success) 0 else 1,
            stdout = if (success) message else "",
            stderr = if (success) "" else message
        )

    fun status(repo: File): GitCommandResult {
        val status = native.status(toRepo(repo))

        return if (status.error != null) {
            GitCommandResult(1, stderr = status.error)
        } else {
            GitCommandResult(0, stdout = status.output)
        }
    }

    fun addAll(repo: File) = native.addAll(toRepo(repo)).toCommand()

    fun commit(repo: File, message: String) =
        native.commit(toRepo(repo), message).toCommand()

    fun branch(repo: File): String = native.branch(toRepo(repo))

    fun remote(repo: File): String? = native.remote(toRepo(repo))

    fun pull(repo: File) = native.pull(toRepo(repo)).toCommand()
}
