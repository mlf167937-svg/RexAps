package com.rexaps.rexmusic

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*

@Composable
internal fun SyncSectionHeading(
    title: String,
    subtitle: String? = null,
    titleColor: androidx.compose.ui.graphics.Color? = null,
    subtitleColor: androidx.compose.ui.graphics.Color? = null
) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = titleColor ?: scheme.primary)
        if (!subtitle.isNullOrBlank()) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = subtitleColor ?: scheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun SyncMembersHeader(count: Int) {
    SyncSectionHeading("Connected members", "$count device${if (count == 1) "" else "s"} in this room")
}

@Composable
internal fun SyncQueueHeader(count: Int) {
    SyncSectionHeading("Shared queue", "$count track${if (count == 1) "" else "s"}")
}

@Composable
internal fun SyncOfflineHeader(count: Int, active: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            SyncSectionHeading("Your offline library", "$count lagu tersedia di perangkat")
        }
        if (active) {
            Text("TAP + TO QUEUE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
