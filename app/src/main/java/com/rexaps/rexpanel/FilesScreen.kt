package com.rexaps.rexpanel

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal val CFolder = Color(0xFFF9E2AF)
internal val CImage = Color(0xFF89B4FA)
internal val CVideo = Color(0xFFF38BA8)
internal val CAudio = Color(0xFFCBA6F7)
internal val CText = Color(0xFF94E2D5)
internal val CArchive = Color(0xFFFAB387)
internal val PreviewFg = Color(0xFFCDD6F4)

// Nama file memakai aksen tema aplikasi; warnanya ikut berubah saat tema RexAps diganti.
@Composable
internal fun nameBlue(): Color = MaterialTheme.colorScheme.primary

internal const val PLACE_TERMUX = "/data/data/com.termux"
internal const val PLACE_STORAGE = "/storage/emulated/0"

internal const val THUMB_MAX = 3L * 1024 * 1024       // thumbnail hanya untuk gambar <= 3 MB
internal const val AUTO_DOWNLOAD = 80L * 1024 * 1024  // video/audio > 80 MB minta konfirmasi dulu

private val dateFmt = SimpleDateFormat("d MMM yyyy, HH:mm", Locale("id", "ID"))
internal fun fmtDate(sec: Long): String = dateFmt.format(Date(sec * 1000))

internal fun fmtBytes(b: Long): String = when {
    b >= 1L shl 30 -> String.format("%.1f GB", b / (1L shl 30).toDouble())
    b >= 1L shl 20 -> String.format("%.1f MB", b / (1L shl 20).toDouble())
    b >= 1L shl 10 -> String.format("%.0f KB", b / 1024.0)
    else -> "$b B"
}

internal data class KindStyle(val icon: ImageVector, val color: Color, val label: String)

internal fun styleOf(f: RemoteFile, fallback: Color): KindStyle = __splitImpl_styleOf(f, fallback)

@Composable
fun FilesScreen(b: FileBrowserState, modifier: Modifier = Modifier) {
        return __splitImpl_FilesScreen(b, modifier)
    }

@Composable
internal fun TransferBar(t: TransferUi, onCancel: () -> Unit) {
        return __splitImpl_TransferBar(t, onCancel)
    }

/* =============================== BROWSER ================================== */

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun FileBrowser(b: FileBrowserState, modifier: Modifier) {
        return __splitImpl_FileBrowser(b, modifier)
    }

@Composable
internal fun SelectionBar(
    count: Int,
    canRename: Boolean,
    onClose: () -> Unit,
    onAll: () -> Unit,
    onDownload: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
        return __splitImpl_SelectionBar(count, canRename, onClose, onAll, onDownload, onRename, onDelete)
    }

@Composable
internal fun BrowserToolbar(
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
        return __splitImpl_BrowserToolbar(b, searching, query, onQuery, onToggleSearch, onNewFolder, onCopyPath, onUpload, onSelectAll)
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlaceChip(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null, Modifier.size(18.dp)) }
    )
}

/** Jalan pintas ke lokasi penting: Home, Termux, Penyimpanan (/sdcard), dan Root. */
@Composable
internal fun QuickPlaces(b: FileBrowserState) {
        return __splitImpl_QuickPlaces(b)
    }

@Composable
internal fun Breadcrumbs(b: FileBrowserState) {
        return __splitImpl_Breadcrumbs(b)
    }

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FileRow(
    f: RemoteFile,
    b: FileBrowserState,
    isSel: Boolean,
    selecting: Boolean,
    onMenu: (RemoteFile) -> Unit
) {
        return __splitImpl_FileRow(f, b, isSel, selecting, onMenu)
    }

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun GridTile(
    f: RemoteFile,
    b: FileBrowserState,
    isSel: Boolean,
    selecting: Boolean,
    onMenu: (RemoteFile) -> Unit
) {
        return __splitImpl_GridTile(f, b, isSel, selecting, onMenu)
    }

@Composable
internal fun FileThumb(f: RemoteFile, b: FileBrowserState, modifier: Modifier, big: Boolean = false) {
        return __splitImpl_FileThumb(f, b, modifier, big)
    }

@Composable
internal fun SheetAction(icon: ImageVector, label: String, danger: Boolean = false, onClick: () -> Unit) {
        return __splitImpl_SheetAction(icon, label, danger, onClick)
    }

@Composable
internal fun NameDialog(
    title: String,
    initial: String,
    confirm: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
        return __splitImpl_NameDialog(title, initial, confirm, onDismiss, onConfirm)
    }

@Composable
internal fun StateMessage(
    icon: ImageVector,
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    secondaryLabel: String? = null,
    onSecondary: () -> Unit = {}
) {
        return __splitImpl_StateMessage(icon, title, message, actionLabel, onAction, secondaryLabel, onSecondary)
    }

@Composable
internal fun SkeletonRows() {
        return __splitImpl_SkeletonRows()
    }

/* =============================== PREVIEW ================================== */

@Composable
internal fun FilePreview(b: FileBrowserState, file: RemoteFile, modifier: Modifier) {
        return __splitImpl_FilePreview(b, file, modifier)
    }

internal sealed interface ImgLoad {
    data class Ready(val bmp: ImageBitmap) : ImgLoad
    data class Failed(val msg: String) : ImgLoad
}

@Composable
internal fun ImagePreview(b: FileBrowserState, f: RemoteFile) {
        return __splitImpl_ImagePreview(b, f)
    }

@Composable
internal fun ZoomableImage(bmp: ImageBitmap, description: String) {
        return __splitImpl_ZoomableImage(bmp, description)
    }

@Composable
internal fun PlayerPreview(b: FileBrowserState, f: RemoteFile) {
        return __splitImpl_PlayerPreview(b, f)
    }

@Composable
internal fun VideoPlayer(file: File, audio: Boolean, onFail: () -> Unit) {
        return __splitImpl_VideoPlayer(file, audio, onFail)
    }

@Composable
internal fun TextPreview(b: FileBrowserState, f: RemoteFile) {
        return __splitImpl_TextPreview(b, f)
    }

@Composable
internal fun NoPreview(f: RemoteFile, message: String = "Pratinjau belum tersedia untuk jenis berkas ini. Kamu tetap bisa mengunduhnya.") {
        return __splitImpl_NoPreview(f, message)
    }

@Composable
internal fun DownloadProgress(done: Long, total: Long, label: String) {
        return __splitImpl_DownloadProgress(done, total, label)
    }

@Composable
internal fun PreviewError(message: String, onRetry: () -> Unit) {
        return __splitImpl_PreviewError(message, onRetry)
    }