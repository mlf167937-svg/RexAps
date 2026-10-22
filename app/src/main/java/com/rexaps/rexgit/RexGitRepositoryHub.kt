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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    modifier: Modifier = Modifier,
    metadata: RexGitGithubRepository? = null,
    tags: List<String> = emptyList(),
    onFileLink: (String) -> Unit = {}
) {
    val scheme = MaterialTheme.colorScheme
    val defaultDark = scheme.surface.luminance() < 0.5f
    var themeChoice by remember(repo.path) { androidx.compose.runtime.mutableStateOf("app") }
    val previewDark = when (themeChoice) {
        "dark" -> true
        "light" -> false
        else -> defaultDark
    }
    val owner = metadata?.ownerLogin ?: repo.globalOwner ?: repo.accountNickname.orEmpty()
    val fullName = metadata?.fullName?.takeIf { it.isNotBlank() }
        ?: repo.remoteUrl?.let { remote ->
            Regex("""(?:github\.com[:/])([^/]+)/([^/]+?)(?:\.git)?/?$""", RegexOption.IGNORE_CASE)
                .find(remote)?.let { "${it.groupValues[1]}/${it.groupValues[2].removeSuffix(".git")}" }
        }
        ?: listOf(owner, repo.name).filter { it.isNotBlank() }.joinToString("/")

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = scheme.surfaceVariant.copy(alpha = 0.72f)),
            border = BorderStroke(1.dp, scheme.outlineVariant)
        ) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                    RexGitRepositoryAvatar(
                        url = metadata?.avatarUrl,
                        fallback = owner,
                        modifier = Modifier.size(48.dp)
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(fullName.ifBlank { repo.name }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(metadata?.description ?: "Repository README preview", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    }
                    OpenWebButton(metadata?.htmlUrl.orEmpty())
                }
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                    MetadataChip("Branch", metadata?.defaultBranch?.takeIf { it.isNotBlank() } ?: repo.branch.ifBlank { "unknown" })
                    MetadataChip(if (repo.globalClone) "Global clone" else "Workspace", repo.accountNickname ?: repo.globalOwner ?: "Local")
                    if (metadata?.private == true) MetadataChip("Visibility", "Private") else if (metadata != null) MetadataChip("Visibility", "Public")
                }
                if (metadata != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                        MetadataChip("Stars", metadata.stars.toString())
                        MetadataChip("Forks", metadata.forks.toString())
                        metadata.language?.let { MetadataChip("Language", it) }
                        metadata.licenseName?.let { MetadataChip("License", it) }
                    }
                    metadata.updatedAt?.takeIf { it.isNotBlank() }?.let {
                        Text("Updated ${it.replace('T', ' ').removeSuffix("Z").take(16)} UTC", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                    }
                }
                if (tags.isNotEmpty()) {
                    Text("TAGS  ·  ${tags.take(8).joinToString("  ·  ")}", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                val clipboard = LocalClipboardManager.current
                val cloneUrl = metadata?.cloneUrl?.takeIf { it.isNotBlank() } ?: repo.remoteUrl.orEmpty()
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (cloneUrl.isNotBlank()) {
                        FilledTonalButton(onClick = { clipboard.setText(AnnotatedString(cloneUrl)) }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Copy Clone URL")
                        }
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Preview theme", style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
            listOf("app" to "App", "light" to "Light", "dark" to "Dark").forEach { (key, label) ->
                val selected = themeChoice == key
                Surface(
                    onClick = { themeChoice = key },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selected) scheme.primaryContainer else scheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, if (selected) scheme.primary.copy(alpha = 0.4f) else scheme.outlineVariant)
                ) {
                    Text(label, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, color = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant)
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
                colors = CardDefaults.cardColors(containerColor = if (previewDark) Color(0xFF0D1117) else Color(0xFFFFFFFF)),
                border = BorderStroke(1.dp, scheme.outlineVariant)
            ) {
                RexGitMarkdownRichText(
                    content = content,
                    rawBaseUrl = githubRawReadmeBase(repo),
                    darkTheme = previewDark,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    onInternalLink = onFileLink
                )
            }
            Text(
                "Rendered with GitHub-style Markdown and safe HTML support. Some GitHub-only embeds and custom components may render differently.",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

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

private fun formatRunDuration(start: String, end: String): String {
    return try {
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
            isLenient = true
        }
        val startDate = formatter.parse(start.take(20)) ?: return "duration unavailable"
        val endDate = formatter.parse(end.take(20)) ?: return "duration unavailable"
        val seconds = ((endDate.time - startDate.time) / 1000L).coerceAtLeast(0L)
        when {
            seconds < 60 -> "${seconds}s"
            seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
            else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
        }
    } catch (_: Throwable) {
        "duration unavailable"
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024L -> "${bytes / 1024L} KB"
    else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
}
