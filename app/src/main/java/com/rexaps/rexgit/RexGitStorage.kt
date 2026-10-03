package com.rexaps.rexgit

import android.os.Environment
import java.io.File

object RexGitStorage {

    private const val ROOT_NAME = "RexAps"
    private const val GITHUB_NAME = "Github"

    val root: File
        get() = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            ),
            ROOT_NAME
        )

    val githubRoot: File
        get() = File(root, GITHUB_NAME)

    fun ensureDirectories(): File {
        if (!githubRoot.exists()) {
            githubRoot.mkdirs()
        }

        return githubRoot
    }

    fun repository(name: String): File {
        return File(githubRoot, name)
    }

    fun scanRepositories(): List<RexGitRepository> {
        ensureDirectories()

        return githubRoot
            .listFiles()
            ?.filter { it.isDirectory }
            ?.sortedBy { it.name.lowercase() }
            ?.map { dir ->
                RexGitRepository(
                    name = dir.name,
                    path = dir.absolutePath,
                    remoteUrl = null,
                    branch = "main",
                    isGitRepository = File(dir, ".git").exists()
                )
            }
            ?: emptyList()
    }
}
