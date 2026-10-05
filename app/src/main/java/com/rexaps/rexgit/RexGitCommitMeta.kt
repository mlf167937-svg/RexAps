package com.rexaps.rexgit

import java.io.File

enum class RexGitCommitType(val label: String) {
    FIX("Fix"),
    CHANGE("Change"),
    DELETE("Delete"),
    ADD("Add")
}

/** Detects real package folders directly below com/rexaps. */
object RexGitCommitMeta {
    private val sourceRoots = listOf(
        "app/src/main/java/com/rexaps",
        "app/src/main/kotlin/com/rexaps",
        "src/main/java/com/rexaps",
        "src/main/kotlin/com/rexaps"
    )

    fun detectFolders(
        repository: RexGitRepository?,
        changes: List<RexGitChange> = emptyList()
    ): List<String> {
        val root = repository?.let { File(it.path) } ?: return emptyList()
        val folders = linkedSetOf<String>()

        sourceRoots
            .asSequence()
            .map { File(root, it) }
            .filter { it.isDirectory }
            .forEach { sourceRoot ->
                sourceRoot.listFiles()
                    ?.asSequence()
                    ?.filter { it.isDirectory && !it.name.startsWith(".") }
                    ?.mapTo(folders) { it.name }
            }

        // A changed path is also used as a real-world hint, but only when it
        // actually belongs to com/rexaps/<folder>/... . MainActivity.kt is ignored.
        changes.forEach { change ->
            folderFromChangedPath(change.path)?.let(folders::add)
        }

        return folders.sortedBy { it.lowercase() }
    }

    fun inferType(changes: List<RexGitChange>): RexGitCommitType {
        val kinds = changes.map { it.kind }.toSet()
        return when {
            kinds.isNotEmpty() && kinds.all { it == RexGitChangeKind.ADDED || it == RexGitChangeKind.UNTRACKED } -> RexGitCommitType.ADD
            kinds.isNotEmpty() && kinds.all { it == RexGitChangeKind.DELETED } -> RexGitCommitType.DELETE
            kinds.isNotEmpty() && kinds.all { it == RexGitChangeKind.MODIFIED } -> RexGitCommitType.FIX
            else -> RexGitCommitType.CHANGE
        }
    }

    fun inferFolder(
        repository: RexGitRepository?,
        changes: List<RexGitChange>
    ): String? {
        val detected = detectFolders(repository, changes)
        if (detected.isEmpty()) return null

        val changedFolders = changes
            .mapNotNull { folderFromChangedPath(it.path) }
            .distinct()

        return changedFolders.singleOrNull { it in detected }
            ?: detected.singleOrNull()
            ?: detected.firstOrNull()
    }

    fun buildMessage(
        version: RexGitVersion?,
        type: RexGitCommitType,
        folder: String,
        description: String
    ): String {
        val prefix = version?.versionName?.let { "[$it] " }.orEmpty()
        return "$prefix${type.label}: $folder: ${description.trim()}"
    }

    private fun folderFromChangedPath(path: String): String? {
        val normalized = path.replace('\\', '/').trimStart('/')
        val markers = listOf(
            "app/src/main/java/com/rexaps/",
            "app/src/main/kotlin/com/rexaps/",
            "src/main/java/com/rexaps/",
            "src/main/kotlin/com/rexaps/"
        )

        val marker = markers.firstOrNull { normalized.startsWith(it) } ?: return null
        val rest = normalized.removePrefix(marker)
        if (rest.isBlank() || rest.startsWith("MainActivity.kt")) return null

        val folder = rest.substringBefore('/').substringBefore('\\')
        return folder.takeIf { it.isNotBlank() && it != "MainActivity.kt" }
    }
}
