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
        gitDev: RexGitGitDevConfig = RexGitGitDevConfig(),
        useGitDev: Boolean = false,
        username: String? = null,
        token: String? = null,
        onProgress: (stage: String, detail: String, logLine: String?) -> Unit = { _, _, _ -> }
    ): RexGitPushResult {
        val dir = File(repository.path)

        if (useGitDev && !gitDev.isReady) {
            return RexGitPushResult(false, "Configure GitDev for this repository first, or turn GitDev metadata off.")
        }

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

            if (useGitDev) {
                val detectedFolders = RexGitCommitMeta.detectFolders(repository, status.changes, gitDev)
                if (commitFolder.isBlank() || commitFolder !in detectedFolders) {
                    return RexGitPushResult(
                        false,
                        "Invalid commit folder. Choose a folder detected under ${gitDev.packagePath}."
                    )
                }
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
            if (useGitDev && bumpVersion) {
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

            val finalMessage = if (useGitDev) {
                RexGitCommitMeta.buildMessage(
                    version = newVersion,
                    type = commitType,
                    folder = commitFolder,
                    description = commitMessage
                )
            } else {
                RexGitCommitMeta.buildStandardMessage(commitType, commitMessage)
            }

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
            RexGitPushResult(false, explainPushFailure(push.message), newVersion)
        }
    }

    /**
     * Translate common JGit remote rejections into actionable guidance.
     * Never retries with force; commits already created locally remain intact.
     */
    private fun explainPushFailure(message: String): String {
        val normalized = message.lowercase()
        return when {
            "rejected_nonfastforward" in normalized ||
                "non-fast-forward" in normalized ||
                "nonfastforward" in normalized -> {
                "The remote branch has commits that are not integrated locally. " +
                    "Your local commit is kept; no force-push was attempted. " +
                    "Pull/integrate the remote changes, resolve any conflicts, then push again.\n\n$message"
            }
            "authentication" in normalized ||
                "not authorized" in normalized ||
                "permission denied" in normalized ||
                "http 401" in normalized ||
                "http 403" in normalized -> {
                "GitHub rejected authentication or repository permission. " +
                    "Check the selected account, token validity, and write access. " +
                    "No force-push was attempted.\n\n$message"
            }
            else -> message
        }
    }
}
