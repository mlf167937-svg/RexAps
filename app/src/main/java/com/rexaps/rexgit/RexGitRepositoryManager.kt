package com.rexaps.rexgit

import java.io.File

/** Returns an error message when the name is not allowed, or null when it is valid. */
internal fun validateEntryName(raw: String): String? {
    val name = raw.trim()

    return when {
        name.isEmpty() -> "Name can't be empty"
        name == "." || name == ".." -> "Invalid name"
        name.contains('/') || name.contains('\\') -> "Name can't contain / or \\"
        name.contains('\u0000') -> "Name contains an invalid character"
        name.equals(".git", ignoreCase = true) -> "\".git\" is reserved"
        name.length > 120 -> "Name is too long"
        else -> null
    }
}

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

    // ---------- create / delete ----------

    private fun insideRepo(root: File, target: File): Boolean {
        return target.path.startsWith(root.path + File.separator)
    }

    private fun resolveParent(
        repository: RexGitRepository,
        directory: String?
    ): Pair<File, String?> {
        val root = File(repository.path).canonicalFile
        val parent = File(directory ?: repository.path).canonicalFile

        if (parent != root && !insideRepo(root, parent)) {
            return parent to "Invalid location"
        }

        if (!parent.isDirectory) {
            return parent to "This folder no longer exists"
        }

        return parent to null
    }

    fun createFile(
        repository: RexGitRepository,
        directory: String?,
        rawName: String
    ): RexGitNativeResult {
        return try {
            validateEntryName(rawName)?.let {
                return RexGitNativeResult(false, it)
            }

            val (parent, problem) = resolveParent(repository, directory)

            if (problem != null) {
                return RexGitNativeResult(false, problem)
            }

            val target = File(parent, rawName.trim())

            if (target.exists()) {
                return RexGitNativeResult(false, "\"${target.name}\" already exists")
            }

            if (target.createNewFile()) {
                RexGitNativeResult(true, "Created ${target.name}")
            } else {
                RexGitNativeResult(false, "Could not create the file")
            }
        } catch (t: Throwable) {
            RexGitNativeResult(false, t.readableMessage("Create file failed"))
        }
    }

    fun createFolder(
        repository: RexGitRepository,
        directory: String?,
        rawName: String
    ): RexGitNativeResult {
        return try {
            validateEntryName(rawName)?.let {
                return RexGitNativeResult(false, it)
            }

            val (parent, problem) = resolveParent(repository, directory)

            if (problem != null) {
                return RexGitNativeResult(false, problem)
            }

            val target = File(parent, rawName.trim())

            if (target.exists()) {
                return RexGitNativeResult(false, "\"${target.name}\" already exists")
            }

            if (target.mkdir()) {
                RexGitNativeResult(true, "Created folder ${target.name}")
            } else {
                RexGitNativeResult(false, "Could not create the folder")
            }
        } catch (t: Throwable) {
            RexGitNativeResult(false, t.readableMessage("Create folder failed"))
        }
    }

    fun delete(
        repository: RexGitRepository,
        path: String
    ): RexGitNativeResult {
        return try {
            val root = File(repository.path).canonicalFile
            val target = File(path).canonicalFile

            if (!insideRepo(root, target)) {
                return RexGitNativeResult(
                    false,
                    "Refusing to delete anything outside the repository"
                )
            }

            val relative = target.path.removePrefix(root.path + File.separator)

            if (relative.split(File.separatorChar).first() == ".git") {
                return RexGitNativeResult(false, "The .git folder is protected")
            }

            if (!target.exists()) {
                return RexGitNativeResult(false, "It was already deleted")
            }

            val wasFolder = target.isDirectory

            val ok =
                if (wasFolder) target.deleteRecursively()
                else target.delete()

            if (ok) {
                RexGitNativeResult(
                    true,
                    if (wasFolder) "Deleted folder ${target.name}"
                    else "Deleted ${target.name}"
                )
            } else {
                RexGitNativeResult(false, "Could not delete ${target.name}")
            }
        } catch (t: Throwable) {
            RexGitNativeResult(false, t.readableMessage("Delete failed"))
        }
    }

    // ---------- git ----------

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