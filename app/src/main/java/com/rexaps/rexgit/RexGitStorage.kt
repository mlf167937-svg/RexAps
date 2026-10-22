package com.rexaps.rexgit

import android.content.Context
import android.os.Environment
import java.io.File

object RexGitStorage {

    private const val ROOT_NAME = "RexAps"
    private const val GITHUB_NAME = "Github"
    private const val GLOBAL_NAME = "Global"

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private val sharedGithubRoot: File
        get() = File(
            File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                ROOT_NAME
            ),
            GITHUB_NAME
        )

    private val privateGithubRoot: File
        get() {
            val ctx = appContext
            val base = ctx?.getExternalFilesDir(null)
                ?: ctx?.filesDir
                ?: File(System.getProperty("java.io.tmpdir") ?: "/data/local/tmp")
            return File(File(base, ROOT_NAME), GITHUB_NAME)
        }

    private fun canWriteShared(): Boolean {
        return try {
            val dir = sharedGithubRoot
            if (!dir.exists() && !dir.mkdirs()) false else {
                val probe = File(dir, ".rexgit_probe")
                probe.writeText("ok")
                probe.delete()
                true
            }
        } catch (_: Throwable) { false }
    }

    val usingFallback: Boolean get() = !canWriteShared()
    val githubRoot: File get() = if (canWriteShared()) sharedGithubRoot else privateGithubRoot
    val root: File get() = githubRoot.parentFile ?: githubRoot

    fun ensureDirectories(): File {
        val dir = githubRoot
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun safeSegment(raw: String): String = raw.trim()
        .replace('/', '_').replace('\\', '_')
        .takeIf { it.isNotBlank() && it != "." && it != ".." } ?: "Unknown"

    /** Account-owned repositories: Github/<GitHub nickname>/<repo name>. */
    fun repository(name: String, accountNickname: String? = null): File {
        val owner = safeSegment(accountNickname ?: "Local")
        return File(File(ensureDirectories(), owner), File(name).name)
    }

    /** Global search clones: Github/Global/<upstream owner>/<repo name>. */
    fun globalRepository(owner: String, name: String): File {
        return File(File(File(ensureDirectories(), GLOBAL_NAME), safeSegment(owner)), File(name).name)
    }

    fun downloadsDirectory(): File = File(ensureDirectories(), "Downloads").apply { mkdirs() }

    /**
     * Deliberately scans only the new account/global layouts. Old repositories at
     * Github/<repo>/ are not auto-detected; this workspace is pre-release and is
     * switching to the owner-scoped layout.
     */
    fun scanRepositories(): List<RexGitRepository> {
        val root = ensureDirectories()
        val found = mutableListOf<RexGitRepository>()
        val owners = root.listFiles()?.filter {
            it.isDirectory && !it.name.equals(GLOBAL_NAME, true) &&
                !it.name.equals("Downloads", true) &&
                !it.name.startsWith(".") && !File(it, ".git").exists()
        }.orEmpty()
        owners.forEach { accountDir ->
            accountDir.listFiles()?.filter { it.isDirectory }?.forEach { repoDir ->
                found += RexGitRepository(
                    name = repoDir.name,
                    path = repoDir.absolutePath,
                    isGitRepository = File(repoDir, ".git").exists(),
                    accountNickname = accountDir.name
                )
            }
        }

        val global = File(root, GLOBAL_NAME)
        global.listFiles()?.filter { it.isDirectory }?.forEach { ownerDir ->
            ownerDir.listFiles()?.filter { it.isDirectory }?.forEach { repoDir ->
                found += RexGitRepository(
                    name = repoDir.name,
                    path = repoDir.absolutePath,
                    isGitRepository = File(repoDir, ".git").exists(),
                    globalClone = true,
                    globalOwner = ownerDir.name
                )
            }
        }
        return found.distinctBy { it.path }.sortedWith(
            compareBy<RexGitRepository> { it.accountNickname ?: "Global/${it.globalOwner.orEmpty()}" }
                .thenBy { it.name.lowercase() }
        )
    }
}
