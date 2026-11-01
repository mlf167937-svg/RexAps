package com.rexaps.rexgit

import org.eclipse.jgit.lib.ProgressMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * JGit progress adapter. It exposes real task progress to the UI without
 * depending on a notification implementation or Android UI classes.
 * A percentage is available only when JGit reports a positive totalWork.
 */
data class GitProgressState(
    val title: String = "",
    val totalWork: Int = 0,
    val completed: Int = 0,
    val isDone: Boolean = false
) {
    val percentage: Int?
        get() = if (totalWork > 0) {
            ((completed.toLong() * 100L) / totalWork).toInt().coerceIn(0, 100)
        } else null
}

class RexGitProgressMonitor : ProgressMonitor {
    private val _progress = MutableStateFlow(GitProgressState())
    val progress = _progress.asStateFlow()

    private var currentWork = 0
    @Volatile private var cancelled = false

    override fun start(totalTasks: Int) {
        currentWork = 0
        cancelled = false
        _progress.value = GitProgressState(title = "Preparing Git operation")
    }

    override fun beginTask(title: String, totalWork: Int) {
        currentWork = 0
        _progress.value = GitProgressState(
            title = title,
            totalWork = totalWork.coerceAtLeast(0),
            completed = 0,
            isDone = false
        )
    }

    override fun update(completed: Int) {
        currentWork = (currentWork + completed.coerceAtLeast(0)).coerceAtLeast(0)
        _progress.update { state ->
            state.copy(
                completed = if (state.totalWork > 0) currentWork.coerceAtMost(state.totalWork)
                else currentWork
            )
        }
    }

    override fun endTask() {
        _progress.update { state ->
            state.copy(
                completed = if (state.totalWork > 0) state.totalWork else state.completed,
                isDone = true
            )
        }
    }

    override fun isCancelled(): Boolean = cancelled

    fun cancel() {
        cancelled = true
    }
}
