package com.rexaps.rexmanager.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rexaps.rexmanager.StorageRoot
import java.io.File

@Composable
fun RexManagerBreadcrumb(
    root: StorageRoot?,
    currentPath: String,
    onSegmentClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (root == null) return
    val segments = remember(root, currentPath) {
        val list = mutableListOf(root.name to root.path)
        if (currentPath.startsWith(root.path + File.separator)) {
            var acc = root.path
            currentPath.removePrefix(root.path).split(File.separator).filter { it.isNotEmpty() }
                .forEach { acc = acc + File.separator + it; list += it to acc }
        }
        list
    }
    val scroll = rememberScrollState()
    LaunchedEffect(segments) { scroll.scrollTo(scroll.maxValue) }
    Row(
        modifier.horizontalScroll(scroll).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        segments.forEachIndexed { i, (label, path) ->
            if (i > 0) Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { onSegmentClick(path) }, enabled = path != currentPath) { Text(label) }
        }
    }
}
