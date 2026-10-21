package com.rexaps.rexgit

import android.content.Context
import java.io.File

/**
 * GitDev mode settings for ONE repository.
 *
 * GitDev replaces the old hard-coded "com/rexaps" lookup. The user tells
 * RexGit where the app's package lives, so any APK project works:
 *
 *   com/rexaps      id/myapp/tools      org/example/notes ...
 */
data class RexGitGitDevConfig(
    val enabled: Boolean = false,
    /** Package path below src/main/java or src/main/kotlin, e.g. "com/rexaps". */
    val packagePath: String = "",
    /** Optional Gradle module folder, e.g. "app" or "feature-login". Blank = auto. */
    val module: String = ""
) {
    /** Push is only unlocked when GitDev is on AND a package path is filled in. */
    val isReady: Boolean
        get() = enabled && packagePath.isNotBlank()
}

object RexGitGitDev {

    private val sourceSets = listOf(
        "src/main/java",
        "src/main/kotlin"
    )

    /**
     * Accepts "com.rexaps", "com/rexaps/", "/com/rexaps" or even a full
     * "app/src/main/java/com/rexaps" and returns "com/rexaps".
     */
    fun normalizePackagePath(raw: String): String {
        var text = raw.trim().replace('\\', '/')

        sourceSets.forEach { set ->
            val marker = "$set/"
            if (text.contains(marker)) {
                text = text.substringAfter(marker)
            }
        }

        if (!text.contains('/')) {
            text = text.replace('.', '/')
        }

        return text
            .split('/')
            .map { it.trim() }
            .filter { it.isNotEmpty() && it != "." && it != ".." }
            .joinToString("/")
    }

    fun normalizeModule(raw: String): String {
        return raw
            .trim()
            .replace('\\', '/')
            .split('/')
            .map { it.trim() }
            .filter { it.isNotEmpty() && it != "." && it != ".." }
            .joinToString("/")
    }

    fun normalize(config: RexGitGitDevConfig): RexGitGitDevConfig {
        return config.copy(
            packagePath = normalizePackagePath(config.packagePath),
            module = normalizeModule(config.module)
        )
    }

    /** Every source directory (relative to the repository root) that is scanned. */
    fun sourceRoots(config: RexGitGitDevConfig): List<String> {
        val pkg = normalizePackagePath(config.packagePath)
        if (pkg.isBlank()) return emptyList()

        val module = normalizeModule(config.module)

        // Blank module: try the standard "app" module first, then the project root.
        val modules =
            if (module.isBlank()) listOf("app", "")
            else listOf(module)

        return modules.flatMap { m ->
            sourceSets.map { set ->
                listOf(m, set, pkg)
                    .filter { it.isNotEmpty() }
                    .joinToString("/")
            }
        }
    }

    /** Sub-folders directly below the package path. Hidden folders are ignored. */
    fun detectFolders(
        repositoryDir: File,
        config: RexGitGitDevConfig
    ): List<String> {
        val folders = linkedSetOf<String>()

        sourceRoots(config)
            .asSequence()
            .map { File(repositoryDir, it) }
            .filter { it.isDirectory }
            .forEach { sourceRoot ->
                sourceRoot.listFiles()
                    ?.asSequence()
                    ?.filter { it.isDirectory && !it.name.startsWith(".") }
                    ?.mapTo(folders) { it.name }
            }

        return folders.sortedBy { it.lowercase() }
    }

    /** Maps a changed file path to its first folder below the package path. */
    fun folderFromChangedPath(
        path: String,
        config: RexGitGitDevConfig
    ): String? {
        val normalized = path.replace('\\', '/').trimStart('/')

        val marker = sourceRoots(config)
            .map { "$it/" }
            .firstOrNull { normalized.startsWith(it) }
            ?: return null

        val rest = normalized.removePrefix(marker)
        if (rest.isBlank() || rest.startsWith("MainActivity.kt")) return null

        val folder = rest.substringBefore('/').substringBefore('\\')
        return folder.takeIf { it.isNotBlank() && it != "MainActivity.kt" }
    }
}

/** Remembers each repository's GitDev settings (keyed by repository folder name). */
class RexGitGitDevStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("rexgit_gitdev", Context.MODE_PRIVATE)

    fun load(repositoryName: String): RexGitGitDevConfig {
        return RexGitGitDevConfig(
            enabled = prefs.getBoolean("$repositoryName.enabled", false),
            packagePath = prefs.getString("$repositoryName.package", "").orEmpty(),
            module = prefs.getString("$repositoryName.module", "").orEmpty()
        )
    }

    fun save(repositoryName: String, config: RexGitGitDevConfig) {
        prefs.edit()
            .putBoolean("$repositoryName.enabled", config.enabled)
            .putString("$repositoryName.package", config.packagePath)
            .putString("$repositoryName.module", config.module)
            .apply()
    }
}
