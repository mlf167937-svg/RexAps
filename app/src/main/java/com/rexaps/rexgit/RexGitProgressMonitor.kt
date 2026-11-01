package com.rexaps.rexgit

import org.eclipse.jgit.lib.ProgressMonitor

/** Progress reported by JGit. Percent is null when JGit does not expose a useful total. */
data class GitProgressState(
    val taskTitle: String = "Preparing Git operation…",
    val completedUnits: Int = 0,
    val totalUnits: Int = 0,
    val completedTasks: Int = 0,
    val totalTasks: Int = 0,
    val percent: Int? = null,
    val finished: Boolean = false
)

/**
 * Converts JGit's task callbacks into a stable UI progress value.
 * JGit reports work units rather than network bytes, so the percentage is an
 * estimate across its tasks and is never presented as downloaded bytes.
 */
class RexGitProgressMonitor(
    private val onProgress: (GitProgressState) -> Unit = {},
    private val cancelled: () -> Boolean = { false }
) : ProgressMonitor {
    private var taskCount = 0
    private var finishedTasks = 0
    private var currentTitle = "Preparing Git operation…"
    private var currentTotal = 0
    private var currentCompleted = 0

    override fun start(totalTasks: Int) {
        taskCount = totalTasks.coerceAtLeast(0)
        finishedTasks = 0
        currentTitle = "Preparing Git operation…"
        currentTotal = 0
        currentCompleted = 0
        publish()
    }

    override fun beginTask(title: String, totalWork: Int) {
        currentTitle = title.ifBlank { "Working…" }
        currentTotal = totalWork.coerceAtLeast(0)
        currentCompleted = 0
        publish()
    }

    override fun update(completed: Int) {
        currentCompleted = (currentCompleted + completed.coerceAtLeast(0))
            .let { if (currentTotal > 0) it.coerceAtMost(currentTotal) else it }
        publish()
    }

    override fun endTask() {
        finishedTasks = (finishedTasks + 1).coerceAtMost(taskCount)
        currentCompleted = currentTotal
        publish()
    }

    override fun isCancelled(): Boolean = cancelled()

    private fun publish() {
        val taskFraction = if (currentTotal > 0) {
            (currentCompleted.toFloat() / currentTotal.toFloat()).coerceIn(0f, 1f)
        } else 0f
        val percent = if (taskCount > 0) {
            (((finishedTasks + taskFraction) / taskCount.toFloat()) * 100f)
                .toInt().coerceIn(0, 100)
        } else null
        onProgress(
            GitProgressState(
                taskTitle = currentTitle,
                completedUnits = currentCompleted,
                totalUnits = currentTotal,
                completedTasks = finishedTasks,
                totalTasks = taskCount,
                percent = percent
            )
        )
    }
}
