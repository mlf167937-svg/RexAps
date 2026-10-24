package com.rexaps.rexmusic

import java.io.File

/** Thumbnail bersifat opsional: kalau gagal, lagu tetap tersimpan tanpa cover. */
    internal fun RexOfflineManager.__split_downloadThumbnail_11(url: String, dir: File) {
        if (!url.startsWith("http")) return
        val tmp = File(dir, "thumbnail.tmp")
        try {
            val conn = openConnection(url)
            try {
                if (conn.responseCode !in 200..299) return
                conn.inputStream.use { input -> tmp.outputStream().use { input.copyTo(it) } }
            } finally {
                conn.disconnect()
            }
            if (tmp.length() <= 0L) return
            val isPng = tmp.inputStream().use { s ->
                val h = ByteArray(4)
                s.read(h) == 4 && h[0] == 0x89.toByte() && h[1] == 0x50.toByte() &&
                    h[2] == 0x4E.toByte() && h[3] == 0x47.toByte()
            }
            File(dir, "thumbnail.jpg").delete()
            File(dir, "thumbnail.png").delete()
            tmp.renameTo(File(dir, if (isPng) "thumbnail.png" else "thumbnail.jpg"))
        } catch (_: Exception) {
            // abaikan
        } finally {
            tmp.delete()
        }
    }
