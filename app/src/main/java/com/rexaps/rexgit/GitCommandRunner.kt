package com.rexaps.rexgit

import java.io.File
import java.util.concurrent.TimeUnit

data class GitCommandResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String
) {
    val success: Boolean
        get() = exitCode == 0
}

object GitCommandRunner {

    fun run(
        workingDirectory: File,
        vararg command: String,
        timeoutSeconds: Long = 120
    ): GitCommandResult {

        return try {
            val process = ProcessBuilder(*command)
                .directory(workingDirectory)
                .redirectErrorStream(false)
                .start()

            val stdout = process.inputStream.bufferedReader().use {
                it.readText()
            }

            val stderr = process.errorStream.bufferedReader().use {
                it.readText()
            }

            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly()

                return GitCommandResult(
                    exitCode = -1,
                    stdout = stdout,
                    stderr = "Command timeout"
                )
            }

            GitCommandResult(
                exitCode = process.exitValue(),
                stdout = stdout.trim(),
                stderr = stderr.trim()
            )
        } catch (e: Exception) {
            GitCommandResult(
                exitCode = -1,
                stdout = "",
                stderr = e.message ?: e.javaClass.simpleName
            )
        }
    }
}
