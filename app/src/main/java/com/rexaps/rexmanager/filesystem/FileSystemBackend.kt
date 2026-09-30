package com.rexaps.rexmanager.filesystem

import com.rexaps.rexmanager.RexFile
import com.rexaps.rexmanager.SearchResult

/**
 * Abstraction over a filesystem. [NormalFileSystemBackend] is the non-root implementation.
 * A future RootFileSystemBackend (RexSUManager) implements the same interface so the UI,
 * ViewModel and Repository need no structural changes.
 *
 * All functions are main-safe: implementations switch to an I/O dispatcher internally and
 * must propagate coroutine cancellation (never swallow CancellationException).
 */
interface FileSystemBackend {
    suspend fun list(path: String): FileOperationResult<List<RexFile>>
    suspend fun exists(path: String): Boolean
    suspend fun createDirectory(path: String): FileOperationResult<Unit>
    suspend fun createFile(path: String): FileOperationResult<Unit>

    /** Returns the new full path. */
    suspend fun rename(path: String, newName: String): FileOperationResult<String>

    suspend fun copy(
        source: String,
        destination: String,
        overwrite: Boolean,
        onBytesCopied: (Long) -> Unit
    ): FileOperationResult<Unit>

    suspend fun move(
        source: String,
        destination: String,
        overwrite: Boolean,
        onBytesCopied: (Long) -> Unit
    ): FileOperationResult<Unit>

    suspend fun delete(path: String): FileOperationResult<Unit>
    suspend fun getMetadata(path: String): FileOperationResult<FileMetadata>
    suspend fun getStorageInfo(path: String): FileOperationResult<StorageInfo>

    /** Total size in bytes (recursive for folders). */
    suspend fun sizeOf(path: String): FileOperationResult<Long>

    /** Bounded recursive search by file name / extension. */
    suspend fun search(
        rootPath: String,
        query: String,
        includeHidden: Boolean,
        maxDepth: Int,
        maxResults: Int,
        maxVisited: Int
    ): FileOperationResult<SearchResult>
}
