package com.rexaps.rexgit

import kotlinx.coroutines.flow.update

internal fun RexGitViewModel.repositorySlug(repo: RexGitRepository): Pair<String, String>? {
    val remote = repo.remoteUrl?.trim().orEmpty()
    val match = Regex("""(?:github\.com[:/])([^/]+)/([^/]+?)(?:\.git)?/?$""", RegexOption.IGNORE_CASE).find(remote)
    if (match != null) return match.groupValues[1] to match.groupValues[2]
    val globalOwner = repo.globalOwner
    if (!globalOwner.isNullOrBlank()) return globalOwner to repo.name
    val account = repo.accountNickname
    if (!account.isNullOrBlank()) return account to repo.name
    return null
}

internal fun RexGitViewModel.refreshRepositoryHubImpl() {
    val repo = _state.value.selectedRepository ?: return
    _state.update { it.copy(isRepoHubLoading = true, repoHubError = null, actionsError = null, releasesError = null) }
    loadRepositoryGithubData(repo)
}
