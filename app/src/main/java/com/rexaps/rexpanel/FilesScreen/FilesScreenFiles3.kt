package com.rexaps.rexpanel

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FileRow(
    f: RemoteFile,
    b: FileBrowserState,
    isSel: Boolean,
    selecting: Boolean,
    onMenu: (RemoteFile) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val sub = buildString {
        append(if (f.isDir) "Folder" else fmtBytes(f.size))
        append(" · ").append(fmtDate(f.mtimeSec))
        if (f.isLink) append(" · tautan")
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSel) colors.primaryContainer.copy(alpha = 0.6f) else Color.Transparent)
            .combinedClickable(
                onClick = { if (selecting) b.toggleSelect(f) else b.open(f) },
                onLongClick = { b.toggleSelect(f) },
                onLongClickLabel = "Pilih"
            )
            .semantics { selected = isSel }
            .padding(start = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selecting) {
            Checkbox(checked = isSel, onCheckedChange = null)
            Spacer(Modifier.width(12.dp))
        }
        FileThumb(f, b, Modifier.size(48.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                f.name,
                style = MaterialTheme.typography.bodyLarge,
                color = nameBlue(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                sub,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (selecting) {
            Spacer(Modifier.width(16.dp))
        } else {
            IconButton(onClick = { onMenu(f) }) { Icon(Icons.Default.MoreVert, "Opsi ${f.name}") }
        }
    }
}

@Composable
internal fun FileThumb(f: RemoteFile, b: FileBrowserState, modifier: Modifier, big: Boolean = false) {
    val st = styleOf(f, MaterialTheme.colorScheme.onSurfaceVariant)
    val wantThumb = f.kind == FileKind.Image && f.size in 1..THUMB_MAX
    var bmp by remember(f.path, f.mtimeSec) {
        mutableStateOf(if (wantThumb) b.cachedThumb(f)?.asImageBitmap() else null)
    }
    if (wantThumb) {
        LaunchedEffect(f.path, f.mtimeSec) {
            if (bmp == null) bmp = b.thumbnail(f)?.asImageBitmap()
        }
    }
    val shown = bmp
    Box(
        modifier
            .clip(RoundedCornerShape(if (big) 16.dp else 14.dp))
            .background(st.color.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        if (shown != null) {
            Image(shown, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Icon(st.icon, null, tint = st.color, modifier = Modifier.fillMaxSize(if (big) 0.42f else 0.55f))
        }
    }
}
