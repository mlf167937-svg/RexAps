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

        // A practically unbounded account count; no artificial two-account limit.
        const val MAX_ACCOUNTS = Int.MAX_VALUE

        /** New format: Account-1-Username / Account-1-Token. */
        private val ACCOUNT_REGEX = Regex(
            """^\s*Account-(\d+)-(Username|Token)\s*=\s*["']?([^"'\r\n]*)["']?\s*$""",
            RegexOption.IGNORE_CASE
        )

        /** Backward compatibility with username=, token=, username2=, token_2=, etc. */
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
     * Reads any number of accounts from token.env. The preferred format is:
     * Account-1-Username=octocat
     * Account-1-Token=ghp_...
     *
     * Older username/token and username2/token2 formats remain readable.
     */
    fun loadAccounts(): List<RexGitEnvAccount> {
        val file = readableFile() ?: return emptyList()
        val content = try {
            file.readText(Charsets.UTF_8)
        } catch (_: Throwable) {
            return emptyList()
        }

        val usernames = sortedMapOf<Int, String>()
        val tokens = sortedMapOf<Int, String>()
        content.lineSequence().forEach { line ->
            val modern = ACCOUNT_REGEX.find(line)
            if (modern != null) {
                val slot = modern.groupValues[1].toIntOrNull() ?: return@forEach
                if (slot < 1) return@forEach
                val kind = modern.groupValues[2].lowercase()
                val value = modern.groupValues[3].trim()
                if (value.isNotEmpty()) {
                    if (kind == "token") tokens.putIfAbsent(slot, value)
                    else usernames.putIfAbsent(slot, value)
                }
                return@forEach
            }

            val legacy = KEY_REGEX.find(line) ?: return@forEach
            val kind = legacy.groupValues[1].lowercase()
            val slot = legacy.groupValues[2].toIntOrNull() ?: 1
            val value = legacy.groupValues[3].trim()
            if (slot < 1 || value.isEmpty()) return@forEach
            if (kind == "token") tokens.putIfAbsent(slot, value)
            else usernames.putIfAbsent(slot, value)
        }

        return tokens.keys.sorted()
            .mapNotNull { slot -> tokens[slot]?.let { RexGitEnvAccount(usernames[slot], it) } }
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

    /** Writes all connected accounts using the explicit Account-N-* format. */
    fun saveAccounts(accounts: List<RexGitEnvAccount>): String {
        val target = readableFile() ?: tokenFile()
        return try {
            write(target, accounts)
            target.absolutePath
        } catch (_: Throwable) {
            val fallback = privateTokenFile()
            write(fallback, accounts)
            fallback.absolutePath
        }
    }

    private fun write(file: File, accounts: List<RexGitEnvAccount>) {
        file.parentFile?.mkdirs()
        val kept = if (file.isFile) {
            file.readText(Charsets.UTF_8)
                .lineSequence()
                .filterNot { ACCOUNT_REGEX.matches(it) || KEY_REGEX.matches(it) }
                .toList()
                .dropLastWhile { it.isBlank() }
        } else emptyList()

        val out = StringBuilder()
        kept.forEach { out.append(it).append('\n') }
        if (kept.isNotEmpty() && accounts.isNotEmpty()) out.append('\n')

        accounts.forEachIndexed { index, account ->
            val slot = index + 1
            val username = account.username.orEmpty().trim().replace('\n', ' ').replace('\r', ' ')
            val token = account.token.trim().replace('\n', ' ').replace('\r', ' ')
            if (username.isNotEmpty()) out.append("Account-$slot-Username=$username\n")
            out.append("Account-$slot-Token=$token\n")
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
