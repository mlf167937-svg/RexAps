package com.rexaps.rexgit

import java.io.File

class RexGitRepositoryManager {

    private val native = RexGitNative()

    fun scan(): List<RexGitRepository> {
        return RexGitStorage.scanRepositories()
    }

    fun status(
        repository: RexGitRepository
    ): RexGitStatus {
        return native.status(repository)
    }

    fun files(
        repository: RexGitRepository
    ): List<RexGitFile> {

        val root = File(repository.path)

        if (!root.exists()) {
            return emptyList()
        }

        return root
            .listFiles()
            ?.filterNot {
                it.name == ".git" ||
                it.name == ".gradle" ||
                it.name == "build"
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

    fun currentBranch(
        repository: RexGitRepository
    ): String {
        return GitCommandRunner.branch(
            File(repository.path)
        )
    }

    fun remote(
        repository: RexGitRepository
    ): String? {
        return GitCommandRunner.remote(
            File(repository.path)
        )
    }

    fun addAll(
        repository: RexGitRepository
    ): GitCommandResult {

        val result = GitCommandRunner.addAll(
            File(repository.path)
        )

        return result
    }

    fun commit(
        repository: RexGitRepository,
        message: String
    ): GitCommandResult {

        return GitCommandRunner.commit(
            File(repository.path),
            message
        )
    }
}
