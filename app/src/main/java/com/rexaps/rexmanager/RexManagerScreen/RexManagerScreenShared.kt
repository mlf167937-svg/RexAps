package com.rexaps.rexmanager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image as ImageIcon
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rexaps.librex.rexcode.RexCode
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

internal const val MAX_EDIT_BYTES = 2L * 1024 * 1024

internal const val HIGHLIGHT_LIMIT = 200_000L

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
internal fun PermissionGate(onGrant: () -> Unit) {
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

@Composable
internal fun iconFor(f: File): Pair<ImageVector, Color> {
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

// ───────────────────────── Compress (ZIP / 7Z) ─────────────────────────
internal enum class ArcFormat(val ext: String) { ZIP("zip"), SEVENZ("7z") }

internal fun walkTree(f: File, rel: String, cb: (File, String) -> Unit) {
    if (f.isDirectory) {
        cb(f, "$rel/")
        f.listFiles()?.forEach { walkTree(it, "$rel/${it.name}", cb) }
    } else {
        cb(f, rel)
    }
}

// ───────────────────────── Archive viewer (tanpa ekstrak) ─────────────────────────
internal data class ArcItem(val name: String, val size: Long, val isDir: Boolean)

internal class RexHighlighter(
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
