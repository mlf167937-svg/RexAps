package com.rexaps.rexgit

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

internal fun RexGitViewModel.__split_missingAuthError_2(): IllegalStateException {
        return IllegalStateException(
            "No GitHub account connected.\n\n" +
                "Open the GitHub dialog and log in with your username and token, " +
                "or create:\n" +
                authStore.tokenPath()
        )
    }

internal fun RexGitViewModel.__split_publishAccounts_3() {
        val list = accounts
        val index = activeIndex.coerceIn(0, (list.size - 1).coerceAtLeast(0))
        activeIndex = index

        _state.update {
            it.copy(
                accounts = list.map { auth -> auth.username },
                activeAccount = index,
                githubUsername = list.getOrNull(index)?.username,
                isGithubConnected = list.isNotEmpty()
            )
        }
    }

/**
     * Reads every account in token.env and asks GitHub which username each
     * token belongs to. Accounts with a bad token are skipped; if EVERY token
     * fails, the first error is thrown like before.
     */
    internal fun RexGitViewModel.__split_reloadAccounts_4(): List<String> {
        val entries = authStore.loadAccounts()

        val resolved = mutableListOf<RexGitAuth>()
        val skipped = mutableListOf<String>()
        var firstError: Throwable? = null

        entries.forEachIndexed { index, entry ->
            try {
                resolved += RexGitAuth(
                    username = github.getUser(entry.token),
                    token = entry.token
                )
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                if (firstError == null) firstError = t
                skipped += entry.username ?: "account ${index + 1}"
            }
        }

        val error = firstError
        if (resolved.isEmpty() && error != null) throw error

        accounts = resolved

        val saved = accountPrefs.getString(ACTIVE_KEY, null)
        activeIndex = resolved
            .indexOfFirst { it.username.equals(saved, ignoreCase = true) }
            .takeIf { it >= 0 }
            ?: activeIndex.coerceIn(0, (resolved.size - 1).coerceAtLeast(0))

        publishAccounts()

        return skipped
    }

/**
     * Returns the account currently in use.
     * [reload] re-reads token.env (used by refresh / connect).
     */
    internal suspend fun RexGitViewModel.__split_externalGithubAuth_5(reload: Boolean = false): RexGitAuth? {
        if (reload || accounts.isEmpty()) {
            val skipped = reloadAccounts()

            if (skipped.isNotEmpty()) {
                _state.update {
                    it.copy(message = "Skipped invalid token: ${skipped.joinToString()}")
                }
            }
        }

        return accounts.getOrNull(activeIndex)
    }

internal fun RexGitViewModel.__split_loadRepositoryGithubData_25(repo: RexGitRepository) {
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

fun RexGitViewModel.__split_downloadGithubAsset_27(url: String, rawName: String) {
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

/**
     * Loads the account(s) from token.env. The parameter is ignored; it stays
     * so older callers keep compiling. To log in with a typed username and
     * token use [addAccount].
     */
    fun RexGitViewModel.__split_connectGithub_28(
        @Suppress("UNUSED_PARAMETER")
        tokenFromUi: String
    ) {
        launchIO {
            _state.update {
                it.copy(
                    isGithubLoading = true,
                    error = null,
                    message = null
                )
            }

            try {
                val auth = externalGithubAuth(reload = true)
                    ?: throw missingAuthError()

                _state.update {
                    it.copy(
                        githubUsername = auth.username,
                        isGithubConnected = true,
                        message = "Connected as @${auth.username}"
                    )
                }

                fetchGithubRepositories(auth)
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                fail(
                    "Could not connect to GitHub:\n" +
                        t.readableMessage(
                            "Invalid or expired token"
                        )
                )
            } finally {
                _state.update {
                    it.copy(isGithubLoading = false)
                }
            }
        }
    }
