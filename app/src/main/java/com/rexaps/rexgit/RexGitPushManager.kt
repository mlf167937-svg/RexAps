package com.rexaps.rexgit

class RexGitPushManager {

    private val git = RexGitRepositoryManager()
    private val version = RexGitVersionManager()

    fun push(
        repository: RexGitRepository,
        commitMessage: String,
        bumpVersion: Boolean = true
    ): RexGitPushResult {

        if (commitMessage.isBlank()) {
            return RexGitPushResult(
                success = false,
                message = "Commit message kosong"
            )
        }

        val repo = java.io.File(repository.path)

        if (!repo.exists()) {
            return RexGitPushResult(
                success = false,
                message = "Repository tidak ditemukan"
            )
        }

        if (!java.io.File(repo, ".git").exists()) {
            return RexGitPushResult(
                success = false,
                message = "Folder ini bukan Git repository"
            )
        }

        var newVersion: RexGitVersion? = null

        if (bumpVersion) {
            newVersion = version.bump(repository)
        }

        val finalMessage = if (newVersion != null) {
            "[${newVersion.versionName}] $commitMessage"
        } else {
            commitMessage
        }

        val add = git.addAll(repository)

        if (!add.success) {
            return RexGitPushResult(
                success = false,
                message = "git add gagal:\n${add.stderr}"
            )
        }

        val commit = git.commit(
            repository,
            finalMessage
        )

        if (!commit.success) {
            return RexGitPushResult(
                success = false,
                message = if (commit.stderr.isNotBlank()) {
                    commit.stderr
                } else {
                    "Commit gagal atau tidak ada perubahan"
                },
                version = newVersion
            )
        }

        val push = git.push(repository)

        if (!push.success) {
            return RexGitPushResult(
                success = false,
                message = if (push.stderr.isNotBlank()) {
                    push.stderr
                } else {
                    "git push gagal"
                },
                version = newVersion
            )
        }

        return RexGitPushResult(
            success = true,
            message = "Push berhasil",
            version = newVersion
        )
    }
}
