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
    val isDirectory: Boolean
)

data class RexGitStatus(
    val output: String = "",
    val modifiedFiles: List<String> = emptyList(),
    val stagedFiles: List<String> = emptyList(),
    val hasChanges: Boolean = false
)

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
    val defaultBranch: String
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
    val isPushing: Boolean = false,
    val isCloning: Boolean = false,
    val isSaving: Boolean = false,
    val isGithubLoading: Boolean = false,

    val message: String? = null,
    val error: String? = null
)
