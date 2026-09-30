package com.rexaps.rexmanager.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.rexaps.rexmanager.RexFile
import com.rexaps.rexmanager.utils.FileTypeResolver

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RexManagerFileList(
    files: List<RexFile>,
    selectedPaths: Set<String>,
    showExtensions: Boolean,
    onClick: (RexFile) -> Unit,
    onLongClick: (RexFile) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(files, key = { it.path }) { file ->
            val selected = file.path in selectedPaths
            ListItem(
                modifier = Modifier.combinedClickable(
                    onClick = { onClick(file) },
                    onLongClick = { onLongClick(file) }
                ),
                colors = ListItemDefaults.colors(
                    containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer
                    else Color.Transparent
                ),
                leadingContent = {
                    if (selected) Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                    else RexFileIcon(file)
                },
                headlineContent = {
                    Text(FileTypeResolver.displayName(file, showExtensions), maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                supportingContent = { Text(file.subtitle(), maxLines = 1, overflow = TextOverflow.Ellipsis) }
            )
        }
    }
}
