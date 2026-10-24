package com.rexaps.rexmanager

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.io.File

// ───────────────────────── Design tokens ─────────────────────────
internal object Rex {
    val bg: Color @Composable get() = MaterialTheme.colorScheme.background
    val surface: Color @Composable get() = MaterialTheme.colorScheme.surface
    val surfaceHigh: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
    val primary: Color @Composable get() = MaterialTheme.colorScheme.primary
    val onSurface: Color @Composable get() = MaterialTheme.colorScheme.onSurface
    val muted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
    val folder: Color @Composable get() = MaterialTheme.colorScheme.tertiary
    val danger: Color @Composable get() = MaterialTheme.colorScheme.error
}

// ───────────────────────── Routes ─────────────────────────
internal sealed interface Route {
    data object Home : Route
    data class Folder(val path: String) : Route
    data class Archive(val path: String) : Route
    data class Editor(val path: String) : Route
    data class Media(val path: String) : Route
    data class Compress(val paths: List<String>) : Route
}

internal val IMAGE_EXT = setOf("jpg", "jpeg", "png")
internal val AUDIO_EXT = setOf("mp3", "ogg")
internal val VIDEO_EXT = setOf("mp4")
internal val ARCHIVE_EXT = setOf("zip", "7z")
internal const val MAX_EDIT_BYTES = 2L * 1024 * 1024
internal const val HIGHLIGHT_LIMIT = 200_000L

// ───────────────────────── Entry ─────────────────────────
@Composable
fun RexManagerApp(onExit: () -> Unit = {}) {
        return __splitImpl_RexManagerApp(onExit)
    }

@Composable
fun RexManagerScreen(onExit: () -> Unit = {}) {
        return __splitImpl_RexManagerScreen(onExit)
    }

internal fun hasAccess(): Boolean = __splitImpl_hasAccess()

@Composable
internal fun PermissionGate(onGrant: () -> Unit) {
        return __splitImpl_PermissionGate(onGrant)
    }

// ───────────────────────── Shared UI ─────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RexTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
        return __splitImpl_RexTopBar(title, subtitle, onBack, actions)
    }

internal fun formatSize(b: Long): String = __splitImpl_formatSize(b)

@Composable
internal fun iconFor(f: File): Pair<ImageVector, Color> {
        return __splitImpl_iconFor(f)
    }

@Composable
internal fun drawerItemColors() = __splitImpl_drawerItemColors()

// ───────────────────────── Home ─────────────────────────
@Composable
internal fun HomeScreen(onOpen: (String) -> Unit, onBack: () -> Unit, onMenu: () -> Unit) {
        return __splitImpl_HomeScreen(onOpen, onBack, onMenu)
    }

// ───────────────────────── Folder ─────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FolderScreen(
    path: String,
    onBack: () -> Unit,
    onOpen: (File) -> Unit,
    onCompress: (List<String>) -> Unit
) {
        return __splitImpl_FolderScreen(path, onBack, onOpen, onCompress)
    }

// ───────────────────────── Compress (ZIP / 7Z) ─────────────────────────
internal enum class ArcFormat(val ext: String) { ZIP("zip"), SEVENZ("7z") }

@Composable
internal fun CompressScreen(sources: List<File>, onDone: () -> Unit) {
        return __splitImpl_CompressScreen(sources, onDone)
    }

internal fun walkTree(f: File, rel: String, cb: (File, String) -> Unit) {
        return __splitImpl_walkTree(f, rel, cb)
    }

internal fun compress(sources: List<File>, out: File, format: ArcFormat) {
        return __splitImpl_compress(sources, out, format)
    }

// ───────────────────────── Archive viewer (tanpa ekstrak) ─────────────────────────
internal data class ArcItem(val name: String, val size: Long, val isDir: Boolean)

@Composable
internal fun ArchiveScreen(file: File, onBack: () -> Unit) {
        return __splitImpl_ArchiveScreen(file, onBack)
    }

internal fun listArchive(f: File): List<ArcItem> = __splitImpl_listArchive(f)

// ───────────────────────── Editor (semua file, syntax RexCode) ─────────────────────────
internal fun tokenColor(type: String): Color {
        return __splitImpl_tokenColor(type)
    }

internal class RexHighlighter(
    internal val fileName: String,
    internal val enabled: Boolean
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return this.__splitImpl_filter(text)
    }
}

@Composable
internal fun EditorScreen(file: File, onBack: () -> Unit) {
        return __splitImpl_EditorScreen(file, onBack)
    }

// ───────────────────────── Media (jpg/png/jpeg/mp3/ogg/mp4) ─────────────────────────
@Composable
internal fun MediaScreen(file: File, onBack: () -> Unit) {
        return __splitImpl_MediaScreen(file, onBack)
    }

@Composable
internal fun ImagePreview(file: File) {
        return __splitImpl_ImagePreview(file)
    }

@Composable
internal fun AudioPreview(file: File) {
        return __splitImpl_AudioPreview(file)
    }

@Composable
internal fun VideoPreview(file: File) {
        return __splitImpl_VideoPreview(file)
    }