package com.rexaps.rexmanager.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rexaps.rexmanager.RexFile
import com.rexaps.rexmanager.utils.FileTypeResolver

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RexManagerFileGrid(
    files: List<RexFile>,
    selectedPaths: Set<String>,
    showExtensions: Boolean,
    onClick: (RexFile) -> Unit,
    onLongClick: (RexFile) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(104.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(files, key = { it.path }) { file ->
            val selected = file.path in selectedPaths
            Column(
                modifier = Modifier
                    .padding(4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                    .combinedClickable(onClick = { onClick(file) }, onLongClick = { onLongClick(file) })
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                RexFileIcon(file, Modifier.size(48.dp))
                Spacer(Modifier.height(4.dp))
                Text(
                    FileTypeResolver.displayName(file, showExtensions),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center
                )
            }
        }
    }
}
