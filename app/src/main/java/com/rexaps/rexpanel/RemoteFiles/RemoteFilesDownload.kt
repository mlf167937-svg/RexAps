package com.rexaps.rexpanel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import java.io.File

suspend internal fun SftpClient.__splitImpl_download(remote: String, dest: File, onProgress: (Long) -> Unit) {
        val tmp = File(dest.path + ".part")
        try {
            xfer.use { c, alive -> c.get(remote, tmp.absolutePath, monitor(alive, onProgress)) }
            tmp.renameTo(dest)
        } finally {
            if (tmp.exists()) tmp.delete()
        }
    }
