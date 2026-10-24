@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.rexaps.rexgit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun RexGitActionsHeaderAndStatus(
    state: RexGitUiState,
    onRefresh: () -> Unit,
    onChooseAccount: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
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
}
