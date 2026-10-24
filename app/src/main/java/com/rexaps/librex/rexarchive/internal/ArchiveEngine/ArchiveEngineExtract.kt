package com.rexaps.librex.rexarchive.internal

import com.rexaps.librex.rexarchive.*
import com.rexaps.librex.rexarchive.security.PathSanitizer
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import java.io.*
import java.util.zip.*

internal fun ArchiveEngine.capabilitiesImpl(format: ArchiveFormat) = when (format) {
    ArchiveFormat.ZIP -> ArchiveCapabilities(true, true, true, true, true, supportsStreaming = true)
    ArchiveFormat.TAR, ArchiveFormat.TAR_GZIP, ArchiveFormat.TAR_XZ, ArchiveFormat.TAR_BZIP2 -> ArchiveCapabilities(true, true, true, true, true, supportsStreaming = true)
    else -> ArchiveCapabilities(canList = false, canExtract = false, canExtractSelected = false, canCreate = false)
}

suspend internal fun ArchiveEngine.extractImpl(input: InputStream, name: String, destination: File, options: ArchiveOptions, progress: (ArchiveProgress) -> Unit): List<File> =
    extractInternal(input, name, destination, null, options, progress)

suspend internal fun ArchiveEngine.extractSelectedImpl(input: InputStream, name: String, destination: File, ids: Set<Int>, options: ArchiveOptions, progress: (ArchiveProgress) -> Unit): List<File> =
    extractInternal(input, name, destination, ids, options, progress)

private suspend fun ArchiveEngine.extractInternal(input: InputStream, name: String, destination: File, selected: Set<Int>?, options: ArchiveOptions, progress: (ArchiveProgress) -> Unit): List<File> {
    val format = ArchiveFormat.fromFileName(name)
    return when (format) {
        ArchiveFormat.ZIP -> extractZip(input, destination, selected, options, progress)
        ArchiveFormat.TAR, ArchiveFormat.TAR_GZIP, ArchiveFormat.TAR_XZ, ArchiveFormat.TAR_BZIP2 -> extractTar(wrapTarInput(input, format), destination, selected, options, progress)
        else -> throw ArchiveError.BackendUnavailable(format)
    }
}

private suspend fun ArchiveEngine.extractZip(input: InputStream, root: File, selected: Set<Int>?, options: ArchiveOptions, progress: (ArchiveProgress) -> Unit): List<File> {
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

private suspend fun ArchiveEngine.extractTar(input: InputStream, root: File, selected: Set<Int>?, options: ArchiveOptions, progress: (ArchiveProgress) -> Unit): List<File> {
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

internal fun ArchiveEngine.wrapTarInput(input: InputStream, format: ArchiveFormat): InputStream = when (format) {
    ArchiveFormat.TAR_GZIP -> GzipCompressorInputStream(input)
    ArchiveFormat.TAR_XZ -> XZCompressorInputStream(input)
    ArchiveFormat.TAR_BZIP2 -> BZip2CompressorInputStream(input)
    else -> input
}

internal fun ArchiveEngine.prepareDestination(destination: File, options: ArchiveOptions) {
    destination.parentFile?.let { if (!it.exists() && !it.mkdirs()) throw ArchiveError.DestinationUnavailable(it.path) }
    if (destination.exists()) when (options.overwritePolicy) {
        OverwritePolicy.OVERWRITE -> if (!destination.delete()) throw ArchiveError.DestinationUnavailable(destination.path)
        OverwritePolicy.FAIL -> throw ArchiveError.DestinationExists(destination.path)
        OverwritePolicy.SKIP, OverwritePolicy.RENAME -> throw ArchiveError.DestinationExists(destination.path)
    }
}

private fun ArchiveEngine.ensureParent(file: File) { val p = file.parentFile ?: throw ArchiveError.DestinationUnavailable(file.path); if (!p.exists() && !p.mkdirs()) throw ArchiveError.DestinationUnavailable(p.path) }

private fun ArchiveEngine.chooseTarget(target: File, policy: OverwritePolicy): File? {
    if (!target.exists()) return target
    return when (policy) {
        OverwritePolicy.SKIP -> null
        OverwritePolicy.OVERWRITE -> { if (target.isDirectory && !target.deleteRecursively()) throw ArchiveError.DestinationUnavailable(target.path); target }
        OverwritePolicy.FAIL -> throw ArchiveError.DestinationExists(target.path)
        OverwritePolicy.RENAME -> { var i = 1; var f: File; do { f = File(target.parentFile, "${target.nameWithoutExtension} ($i)" + if (target.extension.isNotEmpty()) ".${target.extension}" else ""); i++ } while (f.exists()); f }
    }
}

private fun ArchiveEngine.drain(input: InputStream, bufferSize: Int) { val b = ByteArray(bufferSize); while (input.read(b) >= 0) { /* consume */ } }
