package com.rexaps.rexpanel

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal val PreviewFg = Color(0xFFCDD6F4)

@Composable
internal fun FilePreview(b: FileBrowserState, file: RemoteFile, modifier: Modifier) {
    val siblings = remember(b.entries, file.path) {
        if (file.kind == FileKind.Other) emptyList()
        else b.entries.filter { !it.isDir && it.kind == file.kind }.sortedBy { it.name.lowercase() }
    }
    val idx = siblings.indexOfFirst { it.path == file.path }
    val prev = if (idx > 0) siblings[idx - 1] else null
    val next = if (idx in 0 until siblings.lastIndex) siblings[idx + 1] else null

    Column(modifier.background(Color.Black)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { b.preview = null }) {
                Icon(Icons.Default.Close, "Tutup pratinjau", tint = Color.White)
            }
            Column(Modifier.weight(1f)) {
                // Latar pratinjau selalu hitam, jadi pakai biru terang.
                Text(
                    file.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = nameBlue(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${fmtBytes(file.size)} · ${fmtDate(file.mtimeSec)}" +
                        if (siblings.size > 1 && idx >= 0) " · ${idx + 1}/${siblings.size}" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1
                )
            }
            if (siblings.size > 1) {
                IconButton(onClick = { prev?.let { b.preview = it } }, enabled = prev != null) {
                    Icon(Icons.Default.SkipPrevious, "Sebelumnya", tint = Color.White.copy(alpha = if (prev != null) 1f else 0.3f))
                }
                IconButton(onClick = { next?.let { b.preview = it } }, enabled = next != null) {
                    Icon(Icons.Default.SkipNext, "Berikutnya", tint = Color.White.copy(alpha = if (next != null) 1f else 0.3f))
                }
            }
            IconButton(onClick = { b.transfers.download(listOf(file)) }) {
                Icon(Icons.Default.Download, "Unduh ke $DOWNLOAD_LABEL", tint = Color.White)
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            key(file.path) {
                when (file.kind) {
                    FileKind.Image -> ImagePreview(b, file)
                    FileKind.Video, FileKind.Audio -> PlayerPreview(b, file)
                    FileKind.Text -> TextPreview(b, file)
                    else -> NoPreview(file)
                }
            }
        }
    }
}

@Composable
internal fun ImagePreview(b: FileBrowserState, f: RemoteFile) {
    var progress by remember { mutableLongStateOf(0L) }
    var attempt by remember { mutableIntStateOf(0) }
    var result by remember { mutableStateOf<ImgLoad?>(null) }

    LaunchedEffect(f.path, attempt) {
        result = null
        progress = 0
        result = try {
            val local = b.fetch(f) { progress = it }
            val bmp = withContext(Dispatchers.Default) { decodeFileSampled(local, 2048) }
            if (bmp != null) ImgLoad.Ready(bmp.asImageBitmap()) else ImgLoad.Failed("Format gambar ini tidak bisa ditampilkan.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ImgLoad.Failed(sftpMessage(e))
        } catch (e: OutOfMemoryError) {
            ImgLoad.Failed("Gambar terlalu besar untuk ditampilkan.")
        }
    }

    when (val r = result) {
        null -> DownloadProgress(progress, f.size, "Mengunduh gambar")
        is ImgLoad.Ready -> ZoomableImage(r.bmp, f.name)
        is ImgLoad.Failed -> PreviewError(r.msg) { attempt++ }
    }
}

@Composable
internal fun PlayerPreview(b: FileBrowserState, f: RemoteFile) {
    var started by remember { mutableStateOf(f.size <= AUTO_DOWNLOAD) }
    var attempt by remember { mutableIntStateOf(0) }
    var progress by remember { mutableLongStateOf(0L) }
    var local by remember { mutableStateOf<File?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(started, attempt) {
        if (!started) return@LaunchedEffect
        local = null; error = null; progress = 0
        try {
            local = b.fetch(f) { progress = it }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = sftpMessage(e)
        }
    }

    val err = error
    val ready = local
    when {
        err != null -> PreviewError(err) { attempt++ }
        ready != null -> VideoPlayer(ready, audio = f.kind == FileKind.Audio) {
            error = "Format ini tidak didukung pemutar bawaan Android."
        }
        !started -> Column(
            Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                if (f.kind == FileKind.Audio) Icons.Default.Audiotrack else Icons.Default.Movie,
                null, Modifier.size(56.dp), tint = Color.White.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Berkas besar (${fmtBytes(f.size)})",
                style = MaterialTheme.typography.titleMedium, color = Color.White
            )
            Text(
                "Berkas diunduh dulu ke cache lalu diputar. Pakai Wi-Fi jika bisa.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = { started = true }) { Text("Unduh dan putar") }
        }
        else -> DownloadProgress(progress, f.size, "Mengunduh untuk diputar")
    }
}

@Composable
internal fun TextPreview(b: FileBrowserState, f: RemoteFile) {
    var text by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(f.path) {
        text = try {
            b.readText(f)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            "Gagal membaca berkas: ${sftpMessage(e)}"
        }
    }
    val t = text
    when {
        t == null -> CircularProgressIndicator(color = Color.White)
        '\u0000' in t -> NoPreview(f, "Berkas ini biner, jadi tidak bisa ditampilkan sebagai teks.")
        else -> SelectionContainer {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                Text(
                    t,
                    color = PreviewFg,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    softWrap = false
                )
                if (f.size > 65536) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "… dipotong, hanya 64 KB pertama dari ${fmtBytes(f.size)}.",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
internal fun NoPreview(f: RemoteFile, message: String = "Pratinjau belum tersedia untuk jenis berkas ini. Kamu tetap bisa mengunduhnya.") {
    val st = styleOf(f, Color.White)
    Column(
        Modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(st.icon, null, Modifier.size(64.dp), tint = st.color)
        Spacer(Modifier.height(12.dp))
        Text(f.name, style = MaterialTheme.typography.titleMedium, color = nameBlue(), textAlign = TextAlign.Center)
        Text(
            "${st.label} · ${fmtBytes(f.size)} · ${f.perms}",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}
