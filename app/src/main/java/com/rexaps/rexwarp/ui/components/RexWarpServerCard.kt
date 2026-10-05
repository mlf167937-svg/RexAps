package com.rexaps.rexwarp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rexaps.rexwarp.model.RexWarpServer

@Composable
fun RexWarpServerCard(server: RexWarpServer?, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Server", style = MaterialTheme.typography.titleMedium)
            if (server == null) Text("No server information available", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else {
                Text(server.name ?: "N/A", style = MaterialTheme.typography.bodyLarge)
                Text(server.endpoint ?: "N/A", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}