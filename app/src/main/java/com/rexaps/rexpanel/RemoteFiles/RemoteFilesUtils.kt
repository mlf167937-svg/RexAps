package com.rexaps.rexpanel

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Build
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.SftpException
import com.jcraft.jsch.SftpProgressMonitor
import java.io.ByteArrayInputStream
import java.io.File

fun kindOf(name: String, isDir: Boolean): FileKind {
    if (isDir) return FileKind.Other
    val lower = name.lowercase()
    val ext = lower.substringAfterLast('.', "")
    return when {
        ext in IMAGE_EXT -> FileKind.Image
        ext in VIDEO_EXT -> FileKind.Video
        ext in AUDIO_EXT -> FileKind.Audio
        ext in ARCHIVE_EXT -> FileKind.Archive
        ext in TEXT_EXT || lower in TEXT_NAMES || (lower.startsWith(".") && '.' !in lower.drop(1)) -> FileKind.Text
        else -> FileKind.Other
    }
}

fun shq(s: String): String = "'" + s.replace("'", "'\\''") + "'"

fun sftpMessage(e: Exception): String {
    if (e is SftpException) {
        return when (e.id) {
            ChannelSftp.SSH_FX_NO_SUCH_FILE -> "Berkas atau folder tidak ditemukan."
            ChannelSftp.SSH_FX_PERMISSION_DENIED -> "Akses ditolak. Akun ini tidak punya izin untuk lokasi tersebut."
            else -> e.message?.takeIf { it.isNotBlank() }
                ?: "Operasi gagal. Jika menghapus folder, pastikan folder kosong."
        }
    }
    return e.message?.takeIf { it.isNotBlank() } ?: "Terjadi kesalahan."
}

private fun sampleFor(w: Int, h: Int, maxSide: Int): Int {
    var s = 1
    while (maxOf(w, h) / (s * 2) >= maxSide) s *= 2
    return s
}

private fun orient(bmp: Bitmap, o: Int): Bitmap {
    val m = Matrix()
    when (o) {
        ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
        else -> return bmp
    }
    return runCatching { Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true) }.getOrDefault(bmp)
}

fun decodeSampled(bytes: ByteArray, maxSide: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0) return null
    val opts = BitmapFactory.Options().apply { inSampleSize = sampleFor(bounds.outWidth, bounds.outHeight, maxSide) }
    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) ?: return null
    val o = if (Build.VERSION.SDK_INT >= 24) {
        runCatching {
            ExifInterface(ByteArrayInputStream(bytes))
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    } else ExifInterface.ORIENTATION_NORMAL
    return orient(bmp, o)
}

fun decodeFileSampled(file: File, maxSide: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    if (bounds.outWidth <= 0) return null
    val opts = BitmapFactory.Options().apply { inSampleSize = sampleFor(bounds.outWidth, bounds.outHeight, maxSide) }
    val bmp = BitmapFactory.decodeFile(file.absolutePath, opts) ?: return null
    val o = runCatching {
        ExifInterface(file.absolutePath)
            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    return orient(bmp, o)
}

internal fun monitor(alive: () -> Boolean, onCount: (Long) -> Unit = {}) = object : SftpProgressMonitor {
    private var total = 0L
    override fun init(op: Int, src: String?, dest: String?, max: Long) {}
    override fun count(count: Long): Boolean { total += count; onCount(total); return alive() }
    override fun end() {}
}
