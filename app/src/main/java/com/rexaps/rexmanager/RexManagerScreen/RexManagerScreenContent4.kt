package com.rexaps.rexmanager

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image as ImageIcon
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun __splitImpl_CompressScreen(sources: List<File>, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var format by remember { mutableStateOf(ArcFormat.ZIP) }
    var name by remember {
        mutableStateOf(if (sources.size == 1) sources[0].nameWithoutExtension else "Arsip")
    }
    var running by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        RexTopBar("Kompres File", onBack = if (running) null else onDone)
        LazyColumn(
            Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(sources, key = { it.path }) { f ->
                val (icon, tint) = iconFor(f)
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, null, tint = tint)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        f.name, color = Rex.onSurface, modifier = Modifier.weight(1f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(if (f.isFile) formatSize(f.length()) else "Folder", color = Rex.muted, fontSize = 12.sp)
                }
            }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Format Arsip", color = Rex.muted, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ArcFormat.entries.forEach { f ->
                    FilterChip(
                        selected = format == f,
                        onClick = { format = f },
                        enabled = !running,
                        label = { Text(f.ext.uppercase()) }
                    )
                }
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.filter { c -> c != '/' && c != '\\' } },
                label = { Text("Nama arsip") },
                singleLine = true,
                enabled = !running,
                suffix = { Text(".${format.ext}") },
                modifier = Modifier.fillMaxWidth()
            )
            error?.let { Text(it, color = Rex.danger, fontSize = 13.sp) }
            Button(
                onClick = {
                    running = true
                    error = null
                    scope.launch {
                        val out = File(sources[0].parentFile, "${name.ifBlank { "Arsip" }}.${format.ext}")
                        val r = withContext(Dispatchers.IO) { runCatching { compress(sources, out, format) } }
                        running = false
                        r.onSuccess {
                            Toast.makeText(ctx, "Dibuat: ${out.name}", Toast.LENGTH_SHORT).show()
                            onDone()
                        }.onFailure {
                            error = "Gagal membuat arsip: ${it.message}"
                            if (!it.message.orEmpty().contains("sudah ada")) out.delete()
                        }
                    }
                },
                enabled = !running && name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Rex.primary)
            ) {
                if (running) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Mengompres…")
                } else {
                    Text("Buat Arsip")
                }
            }
        }
    }
}

@Composable
internal fun __splitImpl_ArchiveScreen(file: File, onBack: () -> Unit) {
    var entries by remember { mutableStateOf<List<ArcItem>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(file) {
        withContext(Dispatchers.IO) {
            runCatching { listArchive(file) }
                .onSuccess { entries = it }
                .onFailure { error = it.message ?: "Arsip tidak bisa dibaca" }
        }
    }

    Column(Modifier.fillMaxSize()) {
        RexTopBar(file.name, "${formatSize(file.length())} • isi arsip", onBack)
        val list = entries
        when {
            error != null -> Text(
                "Tidak bisa membuka arsip: $error",
                color = Rex.danger, modifier = Modifier.padding(16.dp)
            )
            list == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            list.isEmpty() -> Text("Arsip kosong", color = Rex.muted, modifier = Modifier.padding(16.dp))
            else -> LazyColumn {
                items(list) { entry ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (entry.isDir) Icons.Filled.Folder else Icons.Filled.InsertDriveFile,
                            null,
                            tint = if (entry.isDir) Rex.folder else Rex.muted
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            entry.name, color = Rex.onSurface, modifier = Modifier.weight(1f),
                            maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 14.sp
                        )
                        if (!entry.isDir) Text(formatSize(entry.size), color = Rex.muted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
internal fun __splitImpl_PermissionGate(onGrant: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.Folder, null, tint = Rex.folder, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            "Izinkan akses semua file",
            color = Rex.onSurface, fontSize = 20.sp, fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "RexManager butuh izin ini untuk menampilkan, mengompres, dan mengedit file kamu.",
            color = Rex.muted, fontSize = 14.sp
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onGrant, colors = ButtonDefaults.buttonColors(containerColor = Rex.primary)) {
            Text("Buka pengaturan")
        }
    }
}

@Composable
internal fun __splitImpl_iconFor(f: File): Pair<ImageVector, Color> {
    val e = f.extension.lowercase()
    return when {
        f.isDirectory -> Icons.Filled.Folder to Rex.folder
        e in IMAGE_EXT -> Icons.Filled.ImageIcon to Color(0xFFB05CFF)
        e in AUDIO_EXT -> Icons.Filled.MusicNote to Color(0xFF3BC4F5)
        e in VIDEO_EXT -> Icons.Filled.Movie to Color(0xFFEF5350)
        e in ARCHIVE_EXT -> Icons.Filled.Inventory2 to Color(0xFFF5883B)
        else -> Icons.Filled.InsertDriveFile to Rex.muted
    }
}
