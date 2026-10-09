package com.rexaps.librex.rexarchive.document

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File

/** Android-native PDF page rendering. Text extraction/editing is intentionally not claimed. */
object PdfTools {
    data class Info(val pageCount: Int, val fileSizeBytes: Long?)

    fun getInfo(file: File): Info = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
        PdfRenderer(pfd).use { renderer -> Info(renderer.pageCount, file.length()) }
    }

    fun getInfo(context: Context, uri: Uri): Info = context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
        PdfRenderer(pfd).use { renderer -> Info(renderer.pageCount, null) }
    } ?: throw IllegalArgumentException("Cannot open PDF URI: $uri")

    /** Renders one page to a bitmap. Caller owns and must recycle the returned bitmap. */
    fun renderPage(file: File, pageIndex: Int, maxDimension: Int = 2048, backgroundColor: Int = Color.WHITE): Bitmap {
        require(maxDimension in 128..8192) { "maxDimension must be 128..8192" }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer -> render(renderer, pageIndex, maxDimension, backgroundColor) }
        }
    }

    fun renderPage(context: Context, uri: Uri, pageIndex: Int, maxDimension: Int = 2048, backgroundColor: Int = Color.WHITE): Bitmap {
        require(maxDimension in 128..8192) { "maxDimension must be 128..8192" }
        return context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            PdfRenderer(pfd).use { renderer -> render(renderer, pageIndex, maxDimension, backgroundColor) }
        } ?: throw IllegalArgumentException("Cannot open PDF URI: $uri")
    }

    private fun render(renderer: PdfRenderer, pageIndex: Int, maxDimension: Int, backgroundColor: Int): Bitmap {
        require(pageIndex in 0 until renderer.pageCount) { "Page index out of range: $pageIndex" }
        renderer.openPage(pageIndex).use { page ->
            val scale = minOf(1f, maxDimension.toFloat() / maxOf(page.width, page.height).coerceAtLeast(1))
            val width = (page.width * scale).toInt().coerceAtLeast(1)
            val height = (page.height * scale).toInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(backgroundColor)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            return bitmap
        }
    }
}
