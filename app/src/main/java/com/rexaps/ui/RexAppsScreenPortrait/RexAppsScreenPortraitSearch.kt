// RexAppsScreenPortrait.kt
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

internal fun proFilterApps(apps: List<RexModule>, query: String, filter: String): List<RexModule> {
    val q = query.trim()
    return apps.filter { app ->
        val matchQuery = app.name.contains(q, ignoreCase = true) ||
            app.description.contains(q, ignoreCase = true)
        val matchFilter = when (filter) {
            "Tersedia" -> app.available
            "Segera" -> !app.available
            else -> true
        }
        matchQuery && matchFilter
    }
}

@Composable
internal fun ProSearchBar(
    query: String,
    onQuery: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = MaterialTheme.colorScheme
    ProGlass(modifier.fillMaxWidth(), radius = 20.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, contentDescription = "Cari", tint = c.primary)
            Spacer(Modifier.width(8.dp))
            TextField(
                value = query,
                onValueChange = onQuery,
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("Cari aplikasi atau fitur…") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
internal fun ProFilterRow(selected: String, onSelect: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf("Semua", "Tersedia", "Segera").forEach { f ->
            FilterChip(
                selected = selected == f,
                onClick = { onSelect(f) },
                label = { Text(f) }
            )
        }
    }
}
