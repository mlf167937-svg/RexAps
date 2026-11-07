package com.rexaps.rexadm

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.rexaps.librex.rexsmartmanager.DownloadProgress
import com.rexaps.librex.rexsmartmanager.DownloadRequest
import com.rexaps.librex.rexsmartmanager.DownloadStatus
import java.io.File
import java.net.URI
import java.net.URLConnection

private enum class DownloadFilter(val label: String) {
    ALL("All"), DOWNLOADING("Downloading"), QUEUED("Queued"), COMPLETED("Completed"), PAUSED("Paused"), FAILED("Failed")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RexAdmScreen(onExit: (() -> Unit)? = null) {
    val context = LocalContext.current
    val manager = remember { RexAdmRuntime.manager(context) }
    val downloads by manager.downloads.collectAsState()
    val pendingUrl by RexAdmRuntime.pendingUrl.collectAsState()
    var url by remember { mutableStateOf("") }
    var filename by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(DownloadFilter.ALL) }
    var inputError by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf<DownloadProgress?>(null) }

    LaunchedEffect(pendingUrl) {
        pendingUrl?.let {
            url = it
            inputError = null
            RexAdmRuntime.clearPendingUrl()
        }
    }

    val activeCount = downloads.count { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED }
    val completedCount = downloads.count { it.status == DownloadStatus.COMPLETED }
    val failedCount = downloads.count { it.status == DownloadStatus.FAILED }
    val totalBytes = downloads.sumOf { it.downloadedBytes }
    val visibleDownloads = downloads.filter { item ->
        when (selectedFilter) {
            DownloadFilter.ALL -> item.status != DownloadStatus.CANCELLED
            DownloadFilter.DOWNLOADING -> item.status == DownloadStatus.DOWNLOADING
            DownloadFilter.QUEUED -> item.status == DownloadStatus.QUEUED
            DownloadFilter.COMPLETED -> item.status == DownloadStatus.COMPLETED
            DownloadFilter.PAUSED -> item.status == DownloadStatus.PAUSED
            DownloadFilter.FAILED -> item.status == DownloadStatus.FAILED || item.status == DownloadStatus.ACTION_REQUIRED
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("RexADM", fontWeight = FontWeight.ExtraBold)
                        Text("Professional Download Manager", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    if (onExit != null) IconButton(onClick = onExit) { Icon(Icons.Rounded.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val pasted = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                        if (pasted.isNotBlank()) url = pasted.trim()
                    }) { Icon(Icons.Rounded.ContentPaste, "Paste URL") }
                }
            )
        },
    ) { innerPadding ->
        Column(
            Modifier.fillMaxSize().padding(innerPadding).imePadding().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                DashboardMetric("TOTAL", downloads.size.toString(), Icons.Rounded.Download, Modifier.weight(1f))
                DashboardMetric("ACTIVE", activeCount.toString(), Icons.Rounded.Downloading, Modifier.weight(1f))
                DashboardMetric("DONE", completedCount.toString(), Icons.Rounded.TaskAlt, Modifier.weight(1f))
                DashboardMetric("FAILED", failedCount.toString(), Icons.Rounded.ErrorOutline, Modifier.weight(1f))
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("New download", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(formatBytes(totalBytes), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it; inputError = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("File URL") },
                        placeholder = { Text("https://example.com/archive.zip") },
                        leadingIcon = { Icon(Icons.Rounded.Link, null) },
                        trailingIcon = {
                            if (url.isNotBlank()) IconButton(onClick = { url = "" }) { Icon(Icons.Rounded.Close, "Clear URL") }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = filename,
                        onValueChange = { filename = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Filename (optional)") },
                        placeholder = { Text("Use server filename") },
                        leadingIcon = { Icon(Icons.Rounded.InsertDriveFile, null) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    inputError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    message?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }
                    Button(
                        onClick = {
                            val candidate = url.trim()
                            val parsed = runCatching { URI(candidate) }.getOrNull()
                            if (parsed == null || parsed.scheme?.lowercase() !in listOf("http", "https") || parsed.host.isNullOrBlank()) {
                                inputError = "Masukkan URL HTTP atau HTTPS yang valid."
                            } else {
                                runCatching {
                                    manager.enqueue(DownloadRequest(candidate, filename.trim().ifBlank { null }))
                                    url = ""
                                    filename = ""
                                    inputError = null
                                    message = "Download ditambahkan ke antrean."
                                }.onFailure { inputError = it.message ?: "Tidak dapat memulai download." }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) { Icon(Icons.Rounded.Download, null); Spacer(Modifier.width(8.dp)); Text("Start download") }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Downloads", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("${visibleDownloads.size} item", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DownloadFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter.label) }
                    )
                }
            }
            if (visibleDownloads.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(62.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Download, null, modifier = Modifier.size(30.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer) }
                        }
                        Text("Nothing here yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Paste a file URL or share one from RexFox.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    items(visibleDownloads, key = { it.id }) { item ->
                        DownloadItemCard(
                            item = item,
                            onPause = { manager.pause(item.id) },
                            onResume = { manager.resume(item.id) },
                            onCancel = { manager.cancel(item.id) },
                            onOpen = { openDownloadedFile(context, item) },
                            onShare = { shareDownloadedFile(context, item) },
                            onInstall = { installApk(context, item) },
                            onDelete = { deleting = item },
                            onOpenSource = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url))) } }
                        )
                    }
                }
            }
        }
    }

    deleting?.let { item ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete download?") },
            text = { Text("Remove ${item.fileName} and its local file from RexADM?") },
            confirmButton = {
                TextButton(onClick = {
                    manager.deleteDownload(item.id)
                    deleting = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun DashboardMetric(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(horizontal = 9.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun DownloadItemCard(
    item: DownloadProgress,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onInstall: () -> Unit,
    onDelete: () -> Unit,
    onOpenSource: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val domain = runCatching { URI(item.url).host }.getOrNull().orEmpty()
    val isApk = item.fileName.endsWith(".apk", ignoreCase = true)
    Surface(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(18.dp),
        color = colors.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = RoundedCornerShape(12.dp), color = colors.secondaryContainer, modifier = Modifier.size(42.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(if (isApk) Icons.Rounded.Android else Icons.Rounded.InsertDriveFile, null, tint = colors.onSecondaryContainer)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(item.fileName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(domain, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                StatusPill(item.status)
            }
            Text(item.message ?: statusText(item), style = MaterialTheme.typography.bodySmall, color = if (item.status == DownloadStatus.FAILED) colors.error else colors.onSurfaceVariant)
            if (item.status == DownloadStatus.DOWNLOADING || item.status == DownloadStatus.QUEUED) {
                LinearProgressIndicator(
                    progress = { (item.percent ?: 0) / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = colors.surfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${formatBytes(item.downloadedBytes)}${item.totalBytes?.let { " / ${formatBytes(it)}" } ?: ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                item.percent?.let { Text("$it%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                when (item.status) {
                    DownloadStatus.DOWNLOADING -> OutlinedButton(onClick = onPause) { Icon(Icons.Rounded.Pause, null); Spacer(Modifier.width(4.dp)); Text("Pause") }
                    DownloadStatus.PAUSED, DownloadStatus.FAILED -> Button(onClick = onResume) { Icon(Icons.Rounded.Refresh, null); Spacer(Modifier.width(4.dp)); Text("Retry") }
                    DownloadStatus.ACTION_REQUIRED -> {
                        OutlinedButton(onClick = onOpenSource) { Text("Open source") }
                        Button(onClick = onResume) { Text("Retry") }
                    }
                    DownloadStatus.COMPLETED -> {
                        Button(onClick = onOpen) { Icon(Icons.Rounded.OpenInNew, null); Spacer(Modifier.width(4.dp)); Text("Open") }
                        IconButton(onClick = onShare) { Icon(Icons.Rounded.Share, "Share") }
                        if (isApk) IconButton(onClick = onInstall) { Icon(Icons.Rounded.InstallMobile, "Install APK") }
                    }
                    else -> Unit
                }
                if (item.status in setOf(DownloadStatus.DOWNLOADING, DownloadStatus.QUEUED, DownloadStatus.PAUSED, DownloadStatus.ACTION_REQUIRED)) {
                    TextButton(onClick = onCancel) { Text("Cancel") }
                }
                if (item.status in setOf(DownloadStatus.COMPLETED, DownloadStatus.FAILED, DownloadStatus.CANCELLED)) {
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDelete) { Icon(Icons.Rounded.DeleteOutline, "Delete") }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(status: DownloadStatus) {
    val colors = MaterialTheme.colorScheme
    val (text, tint) = when (status) {
        DownloadStatus.QUEUED -> "Queued" to colors.tertiary
        DownloadStatus.DOWNLOADING -> "Downloading" to colors.primary
        DownloadStatus.PAUSED -> "Paused" to colors.secondary
        DownloadStatus.ACTION_REQUIRED -> "Action required" to colors.tertiary
        DownloadStatus.COMPLETED -> "Completed" to colors.primary
        DownloadStatus.FAILED -> "Failed" to colors.error
        DownloadStatus.CANCELLED -> "Cancelled" to colors.onSurfaceVariant
    }
    Surface(shape = CircleShape, color = tint.copy(alpha = .13f)) {
        Text(text, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = tint, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
    }
}

private fun statusText(item: DownloadProgress): String = when (item.status) {
    DownloadStatus.QUEUED -> "Waiting in queue"
    DownloadStatus.DOWNLOADING -> "Downloading${item.percent?.let { " · $it%" } ?: ""}"
    DownloadStatus.PAUSED -> "Download paused"
    DownloadStatus.ACTION_REQUIRED -> "The server requires sign-in or another action"
    DownloadStatus.COMPLETED -> "Download complete"
    DownloadStatus.FAILED -> "Download failed"
    DownloadStatus.CANCELLED -> "Download cancelled"
}

private fun openDownloadedFile(context: Context, item: DownloadProgress) {
    val file = item.filePath?.let(::File) ?: return
    if (!file.exists()) return
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val mime = URLConnection.guessContentTypeFromName(file.name) ?: "*/*"
    context.startActivity(Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mime)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}

private fun shareDownloadedFile(context: Context, item: DownloadProgress) {
    val file = item.filePath?.let(::File) ?: return
    if (!file.exists()) return
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val mime = URLConnection.guessContentTypeFromName(file.name) ?: "*/*"
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newUri(context.contentResolver, file.name, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }, "Share ${file.name}"))
}

private fun installApk(context: Context, item: DownloadProgress) {
    val file = item.filePath?.let(::File) ?: return
    if (!file.exists() || !file.name.endsWith(".apk", ignoreCase = true)) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
        context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}" )).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    context.startActivity(Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}

private fun formatBytes(value: Long): String {
    if (value < 1024) return "$value B"
    val units = listOf("KB", "MB", "GB", "TB")
    var size = value.toDouble()
    var index = -1
    do { size /= 1024.0; index++ } while (size >= 1024 && index < units.lastIndex)
    return "%.1f %s".format(size, units[index])
}
