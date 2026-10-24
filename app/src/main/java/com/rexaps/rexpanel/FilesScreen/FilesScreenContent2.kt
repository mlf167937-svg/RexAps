package com.rexaps.rexpanel

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
internal fun __splitImpl_SelectionBar(
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

@Composable
internal fun __splitImpl_StateMessage(
    icon: ImageVector,
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    secondaryLabel: String? = null,
    onSecondary: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, Modifier.size(48.dp), tint = colors.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
        if (secondaryLabel != null) {
            TextButton(onClick = onSecondary) { Text(secondaryLabel) }
        }
    }
}

@Composable
internal fun __splitImpl_SkeletonRows() {
    val c = MaterialTheme.colorScheme.surfaceVariant
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        repeat(7) {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(c))
                Spacer(Modifier.width(12.dp))
                Column {
                    Box(Modifier.width(160.dp).height(14.dp).clip(RoundedCornerShape(6.dp)).background(c))
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.width(100.dp).height(10.dp).clip(RoundedCornerShape(6.dp)).background(c))
                }
            }
        }
    }
}

/** Jalan pintas ke lokasi penting: Home, Termux, Penyimpanan (/sdcard), dan Root. */
@Composable
internal fun __splitImpl_QuickPlaces(b: FileBrowserState) {
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
