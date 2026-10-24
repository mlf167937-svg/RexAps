package com.rexaps.rexgit

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

internal fun RexGitViewModel.loadRepositoryGithubData(repo: RexGitRepository) {
    viewModelScope.launch(Dispatchers.IO) {
        try {
            val slug = repositorySlug(repo)
            if (slug == null) {
                _state.update { it.copy(isRepoHubLoading = false, repoHubError = "No GitHub remote is configured for this repository.") }
                return@launch
            }
            val auth = try { externalGithubAuth() } catch (_: Throwable) { null }
            var actionsError: String? = null
            var releasesError: String? = null
            val metadata = try { github.getRepository(auth?.token, slug.first, slug.second) } catch (t: Throwable) {
                null
            }
            val snapshot = try { github.loadActions(auth?.token, slug.first, slug.second) } catch (t: Throwable) {
                actionsError = t.readableMessage("Could not load GitHub Actions")
                RexGitActionsSnapshot(emptyList(), emptyList(), emptyList())
            }
            val releaseData = try { github.listReleases(auth?.token, slug.first, slug.second) } catch (t: Throwable) {
                releasesError = t.readableMessage("Could not load Releases")
                emptyList()
            }
            val tags = try { github.listTags(auth?.token, slug.first, slug.second) } catch (_: Throwable) { emptyList() }
            _state.update { cur ->
                if (cur.selectedRepository?.path != repo.path) cur else cur.copy(
                    workflows = snapshot.workflows,
                    workflowRuns = snapshot.runs,
                    workflowArtifacts = snapshot.artifacts,
                    releases = releaseData,
                    repositoryMetadata = metadata,
                    repositoryTags = tags,
                    isRepoHubLoading = false,
                    repoHubError = if (metadata == null && snapshot.workflows.isEmpty() && releaseData.isEmpty() && tags.isEmpty()) {
                        "GitHub repository data is unavailable. Verify the owner/repository and selected account access."
                    } else null,
                    actionsError = actionsError,
                    releasesError = releasesError
                )
            }
        } catch (t: Throwable) {
            _state.update { cur -> if (cur.selectedRepository?.path != repo.path) cur else cur.copy(isRepoHubLoading = false, repoHubError = t.readableMessage("Could not load GitHub details")) }
        }
    }
}

internal fun RexGitViewModel.downloadGithubAssetImpl(url: String, rawName: String) {
    if (_state.value.selectedRepository == null) return
    val name = rawName.substringAfterLast('/').replace(Regex("[^A-Za-z0-9._-]"), "_").take(120).ifBlank { "github-download.zip" }
    launchIO {
        _state.update {
            it.copy(
                downloadingArtifact = name,
                downloadedBytes = 0L,
                downloadTotalBytes = null,
                downloadError = null
            )
        }
        try {
            val auth = try { externalGithubAuth() } catch (_: Throwable) { null }
            val downloadDir = RexGitStorage.artifactsDirectory()
            val requested = File(downloadDir, name)
            val output = if (!requested.exists()) requested else {
                val base = requested.nameWithoutExtension
                val extension = requested.extension.takeIf { it.isNotBlank() }?.let { ".$it" }.orEmpty()
                var index = 2
                var candidate = File(downloadDir, "$base-$index$extension")
                while (candidate.exists()) { index++; candidate = File(downloadDir, "$base-$index$extension") }
                candidate
            }
            val accept = if (url.contains("/releases/assets/")) "application/octet-stream" else "application/vnd.github+json"
            github.download(url, auth?.token, output, accept) { received, total ->
                _state.update { cur ->
                    cur.copy(
                        downloadedBytes = received,
                        downloadTotalBytes = total
                    )
                }
            }
            _state.update { it.copy(message = "Downloaded $name to ${output.parentFile?.absolutePath}") }
        } catch (t: Throwable) {
            _state.update { it.copy(downloadError = t.readableMessage("Download failed")) }
        } finally {
            _state.update {
                it.copy(
                    downloadingArtifact = null,
                    downloadedBytes = 0L,
                    downloadTotalBytes = null
                )
            }
        }
    }
}

internal fun RexGitViewModel.fetchGithubRepositories(auth: RexGitAuth) {
    _state.update {
        it.copy(isGithubLoading = true)
    }

    try {
        val repos = github.listRepositories(
            auth.token
        )

        _state.update {
            it.copy(
                githubRepositories = repos
            )
        }
    } catch (t: Throwable) {
        if (t is CancellationException) {
            throw t
        }

        fail(
            "Could not load GitHub repositories:\n" +
                t.readableMessage("Network error")
        )
    } finally {
        _state.update {
            it.copy(isGithubLoading = false)
        }
    }
}

internal fun RexGitViewModel.searchGithubRepositoriesImpl(query: String) {
    val clean = query.trim()
    if (clean.isBlank()) {
        _state.update { it.copy(githubSearchMode = false, githubSearchQuery = "", githubSearchResults = emptyList()) }
        return
    }
    launchIO {
        _state.update { it.copy(isGithubLoading = true, githubSearchMode = true, repoHubError = null) }
        try {
            val auth = externalGithubAuth() ?: throw missingAuthError()
            val results = github.searchRepositories(auth.token, clean)
            _state.update {
                it.copy(
                    githubSearchMode = true,
                    githubSearchQuery = clean,
                    githubSearchResults = results,
                    message = "Found ${results.size} repositories for \"$clean\""
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            fail("GitHub repository search failed:\n" + t.readableMessage("Search failed"))
        } finally {
            _state.update { it.copy(isGithubLoading = false) }
        }
    }
}

internal fun RexGitViewModel.reloadGithubImpl() {
    launchIO {
        try {
            val auth = externalGithubAuth(reload = true)

            if (auth == null) {
                fail(missingAuthError().message ?: "No GitHub account connected.")
                return@launchIO
            }

            _state.update {
                it.copy(
                    githubUsername = auth.username,
                    isGithubConnected = true
                )
            }

            fetchGithubRepositories(auth)
        } catch (t: Throwable) {
            if (t is CancellationException) {
                throw t
            }

            fail(
                "Could not load GitHub repositories:\n" +
                    t.readableMessage("Network error")
            )
        }
    }
}

internal fun RexGitViewModel.cloneRepositoryImpl(repository: RexGitGithubRepository) {
    if (_state.value.isCloning) return

    launchIO {
        _state.update {
            it.copy(
                cloningName = repository.name,
                error = null
            )
        }

        try {
            val auth = externalGithubAuth()
                ?: throw missingAuthError()

            val upstreamOwner = repository.fullName.substringBefore('/')
            val result = repositoryManager.clone(
                url = repository.cloneUrl,
                name = repository.name,
                username = auth.username,
                token = auth.token,
                accountNickname = auth.username,
                globalOwner = upstreamOwner.takeIf { !it.equals(auth.username, ignoreCase = true) }
            )

            if (!result.success) {
                fail(result.message)
            } else {
                val repos = repositoryManager.scan()

                _state.update {
                    it.copy(
                        repositories = repos,
                        message = result.message
                    )
                }
            }
        } finally {
            _state.update {
                it.copy(cloningName = null)
            }
        }
    }
}
