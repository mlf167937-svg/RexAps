package com.rexaps.rexgit

import java.io.File

class RexGitRepositoryManager {

    fun scan(): List<RexGitRepository> {
        return RexGitStorage.scanRepositories()
    }

    fun status(repository: RexGitRepository): RexGitStatus {
        val dir = File(repository.path)

        val result = GitCommandRunner.run(
            dir,
            "git",
            "status",
            "--short"
        )

        if (!result.success) {
            return RexGitStatus(
                output = result.stderr
            )
        }

        val lines = result.stdout
            .lines()
            .filter { it.isNotBlank() }

        val modified = lines
            .filter {
                it.startsWith(" M") ||
                    it.startsWith("M ") ||
                    it.startsWith("MM") ||
                    it.startsWith(" D")
            }
            .map { it.drop(3).trim() }

        val staged = lines
            .filter {
                it.length >= 2 &&
                    it[0] != ' ' &&
                    it[0] != '?'
            }
            .map { it.drop(3).trim() }

        return RexGitStatus(
            output = result.stdout,
            modifiedFiles = modified,
            stagedFiles = staged,
            hasChanges = lines.isNotEmpty()
        )
    }

    fun files(repository: RexGitRepository): List<RexGitFile> {
        val root = File(repository.path)

        if (!root.exists()) {
            return emptyList()
        }

        return root
            .listFiles()
            ?.filterNot {
                it.name == ".git" ||
                    it.name == ".gradle"
            }
            ?.sortedWith(
                compareBy<File> { !it.isDirectory }
                    .thenBy { it.name.lowercase() }
            )
            ?.map {
                RexGitFile(
                    name = it.name,
                    path = it.absolutePath,
                    isDirectory = it.isDirectory
                )
            }
            ?: emptyList()
    }

    fun currentBranch(repository: RexGitRepository): String {
        val result = GitCommandRunner.run(
            File(repository.path),
            "git",
            "branch",
            "--show-current"
        )

        return if (result.success && result.stdout.isNotBlank()) {
            result.stdout
        } else {
            "main"
        }
    }

    fun remote(repository: RexGitRepository): String? {
        val result = GitCommandRunner.run(
            File(repository.path),
            "git",
            "remote",
            "get-url",
            "origin"
        )

        return if (result.success && result.stdout.isNotBlank()) {
            result.stdout
        } else {
            null
        }
    }

    fun addAll(repository: RexGitRepository): GitCommandResult {
        return GitCommandRunner.run(
            File(repository.path),
            "git",
            "add",
            "."
        )
    }

    fun commit(
        repository: RexGitRepository,
        message: String
    ): GitCommandResult {
        return GitCommandRunner.run(
            File(repository.path),
            "git",
            "commit",
            "-m",
            message
        )
    }

    fun push(repository: RexGitRepository): GitCommandResult {
        return GitCommandRunner.run(
            File(repository.path),
            "git",
            "push"
        )
    }
}
