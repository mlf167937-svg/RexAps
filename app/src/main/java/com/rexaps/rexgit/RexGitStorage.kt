package com.rexaps.rexgit

import android.content.Context
import android.os.Environment
import java.io.File

object RexGitStorage {

    private const val ROOT_NAME = "RexAps"
    private const val GITHUB_NAME = "Github"

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private val sharedGithubRoot: File
        get() = File(
            File(
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                ),
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
            if (!dir.exists() && !dir.mkdirs()) return false
            val probe = File(dir, ".rexgit_probe")
            probe.writeText("ok")
            probe.delete()
            true
        } catch (_: Throwable) {
            false
        }
    }

    /** True when shared Download storage is not writable and private app storage is used. */
    val usingFallback: Boolean
        get() = !canWriteShared()

    val githubRoot: File
        get() = if (canWriteShared()) sharedGithubRoot else privateGithubRoot

    val root: File
        get() = githubRoot.parentFile ?: githubRoot

    fun ensureDirectories(): File {
        val dir = githubRoot
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun repository(name: String): File {
        return File(githubRoot, File(name).name)
    }

    fun scanRepositories(): List<RexGitRepository> {
        val dir = ensureDirectories()

        return dir
            .listFiles()
            ?.filter { it.isDirectory }
            ?.sortedBy { it.name.lowercase() }
            ?.map {
                RexGitRepository(
                    name = it.name,
                    path = it.absolutePath,
                    isGitRepository = File(it, ".git").exists()
                )
            }
            ?: emptyList()
    }
}
