package com.rexaps.rexgit

import java.io.File

enum class RexGitCommitType(val label: String) {
    FIX("Fix"),
    CHANGE("Change"),
    DELETE("Delete"),
    ADD("Add")
}

/** Detects real package folders below the package path chosen in GitDev mode. */
object RexGitCommitMeta {

    fun detectFolders(
        repository: RexGitRepository?,
        changes: List<RexGitChange> = emptyList(),
        gitDev: RexGitGitDevConfig
    ): List<String> {
        val root = repository?.let { File(it.path) } ?: return emptyList()
        if (!gitDev.isReady) return emptyList()

        val folders = linkedSetOf<String>()

        RexGitGitDev.detectFolders(root, gitDev).forEach(folders::add)

        // A changed path is also used as a real-world hint, but only when it
        // actually belongs to <package path>/<folder>/... . MainActivity.kt is ignored.
        changes.forEach { change ->
            RexGitGitDev.folderFromChangedPath(change.path, gitDev)?.let(folders::add)
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
        changes: List<RexGitChange>,
        gitDev: RexGitGitDevConfig
    ): String? {
        val detected = detectFolders(repository, changes, gitDev)
        if (detected.isEmpty()) return null

        val changedFolders = changes
            .mapNotNull { RexGitGitDev.folderFromChangedPath(it.path, gitDev) }
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
}
