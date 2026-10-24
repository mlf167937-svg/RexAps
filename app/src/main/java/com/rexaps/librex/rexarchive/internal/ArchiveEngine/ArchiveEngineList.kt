package com.rexaps.librex.rexarchive.internal

import com.rexaps.librex.rexarchive.*
import com.rexaps.librex.rexarchive.security.PathSanitizer
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.*
import java.util.zip.*

suspend internal fun ArchiveEngine.listImpl(input: InputStream, name: String, options: ArchiveOptions, progress: (ArchiveProgress) -> Unit): List<ArchiveEntry> {
    return when (val format = ArchiveFormat.fromFileName(name)) {
        ArchiveFormat.ZIP -> listZip(input, options, progress)
        ArchiveFormat.TAR, ArchiveFormat.TAR_GZIP, ArchiveFormat.TAR_XZ, ArchiveFormat.TAR_BZIP2 -> listTar(wrapTarInput(input, format), options, progress)
        else -> throw ArchiveError.BackendUnavailable(format)
    }
}

private suspend fun ArchiveEngine.listZip(input: InputStream, options: ArchiveOptions, progress: (ArchiveProgress) -> Unit): List<ArchiveEntry> {
    val out = mutableListOf<ArchiveEntry>(); var total = 0L
    ZipInputStream(BufferedInputStream(input, options.bufferSize)).use { zin ->
        var index = 0
        while (true) {
            currentCoroutineContext().ensureActive()
            val e = zin.nextEntry ?: break
            if (++index > options.limits.maxEntries) throw ArchiveError.LimitExceeded("Too many archive entries")
            val path = PathSanitizer.relativePath(e.name, options.limits.maxPathLength)
            val size = e.size.takeIf { it >= 0 }
            if (size != null && size > options.limits.maxEntryBytes) throw ArchiveError.LimitExceeded("Entry too large: $path")
            out += ArchiveEntry(index - 1, e.name, path, e.isDirectory, size, e.compressedSize.takeIf { it >= 0 }, e.time.takeIf { it > 0 }, e.crc.takeIf { it >= 0 })
            total += size ?: 0
            if (total > options.limits.maxTotalBytes) throw ArchiveError.LimitExceeded("Archive exceeds total expanded size limit")
            progress(ArchiveProgress(ArchiveOperation.LIST, e.name, index.toLong(), null, total, null, null))
            zin.closeEntry()
        }
    }
    return out
}

private suspend fun ArchiveEngine.listTar(input: InputStream, options: ArchiveOptions, progress: (ArchiveProgress) -> Unit): List<ArchiveEntry> {
    val out = mutableListOf<ArchiveEntry>(); var total = 0L
    TarArchiveInputStream(BufferedInputStream(input, options.bufferSize)).use { tin ->
        var index = 0
        while (true) {
            currentCoroutineContext().ensureActive()
            val e = tin.nextTarEntry ?: break
            if (++index > options.limits.maxEntries) throw ArchiveError.LimitExceeded("Too many archive entries")
            val path = PathSanitizer.relativePath(e.name, options.limits.maxPathLength)
            if (e.size > options.limits.maxEntryBytes) throw ArchiveError.LimitExceeded("Entry too large: $path")
            total += e.size
            if (total > options.limits.maxTotalBytes) throw ArchiveError.LimitExceeded("Archive exceeds total expanded size limit")
            out += ArchiveEntry(index - 1, e.name, path, e.isDirectory, e.size, null, e.lastModifiedDate?.time, null, null, e.isSymbolicLink || e.isLink)
            progress(ArchiveProgress(ArchiveOperation.LIST, e.name, index.toLong(), null, total, null, null))
        }
    }
    return out
}
