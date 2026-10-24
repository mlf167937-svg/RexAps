package com.rexaps.rexadm

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rexaps.librex.rexsmartmanager.DownloadProgress
import com.rexaps.librex.rexsmartmanager.DownloadRequest
import com.rexaps.librex.rexsmartmanager.DownloadStatus
import com.rexaps.librex.rexsmartmanager.OkHttpRexSmartManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var manager: OkHttpRexSmartManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        manager = OkHttpRexSmartManager(applicationContext)
        val initialUrl = extractUrl(intent)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    RexAdmScreen(manager = manager, initialUrl = initialUrl)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Activity recreation handles cold starts; for an existing instance, users can paste the URL.
    }

    private fun extractUrl(intent: Intent?): String {
        val raw = when (intent?.action) {
            Intent.ACTION_VIEW -> intent.dataString
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            else -> null
        }?.trim().orEmpty()
        val match = Regex("https?://\\S+", RegexOption.IGNORE_CASE).find(raw)?.value
        return match?.trimEnd('.', ',', ')', ']', '}', '>', '"', '\'').orEmpty()
    }
}

@Composable
private fun RexAdmScreen(manager: OkHttpRexSmartManager, initialUrl: String) {
    var url by remember(initialUrl) { mutableStateOf(initialUrl) }
    val downloads by manager.downloads.collectAsState()
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("RexADM", style = MaterialTheme.typography.headlineMedium)
        Text("Download engine • RexAps", style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("URL file") },
            placeholder = { Text("https://example.com/file.zip") },
            singleLine = true
        )
        Button(
            onClick = {
                val candidate = url.trim()
                if (candidate.startsWith("https://", true) || candidate.startsWith("http://", true)) {
                    runCatching { manager.enqueue(DownloadRequest(candidate)) }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Mulai download") }
        Text("Unduhan", style = MaterialTheme.typography.titleLarge)
        if (downloads.isEmpty()) {
            Text("Belum ada unduhan. Tempel URL langsung atau bagikan URL dari RexFox ke RexADM.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(downloads, key = { it.id }) { item -> DownloadCard(item, manager::pause, manager::resume, manager::cancel) }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Catatan: versi starter menyimpan file di folder Downloads milik aplikasi. Metadata antrean belum persisten jika aplikasi ditutup.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun DownloadCard(
    item: DownloadProgress,
    pause: (String) -> Unit,
    resume: (String) -> Unit,
    cancel: (String) -> Unit
) {
    val context = LocalContext.current
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(item.fileName, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(item.url, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(statusLabel(item), style = MaterialTheme.typography.bodyMedium)
            item.percent?.let { LinearProgressIndicator(progress = { it / 100f }, modifier = Modifier.fillMaxWidth()) }
            Text("${formatBytes(item.downloadedBytes)}${item.totalBytes?.let { " / ${formatBytes(it)}" } ?: ""}", style = MaterialTheme.typography.bodySmall)
            item.message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (item.status) {
                    DownloadStatus.DOWNLOADING -> Button(onClick = { pause(item.id) }) { Text("Jeda") }
                    DownloadStatus.PAUSED, DownloadStatus.FAILED -> Button(onClick = { resume(item.id) }) { Text("Lanjut") }
                    DownloadStatus.ACTION_REQUIRED -> {
                        Button(onClick = {
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url))) }
                        }) { Text("Buka browser") }
                        Button(onClick = { resume(item.id) }) { Text("Coba lagi") }
                    }
                    else -> Unit
                }
                if (item.status !in setOf(DownloadStatus.COMPLETED, DownloadStatus.CANCELLED)) {
                    Button(onClick = { cancel(item.id) }) { Text("Batal") }
                }
            }
        }
    }
}

private fun statusLabel(item: DownloadProgress): String = when (item.status) {
    DownloadStatus.QUEUED -> "Dalam antrean"
    DownloadStatus.DOWNLOADING -> "Mengunduh${item.percent?.let { " • $it%" } ?: ""}"
    DownloadStatus.PAUSED -> "Dijeda"
    DownloadStatus.ACTION_REQUIRED -> "Perlu tindakan"
    DownloadStatus.COMPLETED -> "Selesai • ${item.filePath.orEmpty()}"
    DownloadStatus.FAILED -> "Gagal"
    DownloadStatus.CANCELLED -> "Dibatalkan"
}

private fun formatBytes(value: Long): String {
    if (value < 1024) return "$value B"
    val units = listOf("KB", "MB", "GB", "TB")
    var size = value.toDouble()
    var index = -1
    do { size /= 1024.0; index++ } while (size >= 1024 && index < units.lastIndex)
    return "%.1f %s".format(size, units[index])
}
