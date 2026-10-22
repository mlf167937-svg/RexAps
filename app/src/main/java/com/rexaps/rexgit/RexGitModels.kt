package com.rexaps.rexgit

data class RexGitRepository(
    val name: String,
    val path: String,
    val remoteUrl: String? = null,
    val branch: String = "main",
    val isGitRepository: Boolean = false,
    /** Account directory that contains this repository, when it is an account repo. */
    val accountNickname: String? = null,
    /** True for repositories cloned from global GitHub search. */
    val globalClone: Boolean = false,
    val globalOwner: String? = null
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
    val description: String? = null,
    val stars: Int = 0,
    val htmlUrl: String? = null
)

data class RexGitWorkflow(
    val id: Long,
    val name: String,
    val path: String,
    val state: String,
    val htmlUrl: String
)

data class RexGitWorkflowRun(
    val id: Long,
    val name: String,
    val title: String,
    val event: String,
    val status: String,
    val conclusion: String?,
    val createdAt: String,
    val updatedAt: String,
    val htmlUrl: String,
    val runNumber: Int
)

data class RexGitArtifact(
    val id: Long,
    val name: String,
    val sizeInBytes: Long,
    val expired: Boolean,
    val archiveDownloadUrl: String,
    val workflowRunId: Long,
    val workflowName: String
)

data class RexGitReleaseAsset(
    val id: Long,
    val name: String,
    val sizeInBytes: Long,
    val downloadUrl: String,
    val contentType: String
)

data class RexGitRelease(
    val id: Long,
    val tagName: String,
    val name: String,
    val body: String,
    val publishedAt: String,
    val htmlUrl: String,
    val draft: Boolean,
    val prerelease: Boolean,
    val assets: List<RexGitReleaseAsset> = emptyList()
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
    val githubSearchMode: Boolean = false,
    val githubSearchQuery: String = "",
    val githubSearchResults: List<RexGitGithubRepository> = emptyList(),
    val githubUsername: String? = null,
    val isGithubConnected: Boolean = false,
    /** Usernames of every connected account. [activeAccount] indexes into this list. */
    val accounts: List<String> = emptyList(),
    val activeAccount: Int = 0,

    /** Optional GitDev metadata configuration for the selected repository; standard Push never depends on it. */
    val gitDev: RexGitGitDevConfig = RexGitGitDevConfig(),

    val readmeContent: String = "",
    val workflows: List<RexGitWorkflow> = emptyList(),
    val workflowRuns: List<RexGitWorkflowRun> = emptyList(),
    val workflowArtifacts: List<RexGitArtifact> = emptyList(),
    val releases: List<RexGitRelease> = emptyList(),
    val isRepoHubLoading: Boolean = false,
    val repoHubError: String? = null,
    val downloadingArtifact: String? = null,

    val editorPath: String? = null,
    val editorContent: String = "",
    val editorDirty: Boolean = false,

    val isLoading: Boolean = false,
    val isRepoLoading: Boolean = false,
    val isPushing: Boolean = false,
    val pushStage: String = "Checking Files...",
    val pushDetail: String = "",
    val pushLog: List<String> = emptyList(),
    val pushFinished: Boolean = false,
    val pushSuccess: Boolean = false,
    val pushError: String? = null,
    val pushCommitMessage: String? = null,
    val pushVersion: RexGitVersion? = null,
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