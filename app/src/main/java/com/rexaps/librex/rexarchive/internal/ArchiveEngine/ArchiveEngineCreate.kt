package com.rexaps.librex.rexarchive.internal

import com.rexaps.librex.rexarchive.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream
import java.io.*
import java.util.zip.*

suspend internal fun ArchiveEngine.createZipImpl(sources: List<File>, destination: File, options: ArchiveOptions, progress: (ArchiveProgress) -> Unit) {
    require(sources.isNotEmpty()) { "At least one source is required" }
    prepareDestination(destination, options)
    var done = 0L; var bytes = 0L
    ZipOutputStream(BufferedOutputStream(FileOutputStream(destination), options.bufferSize)).use { zout ->
        zout.setLevel(options.compressionLevel)
        val all = collectFiles(sources, options)
        for ((file, name) in all) {
            currentCoroutineContext().ensureActive()
            val entry = ZipEntry(if (file.isDirectory) name.trimEnd('/') + "/" else name)
            if (file.exists()) entry.time = file.lastModified()
            zout.putNextEntry(entry)
            if (file.isFile) FileInputStream(file).buffered(options.bufferSize).use { input ->
                val buffer = ByteArray(options.bufferSize)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val n = input.read(buffer); if (n < 0) break
                    zout.write(buffer, 0, n); bytes += n
                }
            }
            zout.closeEntry(); done++
            progress(ArchiveProgress(ArchiveOperation.CREATE, name, done, all.size.toLong(), bytes, all.sumOf { if (it.first.isFile) it.first.length() else 0L }, null))
        }
    }
}

suspend internal fun ArchiveEngine.createTarImpl(sources: List<File>, destination: File, format: ArchiveFormat, options: ArchiveOptions, progress: (ArchiveProgress) -> Unit) {
    require(format in setOf(ArchiveFormat.TAR, ArchiveFormat.TAR_GZIP, ArchiveFormat.TAR_XZ, ArchiveFormat.TAR_BZIP2)) { "Unsupported TAR output format: $format" }
    require(sources.isNotEmpty()) { "At least one source is required" }
    prepareDestination(destination, options)
    val raw = BufferedOutputStream(FileOutputStream(destination), options.bufferSize)
    val compressed: OutputStream = when (format) {
        ArchiveFormat.TAR_GZIP -> GzipCompressorOutputStream(raw)
        ArchiveFormat.TAR_XZ -> XZCompressorOutputStream(raw)
        ArchiveFormat.TAR_BZIP2 -> BZip2CompressorOutputStream(raw)
        else -> raw
    }
    var done = 0L; var bytes = 0L
    TarArchiveOutputStream(compressed).use { tout ->
        tout.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX)
        tout.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX)
        val all = collectFiles(sources, options)
        for ((file, name) in all) {
            currentCoroutineContext().ensureActive()
            val entry = TarArchiveEntry(file, name.trimEnd('/') + if (file.isDirectory) "/" else "")
            tout.putArchiveEntry(entry)
            if (file.isFile) FileInputStream(file).buffered(options.bufferSize).use { input ->
                val buffer = ByteArray(options.bufferSize)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val n = input.read(buffer); if (n < 0) break
                    tout.write(buffer, 0, n); bytes += n
                }
            }
            tout.closeArchiveEntry(); done++
            progress(ArchiveProgress(ArchiveOperation.CREATE, name, done, all.size.toLong(), bytes, all.sumOf { if (it.first.isFile) it.first.length() else 0L }, null))
        }
        tout.finish()
    }
}

private fun ArchiveEngine.collectFiles(sources: List<File>, options: ArchiveOptions): List<Pair<File, String>> {
    val out = mutableListOf<Pair<File, String>>()
    for (source in sources) {
        require(source.exists()) { "Source does not exist: ${source.path}" }
        val rootName = source.name.ifBlank { "item" }
        if (source.isDirectory) {
            if (options.includeRootDirectory) out += source to rootName + "/"
            source.walkTopDown().forEach { f ->
                if (f == source) return@forEach
                if (!options.includeHiddenFiles && f.name.startsWith('.')) return@forEach
                val relative = f.relativeTo(source.parentFile ?: source).invariantSeparatorsPath
                out += f to (if (options.includeRootDirectory) relative else f.relativeTo(source).invariantSeparatorsPath)
            }
        } else out += source to rootName
    }
    return out
}
