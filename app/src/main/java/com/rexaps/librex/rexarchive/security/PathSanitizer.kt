package com.rexaps.librex.rexarchive.security

import com.rexaps.librex.rexarchive.ArchiveError
import java.io.File

internal object PathSanitizer {
    fun relativePath(raw: String, maxLength: Int): String {
        if (raw.isBlank() || raw.length > maxLength) throw ArchiveError.UnsafePath(raw)
        val normalized = raw.replace('\\', '/')
        if (normalized.startsWith('/') || normalized.startsWith("//") || Regex("^[A-Za-z]:").containsMatchIn(normalized)) {
            throw ArchiveError.UnsafePath(raw)
        }
        val parts = normalized.split('/')
        if (parts.any { it == ".." || it == "." || it.indexOf('\u0000') >= 0 }) throw ArchiveError.UnsafePath(raw)
        return parts.filter { it.isNotEmpty() }.joinToString("/").also {
            if (it.isBlank()) throw ArchiveError.UnsafePath(raw)
        }
    }

    fun resolveInside(root: File, relative: String, maxLength: Int): File {
        val safe = relativePath(relative, maxLength)
        val canonicalRoot = root.canonicalFile
        val target = File(canonicalRoot, safe).canonicalFile
        if (target.path != canonicalRoot.path && !target.path.startsWith(canonicalRoot.path + File.separator)) {
            throw ArchiveError.UnsafePath(relative)
        }
        return target
    }
}
