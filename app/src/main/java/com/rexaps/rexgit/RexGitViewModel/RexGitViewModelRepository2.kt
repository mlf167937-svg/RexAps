package com.rexaps.rexgit

import kotlinx.coroutines.flow.update
import java.io.File

internal fun RexGitViewModel.__split_readReadme_23(repositoryPath: String): String {
        return try {
            val root = File(repositoryPath)
            val readme = root.listFiles()?.firstOrNull { it.isFile && it.name.equals("README.md", true) }
                ?: return ""
            if (readme.length() > MAX_EDITOR_BYTES) return "README is too large to preview in RexGit. Open the file from Code to inspect it."
            readme.readText(Charsets.UTF_8)
        } catch (_: Throwable) { "" }
    }

internal fun RexGitViewModel.__split_repositorySlug_24(repo: RexGitRepository): Pair<String, String>? {
        val remote = repo.remoteUrl?.trim().orEmpty()
        val match = Regex("""(?:github\.com[:/])([^/]+)/([^/]+?)(?:\.git)?/?$""", RegexOption.IGNORE_CASE).find(remote)
        if (match != null) return match.groupValues[1] to match.groupValues[2]
        val globalOwner = repo.globalOwner
        if (!globalOwner.isNullOrBlank()) return globalOwner to repo.name
        val account = repo.accountNickname
        if (!account.isNullOrBlank()) return account to repo.name
        return null
    }

fun RexGitViewModel.__split_refreshRepositoryHub_26() {
        val repo = _state.value.selectedRepository ?: return
        _state.update { it.copy(isRepoHubLoading = true, repoHubError = null, actionsError = null, releasesError = null) }
        loadRepositoryGithubData(repo)
    }
