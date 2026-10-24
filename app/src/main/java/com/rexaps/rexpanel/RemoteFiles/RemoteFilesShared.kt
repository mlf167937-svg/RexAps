package com.rexaps.rexpanel

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.SftpException
import java.io.ByteArrayInputStream
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.withLock

suspend internal fun <T> Lane.__splitImpl_use(block: (ChannelSftp, () -> Boolean) -> T): T = lock.withLock {
        withContext(Dispatchers.IO) {
            val job = coroutineContext[Job]
            val alive = { job?.isActive != false }
            val cur = ch
            val chan = if (cur != null && cur.isConnected && !cur.isClosed) cur else opener().also { ch = it }
            try {
                val r = block(chan, alive)
                ensureActive()
                r
            } catch (e: Throwable) {
                // Channel yang transfernya dibatalkan atau error aneh tidak dipakai ulang.
                if (!alive() || e !is SftpException) {
                    runCatching { chan.disconnect() }
                    if (ch === chan) ch = null
                }
                throw e
            }
        }
    }

internal fun __splitImpl_decodeSampled(bytes: ByteArray, maxSide: Int): Bitmap? {
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

internal fun FileBrowserState.__splitImpl_go(target: String) {
        job?.cancel()
        val id = ++req
        job = scope.launch {
            loading = true
            if (path.isEmpty()) error = null
            try {
                val list = sftp.list(target)
                if (id == req) { entries = list; path = target; error = null; selected = emptySet() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (id == req) {
                    if (entries.isEmpty()) { error = sftpMessage(e); path = target }
                    else notice = sftpMessage(e)
                }
            } finally {
                if (id == req) loading = false
            }
        }
    }

suspend internal fun FileBrowserState.__splitImpl_thumbnail(f: RemoteFile): Bitmap? {
        val key = thumbKey(f)
        ThumbCache.get(key)?.let { return it }
        if (key in failedThumbs) return null
        return try {
            val bytes = sftp.readThumb(f.path)
            val bmp = withContext(Dispatchers.Default) { decodeSampled(bytes, 256) }
            if (bmp != null) ThumbCache.put(key, bmp) else failedThumbs.add(key)
            bmp
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            failedThumbs.add(key)
            null
        }
    }

internal fun __splitImpl_sftpMessage(e: Exception): String {
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

internal fun __splitImpl_kindOf(name: String, isDir: Boolean): FileKind {
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

internal fun __splitImpl_orient(bmp: Bitmap, o: Int): Bitmap {
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

/** Unduh ke cache (dipakai ulang jika sudah ada). */
    suspend internal fun FileBrowserState.__splitImpl_fetch(f: RemoteFile, onProgress: (Long) -> Unit): File {
        val dir = File(cacheDir, "rexpanel").apply { mkdirs() }
        val tag = Integer.toHexString((f.path + f.mtimeSec).hashCode())
        val dest = File(dir, "${tag}_${f.name}".replace('/', '_'))
        if (dest.exists() && dest.length() == f.size) return dest
        sftp.download(f.path, dest, onProgress)
        return dest
    }

internal fun FileBrowserState.__splitImpl_start() {
        scope.launch {
            loading = true
            try {
                home = sftp.home()
                go(home)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = sftpMessage(e)
                loading = false
            }
        }
    }

internal fun FileBrowserState.__splitImpl_mutate(ok: String, block: suspend () -> Unit) {
        scope.launch {
            try {
                block()
                notice = ok
                refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                notice = sftpMessage(e)
            }
        }
    }
