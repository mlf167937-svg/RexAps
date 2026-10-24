package com.rexaps.rexmanager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.rexaps.librex.rexcode.RexCode

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

// ───────────────────────── Compress (ZIP / 7Z) ─────────────────────────
internal enum class ArcFormat(val ext: String) { ZIP("zip"), SEVENZ("7z") }

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
