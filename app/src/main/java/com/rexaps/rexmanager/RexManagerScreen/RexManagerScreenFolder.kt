package com.rexaps.rexmanager

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

// ───────────────────────── Folder ─────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FolderScreen(
    path: String,
    onBack: () -> Unit,
    onOpen: (File) -> Unit,
    onCompress: (List<String>) -> Unit
) {
    val ctx = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    var query by rememberSaveable(path) { mutableStateOf("") }
    val dir = File(path)
    val files = remember(path, refresh) {
        dir.listFiles()
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            ?: emptyList()
    }
    val visibleFiles = remember(files, query) {
        if (query.isBlank()) files else files.filter { it.name.contains(query.trim(), ignoreCase = true) }
    }
    val selected = remember(path) { mutableStateListOf<String>() }
    var confirmDelete by remember { mutableStateOf(false) }
    BackHandler(enabled = selected.isNotEmpty()) { selected.clear() }

    Column(Modifier.fillMaxSize()) {
        RexTopBar(
            title = if (selected.isEmpty()) dir.name.ifEmpty { "Penyimpanan" } else "${selected.size} dipilih",
            subtitle = path,
            onBack = onBack
        )
        Box(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(14.dp)).background(Rex.surface).padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.FolderOpen, null, tint = Rex.muted, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(10.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = Rex.onSurface, fontSize = 14.sp),
                    cursorBrush = SolidColor(Rex.primary),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        Box {
                            if (query.isEmpty()) Text("Cari file atau folder...", color = Rex.muted, fontSize = 14.sp)
                            inner()
                        }
                    }
                )
            }
        }
        if (visibleFiles.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.FolderOpen, null, tint = Rex.muted, modifier = Modifier.size(48.dp))
                    Text(if (query.isBlank()) "Folder ini kosong" else "Tidak ada hasil untuk ‘$query’", color = Rex.muted)
                }
            }
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(visibleFiles, key = { it.path }) { f ->
                    val (icon, tint) = iconFor(f)
                    val isSel = f.path in selected
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(if (isSel) Rex.surfaceHigh else Color.Transparent)
                            .combinedClickable(
                                onClick = {
                                    if (selected.isNotEmpty()) {
                                        if (isSel) selected.remove(f.path) else selected.add(f.path)
                                    } else {
                                        onOpen(f)
                                    }
                                },
                                onLongClick = { if (!isSel) selected.add(f.path) }
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, null, tint = tint, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(f.name, color = Rex.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                if (f.isDirectory) "${f.list()?.size ?: 0} item" else formatSize(f.length()),
                                color = Rex.muted, fontSize = 12.sp
                            )
                        }
                        if (isSel) Icon(Icons.Filled.CheckCircle, "Dipilih", tint = Rex.primary)
                    }
                }
            }
        }
        if (selected.isNotEmpty()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Rex.surface)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = { onCompress(selected.toList()) }) {
                    Icon(Icons.Filled.Inventory2, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Kompres")
                }
                TextButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Filled.Delete, null, tint = Rex.danger)
                    Spacer(Modifier.width(6.dp))
                    Text("Hapus", color = Rex.danger)
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Hapus ${selected.size} item?") },
            text = { Text("Item yang dihapus tidak bisa dikembalikan.") },
            confirmButton = {
                TextButton(onClick = {
                    selected.forEach { File(it).deleteRecursively() }
                    selected.clear()
                    confirmDelete = false
                    refresh++
                    Toast.makeText(ctx, "Terhapus", Toast.LENGTH_SHORT).show()
                }) { Text("Hapus", color = Rex.danger) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Batal") } }
        )
    }
}
