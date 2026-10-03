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
        return native.branch(repository)
    }

    fun remote(
        repository: RexGitRepository
    ): String? {
        return native.remote(repository)
    }

    fun addAll(
        repository: RexGitRepository
    ): RexGitNativeResult {
        return native.addAll(repository)
    }

    fun commit(
        repository: RexGitRepository,
        message: String
    ): RexGitNativeResult {
        return native.commit(
            repository,
            message
        )
    }

    fun push(
        repository: RexGitRepository,
        username: String? = null,
        token: String? = null
    ): RexGitNativeResult {
        return native.push(
            repository,
            username,
            token
        )
    }

    fun pull(
        repository: RexGitRepository,
        username: String? = null,
        token: String? = null
    ): RexGitNativeResult {
        return native.pull(
            repository,
            username,
            token
        )
    }

    fun clone(
        url: String,
        name: String
    ): RexGitNativeResult {

        val destination = RexGitStorage.repository(name)

        return native.clone(
            url = url,
            destination = destination
        )
    }
}
