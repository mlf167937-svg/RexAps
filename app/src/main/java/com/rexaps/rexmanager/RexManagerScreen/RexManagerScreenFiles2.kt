package com.rexaps.rexmanager

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import java.io.File
import java.util.zip.ZipFile

@Composable
internal fun ArchiveScreen(file: File, onBack: () -> Unit) {
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

internal fun listArchive(f: File): List<ArcItem> = when (f.extension.lowercase()) {
    "zip" -> ZipFile(f).use { z ->
        z.entries().asSequence().map { ArcItem(it.name, it.size, it.isDirectory) }.toList()
    }
    else -> SevenZFile(f).use { sz ->
        generateSequence { sz.nextEntry }.map { ArcItem(it.name, it.size, it.isDirectory) }.toList()
    }
}
