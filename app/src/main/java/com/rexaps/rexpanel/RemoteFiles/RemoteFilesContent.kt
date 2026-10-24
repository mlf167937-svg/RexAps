package com.rexaps.rexpanel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.jcraft.jsch.ChannelSftp
import java.util.Vector

suspend internal fun SftpClient.__splitImpl_list(path: String): List<RemoteFile> = browse.use { c, _ ->
        val dir = if (path.isEmpty()) "/" else path
        val base = dir.trimEnd('/')
        (c.ls(dir) as Vector<*>).filterIsInstance<ChannelSftp.LsEntry>()
            .filter { it.filename != "." && it.filename != ".." }
            .map { e ->
                val a = e.attrs
                val full = "$base/${e.filename}"
                var isDir = a.isDir
                var size = a.size
                if (a.isLink) {
                    val st = runCatching { c.stat(full) }.getOrNull()
                    isDir = st?.isDir ?: false
                    size = st?.size ?: size
                }
                RemoteFile(
                    name = e.filename, path = full, isDir = isDir, isLink = a.isLink,
                    size = size, mtimeSec = a.mTime.toLong(), perms = a.permissionsString,
                    kind = kindOf(e.filename, isDir)
                )
            }
    }
