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

data class RexGitUiState(
    val repositories: List<RexGitRepository> = emptyList(),
    val selectedRepository: RexGitRepository? = null,
    val files: List<RexGitFile> = emptyList(),
    val gitStatus: RexGitStatus = RexGitStatus(),
    val isLoading: Boolean = false,
    val isPushing: Boolean = false,
    val message: String? = null,
    val error: String? = null
)
