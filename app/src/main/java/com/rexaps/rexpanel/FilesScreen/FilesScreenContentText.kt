package com.rexaps.rexpanel

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException

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

@Composable
internal fun PreviewError(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Warning, null, Modifier.size(48.dp), tint = Color.White.copy(alpha = 0.8f))
        Spacer(Modifier.height(12.dp))
        Text("Pratinjau gagal", style = MaterialTheme.typography.titleMedium, color = Color.White)
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Coba lagi") }
    }
}
