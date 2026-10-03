package com.rexaps.rexgit

import android.content.Context
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.WindowCacheConfig
import org.eclipse.jgit.transport.RemoteRefUpdate
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File

data class RexGitNativeResult(
    val success: Boolean,
    val message: String
)

internal fun Throwable.readableMessage(fallback: String): String {
    val text = message?.trim()
    return if (!text.isNullOrEmpty()) text else (javaClass.simpleName.ifBlank { fallback })
}

/** Makes JGit safe to run on Android (home dir + small memory footprint). */
object RexGitJGit {

    @Volatile
    private var ready = false

    fun init(context: Context) {
        if (ready) return

        synchronized(this) {
            if (ready) return

            try {
                System.setProperty(
                    "user.home",
                    context.applicationContext.filesDir.absolutePath
                )

                WindowCacheConfig().apply {
                    packedGitLimit = 8L * 1024 * 1024
                    packedGitWindowSize = 512 * 1024
                    setPackedGitMMAP(false)
                    deltaBaseCacheLimit = 4 * 1024 * 1024
                    streamFileThreshold = 8 * 1024 * 1024
                }.install()
            } catch (_: Throwable) {
            }

            ready = true
        }
    }
}

class RexGitNative {

    private fun credentials(
        username: String?,
        token: String?
    ): UsernamePasswordCredentialsProvider? {
        return if (!username.isNullOrBlank() && !token.isNullOrBlank()) {
            UsernamePasswordCredentialsProvider(username, token)
        } else {
            null
        }
    }

    fun clone(
        url: String,
        destination: File,
        username: String? = null,
        token: String? = null
    ): RexGitNativeResult {

        if (destination.exists()) {
            return RexGitNativeResult(
                false,
                "Folder ${destination.name} already exists"
            )
        }

        return try {
            destination.parentFile?.mkdirs()

            val command = Git.cloneRepository()
                .setURI(url)
                .setDirectory(destination)

            credentials(username, token)?.let {
                command.setCredentialsProvider(it)
            }

            command.call().close()

            RexGitNativeResult(
                true,
                "${destination.name} cloned successfully"
            )
        } catch (t: Throwable) {
            destination.deleteRecursively()

            RexGitNativeResult(
                false,
                t.readableMessage("Clone failed")
            )
        }
    }

    fun branch(repository: RexGitRepository): String {
        return try {
            Git.open(File(repository.path)).use { git ->
                val name = git.repository.branch ?: "main"
                if (name.length == 40) name.take(7) else name
            }
        } catch (_: Throwable) {
            "main"
        }
    }

    fun remote(repository: RexGitRepository): String? {
        return try {
            Git.open(File(repository.path)).use { git ->
                git.repository.config.getString("remote", "origin", "url")
            }
        } catch (_: Throwable) {
            null
        }
    }

    fun status(repository: RexGitRepository): RexGitStatus {
        return try {
            Git.open(File(repository.path)).use { git ->
                val s = git.status().call()

                val changes = buildList {
                    s.added.forEach { add(RexGitChange(it, RexGitChangeKind.ADDED)) }
                    s.changed.forEach { add(RexGitChange(it, RexGitChangeKind.MODIFIED)) }
                    s.modified.forEach { add(RexGitChange(it, RexGitChangeKind.MODIFIED)) }
                    s.removed.forEach { add(RexGitChange(it, RexGitChangeKind.DELETED)) }
                    s.missing.forEach { add(RexGitChange(it, RexGitChangeKind.DELETED)) }
                    s.conflicting.forEach { add(RexGitChange(it, RexGitChangeKind.CONFLICT)) }
                    s.untracked.forEach { add(RexGitChange(it, RexGitChangeKind.UNTRACKED)) }
                }
                    .distinct()
                    .sortedBy { it.path.lowercase() }

                RexGitStatus(changes = changes)
            }
        } catch (t: Throwable) {
            RexGitStatus(error = t.readableMessage("Status failed"))
        }
    }

    fun addAll(repository: RexGitRepository): RexGitNativeResult {
        return try {
            Git.open(File(repository.path)).use { git ->
                git.add().addFilepattern(".").call()
                // Also stage deletions
                git.add().addFilepattern(".").setUpdate(true).call()
            }

            RexGitNativeResult(true, "All changes staged")
        } catch (t: Throwable) {
            RexGitNativeResult(false, t.readableMessage("Stage failed"))
        }
    }

    fun commit(
        repository: RexGitRepository,
        message: String,
        username: String? = null
    ): RexGitNativeResult {
        return try {
            val name = username?.takeIf { it.isNotBlank() } ?: "RexGit"
            val email = if (username.isNullOrBlank()) {
                "rexgit@users.noreply.github.com"
            } else {
                "$username@users.noreply.github.com"
            }

            Git.open(File(repository.path)).use { git ->
                git.commit()
                    .setMessage(message)
                    .setAuthor(name, email)
                    .setCommitter(name, email)
                    .call()
            }

            RexGitNativeResult(true, "Commit created")
        } catch (t: Throwable) {
            RexGitNativeResult(false, t.readableMessage("Commit failed"))
        }
    }

    fun push(
        repository: RexGitRepository,
        username: String? = null,
        token: String? = null
    ): RexGitNativeResult {
        return try {
            Git.open(File(repository.path)).use { git ->
                val command = git.push().setRemote("origin")

                credentials(username, token)?.let {
                    command.setCredentialsProvider(it)
                }

                val problems = command.call()
                    .flatMap { it.remoteUpdates }
                    .filter {
                        it.status != RemoteRefUpdate.Status.OK &&
                            it.status != RemoteRefUpdate.Status.UP_TO_DATE
                    }

                if (problems.isEmpty()) {
                    RexGitNativeResult(true, "Push successful")
                } else {
                    RexGitNativeResult(
                        false,
                        problems.joinToString("\n") {
                            "${it.remoteName}: ${it.status}" +
                                (it.message?.let { m -> " ($m)" } ?: "")
                        }
                    )
                }
            }
        } catch (t: Throwable) {
            RexGitNativeResult(false, t.readableMessage("Push failed"))
        }
    }

    fun pull(
        repository: RexGitRepository,
        username: String? = null,
        token: String? = null
    ): RexGitNativeResult {
        return try {
            Git.open(File(repository.path)).use { git ->
                val command = git.pull()

                credentials(username, token)?.let {
                    command.setCredentialsProvider(it)
                }

                val result = command.call()

                if (result.isSuccessful) {
                    RexGitNativeResult(
                        true,
                        result.mergeResult?.mergeStatus?.toString()
                            ?.let { "Pull finished: $it" }
                            ?: "Pull successful"
                    )
                } else {
                    RexGitNativeResult(
                        false,
                        "Pull failed: " +
                            (result.mergeResult?.mergeStatus?.toString()
                                ?: "conflicts or no tracking branch")
                    )
                }
            }
        } catch (t: Throwable) {
            RexGitNativeResult(false, t.readableMessage("Pull failed"))
        }
    }
}
