package com.rexaps.rexmanager.archive

import com.rexaps.rexmanager.CollisionPolicy
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/** ZIP using only java.util.zip (no external dependency). */
class ZipArchiveProvider : ArchiveProvider {
    override val format: ArchiveFormat = ArchiveFormat.ZIP

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
            ZipOutputStream(BufferedOutputStream(FileOutputStream(destination))).use { zip ->
                for (e in input.entries) {
                    currentCoroutineContext().ensureActive()
                    val entry = ZipEntry(if (e.isDirectory) e.entryName + "/" else e.entryName)
                    val modified = e.file.lastModified()
                    if (modified > 0L) entry.time = modified
                    zip.putNextEntry(entry)
                    if (!e.isDirectory) {
                        FileInputStream(e.file).use { stream ->
                            stream.copyWithProgress(zip, buffer) { n ->
                                processed += n
                                onProgress(processed, input.totalBytes)
                            }
                        }
                    }
                    zip.closeEntry()
                }
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
        ZipFile(archive).use { zip ->
            val entries = zip.entries().toList()
            val total = entries.sumOf { if (it.size > 0L) it.size else 0L }.coerceAtLeast(1L)
            val buffer = ByteArray(ARCHIVE_BUFFER_SIZE)
            var processed = 0L
            ensureDirectory(destinationDir)

            for (entry in entries) {
                currentCoroutineContext().ensureActive()
                val target = resolveExtractTarget(destinationDir, entry.name)
                if (entry.isDirectory) {
                    ensureDirectory(target)
                    continue
                }
                val finalTarget = resolveCollision(target, policy)
                if (finalTarget == null) {
                    if (entry.size > 0L) processed += entry.size
                    onProgress(processed, total)
                    continue
                }
                finalTarget.parentFile?.let { ensureDirectory(it) }
                zip.getInputStream(entry).use { input ->
                    FileOutputStream(finalTarget).use { output ->
                        input.copyWithProgress(output, buffer) { n ->
                            processed += n
                            onProgress(processed, total)
                        }
                    }
                }
                if (entry.time > 0L) finalTarget.setLastModified(entry.time)
                extracted++
            }
        }
        return extracted
    }
}
