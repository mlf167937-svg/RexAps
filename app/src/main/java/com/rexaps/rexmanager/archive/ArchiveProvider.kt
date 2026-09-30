package com.rexaps.rexmanager.archive

import com.rexaps.rexmanager.CollisionPolicy
import com.rexaps.rexmanager.utils.FileNameUtils
import com.rexaps.rexmanager.utils.isSymbolicLink
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class RexArchiveException(message: String, cause: Throwable? = null) : IOException(message, cause)

/**
 * A provider handles exactly one [ArchiveFormat]. Implementations are called from an I/O
 * dispatcher and must check for cancellation while copying data.
 * Progress callback: (processedUnits, totalUnits).
 */
interface ArchiveProvider {
    val format: ArchiveFormat

    suspend fun compress(
        sources: List<File>,
        destination: File,
        onProgress: (processed: Long, total: Long) -> Unit
    )

    /** Returns the number of files extracted. */
    suspend fun extract(
        archive: File,
        destinationDir: File,
        policy: CollisionPolicy,
        onProgress: (processed: Long, total: Long) -> Unit
    ): Int
}

internal const val ARCHIVE_BUFFER_SIZE = 64 * 1024

internal data class ArchiveInputEntry(
    val file: File,
    /** Relative entry name using '/', without trailing slash. */
    val entryName: String,
    val isDirectory: Boolean
)

internal data class ArchiveInput(val entries: List<ArchiveInputEntry>, val totalBytes: Long)

/** Flattens the sources into an ordered list of archive entries. */
internal suspend fun collectArchiveEntries(sources: List<File>, exclude: File?): ArchiveInput {
    val entries = ArrayList<ArchiveInputEntry>()
    var total = 0L
    val usedTopLevel = HashSet<String>()
    val excludePath = exclude?.canonicalPath

    suspend fun add(file: File, entryName: String) {
        currentCoroutineContext().ensureActive()
        if (!file.exists()) throw RexArchiveException("\"${file.name}\" no longer exists.")
        if (excludePath != null && file.canonicalPath == excludePath) return
        if (file.isDirectory) {
            entries += ArchiveInputEntry(file, entryName, true)
            if (file.isSymbolicLink()) return
            val children = file.listFiles()
                ?: throw RexArchiveException("Can't read folder \"${file.name}\".")
            for (child in children.sortedBy { it.name }) add(child, "$entryName/${child.name}")
        } else {
            entries += ArchiveInputEntry(file, entryName, false)
            total += file.length()
        }
    }

    for (source in sources) {
        var top = source.name
        var n = 1
        while (top in usedTopLevel) top = FileNameUtils.numbered(source.name, n++)
        usedTopLevel += top
        add(source, top)
    }
    return ArchiveInput(entries, total)
}

internal suspend fun InputStream.copyWithProgress(
    out: OutputStream,
    buffer: ByteArray,
    onBytes: (Int) -> Unit
) {
    while (true) {
        currentCoroutineContext().ensureActive()
        val read = read(buffer)
        if (read < 0) break
        out.write(buffer, 0, read)
        onBytes(read)
    }
}

/** Resolves an entry name inside [destinationDir] and blocks path traversal (zip-slip). */
internal fun resolveExtractTarget(destinationDir: File, entryName: String): File {
    val base = destinationDir.canonicalFile
    val target = File(base, entryName).canonicalFile
    if (target.path != base.path && !target.path.startsWith(base.path + File.separator)) {
        throw RexArchiveException("The archive contains an unsafe path and was not extracted.")
    }
    return target
}

/** Returns the file to write, or null when the entry must be skipped. */
internal fun resolveCollision(target: File, policy: CollisionPolicy): File? {
    if (!target.exists()) return target
    return when (policy) {
        CollisionPolicy.REPLACE -> {
            if (target.isDirectory) {
                throw RexArchiveException("Can't replace folder \"${target.name}\" with a file.")
            }
            target
        }
        CollisionPolicy.SKIP -> null
        CollisionPolicy.RENAME -> {
            var n = 1
            var candidate = File(target.parentFile, FileNameUtils.numbered(target.name, n))
            while (candidate.exists()) {
                n++
                candidate = File(target.parentFile, FileNameUtils.numbered(target.name, n))
            }
            candidate
        }
    }
}

internal fun ensureDirectory(dir: File) {
    if (dir.exists() && !dir.isDirectory) {
        throw RexArchiveException("\"${dir.name}\" already exists as a file.")
    }
    if (!dir.exists() && !dir.mkdirs()) {
        throw RexArchiveException("Couldn't create folder \"${dir.name}\".")
    }
}

internal class CountingInputStream(input: InputStream) : FilterInputStream(input) {
    var count: Long = 0L
        private set

    override fun read(): Int {
        val b = super.read()
        if (b >= 0) count++
        return b
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val n = super.read(b, off, len)
        if (n > 0) count += n
        return n
    }

    override fun skip(n: Long): Long {
        val skipped = super.skip(n)
        if (skipped > 0) count += skipped
        return skipped
    }
}
