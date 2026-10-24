package com.rexaps.rexmanager

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Composable
internal fun CompressScreen(sources: List<File>, onDone: () -> Unit) {
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

internal fun compress(sources: List<File>, out: File, format: ArcFormat) {
    require(!out.exists()) { "${out.name} sudah ada" }
    when (format) {
        ArcFormat.ZIP -> ZipOutputStream(out.outputStream().buffered()).use { zos ->
            sources.forEach { s ->
                walkTree(s, s.name) { f, rel ->
                    if (f.absolutePath == out.absolutePath) return@walkTree
                    zos.putNextEntry(ZipEntry(rel))
                    if (f.isFile) f.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }
        ArcFormat.SEVENZ -> SevenZOutputFile(out).use { sz ->
            sources.forEach { s ->
                walkTree(s, s.name) { f, rel ->
                    if (f.absolutePath == out.absolutePath) return@walkTree
                    sz.putArchiveEntry(sz.createArchiveEntry(f, rel))
                    if (f.isFile) {
                        f.inputStream().use { ins ->
                            val buf = ByteArray(64 * 1024)
                            while (true) {
                                val n = ins.read(buf)
                                if (n < 0) break
                                sz.write(buf, 0, n)
                            }
                        }
                    }
                    sz.closeArchiveEntry()
                }
            }
            sz.finish()
        }
    }
}
