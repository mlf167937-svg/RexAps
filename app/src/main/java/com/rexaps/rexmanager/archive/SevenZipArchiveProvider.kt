package com.rexaps.rexmanager.archive

import com.rexaps.rexmanager.CollisionPolicy
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/** 7Z using Apache Commons Compress (+ org.tukaani:xz for LZMA2). */
class SevenZipArchiveProvider : ArchiveProvider {
    override val format: ArchiveFormat = ArchiveFormat.SEVEN_Z

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
            SevenZOutputFile(destination).use { out ->
                for (e in input.entries) {
                    currentCoroutineContext().ensureActive()
                    val entry = out.createArchiveEntry(e.file, e.entryName)
                    out.putArchiveEntry(entry)
                    if (!e.isDirectory) {
                        FileInputStream(e.file).use { stream ->
                            while (true) {
                                currentCoroutineContext().ensureActive()
                                val read = stream.read(buffer)
                                if (read < 0) break
                                out.write(buffer, 0, read)
                                processed += read
                                onProgress(processed, input.totalBytes)
                            }
                        }
                    }
                    out.closeArchiveEntry()
                }
                out.finish()
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
        SevenZFile.builder().setFile(archive).get().use { sevenZ ->
            var total = 0L
            for (e in sevenZ.entries) {
                if (e.hasStream() && e.size > 0L) total += e.size
            }
            total = total.coerceAtLeast(1L)
            val buffer = ByteArray(ARCHIVE_BUFFER_SIZE)
            var processed = 0L
            ensureDirectory(destinationDir)

            var entry = sevenZ.nextEntry
            while (entry != null) {
                currentCoroutineContext().ensureActive()
                val name = entry.name
                if (name != null) {
                    val target = resolveExtractTarget(destinationDir, name)
                    if (entry.isDirectory) {
                        ensureDirectory(target)
                    } else {
                        val finalTarget = resolveCollision(target, policy)
                        if (finalTarget == null) {
                            if (entry.size > 0L) processed += entry.size
                            onProgress(processed, total)
                        } else {
                            finalTarget.parentFile?.let { ensureDirectory(it) }
                            FileOutputStream(finalTarget).use { output ->
                                while (true) {
                                    currentCoroutineContext().ensureActive()
                                    val read = sevenZ.read(buffer)
                                    if (read < 0) break
                                    output.write(buffer, 0, read)
                                    processed += read
                                    onProgress(processed, total)
                                }
                            }
                            extracted++
                        }
                    }
                }
                entry = sevenZ.nextEntry
            }
        }
        return extracted
    }
}
