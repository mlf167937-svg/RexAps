package com.rexaps.rexmanager

import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image as ImageIcon
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

// ───────────────────────── Home ─────────────────────────
@Composable
internal fun HomeScreen(onOpen: (String) -> Unit, onBack: () -> Unit, onMenu: () -> Unit) {
    val root = Environment.getExternalStorageDirectory()
    val stat = remember(root.path) { StatFs(root.path) }
    val total = stat.totalBytes.coerceAtLeast(1L)
    val free = stat.availableBytes.coerceAtLeast(0L)
    val used = (total - free).coerceAtLeast(0L)
    val fraction = (used.toFloat() / total).coerceIn(0f, 1f)
    val shortcuts = listOf(
        Triple("Download", File(root, "Download"), Icons.Filled.Inventory2),
        Triple("Gambar", File(root, "Pictures"), Icons.Filled.ImageIcon),
        Triple("Video", File(root, "Movies"), Icons.Filled.Movie),
        Triple("Musik", File(root, "Music"), Icons.Filled.MusicNote),
        Triple("Dokumen", File(root, "Documents"), Icons.Filled.InsertDriveFile),
        Triple("Kamera", File(root, "DCIM"), Icons.Filled.ImageIcon)
    )

    Column(Modifier.fillMaxSize().background(Rex.bg.copy(alpha = 0.94f))) {
        Row(
            Modifier.fillMaxWidth().padding(start = 10.dp, end = 12.dp, top = 12.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onMenu,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(Rex.surfaceHigh)
            ) {
                Icon(Icons.Filled.Menu, contentDescription = "Buka menu navigasi", tint = Rex.onSurface, modifier = Modifier.size(25.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("RexManager", color = Rex.onSurface, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text("File manager untuk semua kebutuhanmu", color = Rex.muted, fontSize = 11.sp)
            }
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali ke RexAps", tint = Rex.muted)
            }
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Rex.surface)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Rex.surfaceHigh), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.FolderOpen, null, tint = Rex.primary, modifier = Modifier.size(25.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Penyimpanan internal", color = Rex.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Text("${formatSize(free)} tersedia dari ${formatSize(total)}", color = Rex.muted, fontSize = 12.sp)
                        }
                    }
                    LinearProgressIndicator(
                        progress = { fraction }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp)),
                        color = if (fraction > .9f) Rex.danger else Rex.primary, trackColor = Rex.surfaceHigh
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${formatSize(used)} digunakan", color = Rex.muted, fontSize = 11.sp)
                        Text("${(fraction * 100).toInt()}%", color = Rex.onSurface, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = { onOpen(root.path) }, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Rex.primary)
                    ) {
                        Icon(Icons.Filled.FolderOpen, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp)); Text("Jelajahi semua file")
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Akses cepat", color = Rex.onSurface, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text("6 lokasi", color = Rex.muted, fontSize = 11.sp)
                }
                shortcuts.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        pair.forEach { (label, file, icon) ->
                            val count = remember(file.path) { file.list()?.size ?: 0 }
                            Row(
                                Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(Rex.surface)
                                    .clickable { onOpen(file.path) }.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(Rex.surfaceHigh), contentAlignment = Alignment.Center) {
                                    Icon(icon, null, tint = Rex.primary, modifier = Modifier.size(21.dp))
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(label, color = Rex.onSurface, fontWeight = FontWeight.Medium, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(if (file.exists()) "$count item" else "Belum ada", color = Rex.muted, fontSize = 11.sp)
                                }
                            }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Rex.surfaceHigh)
                    .clickable { onOpen(root.path) }.padding(15.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Folder, null, tint = Rex.folder)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Lokasi perangkat", color = Rex.onSurface, fontWeight = FontWeight.Medium)
                    Text(root.path, color = Rex.muted, fontSize = 11.sp)
                }
                Icon(Icons.Filled.ChevronRight, null, tint = Rex.muted)
            }
            Text("RexManager • Kelola file dengan lebih mudah", color = Rex.muted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 18.dp))
        }
    }
}
