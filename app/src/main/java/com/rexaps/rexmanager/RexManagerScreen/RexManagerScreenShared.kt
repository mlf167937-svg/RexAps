package com.rexaps.rexmanager

import android.os.Build
import android.os.Environment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import com.rexaps.librex.rexcode.RexCode
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile

internal fun __splitImpl_compress(sources: List<File>, out: File, format: ArcFormat) {
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

 internal fun RexHighlighter.__splitImpl_filter(text: AnnotatedString): TransformedText {
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

// ───────────────────────── Editor (semua file, syntax RexCode) ─────────────────────────
internal fun __splitImpl_tokenColor(type: String): Color {
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
        else -> Color(0xFFEAEEF8)
    }
}

internal fun __splitImpl_formatSize(b: Long): String = when {
    b < 1024L -> "$b B"
    b < 1024L * 1024 -> "%.1f KB".format(b / 1024.0)
    b < 1024L * 1024 * 1024 -> "%.1f MB".format(b / 1048576.0)
    else -> "%.2f GB".format(b / 1073741824.0)
}

internal fun __splitImpl_walkTree(f: File, rel: String, cb: (File, String) -> Unit) {
    if (f.isDirectory) {
        cb(f, "$rel/")
        f.listFiles()?.forEach { walkTree(it, "$rel/${it.name}", cb) }
    } else {
        cb(f, rel)
    }
}

internal fun __splitImpl_hasAccess(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager() else true
