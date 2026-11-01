@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.rexaps.rexgit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun __splitImpl_RexGitActionsPanel(
    state: RexGitUiState,
    onRefresh: () -> Unit,
    onDownload: (String, String) -> Unit,
    onChooseAccount: () -> Unit = {},
    onSelectRun: (Long) -> Unit = {},
    onCloseRunDetails: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = scheme.surfaceVariant.copy(alpha = 0.7f)),
            border = BorderStroke(1.dp, scheme.outlineVariant)
        ) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(14.dp), color = scheme.primaryContainer, modifier = Modifier.size(44.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Settings, contentDescription = null, tint = scheme.onPrimaryContainer) }
                }
                Column(Modifier.weight(1f)) {
                    Text("GitHub Actions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${state.workflowRuns.size} recent run(s) · ${state.workflows.size} workflow(s)", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                    Text(
                        if (state.isPollingActions) "LIVE · refresh every 5 seconds" else "Automatic refresh pauses outside Actions",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (state.isPollingActions) scheme.primary else scheme.onSurfaceVariant
                    )
                    state.selectedRepository?.remoteUrl?.takeIf { it.isNotBlank() }?.let {
                        Text(it.removePrefix("https://github.com/").removeSuffix(".git"), style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                OutlinedButton(onClick = onRefresh, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Refresh")
                }
            }
        }

        if (state.isRepoHubLoading) {
            Row(Modifier.fillMaxWidth().padding(24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                Text("Loading workflow runs and artifacts…", color = scheme.onSurfaceVariant)
            }
        }

        (state.actionsError ?: state.repoHubError)?.let { errorText ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EmptyHubCard(title = "GitHub Actions unavailable", body = errorText)
                OutlinedButton(onClick = onChooseAccount) { Text("Choose another GitHub account") }
            }
        }
        state.downloadError?.let { EmptyHubCard(title = "Download failed", body = it) }

        if (state.downloadingArtifact != null) {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = scheme.surfaceVariant.copy(alpha = 0.55f))) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Downloading ${state.downloadingArtifact}", style = MaterialTheme.typography.labelLarge)
                    val total = state.downloadTotalBytes
                    if (total != null && total > 0L) {
                        LinearProgressIndicator(
                            progress = (state.downloadedBytes.toFloat() / total.toFloat()).coerceIn(0f, 1f),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("${formatBytes(state.downloadedBytes)} / ${formatBytes(total)}", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(formatBytes(state.downloadedBytes), style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                    }
                    Text("Saving to Download/RexAps/Github/Artefak/", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                }
            }
        }

        val selectedRun = state.workflowRuns.firstOrNull { it.id == state.selectedWorkflowRunId }
        if (state.selectedWorkflowRunId != null) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surface),
                border = BorderStroke(1.dp, scheme.primary.copy(alpha = 0.35f))
            ) {
                Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("LIVE WORKFLOW DETAIL", style = MaterialTheme.typography.labelMedium, color = scheme.primary, fontWeight = FontWeight.Bold)
                            Text(selectedRun?.title ?: "Workflow run #${state.selectedWorkflowRunId}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(onClick = onCloseRunDetails) { Text("Close") }
                    }
                    selectedRun?.let { run ->
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                            StatusDot(run.conclusion ?: run.status)
                            Text("${run.status}${run.conclusion?.let { " · $it" }.orEmpty()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.weight(1f))
                            if (run.htmlUrl.isNotBlank()) OpenWebButton(run.htmlUrl)
                        }
                        Text(
                            listOf("Branch: ${run.branch.ifBlank { "unknown" }}", "Commit: ${run.headSha.take(7)}", "Triggered by: ${run.actor.ifBlank { "unknown" }}", "Event: ${run.event}").joinToString("  ·  "),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                        val activeJob = state.workflowJobs.firstOrNull { it.status.equals("in_progress", true) }
                        val activeStep = activeJob?.steps?.firstOrNull { it.status.equals("in_progress", true) }
                        if (activeJob != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = scheme.primaryContainer.copy(alpha = 0.65f)
                            ) {
                                Column(Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("CURRENT STAGE", style = MaterialTheme.typography.labelSmall, color = scheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                                    Text("${activeJob.name} · ${activeStep?.name ?: "Waiting for step status…"}", style = MaterialTheme.typography.bodySmall, color = scheme.onPrimaryContainer, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        } else if (run.status.equals("queued", true) || run.status.equals("waiting", true)) {
                            Text("Waiting for a GitHub runner…", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        }
                    }
                    if (state.isWorkflowDetailsLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text("Loading jobs and steps…", color = scheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                    } else if (state.workflowJobs.isEmpty()) {
                        Text("GitHub has not returned job details for this run yet.", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                    } else {
                        val stepCount = state.workflowJobs.sumOf { it.steps.size }
                        val completedSteps = state.workflowJobs.sumOf { job -> job.steps.count { it.status.equals("completed", true) } }
                        Text("JOBS & STEPS · ${state.workflowJobs.size} jobs · $completedSteps/$stepCount steps completed", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        state.workflowJobs.forEach { job ->
                            Surface(
                                shape = RoundedCornerShape(15.dp),
                                color = scheme.surfaceVariant.copy(alpha = 0.48f),
                                border = BorderStroke(1.dp, scheme.outlineVariant)
                            ) {
                                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        StatusDot(job.conclusion ?: job.status)
                                        Column(Modifier.weight(1f)) {
                                            Text(job.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                            Text("${job.status}${job.conclusion?.let { " · $it" }.orEmpty()}", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                                        }
                                        if (job.htmlUrl.isNotBlank()) OpenWebButton(job.htmlUrl)
                                    }
                                    job.steps.forEach { step ->
                                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            StatusDot(step.conclusion ?: step.status)
                                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                                Text("${step.number}. ${step.name}", style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                                Text("${step.status}${step.conclusion?.let { " · $it" }.orEmpty()}", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                                            }
                                            if (step.startedAt.isNotBlank()) {
                                                Text(
                                                    if (step.completedAt.isBlank()) "Live" else "Done",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = scheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    state.actionsLastUpdatedAt?.let { updated ->
                        Text("Updated ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(updated))}", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                    }
                }
            }
        }

        Text("RECENT WORKFLOW RUNS", style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        if (state.workflowRuns.isEmpty() && !state.isRepoHubLoading && state.actionsError == null && state.repoHubError == null) {
            EmptyHubCard(title = "No workflow runs yet", body = "No recent runs were returned. Check whether Actions is enabled and whether the selected account can read Actions in this repository.")
        }
        state.workflowRuns.forEach { run ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surface),
                border = BorderStroke(1.dp, if (state.selectedWorkflowRunId == run.id) scheme.primary.copy(alpha = 0.55f) else scheme.outlineVariant)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatusDot(run.conclusion ?: run.status)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(run.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("#${run.runNumber} · ${run.event} · ${run.status}${run.conclusion?.let { " / $it" }.orEmpty()}", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                            val runDetails = listOf(
                                run.branch.takeIf { it.isNotBlank() }?.let { "branch $it" },
                                run.headSha.takeIf { it.isNotBlank() }?.take(7)?.let { "commit $it" },
                                run.actor.takeIf { it.isNotBlank() }?.let { "by $it" }
                            ).filterNotNull().joinToString(" · ")
                            if (runDetails.isNotBlank()) Text(runDetails, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            if (run.commitMessage.isNotBlank()) Text(run.commitMessage.lineSequence().first().take(140), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("Started ${run.createdAt.replace('T', ' ').removeSuffix("Z").take(19)} · Updated ${run.updatedAt.replace('T', ' ').removeSuffix("Z").take(19)} · ${formatRunDuration(run.createdAt, run.updatedAt)}", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                        }
                        OpenWebButton(run.htmlUrl)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { onSelectRun(run.id) }) {
                            Text(if (state.selectedWorkflowRunId == run.id) "Refreshing live" else "View live")
                        }
                        OutlinedButton(
                            onClick = { runLogsApiUrl(state.selectedRepository, run.id)?.let { url -> onDownload(url, "run-${run.runNumber}-logs.zip") } },
                            enabled = !runLogsApiUrl(state.selectedRepository, run.id).isNullOrBlank() && state.downloadingArtifact == null
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp)); Text("Logs ZIP")
                        }
                        val artifactCount = state.workflowArtifacts.count { it.workflowRunId == run.id }
                        if (artifactCount > 0) Text("$artifactCount artifact(s)", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                    }
                    state.workflowArtifacts.filter { it.workflowRunId == run.id }.forEach { artifact ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(artifact.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text("${formatBytes(artifact.sizeInBytes)}${if (artifact.expired) " · expired" else ""}", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                            }
                            Button(
                                onClick = { onDownload(artifact.archiveDownloadUrl, "${artifact.name}.zip") },
                                enabled = !artifact.expired && artifact.archiveDownloadUrl.isNotBlank() && state.downloadingArtifact == null
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(5.dp)); Text(if (state.downloadingArtifact == artifact.name + ".zip") "Saving…" else "Download")
                            }
                        }
                    }
                }
            }
        }

        if (state.workflows.isNotEmpty()) {
            Text("WORKFLOWS", style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            state.workflows.forEach { workflow ->
                Card(shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = scheme.surface), border = BorderStroke(1.dp, scheme.outlineVariant)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatusDot(workflow.state)
                        Column(Modifier.weight(1f)) {
                            Text(workflow.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("${workflow.state} · ${workflow.path}", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        OpenWebButton(workflow.htmlUrl)
                    }
                }
            }
        }
    }
}
