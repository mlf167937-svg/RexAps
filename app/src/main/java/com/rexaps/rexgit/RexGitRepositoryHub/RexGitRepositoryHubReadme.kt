@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.rexaps.rexgit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

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
