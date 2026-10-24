@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.rexaps.rexgit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun __splitImpl_RexGitReleasePanel(
    state: RexGitUiState,
    onRefresh: () -> Unit,
    onDownload: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = scheme.surfaceVariant.copy(alpha = 0.7f)), border = BorderStroke(1.dp, scheme.outlineVariant)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(14.dp), color = scheme.primaryContainer, modifier = Modifier.size(44.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Description, contentDescription = null, tint = scheme.onPrimaryContainer) }
                }
                Column(Modifier.weight(1f)) {
                    Text("Releases", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${state.releases.size} release(s) available", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
                OutlinedButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(5.dp)); Text("Refresh") }
            }
        }
        if (state.isRepoHubLoading) Row(Modifier.fillMaxWidth().padding(24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp); Text("Loading releases…", color = scheme.onSurfaceVariant) }
        state.releasesError?.let { EmptyHubCard(title = "Release data unavailable", body = it) }
        state.downloadError?.let { EmptyHubCard(title = "Download failed", body = it) }
        if (state.releases.isEmpty() && state.repositoryTags.isEmpty() && !state.isRepoHubLoading && state.releasesError == null) EmptyHubCard(title = "No releases published", body = "When the repository publishes releases, tags, notes, and downloadable assets will appear here.")
        if (state.downloadingArtifact != null) {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = scheme.surfaceVariant.copy(alpha = 0.55f))) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Downloading ${state.downloadingArtifact}", style = MaterialTheme.typography.labelLarge)
                    val total = state.downloadTotalBytes
                    if (total != null && total > 0L) {
                        LinearProgressIndicator(progress = (state.downloadedBytes.toFloat() / total.toFloat()).coerceIn(0f, 1f), modifier = Modifier.fillMaxWidth())
                        Text("${formatBytes(state.downloadedBytes)} / ${formatBytes(total)}", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(formatBytes(state.downloadedBytes), style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                    }
                    Text("Saving to Download/RexAps/Github/Artefak/", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                }
            }
        }
        if (state.repositoryTags.isNotEmpty()) {
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = scheme.surface), border = BorderStroke(1.dp, scheme.outlineVariant)) {
                Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Repository tags", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(state.repositoryTags.take(30).joinToString("   ·   "), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
            }
        }
        state.releases.forEach { release ->
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = scheme.surface), border = BorderStroke(1.dp, scheme.outlineVariant)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(color = scheme.primaryContainer, shape = RoundedCornerShape(11.dp), modifier = Modifier.size(40.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Description, contentDescription = null, tint = scheme.onPrimaryContainer) } }
                        Column(Modifier.weight(1f)) {
                            Text(release.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(release.tagName, style = MaterialTheme.typography.labelLarge, color = scheme.primary)
                            Text(release.publishedAt.replace('T', ' ').removeSuffix("Z").take(19), style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (release.prerelease) MetadataChip("Pre-release", "Preview") else MetadataChip("Release", if (release.draft) "Draft" else "Published")
                            OpenWebButton(release.htmlUrl)
                        }
                    }
                    if (release.body.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = scheme.surfaceVariant.copy(alpha = 0.36f),
                            border = BorderStroke(1.dp, scheme.outlineVariant)
                        ) {
                            RexGitMarkdownRichText(
                                content = release.body.take(6000),
                                rawBaseUrl = state.selectedRepository?.let(::githubRawReadmeBase).orEmpty(),
                                darkTheme = scheme.surface.luminance() < 0.5f,
                                modifier = Modifier.fillMaxWidth().padding(12.dp)
                            )
                        }
                    }
                    if (release.assets.isNotEmpty()) {
                        Text("ASSETS", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = scheme.onSurfaceVariant)
                        release.assets.forEach { asset ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text(asset.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text(formatBytes(asset.sizeInBytes), style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                                }
                                FilledTonalButton(onClick = { onDownload(asset.downloadUrl, asset.name) }, enabled = asset.downloadUrl.isNotBlank() && state.downloadingArtifact == null) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(5.dp)); Text("Download")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
