package com.rexaps.rexgit

import java.io.File

class RexGitPushManager {

    private val native = RexGitNative()
    private val version = RexGitVersionManager()

    fun push(
        repository: RexGitRepository,
        commitMessage: String,
        bumpVersion: Boolean = true,
        commitType: RexGitCommitType = RexGitCommitType.FIX,
        commitFolder: String = "",
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

        if (hadChanges) {
            if (commitMessage.isBlank()) {
                return RexGitPushResult(false, "Commit description is empty")
            }

            val detectedFolders = RexGitCommitMeta.detectFolders(repository, status.changes)
            if (commitFolder.isBlank() || commitFolder !in detectedFolders) {
                return RexGitPushResult(
                    false,
                    "Invalid commit folder. Choose a folder detected under com/rexaps."
                )
            }
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

            val finalMessage = RexGitCommitMeta.buildMessage(
                version = newVersion,
                type = commitType,
                folder = commitFolder,
                description = commitMessage
            )

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

        return if (push.success) {
            RexGitPushResult(true, push.message, newVersion)
        } else {
            RexGitPushResult(false, push.message, newVersion)
        }
    }
}
