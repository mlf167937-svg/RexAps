package com.rexaps.rexgit

import java.io.File

class RexGitPushManager {

    private val native = RexGitNative()
    private val version = RexGitVersionManager()

    fun push(
        repository: RexGitRepository,
        commitMessage: String,
        bumpVersion: Boolean = true,
        username: String? = null,
        token: String? = null
    ): RexGitPushResult {

        val dir = File(repository.path)

        if (!dir.exists()) {
            return RexGitPushResult(false, "Repository folder not found")
        }

        if (!File(dir, ".git").exists()) {
            return RexGitPushResult(false, "This folder is not a Git repository")
        }

        val status = native.status(repository)

        if (status.error != null) {
            return RexGitPushResult(
                false,
                "Could not read repository status:\n${status.error}"
            )
        }

        val hadChanges = status.hasChanges

        if (hadChanges && commitMessage.isBlank()) {
            return RexGitPushResult(false, "Commit message is empty")
        }

        var newVersion: RexGitVersion? = null

        if (hadChanges) {
            if (bumpVersion) {
                newVersion = try {
                    version.bump(repository)
                } catch (_: Throwable) {
                    null
                }
            }

            val finalMessage =
                if (newVersion != null) {
                    "[${newVersion.versionName}] $commitMessage"
                } else {
                    commitMessage
                }

            val add = native.addAll(repository)

            if (!add.success) {
                return RexGitPushResult(
                    false,
                    "Staging failed:\n${add.message}",
                    newVersion
                )
            }

            val commit = native.commit(repository, finalMessage, username)

            if (!commit.success) {
                return RexGitPushResult(
                    false,
                    "Commit failed:\n${commit.message}",
                    newVersion
                )
            }
        }

        val push = native.push(repository, username, token)

        if (!push.success) {
            return RexGitPushResult(
                false,
                "Push failed:\n${push.message}",
                newVersion
            )
        }

        return RexGitPushResult(
            true,
            if (hadChanges) "Committed and pushed"
            else "Nothing to commit. Existing commits were pushed.",
            newVersion
        )
    }
}
