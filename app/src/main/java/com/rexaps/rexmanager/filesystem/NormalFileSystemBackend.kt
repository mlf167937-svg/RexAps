package com.rexaps.rexmanager.filesystem

import android.os.StatFs
import com.rexaps.rexmanager.RexFile
import com.rexaps.rexmanager.SearchResult
import com.rexaps.rexmanager.utils.FileNameUtils
import com.rexaps.rexmanager.utils.MimeTypeUtils
import com.rexaps.rexmanager.utils.isSymbolicLink
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.coroutines.cancellation.CancellationException

/** Non-root backend built on java.io.File. No su / root / Magisk. */
class NormalFileSystemBackend(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : FileSystemBackend {

    private companion object {
        const val BUFFER_SIZE = 64 * 1024
    }

    private suspend fun <T> io(block: suspend () -> T): FileOperationResult<T> =
        withContext(ioDispatcher) {
            try {
                FileOperationResult.Success(block())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                FileOperationResult.Failure(e.toFileError())
            }
        }

    private fun fail(type: FileErrorType, detail: String? = null) =
        FileOperationException(FileOperationError(type, detail))

    // ---------------------------------------------------------------- list / metadata

    override suspend fun list(path: String): FileOperationResult<List<RexFile>> = io {
        val dir = File(path)
        if (!dir.exists()) throw fail(FileErrorType.SOURCE_MISSING)
        if (!dir.isDirectory) throw fail(FileErrorType.INVALID_INPUT, "This location is not a folder.")
        val children = dir.listFiles() ?: throw fail(FileErrorType.PERMISSION_DENIED)
        children.map { it.toRexFile() }
    }

    override suspend fun exists(path: String): Boolean = withContext(ioDispatcher) {
        try {
            File(path).exists()
        } catch (e: SecurityException) {
            false
        }
    }

    override suspend fun getMetadata(path: String): FileOperationResult<FileMetadata> = io {
        val f = File(path)
        if (!f.exists()) throw fail(FileErrorType.SOURCE_MISSING)
        val isDir = f.isDirectory
        val ext = if (isDir) null else MimeTypeUtils.extensionOf(f.name)
        FileMetadata(
            path = f.path,
            name = f.name,
            isDirectory = isDir,
            isHidden = f.name.startsWith("."),
            size = if (isDir) 0L else f.length(),
            lastModified = f.lastModified(),
            mimeType = if (isDir) null else MimeTypeUtils.fromExtension(ext),
            extension = ext,
            canRead = f.canRead(),
            canWrite = f.canWrite(),
            canExecute = f.canExecute(),
            itemCount = if (isDir) f.list()?.size else null,
            isSymlink = f.isSymbolicLink()
        )
    }

    override suspend fun getStorageInfo(path: String): FileOperationResult<StorageInfo> = io {
        val stat = StatFs(path)
        StorageInfo(
            total = stat.blockCountLong * stat.blockSizeLong,
            free = stat.availableBlocksLong * stat.blockSizeLong
        )
    }

    override suspend fun sizeOf(path: String): FileOperationResult<Long> = io {
        sizeOfInternal(File(path))
    }

    // ---------------------------------------------------------------- create / rename

    override suspend fun createDirectory(path: String): FileOperationResult<Unit> = io {
        val f = File(path)
        FileNameUtils.validate(f.name, emptySet())?.let { throw fail(FileErrorType.INVALID_INPUT, it) }
        if (f.exists()) throw fail(FileErrorType.DESTINATION_EXISTS)
        if (f.parentFile?.canWrite() == false) throw fail(FileErrorType.PERMISSION_DENIED)
        if (!f.mkdirs()) throw fail(FileErrorType.IO_ERROR)
    }

    override suspend fun createFile(path: String): FileOperationResult<Unit> = io {
        val f = File(path)
        FileNameUtils.validate(f.name, emptySet())?.let { throw fail(FileErrorType.INVALID_INPUT, it) }
        if (f.exists()) throw fail(FileErrorType.DESTINATION_EXISTS)
        val parent = f.parentFile ?: throw fail(FileErrorType.INVALID_INPUT)
        if (!parent.exists()) throw fail(FileErrorType.SOURCE_MISSING)
        if (!parent.canWrite()) throw fail(FileErrorType.PERMISSION_DENIED)
        if (!f.createNewFile()) throw fail(FileErrorType.DESTINATION_EXISTS)
    }

    override suspend fun rename(path: String, newName: String): FileOperationResult<String> = io {
        val src = File(path)
        if (!src.exists() && !src.isSymbolicLink()) throw fail(FileErrorType.SOURCE_MISSING)
        FileNameUtils.validate(newName, emptySet())?.let { throw fail(FileErrorType.INVALID_INPUT, it) }
        val parent = src.parentFile ?: throw fail(FileErrorType.INVALID_INPUT, "The root folder can't be renamed.")
        val dst = File(parent, newName)
        // Shared storage is usually case-insensitive: allow case-only renames.
        val sameIgnoringCase = newName.equals(src.name, ignoreCase = true)
        if (dst.exists() && !sameIgnoringCase) throw fail(FileErrorType.DESTINATION_EXISTS)
        if (newName == src.name) return@io path
        if (!src.renameTo(dst)) {
            throw fail(if (parent.canWrite()) FileErrorType.IO_ERROR else FileErrorType.PERMISSION_DENIED)
        }
        dst.path
    }

    // ---------------------------------------------------------------- copy / move / delete

    override suspend fun copy(
        source: String,
        destination: String,
        overwrite: Boolean,
        onBytesCopied: (Long) -> Unit
    ): FileOperationResult<Unit> = io {
        copyInternal(File(source), File(destination), overwrite, onBytesCopied)
    }

    override suspend fun move(
        source: String,
        destination: String,
        overwrite: Boolean,
        onBytesCopied: (Long) -> Unit
    ): FileOperationResult<Unit> = io {
        val src = File(source)
        val dst = File(destination)
        if (!src.exists() && !src.isSymbolicLink()) throw fail(FileErrorType.SOURCE_MISSING)
        guardSelfCopy(src, dst)
        if (dst.exists() && !overwrite) throw fail(FileErrorType.DESTINATION_EXISTS)

        if (!dst.exists()) {
            val size = sizeOfInternal(src)
            if (src.renameTo(dst)) {
                onBytesCopied(size)
                return@io
            }
        }
        // Different volume, or replacing: copy first, delete source only after success.
        copyInternal(src, dst, overwrite, onBytesCopied)
        deleteInternal(src)
    }

    override suspend fun delete(path: String): FileOperationResult<Unit> = io {
        val f = File(path)
        if (!f.exists() && !f.isSymbolicLink()) throw fail(FileErrorType.SOURCE_MISSING)
        deleteInternal(f)
    }

    // ---------------------------------------------------------------- search

    override suspend fun search(
        rootPath: String,
        query: String,
        includeHidden: Boolean,
        maxDepth: Int,
        maxResults: Int,
        maxVisited: Int
    ): FileOperationResult<SearchResult> = io {
        val root = File(rootPath)
        if (!root.isDirectory) throw fail(FileErrorType.SOURCE_MISSING)
        val q = query.trim().lowercase()
        val extQuery = q.removePrefix(".")
        val results = ArrayList<RexFile>()
        var visited = 0
        var truncated = false

        suspend fun walk(dir: File, depth: Int) {
            currentCoroutineContext().ensureActive()
            val children = dir.listFiles() ?: return
            for (child in children) {
                if (truncated) return
                visited++
                if (visited > maxVisited) {
                    truncated = true
                    return
                }
                val hidden = child.name.startsWith(".")
                if (hidden && !includeHidden) continue
                val isDir = child.isDirectory
                val ext = if (isDir) null else MimeTypeUtils.extensionOf(child.name)
                if (child.name.lowercase().contains(q) || (ext != null && ext == extQuery)) {
                    results += child.toRexFile()
                    if (results.size >= maxResults) {
                        truncated = true
                        return
                    }
                }
                if (isDir && depth < maxDepth && !child.isSymbolicLink()) {
                    walk(child, depth + 1)
                }
            }
        }

        walk(root, 0)
        SearchResult(results, truncated)
    }

    // ---------------------------------------------------------------- internals

    private fun File.toRexFile(): RexFile {
        val isDir = isDirectory
        val ext = if (isDir) null else MimeTypeUtils.extensionOf(name)
        return RexFile(
            path = path,
            name = name,
            isDirectory = isDir,
            isHidden = name.startsWith("."),
            size = if (isDir) 0L else length(),
            lastModified = lastModified(),
            mimeType = if (isDir) null else MimeTypeUtils.fromExtension(ext),
            extension = ext
        )
    }

    private fun guardSelfCopy(src: File, dst: File) {
        val s = src.canonicalFile
        val d = dst.canonicalFile
        if (s == d) throw fail(FileErrorType.INVALID_INPUT, "Source and destination are the same.")
        if (src.isDirectory && d.path.startsWith(s.path + File.separator)) {
            throw fail(FileErrorType.INVALID_INPUT, "A folder can't be copied or moved into itself.")
        }
    }

    private suspend fun sizeOfInternal(f: File): Long {
        currentCoroutineContext().ensureActive()
        if (!f.isDirectory) return f.length()
        if (f.isSymbolicLink()) return 0L
        var total = 0L
        val children = f.listFiles() ?: return 0L
        for (c in children) total += sizeOfInternal(c)
        return total
    }

    private suspend fun copyInternal(
        src: File,
        dst: File,
        overwrite: Boolean,
        onBytes: (Long) -> Unit
    ) {
        if (!src.exists()) throw fail(FileErrorType.SOURCE_MISSING)
        guardSelfCopy(src, dst)
        val parent = dst.parentFile ?: throw fail(FileErrorType.INVALID_INPUT)
        if (!parent.exists()) throw fail(FileErrorType.SOURCE_MISSING, "The destination folder doesn't exist.")
        if (!parent.canWrite()) throw fail(FileErrorType.PERMISSION_DENIED)

        val replacing = dst.exists()
        if (replacing && !overwrite) throw fail(FileErrorType.DESTINATION_EXISTS)

        val needed = sizeOfInternal(src)
        if (parent.usableSpace < needed) throw fail(FileErrorType.NOT_ENOUGH_STORAGE)

        // When replacing, write to a temp sibling first so a failure never destroys the original.
        val target = if (replacing) File(parent, ".${dst.name}.rexpart-${System.nanoTime()}") else dst
        var success = false
        try {
            copyTree(src, target, onBytes)
            if (replacing) {
                deleteInternal(dst)
                if (!target.renameTo(dst)) throw fail(FileErrorType.IO_ERROR)
            }
            success = true
        } finally {
            if (!success) target.deleteRecursively()
        }
    }

    private suspend fun copyTree(src: File, dst: File, onBytes: (Long) -> Unit) {
        currentCoroutineContext().ensureActive()
        if (src.isDirectory) {
            if (src.isSymbolicLink()) return // never follow directory links
            if (!dst.exists() && !dst.mkdirs()) throw fail(FileErrorType.IO_ERROR)
            val children = src.listFiles() ?: throw fail(FileErrorType.PERMISSION_DENIED)
            for (child in children) copyTree(child, File(dst, child.name), onBytes)
            dst.setLastModified(src.lastModified())
        } else {
            copyFile(src, dst, onBytes)
        }
    }

    private suspend fun copyFile(src: File, dst: File, onBytes: (Long) -> Unit) {
        val buffer = ByteArray(BUFFER_SIZE)
        FileInputStream(src).use { input ->
            FileOutputStream(dst).use { output ->
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    onBytes(read.toLong())
                }
            }
        }
        dst.setLastModified(src.lastModified())
    }

    private suspend fun deleteInternal(f: File) {
        currentCoroutineContext().ensureActive()
        if (f.isDirectory && !f.isSymbolicLink()) {
            val children = f.listFiles() ?: throw fail(FileErrorType.PERMISSION_DENIED)
            for (c in children) deleteInternal(c)
        }
        if (!f.delete() && f.exists()) {
            throw if (f.parentFile?.canWrite() == false) fail(FileErrorType.PERMISSION_DENIED)
            else fail(FileErrorType.IO_ERROR)
        }
    }
}
