@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.rexaps.rexgit

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
        return __splitImpl_RexGitReadmePreview(repo, content, modifier, metadata, tags, onFileLink)
    }

@Composable
internal fun RexGitActionsPanel(
    state: RexGitUiState,
    onRefresh: () -> Unit,
    onDownload: (String, String) -> Unit,
    onChooseAccount: () -> Unit = {},
    modifier: Modifier = Modifier
) {
        return __splitImpl_RexGitActionsPanel(state, onRefresh, onDownload, onChooseAccount, modifier)
    }

@Composable
internal fun RexGitReleasePanel(
    state: RexGitUiState,
    onRefresh: () -> Unit,
    onDownload: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
        return __splitImpl_RexGitReleasePanel(state, onRefresh, onDownload, modifier)
    }

@Composable
internal fun MetadataChip(label: String, value: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(9.dp)) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun EmptyHubCard(title: String, body: String) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f)), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 21.sp)
        }
    }
}

@Composable
internal fun StatusDot(status: String) {
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
internal fun OpenWebButton(url: String) {
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

internal fun runLogsApiUrl(repo: RexGitRepository?, runId: Long): String? {
    if (repo == null) return null
    val remote = repo.remoteUrl.orEmpty()
    val match = Regex("""(?:github\.com[:/])([^/]+)/([^/]+?)(?:\.git)?/?$""", RegexOption.IGNORE_CASE).find(remote)
    val owner = match?.groupValues?.getOrNull(1) ?: repo.globalOwner ?: repo.accountNickname ?: return null
    val name = match?.groupValues?.getOrNull(2) ?: repo.name
    return "https://api.github.com/repos/$owner/$name/actions/runs/$runId/logs"
}

internal fun formatRunDuration(start: String, end: String): String {
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

internal fun formatBytes(bytes: Long): String = when {
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024L -> "${bytes / 1024L} KB"
    else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
}
