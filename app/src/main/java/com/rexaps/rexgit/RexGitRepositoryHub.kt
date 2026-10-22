@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.rexaps.rexgit

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class RexGitMarkdownChunk(val kind: String, val text: String)

@Composable
internal fun RepositoryTabBar(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val labels = listOf("README", "Code", "Actions", "Release")
    val icons = listOf(Icons.Default.MenuBook, Icons.Default.Code, Icons.Default.Settings, Icons.Default.Description)
    Row(
        modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        labels.forEachIndexed { index, label ->
            val active = selected == index
            Surface(
                onClick = { onSelect(index) },
                shape = RoundedCornerShape(14.dp),
                color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.36f) else MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.heightIn(min = 42.dp)
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Icon(icons[index], contentDescription = null, modifier = Modifier.size(17.dp), tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(label, style = MaterialTheme.typography.labelLarge, color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
internal fun RexGitReadmePreview(
    repo: RexGitRepository,
    content: String,
    modifier: Modifier = Modifier
) {
    val chunks = remember(content) { parseMarkdown(content) }
    val scheme = MaterialTheme.colorScheme
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = scheme.surfaceVariant.copy(alpha = 0.72f)),
            border = BorderStroke(1.dp, scheme.outlineVariant)
        ) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(shape = RoundedCornerShape(13.dp), color = scheme.primaryContainer, modifier = Modifier.size(42.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, tint = scheme.onPrimaryContainer)
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(repo.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(repo.remoteUrl?.removePrefix("https://") ?: repo.path, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                    MetadataChip("Branch", repo.branch.ifBlank { "unknown" })
                    MetadataChip(if (repo.globalClone) "Global clone" else "Workspace", repo.accountNickname ?: repo.globalOwner ?: "Local")
                }
            }
        }

        if (content.isBlank()) {
            EmptyHubCard(
                title = "README.md not found",
                body = "Add a README.md in the repository root to show the project overview here."
            )
        } else {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surface),
                border = BorderStroke(1.dp, scheme.outlineVariant)
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    chunks.forEach { chunk ->
                        when (chunk.kind) {
                            "h1" -> Text(markdownInline(chunk.text), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = scheme.onSurface)
                            "h2" -> Text(markdownInline(chunk.text), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = scheme.onSurface)
                            "h3" -> Text(markdownInline(chunk.text), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
                            "bullet" -> Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.Top) {
                                Text("•", color = scheme.primary, fontWeight = FontWeight.Bold)
                                Text(markdownInline(chunk.text), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface, modifier = Modifier.weight(1f))
                            }
                            "quote" -> Surface(color = scheme.surfaceVariant.copy(alpha = 0.7f), shape = RoundedCornerShape(topEnd = 13.dp, bottomEnd = 13.dp), border = BorderStroke(0.dp, Color.Transparent)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Surface(Modifier.width(4.dp).heightIn(min = 34.dp), color = scheme.primary, shape = CircleShape) {}
                                    Text(markdownInline(chunk.text), modifier = Modifier.padding(vertical = 11.dp, end = 12.dp), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                                }
                            }
                            "code" -> Surface(color = scheme.surfaceVariant.copy(alpha = 0.8f), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, scheme.outlineVariant)) {
                                Text(chunk.text, modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(14.dp), fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 18.sp, color = scheme.onSurface)
                            }
                            "table" -> Surface(color = scheme.surfaceVariant.copy(alpha = 0.45f), shape = RoundedCornerShape(9.dp)) {
                                Text(markdownInline(chunk.text), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(8.dp), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, color = scheme.onSurface)
                            }
                            else -> Text(markdownInline(chunk.text), style = MaterialTheme.typography.bodyMedium, lineHeight = 23.sp, color = scheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun RexGitActionsPanel(
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
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Settings, contentDescription = null, tint = scheme.onPrimaryContainer) }
                }
                Column(Modifier.weight(1f)) {
                    Text("GitHub Actions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${state.workflows.size} workflow(s) · ${state.workflowRuns.size} recent run(s)", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
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
                Text("Loading workflows and run history…", color = scheme.onSurfaceVariant)
            }
        }
        state.repoHubError?.let { EmptyHubCard(title = "GitHub data unavailable", body = it) }
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
        if (state.workflowRuns.isNotEmpty()) {
            Text("RECENT RUNS", style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            state.workflowRuns.forEach { run ->
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = scheme.surface), border = BorderStroke(1.dp, scheme.outlineVariant)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatusDot(run.conclusion ?: run.status)
                            Column(Modifier.weight(1f)) {
                                Text(run.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text("#${run.runNumber} · ${run.event} · ${run.status}${run.conclusion?.let { " / $it" }.orEmpty()}", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                                Text("Started ${run.createdAt.replace('T', ' ').removeSuffix("Z").take(19)} · Updated ${run.updatedAt.replace('T', ' ').removeSuffix("Z").take(19)}", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                            }
                            OpenWebButton(run.htmlUrl)
                        }
                        val artifacts = state.workflowArtifacts.filter { it.workflowRunId == run.id }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(onClick = { runLogsApiUrl(state.selectedRepository, run.id)?.let { url -> onDownload(url, "run-${run.runNumber}-logs.zip") } }, enabled = !runLogsApiUrl(state.selectedRepository, run.id).isNullOrBlank()) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(5.dp)); Text("Logs ZIP")
                            }
                            if (artifacts.isNotEmpty()) Text("${artifacts.size} artifact(s)", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                        }
                        artifacts.forEach { artifact ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text(artifact.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text("${formatBytes(artifact.sizeInBytes)}${if (artifact.expired) " · expired" else ""}", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                                }
                                Button(onClick = { onDownload(artifact.archiveDownloadUrl, "${artifact.name}.zip") }, enabled = !artifact.expired && artifact.archiveDownloadUrl.isNotBlank() && state.downloadingArtifact == null) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(5.dp)); Text(if (state.downloadingArtifact == artifact.name + ".zip") "Saving…" else "Download")
                                }
                            }
                        }
                    }
                }
            }
        }
        if (!state.isRepoHubLoading && state.workflows.isEmpty() && state.workflowRuns.isEmpty() && state.repoHubError == null) {
            EmptyHubCard(title = "No workflow runs yet", body = "Add a workflow under .github/workflows to see CI status, logs, and downloadable artifacts here.")
        }
    }
}

@Composable
internal fun RexGitReleasePanel(
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
        if (state.releases.isEmpty() && !state.isRepoHubLoading) EmptyHubCard(title = "No releases published", body = state.repoHubError ?: "When the repository publishes releases, tags, notes, and downloadable assets will appear here.")
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
                    if (release.body.isNotBlank()) Text(release.body.take(900), style = MaterialTheme.typography.bodyMedium, lineHeight = 21.sp, color = scheme.onSurface)
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

@Composable
private fun MetadataChip(label: String, value: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(9.dp)) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun EmptyHubCard(title: String, body: String) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f)), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 21.sp)
        }
    }
}

@Composable
private fun StatusDot(status: String) {
    val normalized = status.lowercase()
    val color = when {
        "success" in normalized || "completed" in normalized || "active" in normalized -> Color(0xFF22A06B)
        "failure" in normalized || "failed" in normalized || "cancel" in normalized -> MaterialTheme.colorScheme.error
        "queued" in normalized || "progress" in normalized || "waiting" in normalized -> Color(0xFFE0A020)
        else -> MaterialTheme.colorScheme.primary
    }
    Surface(color = color.copy(alpha = 0.14f), shape = CircleShape, modifier = Modifier.size(28.dp)) {
        Box(contentAlignment = Alignment.Center) { Surface(color = color, shape = CircleShape, modifier = Modifier.size(8.dp)) {} }
    }
}

@Composable
private fun OpenWebButton(url: String) {
    val context = LocalContext.current
    if (url.isNotBlank()) {
        OutlinedButton(
            onClick = {
                try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (_: Throwable) { }
            },
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 9.dp, vertical = 6.dp),
            modifier = Modifier.heightIn(min = 34.dp)
        ) {
            Icon(Icons.Default.ArrowForward, contentDescription = "Open on GitHub", modifier = Modifier.size(15.dp))
        }
    }
}

private fun runLogsApiUrl(repo: RexGitRepository?, runId: Long): String? {
    if (repo == null) return null
    val remote = repo.remoteUrl.orEmpty()
    val match = Regex("""(?:github\.com[:/])([^/]+)/([^/]+?)(?:\.git)?/?$""", RegexOption.IGNORE_CASE).find(remote)
    val owner = match?.groupValues?.getOrNull(1) ?: repo.globalOwner ?: repo.accountNickname ?: return null
    val name = match?.groupValues?.getOrNull(2) ?: repo.name
    return "https://api.github.com/repos/$owner/$name/actions/runs/$runId/logs"
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024L -> "${bytes / 1024L} KB"
    else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
}

private fun parseMarkdown(content: String): List<RexGitMarkdownChunk> {
    val result = mutableListOf<RexGitMarkdownChunk>()
    val paragraph = mutableListOf<String>()
    val code = StringBuilder()
    var insideCode = false
    fun flushParagraph() {
        if (paragraph.isNotEmpty()) {
            result += RexGitMarkdownChunk("paragraph", paragraph.joinToString(" ").trim())
            paragraph.clear()
        }
    }
    content.lineSequence().forEach { raw ->
        val line = raw.trimEnd()
        if (line.trimStart().startsWith("```")) {
            if (insideCode) {
                result += RexGitMarkdownChunk("code", code.toString().trimEnd())
                code.clear()
                insideCode = false
            } else {
                flushParagraph()
                insideCode = true
            }
            return@forEach
        }
        if (insideCode) {
            code.append(line).append('\n')
            return@forEach
        }
        if (line.isBlank()) { flushParagraph(); return@forEach }
        when {
            line.startsWith("### ") -> { flushParagraph(); result += RexGitMarkdownChunk("h3", line.removePrefix("### ")) }
            line.startsWith("## ") -> { flushParagraph(); result += RexGitMarkdownChunk("h2", line.removePrefix("## ")) }
            line.startsWith("# ") -> { flushParagraph(); result += RexGitMarkdownChunk("h1", line.removePrefix("# ")) }
            line.startsWith(">") -> { flushParagraph(); result += RexGitMarkdownChunk("quote", line.removePrefix(">").trim()) }
            line.startsWith("- ") || line.startsWith("* ") || Regex("^\\d+\\. ").containsMatchIn(line) -> {
                flushParagraph()
                result += RexGitMarkdownChunk("bullet", line.replaceFirst(Regex("^(?:[-*]|\\d+\\.)\\s+"), ""))
            }
            line.startsWith("|") -> { flushParagraph(); if (!Regex("^\\|[ :|\\-]+\\|$").matches(line.trim())) result += RexGitMarkdownChunk("table", line) }
            line.startsWith("---") -> { flushParagraph(); result += RexGitMarkdownChunk("quote", "────────────────────────") }
            else -> paragraph += line
        }
    }
    if (insideCode && code.isNotEmpty()) result += RexGitMarkdownChunk("code", code.toString().trimEnd())
    flushParagraph()
    return result
}

private fun markdownInline(text: String): String {
    var value = text
    value = Regex("!\\[([^]]*)]\\([^)]*\\)").replace(value, "$1")
    value = Regex("\\[([^]]+)]\\([^)]*\\)").replace(value, "$1")
    value = value.replace("**", "").replace("__", "").replace("~~", "")
    value = value.replace("`", "")
    return value
}
