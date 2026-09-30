package com.rexaps.rexmanager.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rexaps.rexmanager.StorageRoot
import com.rexaps.rexmanager.filesystem.StorageInfo
import com.rexaps.rexmanager.utils.FileSizeFormatter

@Composable
fun RexManagerStorageHeader(
    root: StorageRoot?,
    roots: List<StorageRoot>,
    currentPath: String,
    storageInfo: StorageInfo?,
    onSelectRoot: (StorageRoot) -> Unit,
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                Row(Modifier.clickable { open = true }, verticalAlignment = Alignment.CenterVertically) {
                    Text(root?.name ?: "Storage", style = MaterialTheme.typography.titleMedium)
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = "Choose storage")
                }
                DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                    roots.forEach { r ->
                        DropdownMenuItem(text = { Text(r.name) }, onClick = { open = false; onSelectRoot(r) })
                    }
                }
            }
        }
        if (storageInfo != null) {
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(progress = { storageInfo.usedFraction }, modifier = Modifier.fillMaxWidth())
            Text(
                "${FileSizeFormatter.format(storageInfo.used)} used of ${FileSizeFormatter.format(storageInfo.total)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
