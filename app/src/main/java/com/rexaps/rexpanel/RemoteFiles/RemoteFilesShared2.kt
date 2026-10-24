package com.rexaps.rexpanel

import android.graphics.Bitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

internal fun FileBrowserState.__split_mutate_3(ok: String, block: suspend () -> Unit) {
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

fun FileBrowserState.__split_deleteAll_4(list: List<RemoteFile>) {
        scope.launch {
            var ok = 0
            var fail = 0
            var last: String? = null
            for (f in list) {
                try {
                    sftp.remove(f)
                    ok++
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    fail++
                    last = sftpMessage(e)
                }
            }
            notice = if (fail == 0) "$ok item dihapus" else "$ok dihapus, $fail gagal" + (last?.let { ": $it" } ?: "")
            refresh()
        }
    }

suspend internal fun FileBrowserState.__split_thumbnail_5(f: RemoteFile): Bitmap? {
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

/** Unduh ke cache (dipakai ulang jika sudah ada). */
    suspend internal fun FileBrowserState.__split_fetch_6(f: RemoteFile, onProgress: (Long) -> Unit): File {
        val dir = File(cacheDir, "rexpanel").apply { mkdirs() }
        val tag = Integer.toHexString((f.path + f.mtimeSec).hashCode())
        val dest = File(dir, "${tag}_${f.name}".replace('/', '_'))
        if (dest.exists() && dest.length() == f.size) return dest
        sftp.download(f.path, dest, onProgress)
        return dest
    }
