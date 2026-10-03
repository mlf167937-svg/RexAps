package com.rexaps.rexpanel

import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val CFolder = Color(0xFFF9E2AF)
private val CImage = Color(0xFF89B4FA)
private val CVideo = Color(0xFFF38BA8)
private val CAudio = Color(0xFFCBA6F7)
private val CText = Color(0xFF94E2D5)
private val CArchive = Color(0xFFFAB387)
private val PreviewFg = Color(0xFFCDD6F4)

private const val THUMB_MAX = 3L * 1024 * 1024       // thumbnail hanya untuk gambar <= 3 MB
private const val AUTO_DOWNLOAD = 80L * 1024 * 1024  // video/audio > 80 MB minta konfirmasi dulu

private val dateFmt = SimpleDateFormat("d MMM yyyy, HH:mm", Locale("id", "ID"))
private fun fmtDate(sec: Long): String = dateFmt.format(Date(sec * 1000))

private fun fmtBytes(b: Long): String = when {
    b >= 1L shl 30 -> String.format("%.1f GB", b / (1L shl 30).toDouble())
    b >= 1L shl 20 -> String.format("%.1f MB", b / (1L shl 20).toDouble())
    b >= 1L shl 10 -> String.format("%.0f KB", b / 1024.0)
    else -> "$b B"
}

private data class KindStyle(val icon: ImageVector, val color: Color, val label: String)

private fun styleOf(f: RemoteFile, fallback: Color): KindStyle = when {
    f.isDir -> KindStyle(Icons.Default.Folder, CFolder, "Folder")
    else -> when (f.kind) {
        FileKind.Image -> KindStyle(Icons.Default.Image, CImage, "Gambar")
        FileKind.Video -> KindStyle(Icons.Default.Movie, CVideo, "Video")
        FileKind.Audio -> KindStyle(Icons.Default.Audiotrack, CAudio, "Audio")
        FileKind.Text -> KindStyle(Icons.Default.Description, CText, "Teks")
        FileKind.Archive -> KindStyle(Icons.Default.Archive, CArchive, "Arsip")
        FileKind.Other -> KindStyle(Icons.Default.InsertDriveFile, fallback, "Berkas")
    }
}

@Composable
fun FilesScreen(b: FileBrowserState, modifier: Modifier = Modifier) {
    val preview = b.preview
    if (preview != null) FilePreview(b, preview, modifier) else FileBrowser(b, modifier)
}

/* =============================== BROWSER ================================== */

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun FileBrowser(b: FileBrowserState, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val snackbar = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current

    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var menuFor by remember { mutableStateOf<RemoteFile?>(null) }
    var renameFor by remember { mutableStateOf<RemoteFile?>(null) }
    var deleteFor by remember { mutableStateOf<RemoteFile?>(null) }
    var newFolder by remember { mutableStateOf(false) }

    LaunchedEffect(b.notice) {
        b.notice?.let {
            snackbar.showSnackbar(it)
            b.notice = null
        }
    }

    val visible = remember(b.entries, b.showHidden, b.sort, query) {
        val cmp: Comparator<RemoteFile> = when (b.sort) {
            SortBy.Name -> compareBy<RemoteFile> { it.name.lowercase() }
            SortBy.Size -> compareByDescending<RemoteFile> { it.size }
            SortBy.Date -> compareByDescending<RemoteFile> { it.mtimeSec }
        }
        b.entries
            .filter { b.showHidden || !it.name.startsWith(".") }
            .filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) }
            .sortedWith(compareByDescending<RemoteFile> { it.isDir }.then(cmp))
    }

    Box(modifier) {
        Column(Modifier.fillMaxSize()) {
            BrowserToolbar(
                b = b,
                searching = searching,
                query = query,
                onQuery = { query = it },
                onToggleSearch = { searching = !searching; if (!searching) query = "" },
                onNewFolder = { newFolder = true },
                onCopyPath = { clipboard.setText(AnnotatedString(b.path)); b.notice = "Path disalin" }
            )
            Breadcrumbs(b)

            Box(Modifier.fillMaxWidth().height(3.dp)) {
                if (b.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                val err = b.error
                when {
                    err != null && b.entries.isEmpty() -> StateMessage(
                        icon = Icons.Default.Warning,
                        title = "Tidak bisa membuka folder",
                        message = err,
                        actionLabel = "Coba lagi",
                        onAction = { b.refresh() },
                        secondaryLabel = "Ke home",
                        onSecondary = { b.goHome() }
                    )

                    b.loading && b.entries.isEmpty() -> SkeletonRows()

                    visible.isEmpty() -> StateMessage(
                        icon = Icons.Default.FolderOpen,
                        title = if (query.isNotBlank()) "Tidak ada hasil" else "Folder kosong",
                        message = if (query.isNotBlank()) "Tidak ada berkas yang cocok dengan “${query.trim()}”."
                        else if (!b.showHidden && b.entries.isNotEmpty()) "Hanya ada berkas tersembunyi. Aktifkan “Tampilkan tersembunyi” di menu ⋮."
                        else "Belum ada berkas di sini.",
                        actionLabel = if (query.isBlank() && b.entries.isEmpty()) "Buat folder" else null,
                        onAction = { newFolder = true }
                    )

                    b.grid -> LazyVerticalGrid(
                        columns = GridCells.Adaptive(104.dp),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(visible, key = { it.path }, contentType = { it.kind }) { f ->
                            GridTile(f, b, onMenu = { menuFor = it })
                        }
                    }

                    else -> LazyColumn(
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(visible, key = { it.path }, contentType = { it.kind }) { f ->
                            FileRow(f, b, onMenu = { menuFor = it })
                        }
                        item {
                            val dirs = visible.count { it.isDir }
                            Text(
                                "$dirs folder · ${visible.size - dirs} berkas",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(16.dp)
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(12.dp))
    }

    /* ---- bottom sheet aksi ---- */
    val sheet = menuFor
    if (sheet != null) {
        val st = styleOf(sheet, colors.onSurfaceVariant)
        ModalBottomSheet(onDismissRequest = { menuFor = null }) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FileThumb(sheet, b, Modifier.size(48.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(sheet.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(
                            if (sheet.isDir) st.label else "${st.label} · ${fmtBytes(sheet.size)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    sheet.path,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = colors.onSurfaceVariant
                )
                Text(
                    "${sheet.perms} · ${fmtDate(sheet.mtimeSec)}" + if (sheet.isLink) " · tautan" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                SheetAction(
                    if (sheet.isDir) Icons.Default.FolderOpen else Icons.Default.Visibility,
                    if (sheet.isDir) "Buka folder" else "Pratinjau"
                ) { menuFor = null; b.open(sheet) }
                SheetAction(Icons.Default.ContentCopy, "Salin path") {
                    clipboard.setText(AnnotatedString(sheet.path)); b.notice = "Path disalin"; menuFor = null
                }
                SheetAction(Icons.Default.DriveFileRenameOutline, "Ganti nama") { menuFor = null; renameFor = sheet }
                SheetAction(Icons.Default.Delete, "Hapus", danger = true) { menuFor = null; deleteFor = sheet }
            }
        }
    }

    renameFor?.let { f ->
        NameDialog("Ganti nama", f.name, "Simpan", { renameFor = null }) { name ->
            renameFor = null
            if (name != f.name) b.rename(f, name)
        }
    }

    if (newFolder) {
        NameDialog("Folder baru", "", "Buat", { newFolder = false }) { name ->
            newFolder = false
            b.mkdir(name)
        }
    }

    deleteFor?.let { f ->
        AlertDialog(
            onDismissRequest = { deleteFor = null },
            title = { Text("Hapus “${f.name}”?") },
            text = {
                Text(
                    if (f.isDir) "Hanya folder kosong yang bisa dihapus. Tindakan ini tidak bisa dibatalkan."
                    else "Berkas akan dihapus permanen dari server. Tindakan ini tidak bisa dibatalkan."
                )
            },
            confirmButton = {
                TextButton(onClick = { deleteFor = null; b.delete(f) }) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deleteFor = null }) { Text("Batal") } }
        )
    }
}

@Composable
private fun BrowserToolbar(
    b: FileBrowserState,
    searching: Boolean,
    query: String,
    onQuery: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onNewFolder: () -> Unit,
    onCopyPath: () -> Unit
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(searching) { if (searching) focus.requestFocus() }

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (searching) {
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                modifier = Modifier.weight(1f).padding(vertical = 4.dp).focusRequester(focus),
                singleLine = true,
                placeholder = { Text("Cari di folder ini") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )
            IconButton(onClick = onToggleSearch) { Icon(Icons.Default.Close, "Tutup pencarian") }
        } else {
            IconButton(onClick = { b.up() }, enabled = b.canGoUp) {
                Icon(Icons.Default.ArrowUpward, "Naik satu folder")
            }
            IconButton(onClick = { b.goHome() }) { Icon(Icons.Default.Home, "Ke folder home") }
            IconButton(onClick = onToggleSearch) { Icon(Icons.Default.Search, "Cari") }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { b.grid = !b.grid }) {
                Icon(
                    if (b.grid) Icons.Default.ViewList else Icons.Default.GridView,
                    if (b.grid) "Tampilan daftar" else "Tampilan kotak"
                )
            }

            var sortOpen by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { sortOpen = true }) { Icon(Icons.Default.Sort, "Urutkan") }
                DropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }) {
                    SortBy.values().forEach { s ->
                        DropdownMenuItem(
                            text = { Text(s.label) },
                            onClick = { b.sort = s; sortOpen = false },
                            trailingIcon = { if (b.sort == s) Icon(Icons.Default.Check, "Dipilih") }
                        )
                    }
                }
            }

            var moreOpen by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { moreOpen = true }) { Icon(Icons.Default.MoreVert, "Menu lainnya") }
                DropdownMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Tampilkan tersembunyi") },
                        onClick = { b.showHidden = !b.showHidden; moreOpen = false },
                        trailingIcon = { if (b.showHidden) Icon(Icons.Default.Check, "Aktif") }
                    )
                    DropdownMenuItem(
                        text = { Text("Folder baru") },
                        leadingIcon = { Icon(Icons.Default.CreateNewFolder, null) },
                        onClick = { moreOpen = false; onNewFolder() }
                    )
                    DropdownMenuItem(
                        text = { Text("Salin path folder") },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                        onClick = { moreOpen = false; onCopyPath() }
                    )
                    DropdownMenuItem(
                        text = { Text("Muat ulang") },
                        leadingIcon = { Icon(Icons.Default.Refresh, null) },
                        onClick = { moreOpen = false; b.refresh() }
                    )
                }
            }
        }
    }
}

@Composable
private fun Breadcrumbs(b: FileBrowserState) {
    val colors = MaterialTheme.colorScheme
    val scroll = rememberScrollState()
    val segs = remember(b.path) {
        buildList {
            add("/" to "/")
            var acc = ""
            b.path.split('/').filter { it.isNotEmpty() }.forEach { acc += "/$it"; add(it to acc) }
        }
    }
    LaunchedEffect(segs) {
        withFrameNanos { }
        scroll.animateScrollTo(scroll.maxValue)
    }
    Row(
        Modifier.fillMaxWidth().horizontalScroll(scroll).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        segs.forEachIndexed { i, (label, target) ->
            val last = i == segs.lastIndex
            if (i > 0) Icon(Icons.Default.ChevronRight, null, Modifier.size(16.dp), tint = colors.onSurfaceVariant)
            Box(
                Modifier
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button, enabled = !last) { b.go(target) }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (last) colors.primary else colors.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileRow(f: RemoteFile, b: FileBrowserState, onMenu: (RemoteFile) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val sub = buildString {
        append(if (f.isDir) "Folder" else fmtBytes(f.size))
        append(" · ").append(fmtDate(f.mtimeSec))
        if (f.isLink) append(" · tautan")
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = { b.open(f) },
                onLongClick = { onMenu(f) },
                onLongClickLabel = "Opsi"
            )
            .padding(start = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FileThumb(f, b, Modifier.size(48.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(f.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                sub,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,