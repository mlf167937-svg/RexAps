package com.rexaps.rexpanel

import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
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

// Warna nama file: biru terang di tema gelap, biru lebih pekat di tema terang (kontras tetap terjaga).
private val NameBlueDark = Color(0xFF89B4FA)
private val NameBlueLight = Color(0xFF1E66F5)

@Composable
private fun nameBlue(): Color = if (isSystemInDarkTheme()) NameBlueDark else NameBlueLight

private const val PLACE_TERMUX = "/data/data/com.termux"
private const val PLACE_STORAGE = "/storage/emulated/0"

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
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(b.notice) {
        b.notice?.let {
            snackbar.showSnackbar(it)
            b.notice = null
        }
    }

    Box(modifier) {
        val p = b.preview
        if (p != null) FilePreview(b, p, Modifier.fillMaxSize()) else FileBrowser(b, Modifier.fillMaxSize())

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            b.transfers.ui?.let { TransferBar(it, onCancel = { b.transfers.cancel() }) }
            SnackbarHost(snackbar)
        }
    }
}

@Composable
private fun TransferBar(t: TransferUi, onCancel: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceVariant,
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
    ) {
        Row(
            Modifier.padding(start = 16.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${t.title} ${t.index}/${t.count}",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant
                )
                Text(t.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                if (t.total > 0) {
                    LinearProgressIndicator(
                        progress = (t.done.toFloat() / t.total).coerceIn(0f, 1f),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "${fmtBytes(t.done)} / ${fmtBytes(t.total)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                } else {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
            IconButton(onClick = onCancel) { Icon(Icons.Default.Close, "Batalkan transfer") }
        }
    }
}

/* =============================== BROWSER ================================== */

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun FileBrowser(b: FileBrowserState, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val clipboard = LocalClipboardManager.current

    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var menuFor by remember { mutableStateOf<RemoteFile?>(null) }
    var renameFor by remember { mutableStateOf<RemoteFile?>(null) }
    var deleteFor by remember { mutableStateOf<List<RemoteFile>?>(null) }
    var newFolder by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) b.transfers.upload(uris, b.path)
    }
    fun startUpload() {
        if (b.path.isEmpty()) b.notice = "Tunggu folder selesai dimuat." else picker.launch(arrayOf("*/*"))
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
    val chosen = remember(b.entries, b.selected) { b.entries.filter { it.path in b.selected } }
    val selecting = b.selecting

    Column(modifier) {
        if (selecting) {
            SelectionBar(
                count = chosen.size,
                canRename = chosen.size == 1,
                onClose = { b.clearSelection() },
                onAll = { b.selectAll(visible) },
                onDownload = { if (b.transfers.download(chosen)) b.clearSelection() },
                onRename = { chosen.firstOrNull()?.let { renameFor = it } },
                onDelete = { deleteFor = chosen }
            )
        } else {
            BrowserToolbar(
                b = b,
                searching = searching,
                query = query,
                onQuery = { query = it },
                onToggleSearch = { searching = !searching; if (!searching) query = "" },
                onNewFolder = { newFolder = true },
                onCopyPath = { clipboard.setText(AnnotatedString(b.path)); b.notice = "Path disalin" },
                onUpload = { startUpload() },
                onSelectAll = { b.selectAll(visible) }
            )
        }
        QuickPlaces(b)
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
                    actionLabel = if (query.isBlank() && b.entries.isEmpty()) "Unggah berkas" else null,
                    onAction = { startUpload() },
                    secondaryLabel = if (query.isBlank() && b.entries.isEmpty()) "Buat folder" else null,
                    onSecondary = { newFolder = true }
                )

                b.grid -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(104.dp),
                    contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(visible, key = { it.path }, contentType = { it.kind }) { f ->
                        GridTile(f, b, isSel = f.path in b.selected, selecting = selecting, onMenu = { menuFor = it })
                    }
                }

                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(visible, key = { it.path }, contentType = { it.kind }) { f ->
                        FileRow(f, b, isSel = f.path in b.selected, selecting = selecting, onMenu = { menuFor = it })
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

    /* ---- bottom sheet aksi (satu berkas) ---- */
    val sheet = menuFor
    if (sheet != null) {
        val st = styleOf(sheet, colors.onSurfaceVariant)
        ModalBottomSheet(onDismissRequest = { menuFor = null }) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FileThumb(sheet, b, Modifier.size(48.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            sheet.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = nameBlue(),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
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
                if (!sheet.isDir) {
                    SheetAction(Icons.Default.Download, "Unduh ke $DOWNLOAD_LABEL") {
                        menuFor = null
                        b.transfers.download(listOf(sheet))
                    }
                }
                SheetAction(Icons.Default.CheckCircle, "Pilih") { menuFor = null; b.toggleSelect(sheet) }
                SheetAction(Icons.Default.ContentCopy, "Salin path") {
                    clipboard.setText(AnnotatedString(sheet.path)); b.notice = "Path disalin"; menuFor = null
                }
                SheetAction(Icons.Default.DriveFileRenameOutline, "Ganti nama") { menuFor = null; renameFor = sheet }
                SheetAction(Icons.Default.Delete, "Hapus", danger = true) { menuFor = null; deleteFor = listOf(sheet) }
            }
        }
    }

    renameFor?.let { f ->
        NameDialog("Ganti nama", f.name, "Simpan", { renameFor = null }) { name ->
            renameFor = null
            b.clearSelection()
            if (name != f.name) b.rename(f, name)
        }
    }

    if (newFolder) {
        NameDialog("Folder baru", "", "Buat", { newFolder = false }) { name ->
            newFolder = false
            b.mkdir(name)
        }
    }

    deleteFor?.let { items ->
        val one = items.singleOrNull()
        AlertDialog(
            onDismissRequest = { deleteFor = null },
            title = { Text(if (one != null) "Hapus “${one.name}”?" else "Hapus ${items.size} item?") },
            text = {
                Text(
                    if (items.any { it.isDir }) "Folder hanya bisa dihapus jika kosong. Tindakan ini tidak bisa dibatalkan."
                    else "Berkas akan dihapus permanen dari server. Tindakan ini tidak bisa dibatalkan."
                )
            },
            confirmButton = {
                TextButton(onClick = { deleteFor = null; b.deleteAll(items) }) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deleteFor = null }) { Text("Batal") } }
        )
    }
}

@Composable
private fun SelectionBar(
    count: Int,
    canRename: Boolean,
    onClose: () -> Unit,
    onAll: () -> Unit,
    onDownload: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    CompositionLocalProvider(LocalContentColor provides colors.onPrimaryContainer) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(colors.primaryContainer)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Batalkan pilihan") }
            Text(
                "$count dipilih",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .weight(1f)
                    .semantics { liveRegion = LiveRegionMode.Polite }
            )
            if (canRename) {
                IconButton(onClick = onRename) { Icon(Icons.Default.DriveFileRenameOutline, "Ganti nama") }
            }
            IconButton(onClick = onAll) { Icon(Icons.Default.SelectAll, "Pilih semua") }
            IconButton(onClick = onDownload) { Icon(Icons.Default.Download, "Unduh yang dipilih") }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Hapus yang dipilih", tint = colors.error)
            }
        }
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
    onCopyPath: () -> Unit,
    onUpload: () -> Unit,
    onSelectAll: () -> Unit
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
            IconButton(onClick = onToggleSearch) { Icon(Icons.Default.Search, "Cari") }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onUpload) { Icon(Icons.Default.Upload, "Unggah berkas ke folder ini") }
            IconButton(onClick = { b.grid = !b.grid }) {
                Icon(
                    if (b.grid) Icons.Default.ViewList else Icons.Default.GridView,
                    if (b.grid) "Tampilan daftar" else "Tampilan kotak"
                )
            }

            var moreOpen by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { moreOpen = true }) { Icon(Icons.Default.MoreVert, "Menu lainnya") }
                DropdownMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Pilih semua") },
                        leadingIcon = { Icon(Icons.Default.SelectAll, null) },
                        onClick = { moreOpen = false; onSelectAll() }
                    )
                    SortBy.values().forEach { s ->
                        DropdownMenuItem(
                            text = { Text("Urutkan: ${s.label}") },
                            onClick = { b.sort = s; moreOpen = false },
                            trailingIcon = { if (b.sort == s) Icon(Icons.Default.Check, "Dipilih") }
                        )
                    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceChip(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null, Modifier.size(18.dp)) }
    )
}

/** Jalan pintas ke lokasi penting: Home, Termux, Penyimpanan (/sdcard), dan Root. */
@Composable
private fun QuickPlaces(b: FileBrowserState) {
    val inStorage = b.path.startsWith(PLACE_STORAGE) || b.path.startsWith("/sdcard")
    val inTermux = b.path.startsWith(PLACE_TERMUX) && !b.isHome
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PlaceChip("Home", Icons.Default.Home, b.isHome) { b.goHome() }
        PlaceChip("Termux", Icons.Default.Terminal, inTermux) { b.go(PLACE_TERMUX) }
        PlaceChip("Penyimpanan", Icons.Default.SdStorage, inStorage) { b.go(PLACE_STORAGE) }
        PlaceChip("Root /", Icons.Default.Storage, b.path == "/") { b.go("/") }
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
private fun FileRow(
    f: RemoteFile,
    b: FileBrowserState,
    isSel: Boolean,
    selecting: Boolean,
    onMenu: (RemoteFile) -> Unit
) {
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
            .background(if (isSel) colors.primaryContainer.copy(alpha = 0.6f) else Color.Transparent)
            .combinedClickable(
                onClick = { if (selecting) b.toggleSelect(f) else b.open(f) },
                onLongClick = { b.toggleSelect(f) },
                onLongClickLabel = "Pilih"
            )
            .semantics { selected = isSel }
            .padding(start = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selecting) {
            Checkbox(checked = isSel, onCheckedChange = null)
            Spacer(Modifier.width(12.dp))
        }
        FileThumb(f, b, Modifier.size(48.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                f.name,
                style = MaterialTheme.typography.bodyLarge,
                color = nameBlue(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                sub,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (selecting) {
            Spacer(Modifier.width(16.dp))
        } else {
            IconButton(onClick = { onMenu(f) }) { Icon(Icons.Default.MoreVert, "Opsi ${f.name}") }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GridTile(
    f: RemoteFile,
    b: FileBrowserState,
    isSel: Boolean,
    selecting: Boolean,
    onMenu: (RemoteFile) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSel) colors.primaryContainer.copy(alpha = 0.6f) else Color.Transparent)
            .combinedClickable(
                onClick = { if (selecting) b.toggleSelect(f) else b.open(f) },
                onLongClick = { b.toggleSelect(f) },
                onLongClickLabel = "Pilih"
            )
            .semantics {
                selected = isSel
                customActions = listOf(CustomAccessibilityAction("Opsi") { onMenu(f); true })
            }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
            FileThumb(f, b, Modifier.fillMaxSize(), big = true)
            if (f.kind == FileKind.Video) {
                Icon(
                    Icons.Default.PlayArrow, null,
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                )
            }
            if (selecting) {
                Icon(
                    if (isSel) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    null,
                    tint = if (isSel) colors.primary else Color.White,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .size(24.dp)
                        .background(Color.Black.copy(alpha = 0.25f), CircleShape)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            f.name,
            style = MaterialTheme.typography.labelMedium,
            color = nameBlue(),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        if (!f.isDir) {
            Text(
                fmtBytes(f.size),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FileThumb(f: RemoteFile, b: FileBrowserState, modifier: Modifier, big: Boolean = false) {
    val st = styleOf(f, MaterialTheme.colorScheme.onSurfaceVariant)
    val wantThumb = f.kind == FileKind.Image && f.size in 1..THUMB_MAX
    var bmp by remember(f.path, f.mtimeSec) {
        mutableStateOf(if (wantThumb) b.cachedThumb(f)?.asImageBitmap() else null)
    }
    if (wantThumb) {
        LaunchedEffect(f.path, f.mtimeSec) {
            if (bmp == null) bmp = b.thumbnail(f)?.asImageBitmap()
        }
    }
    val shown = bmp
    Box(
        modifier
            .clip(RoundedCornerShape(if (big) 16.dp else 14.dp))
            .background(st.color.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        if (shown != null) {
            Image(shown, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Icon(st.icon, null, tint = st.color, modifier = Modifier.fillMaxSize(if (big) 0.42f else 0.55f))
        }
    }
}

@Composable
private fun SheetAction(icon: ImageVector, label: String, danger: Boolean = false, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val c = if (danger) colors.error else colors.onSurface
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = c)
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = c)
    }
}

@Composable
private fun NameDialog(
    title: String,
    initial: String,
    confirm: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    val invalid = '/' in text
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text("Nama") },
                isError = invalid,
                supportingText = { if (invalid) Text("Nama tidak boleh mengandung “/”.") },
                shape = RoundedCornerShape(16.dp)
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.trim()) }, enabled = text.isNotBlank() && !invalid) {
                Text(confirm)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
private fun StateMessage(
    icon: ImageVector,
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    secondaryLabel: String? = null,
    onSecondary: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, Modifier.size(48.dp), tint = colors.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
        if (secondaryLabel != null) {
            TextButton(onClick = onSecondary) { Text(secondaryLabel) }
        }
    }
}

@Composable
private fun SkeletonRows() {
    val c = MaterialTheme.colorScheme.surfaceVariant
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        repeat(7) {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(c))
                Spacer(Modifier.width(12.dp))
                Column {
                    Box(Modifier.width(160.dp).height(14.dp).clip(RoundedCornerShape(6.dp)).background(c))
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.width(100.dp).height(10.dp).clip(RoundedCornerShape(6.dp)).background(c))
                }
            }
        }
    }
}

/* =============================== PREVIEW ================================== */

@Composable
private fun FilePreview(b: FileBrowserState, file: RemoteFile, modifier: Modifier) {
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
                    color = NameBlueDark,
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

private sealed interface ImgLoad {
    data class Ready(val bmp: ImageBitmap) : ImgLoad
    data class Failed(val msg: String) : ImgLoad
}

@Composable
private fun ImagePreview(b: FileBrowserState, f: RemoteFile) {
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
private fun ZoomableImage(bmp: ImageBitmap, description: String) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var box by remember { mutableStateOf(IntSize.Zero) }

    fun clamp(o: Offset, s: Float): Offset {
        val mx = box.width * (s - 1f) / 2f
        val my = box.height * (s - 1f) / 2f
        return Offset(o.x.coerceIn(-mx, mx), o.y.coerceIn(-my, my))
    }

    Box(
        Modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { box = it }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val s = (scale * zoom).coerceIn(1f, 6f)
                    scale = s
                    offset = if (s == 1f) Offset.Zero else clamp(offset + pan, s)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    if (scale > 1f) { scale = 1f; offset = Offset.Zero } else scale = 2.5f
                })
            }
    ) {
        Image(
            bitmap = bmp,
            contentDescription = description,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale; scaleY = scale
                    translationX = offset.x; translationY = offset.y
                }
        )
    }
}

@Composable
private fun PlayerPreview(b: FileBrowserState, f: RemoteFile) {
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
private fun VideoPlayer(file: File, audio: Boolean, onFail: () -> Unit) {
    val holder = remember { arrayOfNulls<VideoView>(1) }
    DisposableEffect(Unit) { onDispose { holder[0]?.stopPlayback() } }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val vv = VideoView(ctx)
                val mc = MediaController(ctx)
                mc.setAnchorView(vv)
                vv.setMediaController(mc)
                vv.setOnPreparedListener { it.start() }
                vv.setOnErrorListener { _, _, _ -> onFail(); true }
                vv.setVideoPath(file.absolutePath)
                holder[0] = vv
                FrameLayout(ctx).apply {
                    addView(
                        vv,
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            Gravity.CENTER
                        )
                    )
                }
            }
        )
        if (audio) {
            Icon(
                Icons.Default.Audiotrack, null,
                tint = CAudio,
                modifier = Modifier.align(Alignment.Center).size(96.dp)
            )
        }
    }
}

@Composable
private fun TextPreview(b: FileBrowserState, f: RemoteFile) {
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
private fun NoPreview(f: RemoteFile, message: String = "Pratinjau belum tersedia untuk jenis berkas ini. Kamu tetap bisa mengunduhnya.") {
    val st = styleOf(f, Color.White)
    Column(
        Modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(st.icon, null, Modifier.size(64.dp), tint = st.color)
        Spacer(Modifier.height(12.dp))
        Text(f.name, style = MaterialTheme.typography.titleMedium, color = NameBlueDark, textAlign = TextAlign.Center)
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
private fun DownloadProgress(done: Long, total: Long, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (total > 0) {
            CircularProgressIndicator(progress = (done.toFloat() / total).coerceIn(0f, 1f), color = Color.White)
        } else {
            CircularProgressIndicator(color = Color.White)
        }
        Spacer(Modifier.height(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.White)
        if (total > 0) {
            Text(
                "${fmtBytes(done)} / ${fmtBytes(total)}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun PreviewError(message: String, onRetry: () -> Unit) {
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