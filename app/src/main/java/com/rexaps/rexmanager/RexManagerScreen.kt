package com.rexaps.rexmanager

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image as ImageIcon
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.rexaps.librex.rexcode.RexCode
import com.rexaps.librex.rexcode.RexLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

// ───────────────────────── Design tokens ─────────────────────────
private object Rex {
    val bg = Color(0xFF0B1020)
    val surface = Color(0xFF131A2E)
    val surfaceHigh = Color(0xFF1B2440)
    val primary = Color(0xFF3B6CF6)
    val onSurface = Color(0xFFEAEEF8)
    val muted = Color(0xFF9AA4BF)
    val folder = Color(0xFFF5B73B)
    val danger = Color(0xFFEF5350)
}

// ───────────────────────── Routes ─────────────────────────
private sealed interface Route {
    data object Home : Route
    data class Folder(val path: String) : Route
    data class Archive(val path: String) : Route
    data class Editor(val path: String) : Route
    data class Media(val path: String) : Route
    data class Compress(val paths: List<String>) : Route
}

private val IMAGE_EXT = setOf("jpg", "jpeg", "png")
private val AUDIO_EXT = setOf("mp3", "ogg")
private val VIDEO_EXT = setOf("mp4")
private val ARCHIVE_EXT = setOf("zip", "7z")
private const val MAX_EDIT_BYTES = 2L * 1024 * 1024
private const val HIGHLIGHT_LIMIT = 200_000L

// ───────────────────────── Entry ─────────────────────────
@Composable
fun RexManagerApp(onExit: () -> Unit = {}) {
    var splash by rememberSaveable { mutableStateOf(true) }
    if (splash) {
        RexSplashManager(onStart = { splash = false }, onExit = onExit)
    } else {
        RexManagerScreen(onExit = onExit)
    }
}

@Composable
fun RexManagerScreen(onExit: () -> Unit = {}) {
    val ctx = LocalContext.current
    var granted by remember { mutableStateOf(hasAccess()) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) granted = hasAccess()
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    val stack = remember { mutableStateListOf<Route>(Route.Home) }
    val route = stack.last()

    BackHandler {
        if (stack.size > 1) stack.removeAt(stack.lastIndex) else onExit()
    }

    fun pop() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    fun open(f: File) {
        val ext = f.extension.lowercase()
        stack.add(
            when {
                f.isDirectory -> Route.Folder(f.path)
                ext in ARCHIVE_EXT -> Route.Archive(f.path)
                ext in IMAGE_EXT || ext in AUDIO_EXT || ext in VIDEO_EXT -> Route.Media(f.path)
                else -> Route.Editor(f.path)
            }
        )
    }

    Surface(Modifier.fillMaxSize(), color = Rex.bg) {
        if (!granted) {
            PermissionGate {
                ctx.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:${ctx.packageName}")
                    )
                )
            }
        } else {
            when (route) {
                Route.Home -> HomeScreen(
                    onOpen = { stack.add(Route.Folder(it)) },
                    onBack = onExit
                )
                is Route.Folder -> FolderScreen(
                    path = route.path,
                    onBack = ::pop,
                    onOpen = ::open,
                    onCompress = { stack.add(Route.Compress(it)) }
                )
                is Route.Archive -> ArchiveScreen(File(route.path), ::pop)
                is Route.Editor -> EditorScreen(File(route.path), ::pop)
                is Route.Media -> MediaScreen(File(route.path), ::pop)
                is Route.Compress -> CompressScreen(route.paths.map { File(it) }, ::pop)
            }
        }
    }
}

private fun hasAccess(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager() else true

@Composable
private fun PermissionGate(onGrant: () -> Unit) {
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

// ───────────────────────── Shared UI ─────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RexTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Column {
                Text(title, color = Rex.onSurface, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) {
                    Text(subtitle, color = Rex.muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, "Kembali", tint = Rex.onSurface)
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Rex.bg)
    )
}

private fun formatSize(b: Long): String = when {
    b < 1024L -> "$b B"
    b < 1024L * 1024 -> "%.1f KB".format(b / 1024.0)
    b < 1024L * 1024 * 1024 -> "%.1f MB".format(b / 1048576.0)
    else -> "%.2f GB".format(b / 1073741824.0)
}

private fun iconFor(f: File): Pair<ImageVector, Color> {
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

// ───────────────────────── Home ─────────────────────────
@Composable
private fun HomeScreen(onOpen: (String) -> Unit, onBack: () -> Unit) {
    val root = Environment.getExternalStorageDirectory()
    val stat = remember { StatFs(root.path) }
    val total = stat.totalBytes
    val used = total - stat.availableBytes
    val fraction = if (total > 0) (used.toFloat() / total).coerceIn(0f, 1f) else 0f

    Column(Modifier.fillMaxSize()) {
        RexTopBar("RexManager", onBack = onBack)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Rex.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Penyimpanan Internal", color = Rex.onSurface, fontWeight = FontWeight.SemiBold)
                    Text("${formatSize(used)} / ${formatSize(total)}", color = Rex.muted, fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = Rex.primary,
                        trackColor = Rex.surfaceHigh
                    )
                }
            }
            Text("Lokasi", color = Rex.muted, fontSize = 13.sp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Rex.surface)
                    .clickable { onOpen(root.path) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Folder, null, tint = Rex.folder)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Penyimpanan Internal", color = Rex.onSurface)
                    Text(root.path, color = Rex.muted, fontSize = 11.sp)
                }
                Icon(Icons.Filled.ChevronRight, null, tint = Rex.muted)
            }
            Text("Cloud akan hadir di versi berikutnya.", color = Rex.muted, fontSize = 12.sp)
        }
    }
}

// ───────────────────────── Folder ─────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderScreen(
    path: String,
    onBack: () -> Unit,
    onOpen: (File) -> Unit,
    onCompress: (List<String>) -> Unit
) {
    val ctx = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    val dir = File(path)
    val files = remember(path, refresh) {
        dir.listFiles()
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            ?: emptyList()
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
        if (files.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.FolderOpen, null, tint = Rex.muted, modifier = Modifier.size(48.dp))
                    Text("Folder ini kosong", color = Rex.muted)
                }
            }
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(files, key = { it.path }) { f ->
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

// ───────────────────────── Compress (ZIP / 7Z) ─────────────────────────
private enum class ArcFormat(val ext: String) { ZIP("zip"), SEVENZ("7z") }

@Composable
private fun CompressScreen(sources: List<File>, onDone: () -> Unit) {
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

private fun walkTree(f: File, rel: String, cb: (File, String) -> Unit) {
    if (f.isDirectory) {
        cb(f, "$rel/")
        f.listFiles()?.forEach { walkTree(it, "$rel/${it.name}", cb) }
    } else {
        cb(f, rel)
    }
}

private fun compress(sources: List<File>, out: File, format: ArcFormat) {
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

// ───────────────────────── Archive viewer (tanpa ekstrak) ─────────────────────────
private data class ArcItem(val name: String, val size: Long, val isDir: Boolean)

@Composable
private fun ArchiveScreen(file: File, onBack: () -> Unit) {
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

private fun listArchive(f: File): List<ArcItem> = when (f.extension.lowercase()) {
    "zip" -> ZipFile(f).use { z ->
        z.entries().asSequence().map { ArcItem(it.name, it.size, it.isDirectory) }.toList()
    }
    else -> SevenZFile(f).use { sz ->
        generateSequence { sz.nextEntry }.map { ArcItem(it.name, it.size, it.isDirectory) }.toList()
    }
}

// ───────────────────────── Editor (semua file, syntax RexCode) ─────────────────────────
private fun tokenColor(type: String): Color {
    val t = type.uppercase()
    return when {
        "KEYWORD" in t -> Color(0xFFC792EA)
        "STRING" in t || "CHAR" in t -> Color(0xFFC3E88D)
        "COMMENT" in t -> Color(0xFF6B7794)
        "NUMBER" in t || "LITERAL" in t -> Color(0xFFF78C6C)
        "FUNCTION" in t || "METHOD" in t -> Color(0xFF82AAFF)
        "TYPE" in t || "CLASS" in t -> Color(0xFFFFCB6B)
        "ANNOTATION" in t || "TAG" in t || "ATTRIBUTE" in t -> Color(0xFF89DDFF)
        "OPERATOR" in t || "PUNCT" in t -> Color(0xFF89DDFF)
        else -> Rex.onSurface
    }
}

private class RexHighlighter(
    private val fileName: String,
    private val enabled: Boolean
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val plain = TransformedText(text, OffsetMapping.Identity)
        if (!enabled) return plain
        return try {
            val result = RexCode.highlight(text.text, fileName)
            val b = AnnotatedString.Builder()
            var len = 0
            for (token in result.tokens) {
                b.pushStyle(SpanStyle(color = tokenColor(token.type.toString())))
                b.append(token.text)
                b.pop()
                len += token.text.length
            }
            // Jika token tidak menutup seluruh teks, tampilkan polos agar kursor tidak bergeser.
            if (len == text.length) TransformedText(b.toAnnotatedString(), OffsetMapping.Identity) else plain
        } catch (e: Exception) {
            plain
        }
    }
}

@Composable
private fun EditorScreen(file: File, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val tooBig = file.length() > MAX_EDIT_BYTES
    var loaded by remember { mutableStateOf(false) }
    var value by remember { mutableStateOf(TextFieldValue("")) }
    var original by remember { mutableStateOf("") }
    var fontPx by rememberSaveable { mutableFloatStateOf(14f) } // 4..22
    var highlight by rememberSaveable { mutableStateOf(true) }
    var confirmExit by remember { mutableStateOf(false) }
    val dirty = loaded && value.text != original

    LaunchedEffect(file) {
        if (!tooBig) {
            val t = withContext(Dispatchers.IO) { runCatching { file.readText() }.getOrDefault("") }
            original = t
            value = TextFieldValue(t)
            loaded = true
            if (file.length() > HIGHLIGHT_LIMIT) highlight = false
        }
    }

    fun tryBack() {
        if (dirty) confirmExit = true else onBack()
    }
    BackHandler(onBack = ::tryBack)

    Column(Modifier.fillMaxSize()) {
        RexTopBar(
            title = file.name,
            subtitle = if (dirty) "Belum disimpan" else formatSize(file.length()),
            onBack = ::tryBack,
            actions = {
                IconButton(
                    enabled = dirty,
                    onClick = {
                        scope.launch {
                            val text = value.text
                            withContext(Dispatchers.IO) { file.writeText(text) }
                            original = text
                            Toast.makeText(ctx, "Tersimpan", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(Icons.Filled.Save, "Simpan", tint = if (dirty) Rex.primary else Rex.muted)
                }
            }
        )

        if (tooBig) {
            Text(
                "File lebih dari 2 MB, terlalu besar untuk editor.",
                color = Rex.muted, modifier = Modifier.padding(16.dp)
            )
        } else {
            // Kontrol ukuran font 4–22 px
            Row(
                Modifier.fillMaxWidth().background(Rex.surface).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { fontPx = (fontPx - 1f).coerceAtLeast(4f) }) {
                    Icon(Icons.Filled.Remove, "Perkecil teks", tint = Rex.onSurface)
                }
                Slider(
                    value = fontPx,
                    onValueChange = { fontPx = it.toInt().toFloat() },
                    valueRange = 4f..22f,
                    steps = 17,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { fontPx = (fontPx + 1f).coerceAtMost(22f) }) {
                    Icon(Icons.Filled.Add, "Perbesar teks", tint = Rex.onSurface)
                }
                Text(
                    "${fontPx.toInt()}px", color = Rex.muted, fontSize = 12.sp,
                    modifier = Modifier.widthIn(min = 36.dp)
                )
                Switch(
                    checked = highlight,
                    onCheckedChange = { highlight = it },
                    modifier = Modifier.semantics { contentDescription = "Syntax highlight" }
                )
            }

            if (!loaded) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val transformation = remember(file.name, highlight) { RexHighlighter(file.name, highlight) }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                        .padding(12.dp)
                ) {
                    BasicTextField(
                        value = value,
                        onValueChange = { value = it },
                        textStyle = TextStyle(
                            color = Rex.onSurface,
                            fontFamily = FontFamily.Monospace,
                            fontSize = fontPx.sp,
                            lineHeight = (fontPx * 1.4f).sp
                        ),
                        cursorBrush = SolidColor(Rex.primary),
                        visualTransformation = transformation,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 300.dp)
                    )
                }
            }
        }
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("Perubahan belum disimpan") },
            text = { Text("Keluar tanpa menyimpan?") },
            confirmButton = {
                TextButton(onClick = { confirmExit = false; onBack() }) {
                    Text("Keluar", color = Rex.danger)
                }
            },
            dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("Lanjut edit") } }
        )
    }
}

// ───────────────────────── Media (jpg/png/jpeg/mp3/ogg/mp4) ─────────────────────────
@Composable
private fun MediaScreen(file: File, onBack: () -> Unit) {
    val ext = file.extension.lowercase()
    Column(Modifier.fillMaxSize()) {
        RexTopBar(file.name, formatSize(file.length()), onBack)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            when (ext) {
                in IMAGE_EXT -> ImagePreview(file)
                in AUDIO_EXT -> AudioPreview(file)
                in VIDEO_EXT -> VideoPreview(file)
            }
        }
    }
}

@Composable
private fun ImagePreview(file: File) {
    val bmp by produceState<Bitmap?>(null, file) {
        value = withContext(Dispatchers.IO) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            var s = 1
            while (bounds.outWidth / s > 2048 || bounds.outHeight / s > 2048) s *= 2
            BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = s })
        }
    }
    val b = bmp
    if (b != null) {
        Image(b.asImageBitmap(), file.name, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
    } else {
        CircularProgressIndicator()
    }
}

@Composable
private fun AudioPreview(file: File) {
    val player = remember(file) {
        runCatching { MediaPlayer().apply { setDataSource(file.path); prepare() } }.getOrNull()
    }
    if (player == null) {
        Text("Audio tidak bisa diputar", color = Rex.danger)
        return
    }
    var playing by remember { mutableStateOf(false) }
    var pos by remember { mutableFloatStateOf(0f) }
    DisposableEffect(player) { onDispose { player.release() } }
    LaunchedEffect(playing) {
        while (playing) {
            pos = player.currentPosition.toFloat()
            if (!player.isPlaying) playing = false
            delay(250)
        }
    }
    Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.MusicNote, null, tint = Color(0xFF3BC4F5), modifier = Modifier.size(96.dp))
        Spacer(Modifier.height(16.dp))
        Slider(
            value = pos,
            valueRange = 0f..player.duration.toFloat().coerceAtLeast(1f),
            onValueChange = { pos = it; player.seekTo(it.toInt()) }
        )
        FilledIconButton(
            onClick = {
                if (player.isPlaying) {
                    player.pause(); playing = false
                } else {
                    player.start(); playing = true
                }
            },
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Rex.primary),
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                if (playing) "Jeda" else "Putar",
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun VideoPreview(file: File) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { c ->
            VideoView(c).apply {
                setMediaController(MediaController(c).also { it.setAnchorView(this) })
                setVideoURI(Uri.fromFile(file))
                setOnPreparedListener { start() }
            }
        },
        onRelease = { it.stopPlayback() }
    )
}