package com.rexaps.rexpanel

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun __splitImpl_FileBrowser(b: FileBrowserState, modifier: Modifier) {
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
