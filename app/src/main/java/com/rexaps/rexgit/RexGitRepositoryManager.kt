package com.rexaps.rexgit

import java.io.File

class RexGitRepositoryManager {

    private val native = RexGitNative()

    fun scan(): List<RexGitRepository> {
        return try {
            RexGitStorage.scanRepositories()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun status(repository: RexGitRepository): RexGitStatus {
        return native.status(repository)
    }

    fun files(
        repository: RexGitRepository,
        directory: String? = null
    ): List<RexGitFile> {

        val root = File(directory ?: repository.path)

        if (!root.exists() || !root.isDirectory) {
            return emptyList()
        }

        return try {
            root.listFiles()
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
                        isDirectory = it.isDirectory,
                        size = if (it.isFile) it.length() else 0L
                    )
                }
                ?: emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun currentBranch(repository: RexGitRepository): String {
        return native.branch(repository)
    }

    fun remote(repository: RexGitRepository): String? {
        return native.remote(repository)
    }

    fun addAll(repository: RexGitRepository): RexGitNativeResult {
        return native.addAll(repository)
    }

    fun commit(
        repository: RexGitRepository,
        message: String,
        username: String? = null
    ): RexGitNativeResult {
        return native.commit(repository, message, username)
    }

    fun push(
        repository: RexGitRepository,
        username: String? = null,
        token: String? = null
    ): RexGitNativeResult {
        return native.push(repository, username, token)
    }

    fun pull(
        repository: RexGitRepository,
        username: String? = null,
        token: String? = null
    ): RexGitNativeResult {
        return native.pull(repository, username, token)
    }

    fun clone(
        url: String,
        name: String,
        username: String? = null,
        token: String? = null
    ): RexGitNativeResult {
        return native.clone(
            url = url,
            destination = RexGitStorage.repository(name),
            username = username,
            token = token
        )
    }
}
