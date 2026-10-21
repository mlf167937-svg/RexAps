package com.rexaps.rexgit

import android.content.Context
import java.io.File

/** One account as written in token.env. The username is optional there. */
data class RexGitEnvAccount(
    val username: String?,
    val token: String
)

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

        const val MAX_ACCOUNTS = 2

        /** username=, user=, token= with an optional slot: token2=, token_2=, username2= ... */
        private val KEY_REGEX = Regex(
            """^\s*(username|user|token)_?(\d*)\s*=\s*["']?([^"'\r\n]*)["']?\s*$""",
            RegexOption.IGNORE_CASE
        )
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

    /** Used only when shared Download storage is not writable. */
    private fun privateTokenFile(): File {
        val base = appContext.getExternalFilesDir(null) ?: appContext.filesDir
        return File(File(File(base, ROOT_DIR), GITHUB_DIR), TOKEN_FILE_NAME)
    }

    private fun readableFile(): File? {
        val shared = tokenFile()
        if (shared.isFile) return shared

        val private = privateTokenFile()
        if (private.isFile) return private

        return null
    }

    /**
     * token.env supports up to two accounts:
     *
     * username="rex"
     * token="ghp_xxxxxxxxx"
     *
     * username2="rex-work"
     * token2="ghp_yyyyyyyyy"
     *
     * A file that only has token="..." still works (username comes from GitHub).
     */
    fun loadAccounts(): List<RexGitEnvAccount> {
        val file = readableFile() ?: return emptyList()

        val content = try {
            file.readText(Charsets.UTF_8)
        } catch (_: Throwable) {
            return emptyList()
        }

        val usernames = mutableMapOf<Int, String>()
        val tokens = mutableMapOf<Int, String>()

        content.lineSequence().forEach { line ->
            val match = KEY_REGEX.find(line) ?: return@forEach

            val key = match.groupValues[1].lowercase()
            val slot = match.groupValues[2].toIntOrNull() ?: 1
            val value = match.groupValues[3].trim()

            if (value.isEmpty() || slot !in 1..MAX_ACCOUNTS) return@forEach

            if (key == TOKEN_KEY) tokens.putIfAbsent(slot, value)
            else usernames.putIfAbsent(slot, value)
        }

        return tokens.keys
            .sorted()
            .map { slot -> RexGitEnvAccount(usernames[slot], tokens.getValue(slot)) }
            .distinctBy { it.token }
    }

    /**
     * Reads the first token (kept for older callers):
     *
     * token="ghp_xxxxxxxxx"
     */
    fun loadToken(): String? {
        return loadAccounts().firstOrNull()?.token
    }

    /**
     * Writes up to two accounts to token.env. Lines that are not account
     * entries (comments, other keys) are kept untouched.
     *
     * Returns the path that was written. If /Download/RexAps/Github isn't
     * writable the file is written to private app storage instead.
     */
    fun saveAccounts(accounts: List<RexGitEnvAccount>): String {
        val limited = accounts.take(MAX_ACCOUNTS)

        val target = readableFile() ?: tokenFile()

        return try {
            write(target, limited)
            target.absolutePath
        } catch (_: Throwable) {
            val fallback = privateTokenFile()
            write(fallback, limited)
            fallback.absolutePath
        }
    }

    private fun write(file: File, accounts: List<RexGitEnvAccount>) {
        file.parentFile?.mkdirs()

        val kept = if (file.isFile) {
            file.readText(Charsets.UTF_8)
                .lineSequence()
                .filterNot { KEY_REGEX.matches(it) }
                .toList()
                .dropLastWhile { it.isBlank() }
        } else {
            emptyList()
        }

        val out = StringBuilder()

        kept.forEach { out.append(it).append('\n') }
        if (kept.isNotEmpty() && accounts.isNotEmpty()) out.append('\n')

        accounts.forEachIndexed { index, account ->
            val suffix = if (index == 0) "" else "${index + 1}"

            if (!account.username.isNullOrBlank()) {
                out.append("username$suffix=\"${account.username}\"\n")
            }
            out.append("token$suffix=\"${account.token}\"\n")

            if (index != accounts.lastIndex) out.append('\n')
        }

        file.writeText(out.toString(), Charsets.UTF_8)
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
        return (readableFile() ?: tokenFile()).absolutePath
    }
}
