package com.rexaps.rexmusic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

fun LazyListScope.offlineSection(
    state: RexMusicUiState,
    actions: RexActions,
    query: String,
    full: Boolean
) {
    val offline = state.offline
    val q = query.trim()
    val shown = if (q.isEmpty()) offline.entries else offline.entries.filter {
        it.track.title.contains(q, ignoreCase = true) || it.track.artist.contains(q, ignoreCase = true)
    }

    if (full) {
        item(key = "off-hero") {
            OfflineHeroCard(
                count = offline.entries.size,
                bytes = offline.totalBytes,
                enabled = offline.entries.isNotEmpty(),
                onPlayAll = { actions.playOfflineAll(false) },
                onShuffle = { actions.playOfflineAll(true) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        if (!offline.hasAccess) {
            item(key = "off-permission") {
                PermissionCard(actions.requestAccess, Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }
        }
    }

    downloadsSection(offline.downloads, actions.cancelDownload)

    if (shown.isEmpty()) {
        item(key = "off-empty") {
            EmptyState(
                Icons.Default.CloudOff,
                if (q.isNotEmpty()) "Tidak ada lagu yang cocok" else "Belum ada lagu offline",
                if (q.isNotEmpty()) "Coba kata kunci lain."
                else "Pindah ke mode Online, lalu buka menu  pada lagu dan pilih Unduh."
            )
        }
    } else {
        item(key = "off-title") {
            SectionTitle(if (full) "Lagu offline  ${shown.size}" else "Hasil offline  ${shown.size}")
        }
        items(shown, key = { "o-${it.key}" }) { entry ->
            TrackRowFor(
                entry.track, state, actions,
                subtitle = "${entry.track.artist}  ${formatBytes(entry.sizeBytes)}"
            ) { actions.playOffline(entry.track) }
        }
        if (full) {
            item(key = "off-delete-all") {
                TextButton(
                    onClick = actions.deleteAll,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Hapus semua unduhan")
                }
            }
        }
    }
}

@Composable
internal fun LibraryChip(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Box(
        Modifier
            .height(36.dp)
            .clip(CircleShape)
            .background(
                if (selected) scheme.primary
                else scheme.surfaceVariant
            )
            .clickable(
                role = Role.Tab,
                onClick = onClick
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count > 0) "$label $count" else label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) scheme.onPrimary
            else scheme.onSurface,
            maxLines = 1
        )
    }
}
