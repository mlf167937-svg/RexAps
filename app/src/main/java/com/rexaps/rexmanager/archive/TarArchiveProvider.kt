package com.rexaps.rexmanager.archive

import com.rexaps.rexmanager.CollisionPolicy
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Plain TAR (container only, NOT compressed) using Apache Commons Compress.
 * tar.gz / tar.xz / tar.zst can be added later as extra providers.
 */
class TarArchiveProvider : ArchiveProvider {
    override val format: ArchiveFormat = ArchiveFormat.TAR

    override suspend fun compress(
        sources: List<File>,
        destination: File,
        onProgress: (processed: Long, total: Long) -> Unit
    ) {
        val input = collectArchiveEntries(sources, destination)
        val buffer = ByteArray(ARCHIVE_BUFFER_SIZE)
        var processed = 0L
        var success = false
        try {
            TarArchiveOutputStream(BufferedOutputStream(FileOutputStream(destination))).use { tar ->
                tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX)
                tar.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX)
                for (e in input.entries) {
                    currentCoroutineContext().ensureActive()
                    val name = if (e.isDirectory) e.entryName + "/" else e.entryName
                    tar.putArchiveEntry(TarArchiveEntry(e.file, name))
                    if (!e.isDirectory) {
                        FileInputStream(e.file).use { stream ->
                            stream.copyWithProgress(tar, buffer) { n ->
                                processed += n
                                onProgress(processed, input.totalBytes)
                            }
                        }
                    }
                    tar.closeArchiveEntry()
                }
                tar.finish()
            }
            success = true
        } finally {
            if (!success) destination.delete()
        }
    }

    override suspend fun extract(
        archive: File,
        destinationDir: File,
        policy: CollisionPolicy,
        onProgress: (processed: Long, total: Long) -> Unit
    ): Int {
        var extracted = 0
        val totalBytes = archive.length().coerceAtLeast(1L)
        val counting = CountingInputStream(BufferedInputStream(FileInputStream(archive)))
        val buffer = ByteArray(ARCHIVE_BUFFER_SIZE)
        ensureDirectory(destinationDir)

        TarArchiveInputStream(counting).use { tar ->
            var entry = tar.nextEntry
            while (entry != null) {
                currentCoroutineContext().ensureActive()
                val isLinkOrSpecial = !entry.isDirectory &&
                    (entry.isSymbolicLink || entry.isLink || !entry.isFile)
                if (!isLinkOrSpecial) {
                    val target = resolveExtractTarget(destinationDir, entry.name)
                    if (entry.isDirectory) {
                        ensureDirectory(target)
                    } else {
                        val finalTarget = resolveCollision(target, policy)
                        if (finalTarget != null) {
                            finalTarget.parentFile?.let { ensureDirectory(it) }
                            FileOutputStream(finalTarget).use { output ->
                                tar.copyWithProgress(output, buffer) {
                                    onProgress(counting.count, totalBytes)
                                }
                            }
                            val modified = entry.lastModifiedDate?.time ?: 0L
                            if (modified > 0L) finalTarget.setLastModified(modified)
                            extracted++
                        }
                    }
                }
                onProgress(counting.count, totalBytes)
                entry = tar.nextEntry
            }
        }
        return extracted
    }
}
