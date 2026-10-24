package com.rexaps.librex.rexarchive.internal

import com.github.junrar.Archive
import com.rexaps.librex.rexarchive.*
import com.rexaps.librex.rexarchive.security.PathSanitizer
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/** Random-access backends: 7z needs a seekable file; RAR is read/extract only. */
internal object RandomAccessArchiveEngine {
    suspend fun list(file: File, format: ArchiveFormat, options: ArchiveOptions, progress: (ArchiveProgress) -> Unit): List<ArchiveEntry> = when (format) {
        ArchiveFormat.SEVEN_ZIP -> list7z(file, options, progress)
        ArchiveFormat.RAR -> listRar(file, options, progress)
        else -> throw ArchiveError.BackendUnavailable(format)
    }

    suspend fun extract(file: File, format: ArchiveFormat, root: File, selected: Set<Int>?, options: ArchiveOptions, progress: (ArchiveProgress) -> Unit): List<File> = when (format) {
        ArchiveFormat.SEVEN_ZIP -> extract7z(file, root, selected, options, progress)
        ArchiveFormat.RAR -> extractRar(file, root, selected, options, progress)
        else -> throw ArchiveError.BackendUnavailable(format)
    }

    private suspend fun list7z(file: File, options: ArchiveOptions, progress: (ArchiveProgress) -> Unit): List<ArchiveEntry> {
        val out = mutableListOf<ArchiveEntry>(); var total = 0L
        SevenZFile(file).use { archive ->
            var i = 0
            for (e in archive.entries) {
                currentCoroutineContext().ensureActive()
                if (++i > options.limits.maxEntries) throw ArchiveError.LimitExceeded("Too many archive entries")
                val name = e.name ?: "entry-${i - 1}"
                val path = PathSanitizer.relativePath(name, options.limits.maxPathLength)
                val size = e.size.takeIf { it >= 0 }
                if (size != null && size > options.limits.maxEntryBytes) throw ArchiveError.LimitExceeded("Entry too large: $path")
                total += size ?: 0
                if (total > options.limits.maxTotalBytes) throw ArchiveError.LimitExceeded("Archive exceeds total expanded size limit")
                out += ArchiveEntry(i - 1, name, path, e.isDirectory, size, null, e.lastModifiedDate?.time, null, null)
                progress(ArchiveProgress(ArchiveOperation.LIST, name, i.toLong(), null, total, null, null))
            }
        }
        return out
    }

    private suspend fun extract7z(file: File, root: File, selected: Set<Int>?, options: ArchiveOptions, progress: (ArchiveProgress) -> Unit): List<File> {
        val out = mutableListOf<File>(); var total = 0L; var i = 0
        SevenZFile(file).use { archive ->
            while (true) {
                currentCoroutineContext().ensureActive()
                val e = archive.nextEntry ?: break
                val id = i++
                if (i > options.limits.maxEntries) throw ArchiveError.LimitExceeded("Too many archive entries")
                val name = e.name ?: "entry-$id"
                val rel = PathSanitizer.relativePath(name, options.limits.maxPathLength)
                if (selected != null && id !in selected) { drainCurrent(archive, options.bufferSize); continue }
                val target = PathSanitizer.resolveInside(root, rel, options.limits.maxPathLength)
                if (e.isDirectory) {
                    if (!target.exists() && !target.mkdirs()) throw ArchiveError.DestinationUnavailable(target.path)
                } else {
                    if (e.size > options.limits.maxEntryBytes) throw ArchiveError.LimitExceeded("Entry too large: $rel")
                    target.parentFile?.let { if (!it.exists() && !it.mkdirs()) throw ArchiveError.DestinationUnavailable(it.path) }
                    val actual = chooseTarget(target, options.overwritePolicy)
                    if (actual != null) {
                        var entryBytes = 0L
                        FileOutputStream(actual).buffered(options.bufferSize).use { sink ->
                            val buf = ByteArray(options.bufferSize)
                            while (true) {
                                currentCoroutineContext().ensureActive()
                                val n = archive.read(buf); if (n < 0) break
                                entryBytes += n; total += n
                                if (entryBytes > options.limits.maxEntryBytes || total > options.limits.maxTotalBytes) throw ArchiveError.LimitExceeded("Expanded data limit exceeded")
                                sink.write(buf, 0, n)
                            }
                        }
                        e.lastModifiedDate?.let { actual.setLastModified(it.time) }
                        out += actual
                    } else drainCurrent(archive, options.bufferSize)
                }
                progress(ArchiveProgress(ArchiveOperation.EXTRACT, name, i.toLong(), null, total, null, null))
            }
        }
        return out
    }

    private suspend fun listRar(file: File, options: ArchiveOptions, progress: (ArchiveProgress) -> Unit): List<ArchiveEntry> {
        val out = mutableListOf<ArchiveEntry>(); var total = 0L
        Archive(file).use { archive ->
            val headers = archive.fileHeaders
            if (headers.size > options.limits.maxEntries) throw ArchiveError.LimitExceeded("Too many archive entries")
            headers.forEachIndexed { i, h ->
                currentCoroutineContext().ensureActive()
                val name = h.fileName ?: "entry-$i"
                val path = PathSanitizer.relativePath(name, options.limits.maxPathLength)
                val size = h.fullUnpackSize.takeIf { it >= 0 }
                if (size != null && size > options.limits.maxEntryBytes) throw ArchiveError.LimitExceeded("Entry too large: $path")
                total += size ?: 0
                if (total > options.limits.maxTotalBytes) throw ArchiveError.LimitExceeded("Archive exceeds total expanded size limit")
                out += ArchiveEntry(i, name, path, h.isDirectory, size, h.fullPackSize.takeIf { it >= 0L }, h.mTime?.time, null, h.isEncrypted)
                progress(ArchiveProgress(ArchiveOperation.LIST, name, (i + 1).toLong(), headers.size.toLong(), total, null, null))
            }
        }
        return out
    }

    private suspend fun extractRar(file: File, root: File, selected: Set<Int>?, options: ArchiveOptions, progress: (ArchiveProgress) -> Unit): List<File> {
        val out = mutableListOf<File>(); var total = 0L
        Archive(file).use { archive ->
            val headers = archive.fileHeaders
            if (headers.size > options.limits.maxEntries) throw ArchiveError.LimitExceeded("Too many archive entries")
            headers.forEachIndexed { i, h ->
                currentCoroutineContext().ensureActive()
                val name = h.fileName ?: "entry-$i"
                val rel = PathSanitizer.relativePath(name, options.limits.maxPathLength)
                if (selected != null && i !in selected) return@forEachIndexed
                val target = PathSanitizer.resolveInside(root, rel, options.limits.maxPathLength)
                if (h.isDirectory) {
                    if (!target.exists() && !target.mkdirs()) throw ArchiveError.DestinationUnavailable(target.path)
                } else {
                    val size = h.fullUnpackSize
                    if (size > options.limits.maxEntryBytes) throw ArchiveError.LimitExceeded("Entry too large: $rel")
                    target.parentFile?.let { if (!it.exists() && !it.mkdirs()) throw ArchiveError.DestinationUnavailable(it.path) }
                    val actual = chooseTarget(target, options.overwritePolicy)
                    if (actual != null) {
                        FileOutputStream(actual).buffered(options.bufferSize).use { archive.extractFile(h, it) }
                        total += actual.length()
                        if (total > options.limits.maxTotalBytes) { actual.delete(); throw ArchiveError.LimitExceeded("Archive exceeds total expanded size limit") }
                        h.mTime?.let { actual.setLastModified(it.time) }
                        out += actual
                    }
                }
                progress(ArchiveProgress(ArchiveOperation.EXTRACT, name, (i + 1).toLong(), headers.size.toLong(), total, null, null))
            }
        }
        return out
    }

    suspend fun create7z(sources: List<File>, destination: File, options: ArchiveOptions, progress: (ArchiveProgress) -> Unit) {
        require(sources.isNotEmpty()) { "At least one source is required" }
        destination.parentFile?.let { if (!it.exists() && !it.mkdirs()) throw ArchiveError.DestinationUnavailable(it.path) }
        if (destination.exists()) when (options.overwritePolicy) {
            OverwritePolicy.OVERWRITE -> if (!destination.delete()) throw ArchiveError.DestinationUnavailable(destination.path)
            OverwritePolicy.FAIL, OverwritePolicy.SKIP, OverwritePolicy.RENAME -> throw ArchiveError.DestinationExists(destination.path)
        }
        val entries = collect(sources, options)
        var bytes = 0L
        SevenZOutputFile(destination).use { archive ->
            for ((file, name) in entries) {
                currentCoroutineContext().ensureActive()
                val entry: SevenZArchiveEntry = archive.createArchiveEntry(file, name.trimEnd('/') + if (file.isDirectory) "/" else "")
                archive.putArchiveEntry(entry)
                if (file.isFile) file.inputStream().buffered(options.bufferSize).use { input ->
                    val buf = ByteArray(options.bufferSize)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val n = input.read(buf); if (n < 0) break
                        archive.write(buf, 0, n); bytes += n
                    }
                }
                archive.closeArchiveEntry()
                progress(ArchiveProgress(ArchiveOperation.CREATE, name, entries.indexOfFirst { it.first == file && it.second == name }.toLong() + 1, entries.size.toLong(), bytes, entries.sumOf { if (it.first.isFile) it.first.length() else 0L }, null))
            }
            archive.finish()
        }
    }

    private fun collect(sources: List<File>, options: ArchiveOptions): List<Pair<File, String>> {
        val out = mutableListOf<Pair<File, String>>()
        sources.forEach { source ->
            require(source.exists()) { "Source does not exist: ${source.path}" }
            if (source.isDirectory) {
                if (options.includeRootDirectory) out += source to "${source.name}/"
                source.walkTopDown().forEach { f ->
                    if (f == source) return@forEach
                    if (!options.includeHiddenFiles && f.name.startsWith('.')) return@forEach
                    val rel = if (options.includeRootDirectory) f.relativeTo(source.parentFile ?: source).invariantSeparatorsPath else f.relativeTo(source).invariantSeparatorsPath
                    out += f to rel
                }
            } else out += source to source.name
        }
        return out
    }

    private suspend fun drainCurrent(archive: SevenZFile, size: Int) {
        val buf = ByteArray(size)
        while (true) { currentCoroutineContext().ensureActive(); if (archive.read(buf) < 0) break }
    }

    private fun chooseTarget(target: File, policy: OverwritePolicy): File? {
        if (!target.exists()) return target
        return when (policy) {
            OverwritePolicy.SKIP -> null
            OverwritePolicy.OVERWRITE -> { if (target.isDirectory && !target.deleteRecursively()) throw ArchiveError.DestinationUnavailable(target.path); target }
            OverwritePolicy.FAIL -> throw ArchiveError.DestinationExists(target.path)
            OverwritePolicy.RENAME -> { var i = 1; var f: File; do { f = File(target.parentFile, "${target.nameWithoutExtension} ($i)" + if (target.extension.isNotEmpty()) ".${target.extension}" else ""); i++ } while (f.exists()); f }
        }
    }
}
