package com.rexaps.rexgit

import androidx.lifecycle.viewModelScope
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal fun RexGitViewModel.__splitImpl_selectRepository(repository: RexGitRepository) {
        if (_state.value.isRepoLoading) return

        launchIO {
            _state.update {
                it.copy(
                    isRepoLoading = true,
                    error = null
                )
            }

            try {
                val savedAccount = accountPrefs.getString(RexGitViewModel.REPO_ACCOUNT_PREFIX + repository.path, null)
                val desiredAccount = savedAccount ?: repository.accountNickname
                val desiredIndex = accounts.indexOfFirst { it.username.equals(desiredAccount, ignoreCase = true) }
                if (desiredIndex >= 0 && desiredIndex != activeIndex) {
                    activeIndex = desiredIndex
                    accountPrefs.edit().putString(RexGitViewModel.ACTIVE_KEY, accounts[desiredIndex].username).apply()
                    publishAccounts()
                }
                val selectedAccountName = accounts.getOrNull(activeIndex)?.username
                if (selectedAccountName != null) {
                    accountPrefs.edit().putString(RexGitViewModel.REPO_ACCOUNT_PREFIX + repository.path, selectedAccountName).apply()
                }

                val isGit = File(repository.path, ".git").exists()

                val branch =
                    if (isGit) {
                        repositoryManager.currentBranch(repository)
                    } else {
                        ""
                    }

                val remote =
                    if (isGit) {
                        repositoryManager.remote(repository)
                    } else {
                        null
                    }

                val files = repositoryManager.files(repository)

                val status =
                    if (isGit) {
                        repositoryManager.status(repository)
                    } else {
                        RexGitStatus()
                    }

                editorBuffer = ""

                _state.update {
                    it.copy(
                        selectedRepository = repository.copy(
                            branch = branch,
                            remoteUrl = remote,
                            isGitRepository = isGit
                        ),
                        files = files,
                        currentDirectory = null,
                        gitStatus = status,
                        editorPath = null,
                        editorContent = "",
                        editorDirty = false,
                        gitDev = gitDevStore.load(repository.path),
                        readmeContent = readReadme(repository.path),
                        workflows = emptyList(),
                        workflowRuns = emptyList(),
                        workflowArtifacts = emptyList(),
                        releases = emptyList(),
                        repositoryMetadata = null,
                        repositoryTags = emptyList(),
                        isRepoHubLoading = true,
                        repoHubError = null,
                        actionsError = null,
                        releasesError = null,
                        downloadError = null,
                        downloadedBytes = 0L,
                        downloadTotalBytes = null,
                        message = null
                    )
                }
                loadRepositoryGithubData(repository.copy(branch = branch, remoteUrl = remote, isGitRepository = isGit))
            } finally {
                _state.update {
                    it.copy(isRepoLoading = false)
                }
            }
        }
    }

internal fun RexGitViewModel.__splitImpl_loadRepositoryGithubData(repo: RexGitRepository) {
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

internal fun RexGitViewModel.__splitImpl_cloneRepository(
        repository: RexGitGithubRepository
    ) {
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

internal fun RexGitViewModel.__splitImpl_searchGithubRepositories(query: String) {
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
