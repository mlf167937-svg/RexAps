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

fun ArchiveEngine.__split_capabilities_1(format: ArchiveFormat) = when (format) {
        ArchiveFormat.ZIP -> ArchiveCapabilities(true, true, true, true, true, supportsStreaming = true)
        ArchiveFormat.TAR, ArchiveFormat.TAR_GZIP, ArchiveFormat.TAR_XZ, ArchiveFormat.TAR_BZIP2 -> ArchiveCapabilities(true, true, true, true, true, supportsStreaming = true)
        else -> ArchiveCapabilities(canList = false, canExtract = false, canExtractSelected = false, canCreate = false)
    }

suspend internal fun ArchiveEngine.__split_list_2(input: InputStream, name: String, options: ArchiveOptions, progress: (ArchiveProgress) -> Unit): List<ArchiveEntry> {
        return when (val format = ArchiveFormat.fromFileName(name)) {
            ArchiveFormat.ZIP -> listZip(input, options, progress)
            ArchiveFormat.TAR, ArchiveFormat.TAR_GZIP, ArchiveFormat.TAR_XZ, ArchiveFormat.TAR_BZIP2 -> listTar(wrapTarInput(input, format), options, progress)
            else -> throw ArchiveError.BackendUnavailable(format)
        }
    }

internal suspend fun ArchiveEngine.__split_listZip_3(input: InputStream, options: ArchiveOptions, progress: (ArchiveProgress) -> Unit): List<ArchiveEntry> {
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

internal suspend fun ArchiveEngine.__split_listTar_4(input: InputStream, options: ArchiveOptions, progress: (ArchiveProgress) -> Unit): List<ArchiveEntry> {
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

internal fun ArchiveEngine.__split_wrapTarInput_10(input: InputStream, format: ArchiveFormat): InputStream = when (format) {
        ArchiveFormat.TAR_GZIP -> GzipCompressorInputStream(input)
        ArchiveFormat.TAR_XZ -> XZCompressorInputStream(input)
        ArchiveFormat.TAR_BZIP2 -> BZip2CompressorInputStream(input)
        else -> input
    }

internal fun ArchiveEngine.__split_collectFiles_11(sources: List<File>, options: ArchiveOptions): List<Pair<File, String>> {
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

internal fun ArchiveEngine.__split_prepareDestination_12(destination: File, options: ArchiveOptions) {
        destination.parentFile?.let { if (!it.exists() && !it.mkdirs()) throw ArchiveError.DestinationUnavailable(it.path) }
        if (destination.exists()) when (options.overwritePolicy) {
            OverwritePolicy.OVERWRITE -> if (!destination.delete()) throw ArchiveError.DestinationUnavailable(destination.path)
            OverwritePolicy.FAIL -> throw ArchiveError.DestinationExists(destination.path)
            OverwritePolicy.SKIP, OverwritePolicy.RENAME -> throw ArchiveError.DestinationExists(destination.path)
        }
    }

internal fun ArchiveEngine.__split_chooseTarget_13(target: File, policy: OverwritePolicy): File? {
        if (!target.exists()) return target
        return when (policy) {
            OverwritePolicy.SKIP -> null
            OverwritePolicy.OVERWRITE -> { if (target.isDirectory && !target.deleteRecursively()) throw ArchiveError.DestinationUnavailable(target.path); target }
            OverwritePolicy.FAIL -> throw ArchiveError.DestinationExists(target.path)
            OverwritePolicy.RENAME -> { var i = 1; var f: File; do { f = File(target.parentFile, "${target.nameWithoutExtension} ($i)" + if (target.extension.isNotEmpty()) ".${target.extension}" else ""); i++ } while (f.exists()); f }
        }
    }
