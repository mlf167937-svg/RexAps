package com.rexaps.rexgit

import android.content.Context
import java.io.File

class RexGitAuthStore(
    context: Context
) {
    private val appContext = context.applicationContext

    companion object {
        private const val TOKEN_FILE_NAME = "token.env"
        private const val TOKEN_KEY = "token"

        private const val DOWNLOAD_DIR = "Download"
        private const val ROOT_DIR = "RexAps"
        private const val GITHUB_DIR = "Github"
    }

    private fun tokenFile(): File {
        return File(
            File(
                File(
                    android.os.Environment.getExternalStorageDirectory(),
                    DOWNLOAD_DIR
                ),
                ROOT_DIR
            ),
            GITHUB_DIR
        ).resolve(TOKEN_FILE_NAME)
    }

    /**
     * Reads:
     *
     * token="ghp_xxxxxxxxx"
     *
     * from:
     *
     * /sdcard/Download/RexAps/Github/token.env
     */
    fun loadToken(): String? {
        val file = tokenFile()

        if (!file.isFile) {
            return null
        }

        return try {
            val content = file.readText(Charsets.UTF_8)

            val regex = Regex(
                """(?m)^\s*token\s*=\s*["']?([^"'\r\n]+)["']?\s*$"""
            )

            regex.find(content)
                ?.groupValues
                ?.getOrNull(1)
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * GitHub username is obtained from the GitHub API,
     * so we don't need to store it locally.
     */
    fun load(): RexGitAuth? {
        return null
    }

    fun clear() {
        // Token is managed externally in token.env.
        // Disconnecting from the UI does not delete the file.
    }

    fun tokenPath(): String {
        return tokenFile().absolutePath
    }
}