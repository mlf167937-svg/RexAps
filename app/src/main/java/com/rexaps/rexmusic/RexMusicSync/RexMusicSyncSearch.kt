package com.rexaps.rexmusic

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*

@Composable
internal fun SyncOnlineSearchCard(
    query: String,
    onQueryChange: (String) -> Unit,
    searching: Boolean,
    searchMessage: String,
    onSearch: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SyncSectionHeading("Find music", "Search online tracks to download for the shared room")
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text("Judul lagu atau artis") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !searching,
                trailingIcon = {
                    TextButton(
                        enabled = !searching && query.isNotBlank(),
                        onClick = onSearch
                    ) {
                        Text(if (searching) "…" else "CARI")
                    }
                }
            )
            if (searching) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("Mencari lagu…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (searchMessage.isNotBlank()) {
                Text(
                    searchMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
