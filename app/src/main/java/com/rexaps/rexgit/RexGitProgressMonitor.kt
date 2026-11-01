package com.rexaps.rexgit

import org.eclipse.jgit.lib.ProgressMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

data class GitProgressState(
    val title: String = "",
    val totalWork: Int = 0,
    val completed: Int = 0,
    val isDone: Boolean = false
) {
    val percentage: Int? get() = if (totalWork > 0) (completed * 100) / totalWork else null
}

class RexGitProgressMonitor(
    private val notificationManager: RexGitNotificationManager,
    private val repoName: String,
    private val operation: String // "Clone" atau "Pull"
) : ProgressMonitor {

    val progressFlow = MutableStateFlow(GitProgressState())
    private var currentWork = 0
    private var currentTitle = ""

    override fun start(totalTasks: Int) {}

    override fun beginTask(title: String, totalWork: Int) {
        currentTitle = title
        currentWork = 0
        progressFlow.update { it.copy(title = title, totalWork = totalWork, completed = 0, isDone = false) }
        notificationManager.showProgress(repoName, operation, title, 0)
    }

    override fun update(completed: Int) {
        currentWork += completed
        progressFlow.update { it.copy(completed = currentWork) }
        
        val percent = progressFlow.value.percentage
        if (percent != null && percent % 5 == 0) { // Update notif setiap kelipatan 5% untuk cegah spam
            notificationManager.showProgress(repoName, operation, currentTitle, percent)
        }
    }

    override fun endTask() {
        progressFlow.update { it.copy(isDone = true) }
        notificationManager.showSuccess(repoName, operation)
    }

    override fun isCancelled(): Boolean = false
}
