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
        token: String? = null,
        onProgress: (stage: String, detail: String, logLine: String?) -> Unit = { _, _, _ -> }
    ): RexGitPushResult {
        val dir = File(repository.path)

        onProgress("Checking Files...", "Reading repository status", null)

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
        onProgress(
            "Updating",
            if (hadChanges) "${status.changes.size} changed file(s) detected" else "No local changes",
            null
        )

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

            // Show the actual Git changes, not simulated entries.
            status.changes.forEach { change ->
                val line = when (change.kind) {
                    RexGitChangeKind.ADDED,
                    RexGitChangeKind.UNTRACKED -> "+ add files ${change.path}"
                    RexGitChangeKind.MODIFIED -> "~ edit files ${change.path}"
                    RexGitChangeKind.DELETED -> "- delete files ${change.path}"
                    RexGitChangeKind.CONFLICT -> "! conflict files ${change.path}"
                }
                onProgress("Updating", change.path, line)
            }
        }

        var newVersion: RexGitVersion? = null

        if (hadChanges) {
            if (bumpVersion) {
                onProgress("Updating", "Updating app version", null)
                newVersion = try {
                    version.bump(repository)
                } catch (_: Throwable) {
                    null
                }

                newVersion?.let {
                    onProgress(
                        "Updating",
                        "Version ${it.versionName} (${it.versionCode})",
                        "~ edit files build.gradle (version bump)"
                    )
                }
            }

            val finalMessage = RexGitCommitMeta.buildMessage(
                version = newVersion,
                type = commitType,
                folder = commitFolder,
                description = commitMessage
            )

            onProgress("Staging", "Preparing files for commit", null)

            val add = native.addAll(repository)

            if (!add.success) {
                return RexGitPushResult(
                    false,
                    "Staging failed:\n${add.message}",
                    newVersion
                )
            }

            onProgress("Committing", finalMessage, null)

            val commit = native.commit(repository, finalMessage, username)

            if (!commit.success) {
                return RexGitPushResult(
                    false,
                    "Commit failed:\n${commit.message}",
                    newVersion
                )
            }
        }

        onProgress("Pushing", "Uploading to origin", null)

        val push = native.push(repository, username, token)

        return if (push.success) {
            RexGitPushResult(true, push.message, newVersion)
        } else {
            RexGitPushResult(false, push.message, newVersion)
        }
    }
}
