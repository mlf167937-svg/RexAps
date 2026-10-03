package com.rexaps.rexgit

data class RexGitRepository(
    val name: String,
    val path: String,
    val remoteUrl: String? = null,
    val branch: String = "main",
    val isGitRepository: Boolean = false
)

data class RexGitFile(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long = 0L
)

enum class RexGitChangeKind(val symbol: String, val label: String) {
    ADDED("A", "Added"),
    MODIFIED("M", "Modified"),
    DELETED("D", "Deleted"),
    UNTRACKED("?", "New file"),
    CONFLICT("!", "Conflict")
}

data class RexGitChange(
    val path: String,
    val kind: RexGitChangeKind
)

data class RexGitStatus(
    val changes: List<RexGitChange> = emptyList(),
    val error: String? = null
) {
    val hasChanges: Boolean
        get() = changes.isNotEmpty()

    val output: String
        get() = changes.joinToString("\n") { "${it.kind.symbol} ${it.path}" }
}

data class RexGitVersion(
    val versionName: String,
    val versionCode: Int
)

data class RexGitPushResult(
    val success: Boolean,
    val message: String,
    val version: RexGitVersion? = null
)

data class RexGitGithubRepository(
    val name: String,
    val fullName: String,
    val cloneUrl: String,
    val private: Boolean,
    val defaultBranch: String,
    val description: String? = null
)

data class RexGitAuth(
    val username: String,
    val token: String
)

data class RexGitUiState(
    val repositories: List<RexGitRepository> = emptyList(),
    val selectedRepository: RexGitRepository? = null,
    val files: List<RexGitFile> = emptyList(),
    val currentDirectory: String? = null,
    val gitStatus: RexGitStatus = RexGitStatus(),

    val githubRepositories: List<RexGitGithubRepository> = emptyList(),
    val githubUsername: String? = null,
    val isGithubConnected: Boolean = false,

    val editorPath: String? = null,
    val editorContent: String = "",
    val editorDirty: Boolean = false,

    val isLoading: Boolean = false,
    val isRepoLoading: Boolean = false,
    val isPushing: Boolean = false,
    val isPulling: Boolean = false,
    val isSaving: Boolean = false,
    val isGithubLoading: Boolean = false,
    val cloningName: String? = null,

    val storagePath: String = "",
    val storageFallback: Boolean = false,

    val message: String? = null,
    val error: String? = null
) {
    val isCloning: Boolean
        get() = cloningName != null
}
