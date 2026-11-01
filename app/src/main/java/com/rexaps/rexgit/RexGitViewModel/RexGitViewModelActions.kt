package com.rexaps.rexgit

import androidx.lifecycle.viewModelScope

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.update

/** Start one lifecycle-owned polling loop. The screen calls stop when Actions is no longer visible. */
internal fun RexGitViewModel.startPollingActions() {
    if (actionsPollingJob?.isActive == true) return
    _state.update { it.copy(isPollingActions = true) }
    actionsPollingJob = viewModelScope.launch(Dispatchers.IO) {
        while (isActive && _state.value.isPollingActions) {
            val startedAt = System.currentTimeMillis()
            var nextDelay = 5_000L
            try {
                actionsRefreshMutex.withLock {
                    refreshActionsSnapshot()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (rateLimit: RexGitRateLimitException) {
                nextDelay = rateLimit.retryAfterMillis.coerceAtLeast(5_000L)
                _state.update { it.copy(actionsError = rateLimit.message) }
            } catch (error: Throwable) {
                _state.update { it.copy(actionsError = error.readableMessage("Could not refresh GitHub Actions")) }
            }
            val elapsed = System.currentTimeMillis() - startedAt
            delay((nextDelay - elapsed).coerceAtLeast(750L))
        }
    }
}

internal fun RexGitViewModel.stopPollingActions() {
    _state.update { it.copy(isPollingActions = false) }
    actionsPollingJob?.cancel()
    actionsPollingJob = null
}

/** Called by the row selection. Jobs/steps come from GitHub's workflow-run jobs endpoint. */
internal fun RexGitViewModel.__splitImpl_selectWorkflowRun(runId: Long) {
    _state.update {
        it.copy(
            selectedWorkflowRunId = runId,
            workflowJobs = emptyList(),
            isWorkflowDetailsLoading = true,
            actionsError = null
        )
    }
    launchIO {
        try {
            actionsRefreshMutex.withLock { refreshActionsSnapshot() }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            _state.update {
                it.copy(
                    isWorkflowDetailsLoading = false,
                    actionsError = error.readableMessage("Could not load workflow details")
                )
            }
        }
    }
}

/**
 * Lightweight five-second refresh. Unlike the initial repository load, this does not refetch
 * metadata, releases, tags or all artifact lists on every tick. It fetches run status and,
 * when a run is open, that run's complete jobs/steps.
 */
private suspend fun RexGitViewModel.refreshActionsSnapshot() {
    val repo = _state.value.selectedRepository ?: return
    val slug = repositorySlug(repo) ?: return
    // Public repositories can expose Actions data without authentication; private repos
    // still surface GitHub's normal 403/404 response when the token is missing or limited.
    val auth = externalGithubAuth()
    val runs = github.listWorkflowRuns(auth?.token, slug.first, slug.second)
    val current = _state.value
    val selectedRunId = current.selectedWorkflowRunId
        ?.takeIf { id -> runs.any { it.id == id } }
        ?: current.selectedWorkflowRunId
    val jobs = selectedRunId?.let {
        github.listWorkflowJobs(auth?.token, slug.first, slug.second, it)
    }.orEmpty()

    _state.update { latest ->
        if (latest.selectedRepository?.path != repo.path) latest else {
            val selectionStillMatches = latest.selectedWorkflowRunId == selectedRunId
            latest.copy(
                workflowRuns = runs,
                workflowJobs = if (selectionStillMatches) jobs else latest.workflowJobs,
                isWorkflowDetailsLoading = if (selectionStillMatches) false else latest.isWorkflowDetailsLoading,
                actionsLastUpdatedAt = System.currentTimeMillis(),
                actionsError = null
            )
        }
    }
}

/** Explicit refresh button; the same lightweight path is used to avoid redundant API calls. */
internal fun RexGitViewModel.refreshActions() {
    launchIO {
        try {
            actionsRefreshMutex.withLock { refreshActionsSnapshot() }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            _state.update {
                it.copy(
                    isWorkflowDetailsLoading = false,
                    actionsError = error.readableMessage("Could not refresh GitHub Actions")
                )
            }
        }
    }
}
