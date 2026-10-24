package com.rexaps.rexpanel

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FileBrowserDialogs(
    b: FileBrowserState,
    clipboard: androidx.compose.ui.platform.ClipboardManager,
    menuFor: RemoteFile?,
    onMenuFor: (RemoteFile?) -> Unit,
    renameFor: RemoteFile?,
    onRenameFor: (RemoteFile?) -> Unit,
    newFolder: Boolean,
    onNewFolder: (Boolean) -> Unit,
    deleteFor: List<RemoteFile>?,
    onDeleteFor: (List<RemoteFile>?) -> Unit
) {
    /* ---- bottom sheet aksi (satu berkas) ---- */
    val sheet = menuFor
    if (sheet != null) {
        val st = styleOf(sheet, colors.onSurfaceVariant)
        ModalBottomSheet(onDismissRequest = { onMenuFor(null) }) {
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
                ) { onMenuFor(null); b.open(sheet) }
                if (!sheet.isDir) {
                    SheetAction(Icons.Default.Download, "Unduh ke $DOWNLOAD_LABEL") {
                        onMenuFor(null)
                        b.transfers.download(listOf(sheet))
                    }
                }
                SheetAction(Icons.Default.CheckCircle, "Pilih") { onMenuFor(null); b.toggleSelect(sheet) }
                SheetAction(Icons.Default.ContentCopy, "Salin path") {
                    clipboard.setText(AnnotatedString(sheet.path)); b.notice = "Path disalin"; onMenuFor(null)
                }
                SheetAction(Icons.Default.DriveFileRenameOutline, "Ganti nama") { onMenuFor(null); onRenameFor(sheet) }
                SheetAction(Icons.Default.Delete, "Hapus", danger = true) { onMenuFor(null); onDeleteFor(listOf(sheet)) }
            }
        }
    }

    renameFor?.let { f ->
        NameDialog("Ganti nama", f.name, "Simpan", { onRenameFor(null) }) { name ->
            onRenameFor(null)
            b.clearSelection()
            if (name != f.name) b.rename(f, name)
        }
    }

    if (newFolder) {
        NameDialog("Folder baru", "", "Buat", { onNewFolder(false) }) { name ->
            onNewFolder(false)
            b.mkdir(name)
        }
    }

    deleteFor?.let { items ->
        val one = items.singleOrNull()
        AlertDialog(
            onDismissRequest = { onDeleteFor(null) },
            title = { Text(if (one != null) "Hapus “${one.name}”?" else "Hapus ${items.size} item?") },
            text = {
                Text(
                    if (items.any { it.isDir }) "Folder hanya bisa dihapus jika kosong. Tindakan ini tidak bisa dibatalkan."
                    else "Berkas akan dihapus permanen dari server. Tindakan ini tidak bisa dibatalkan."
                )
            },
            confirmButton = {
                TextButton(onClick = { onDeleteFor(null); b.deleteAll(items) }) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { onDeleteFor(null) }) { Text("Batal") } }
        )
    }
}
