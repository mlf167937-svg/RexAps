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

        val repo = File(repository.path)

        if (!repo.exists()) {
            return RexGitPushResult(
                false,
                "Repository tidak ditemukan"
            )
        }

        if (!File(repo, ".git").exists()) {
            return RexGitPushResult(
                false,
                "Folder ini bukan Git repository"
            )
        }

        if (commitMessage.isBlank()) {
            return RexGitPushResult(
                false,
                "Commit message kosong"
            )
        }

        var newVersion: RexGitVersion? = null

        if (bumpVersion) {
            newVersion = version.bump(repository)
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
                "Stage gagal:\n${add.message}",
                newVersion
            )
        }

        val commit = native.commit(
            repository,
            finalMessage
        )

        if (!commit.success) {
            return RexGitPushResult(
                false,
                "Commit gagal:\n${commit.message}",
                newVersion
            )
        }

        val push = native.push(
            repository,
            username,
            token
        )

        if (!push.success) {
            return RexGitPushResult(
                false,
                "Push gagal:\n${push.message}",
                newVersion
            )
        }

        return RexGitPushResult(
            true,
            "Push berhasil",
            newVersion
        )
    }
}
