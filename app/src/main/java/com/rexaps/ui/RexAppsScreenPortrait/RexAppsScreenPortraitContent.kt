package com.rexaps.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
internal fun ProEmptyState() {
    val c = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = c.onSurfaceVariant, modifier = Modifier.size(36.dp))
        Text("Aplikasi tidak ditemukan", style = MaterialTheme.typography.titleMedium)
        Text(
            "Coba kata kunci lain atau ubah filter.",
            style = MaterialTheme.typography.bodySmall,
            color = c.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
