package com.rexaps.librex.rexarchive.internal

import com.rexaps.librex.rexarchive.*
import com.rexaps.librex.rexarchive.security.PathSanitizer
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.*
import java.util.zip.*

internal suspend fun ArchiveEngine.__split_extractInternal_5(input: InputStream, name: String, destination: File, selected: Set<Int>?, options: ArchiveOptions, progress: (ArchiveProgress) -> Unit): List<File> {
        val format = ArchiveFormat.fromFileName(name)
        return when (format) {
            ArchiveFormat.ZIP -> extractZip(input, destination, selected, options, progress)
            ArchiveFormat.TAR, ArchiveFormat.TAR_GZIP, ArchiveFormat.TAR_XZ, ArchiveFormat.TAR_BZIP2 -> extractTar(wrapTarInput(input, format), destination, selected, options, progress)
            else -> throw ArchiveError.BackendUnavailable(format)
        }
    }

internal suspend fun ArchiveEngine.__split_extractZip_6(input: InputStream, root: File, selected: Set<Int>?, options: ArchiveOptions, progress: (ArchiveProgress) -> Unit): List<File> {
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

internal suspend fun ArchiveEngine.__split_extractTar_7(input: InputStream, root: File, selected: Set<Int>?, options: ArchiveOptions, progress: (ArchiveProgress) -> Unit): List<File> {
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
