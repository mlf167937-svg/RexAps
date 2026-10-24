package com.rexaps.rexpanel

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

// Nama file memakai aksen tema aplikasi; warnanya ikut berubah saat tema RexAps diganti.
@Composable
internal fun nameBlue(): Color = MaterialTheme.colorScheme.primary

@Composable
internal fun TransferBar(t: TransferUi, onCancel: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceVariant,
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
    ) {
        Row(
            Modifier.padding(start = 16.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${t.title} ${t.index}/${t.count}",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant
                )
                Text(t.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                if (t.total > 0) {
                    LinearProgressIndicator(
                        progress = (t.done.toFloat() / t.total).coerceIn(0f, 1f),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "${fmtBytes(t.done)} / ${fmtBytes(t.total)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                } else {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
            IconButton(onClick = onCancel) { Icon(Icons.Default.Close, "Batalkan transfer") }
        }
    }
}

@Composable
internal fun SelectionBar(
    count: Int,
    canRename: Boolean,
    onClose: () -> Unit,
    onAll: () -> Unit,
    onDownload: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    CompositionLocalProvider(LocalContentColor provides colors.onPrimaryContainer) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(colors.primaryContainer)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Batalkan pilihan") }
            Text(
                "$count dipilih",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .weight(1f)
                    .semantics { liveRegion = LiveRegionMode.Polite }
            )
            if (canRename) {
                IconButton(onClick = onRename) { Icon(Icons.Default.DriveFileRenameOutline, "Ganti nama") }
            }
            IconButton(onClick = onAll) { Icon(Icons.Default.SelectAll, "Pilih semua") }
            IconButton(onClick = onDownload) { Icon(Icons.Default.Download, "Unduh yang dipilih") }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Hapus yang dipilih", tint = colors.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlaceChip(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null, Modifier.size(18.dp)) }
    )
}

/** Jalan pintas ke lokasi penting: Home, Termux, Penyimpanan (/sdcard), dan Root. */
@Composable
internal fun QuickPlaces(b: FileBrowserState) {
    val inStorage = b.path.startsWith(PLACE_STORAGE) || b.path.startsWith("/sdcard")
    val inTermux = b.path.startsWith(PLACE_TERMUX) && !b.isHome
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PlaceChip("Home", Icons.Default.Home, b.isHome) { b.goHome() }
        PlaceChip("Termux", Icons.Default.Terminal, inTermux) { b.go(PLACE_TERMUX) }
        PlaceChip("Penyimpanan", Icons.Default.SdStorage, inStorage) { b.go(PLACE_STORAGE) }
        PlaceChip("Root /", Icons.Default.Storage, b.path == "/") { b.go("/") }
    }
}

@Composable
internal fun Breadcrumbs(b: FileBrowserState) {
    val colors = MaterialTheme.colorScheme
    val scroll = rememberScrollState()
    val segs = remember(b.path) {
        buildList {
            add("/" to "/")
            var acc = ""
            b.path.split('/').filter { it.isNotEmpty() }.forEach { acc += "/$it"; add(it to acc) }
        }
    }
    LaunchedEffect(segs) {
        withFrameNanos { }
        scroll.animateScrollTo(scroll.maxValue)
    }
    Row(
        Modifier.fillMaxWidth().horizontalScroll(scroll).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        segs.forEachIndexed { i, (label, target) ->
            val last = i == segs.lastIndex
            if (i > 0) Icon(Icons.Default.ChevronRight, null, Modifier.size(16.dp), tint = colors.onSurfaceVariant)
            Box(
                Modifier
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button, enabled = !last) { b.go(target) }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (last) colors.primary else colors.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun GridTile(
    f: RemoteFile,
    b: FileBrowserState,
    isSel: Boolean,
    selecting: Boolean,
    onMenu: (RemoteFile) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSel) colors.primaryContainer.copy(alpha = 0.6f) else Color.Transparent)
            .combinedClickable(
                onClick = { if (selecting) b.toggleSelect(f) else b.open(f) },
                onLongClick = { b.toggleSelect(f) },
                onLongClickLabel = "Pilih"
            )
            .semantics {
                selected = isSel
                customActions = listOf(CustomAccessibilityAction("Opsi") { onMenu(f); true })
            }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
            FileThumb(f, b, Modifier.fillMaxSize(), big = true)
            if (f.kind == FileKind.Video) {
                Icon(
                    Icons.Default.PlayArrow, null,
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                )
            }
            if (selecting) {
                Icon(
                    if (isSel) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    null,
                    tint = if (isSel) colors.primary else Color.White,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .size(24.dp)
                        .background(Color.Black.copy(alpha = 0.25f), CircleShape)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            f.name,
            style = MaterialTheme.typography.labelMedium,
            color = nameBlue(),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        if (!f.isDir) {
            Text(
                fmtBytes(f.size),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}
