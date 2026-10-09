package com.rexaps.librex.rexarchive.internal

import com.rexaps.librex.rexarchive.*
import com.rexaps.librex.rexarchive.security.PathSanitizer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream
import java.io.*
import java.util.zip.*

internal class ArchiveEngine {
    fun capabilities(format: ArchiveFormat) = when (format) {
        ArchiveFormat.ZIP -> ArchiveCapabilities(true, true, true, true, true, supportsStreaming = true)
        ArchiveFormat.TAR, ArchiveFormat.TAR_GZIP, ArchiveFormat.TAR_XZ, ArchiveFormat.TAR_BZIP2 -> ArchiveCapabilities(true, true, true, true, true, supportsStreaming = true)
        else -> ArchiveCapabilities(canList = false, canExtract = false, canExtractSelected = false, canCreate = false)
    }

    suspend fun list(input: InputStream, name: String, options: ArchiveOptions, progress: (ArchiveProgress) -> Unit): List<ArchiveEntry> {
        return when (val format = ArchiveFormat.fromFileName(name)) {
            ArchiveFormat.ZIP -> listZip(input, options, progress)
            ArchiveFormat.TAR, ArchiveFormat.TAR_GZIP, ArchiveFormat.TAR_XZ, ArchiveFormat.TAR_BZIP2 -> listTar(wrapTarInput(input, format), options, progress)
            else -> throw ArchiveError.BackendUnavailable(format)
        }
    }

    private suspend fun listZip(input: InputStream, options: ArchiveOptions, progress: (ArchiveProgress) -> Unit): List<ArchiveEntry> {
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

    private suspend fun listTar(input: InputStream, options: ArchiveOptions, progress: (ArchiveProgress) -> Unit): List<ArchiveEntry> {
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

    suspend fun extract(input: InputStream, name: String, destination: File, options: ArchiveOptions, progress: (ArchiveProgress) -> Unit): List<File> =
        extractInternal(input, name, destination, null, options, progress)

    suspend fun extractSelected(input: InputStream, name: String, destination: File, ids: Set<Int>, options: ArchiveOptions, progress: (ArchiveProgress) -> Unit): List<File> =
        extractInternal(input, name, destination, ids, options, progress)

    private suspend fun extractInternal(input: InputStream, name: String, destination: File, selected: Set<Int>?, options: ArchiveOptions, progress: (ArchiveProgress) -> Unit): List<File> {
        val format = ArchiveFormat.fromFileName(name)
        return when (format) {
            ArchiveFormat.ZIP -> extractZip(input, destination, selected, options, progress)
            ArchiveFormat.TAR, ArchiveFormat.TAR_GZIP, ArchiveFormat.TAR_XZ, ArchiveFormat.TAR_BZIP2 -> extractTar(wrapTarInput(input, format), destination, selected, options, progress)
            else -> throw ArchiveError.BackendUnavailable(format)
        }
    }

    private suspend fun extractZip(input: InputStream, root: File, selected: Set<Int>?, options: ArchiveOptions, progress: (ArchiveProgress) -> Unit): List<File> {
        val written = mutableListOf<File>(); var entries = 0L; var bytes = 0L
        ZipInputStream(BufferedInputStream(input, options.bufferSize)).use { zin ->
            var index = 0
            while (true) {
                currentCoroutineContext().ensureActive()
                val e = zin.nextEntry ?: break
                val id = index++
                if (++entries > options.limits.maxEntries) throw ArchiveError.LimitExceeded("Too many archive entries")
                val relative = PathSanitizer.relativePath(e.name, options.limits.maxPathLength)
                if (selected != null && id !in selected) { zin.closeEntry(); continue }
                val target = PathSanitizer.resolveInside(root, relative, options.limits.maxPathLength)
                if (e.isDirectory) { if (!target.exists() && !target.mkdirs()) throw ArchiveError.DestinationUnavailable(target.path) }
                else {
                    ensureParent(target)
                    val actual = chooseTarget(target, options.overwritePolicy)
                    if (actual != null) {
                        var entryBytes = 0L
                        FileOutputStream(actual).buffered(options.bufferSize).use { out ->
                            val buffer = ByteArray(options.bufferSize)
                            while (true) {
                                currentCoroutineContext().ensureActive()
                                val n = zin.read(buffer); if (n < 0) break
                                entryBytes += n; bytes += n
                                if (entryBytes > options.limits.maxEntryBytes || bytes > options.limits.maxTotalBytes) throw ArchiveError.LimitExceeded("Expanded data limit exceeded")
                                if (e.compressedSize > 0 && entryBytes.toDouble() / e.compressedSize > options.limits.maxCompressionRatio) throw ArchiveError.LimitExceeded("Compression ratio limit exceeded")
                                out.write(buffer, 0, n)
                            }
                        }
                        if (e.time > 0) actual.setLastModified(e.time)
                        written += actual
                    } else { drain(zin, options.bufferSize) }
                }
                progress(ArchiveProgress(ArchiveOperation.EXTRACT, e.name, entries, null, bytes, null, null))
                zin.closeEntry()
            }
        }
        return written
    }

    private suspend fun extractTar(input: InputStream, root: File, selected: Set<Int>?, options: ArchiveOptions, progress: (ArchiveProgress) -> Unit): List<File> {
        val written = mutableListOf<File>(); var entries = 0L; var bytes = 0L
        TarArchiveInputStream(BufferedInputStream(input, options.bufferSize)).use { tin ->
            var index = 0
            while (true) {
                currentCoroutineContext().ensureActive()
                val e = tin.nextTarEntry ?: break
                val id = index++
                if (++entries > options.limits.maxEntries) throw ArchiveError.LimitExceeded("Too many archive entries")
                if (e.isSymbolicLink || e.isLink) {
                    if (options.symlinkPolicy == SymlinkPolicy.REJECT) throw ArchiveError.InvalidArchive("Link entry rejected: ${e.name}")
                    continue
                }
                val relative = PathSanitizer.relativePath(e.name, options.limits.maxPathLength)
                if (selected != null && id !in selected) { drain(tin, options.bufferSize); continue }
                val target = PathSanitizer.resolveInside(root, relative, options.limits.maxPathLength)
                if (e.isDirectory) { if (!target.exists() && !target.mkdirs()) throw ArchiveError.DestinationUnavailable(target.path) }
                else {
                    if (e.size > options.limits.maxEntryBytes) throw ArchiveError.LimitExceeded("Entry too large: $relative")
                    ensureParent(target)
                    val actual = chooseTarget(target, options.overwritePolicy)
                    if (actual != null) {
                        var entryBytes = 0L
                        FileOutputStream(actual).buffered(options.bufferSize).use { out ->
                            val buffer = ByteArray(options.bufferSize)
                            while (true) {
                                currentCoroutineContext().ensureActive()
                                val n = tin.read(buffer); if (n < 0) break
                                entryBytes += n; bytes += n
                                if (entryBytes > options.limits.maxEntryBytes || bytes > options.limits.maxTotalBytes) throw ArchiveError.LimitExceeded("Expanded data limit exceeded")
                                out.write(buffer, 0, n)
                            }
                        }
                        if (e.lastModifiedDate != null) actual.setLastModified(e.lastModifiedDate.time)
                        written += actual
                    } else drain(tin, options.bufferSize)
                }
                progress(ArchiveProgress(ArchiveOperation.EXTRACT, e.name, entries, null, bytes, null, null))
            }
        }
        return written
    }

    suspend fun createZip(sources: List<File>, destination: File, options: ArchiveOptions, progress: (ArchiveProgress) -> Unit) {
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

    suspend fun createTar(sources: List<File>, destination: File, format: ArchiveFormat, options: ArchiveOptions, progress: (ArchiveProgress) -> Unit) {
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

    private fun wrapTarInput(input: InputStream, format: ArchiveFormat): InputStream = when (format) {
        ArchiveFormat.TAR_GZIP -> GzipCompressorInputStream(input)
        ArchiveFormat.TAR_XZ -> XZCompressorInputStream(input)
        ArchiveFormat.TAR_BZIP2 -> BZip2CompressorInputStream(input)
        else -> input
    }

    private fun collectFiles(sources: List<File>, options: ArchiveOptions): List<Pair<File, String>> {
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

    private fun prepareDestination(destination: File, options: ArchiveOptions) {
        destination.parentFile?.let { if (!it.exists() && !it.mkdirs()) throw ArchiveError.DestinationUnavailable(it.path) }
        if (destination.exists()) when (options.overwritePolicy) {
            OverwritePolicy.OVERWRITE -> if (!destination.delete()) throw ArchiveError.DestinationUnavailable(destination.path)
            OverwritePolicy.FAIL -> throw ArchiveError.DestinationExists(destination.path)
            OverwritePolicy.SKIP, OverwritePolicy.RENAME -> throw ArchiveError.DestinationExists(destination.path)
        }
    }

    private fun ensureParent(file: File) { val p = file.parentFile ?: throw ArchiveError.DestinationUnavailable(file.path); if (!p.exists() && !p.mkdirs()) throw ArchiveError.DestinationUnavailable(p.path) }
    private fun chooseTarget(target: File, policy: OverwritePolicy): File? {
        if (!target.exists()) return target
        return when (policy) {
            OverwritePolicy.SKIP -> null
            OverwritePolicy.OVERWRITE -> { if (target.isDirectory && !target.deleteRecursively()) throw ArchiveError.DestinationUnavailable(target.path); target }
            OverwritePolicy.FAIL -> throw ArchiveError.DestinationExists(target.path)
            OverwritePolicy.RENAME -> { var i = 1; var f: File; do { f = File(target.parentFile, "${target.nameWithoutExtension} ($i)" + if (target.extension.isNotEmpty()) ".${target.extension}" else ""); i++ } while (f.exists()); f }
        }
    }
    private fun drain(input: InputStream, bufferSize: Int) { val b = ByteArray(bufferSize); while (input.read(b) >= 0) { /* consume */ } }
}
