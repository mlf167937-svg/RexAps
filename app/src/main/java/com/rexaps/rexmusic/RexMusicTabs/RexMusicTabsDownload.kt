package com.rexaps.rexmusic

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*

fun LazyListScope.downloadsSection(
    downloads: Map<String, DownloadStatus>,
    onCancel: (RexTrack) -> Unit
) {
    if (downloads.isEmpty()) return
    item(key = "dl-title") { SectionTitle("Sedang diunduh  ${downloads.size}") }
    items(downloads.entries.toList(), key = { "dl-${it.key}" }) { entry ->
        DownloadRow(entry.value) { onCancel(entry.value.track) }
    }
}
