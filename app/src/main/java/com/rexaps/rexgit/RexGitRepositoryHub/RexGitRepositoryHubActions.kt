@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.rexaps.rexgit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.CloudDownload
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
internal fun RexGitActionsPanel(
    state: RexGitUiState,
    onRefresh: () -> Unit,
    onDownload: (String, String) -> Unit,
    onChooseAccount: () -> Unit = {},
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

        Text("RECENT WORKFLOW RUNS", style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        if (state.workflowRuns.isEmpty() && !state.isRepoHubLoading && state.actionsError == null && state.repoHubError == null) {
            EmptyHubCard(title = "No workflow runs yet", body = "No recent runs were returned. Check whether Actions is enabled and whether the selected account can read Actions in this repository.")
        }
        state.workflowRuns.forEach { run ->
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = scheme.surface), border = BorderStroke(1.dp, scheme.outlineVariant)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
