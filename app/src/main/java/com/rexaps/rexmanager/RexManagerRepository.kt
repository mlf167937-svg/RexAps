package com.rexaps.rexmanager

import android.content.Context
import android.os.Environment
import com.rexaps.rexmanager.archive.ArchiveFormat
import com.rexaps.rexmanager.archive.ArchiveManager
import com.rexaps.rexmanager.filesystem.FileErrorType
import com.rexaps.rexmanager.filesystem.FileMetadata
import com.rexaps.rexmanager.filesystem.FileOperationError
import com.rexaps.rexmanager.filesystem.FileOperationResult
import com.rexaps.rexmanager.filesystem.FileSystemBackend
import com.rexaps.rexmanager.filesystem.NormalFileSystemBackend
import com.rexaps.rexmanager.filesystem.StorageInfo
import com.rexaps.rexmanager.filesystem.getOrNull
import com.rexaps.rexmanager.filesystem.toFileError
import com.rexaps.rexmanager.utils.FileNameUtils
import com.rexaps.rexmanager.utils.ProgressTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

/**
 * Single access point for file operations. Talks only to [FileSystemBackend] (and
 * [ArchiveManager]); swapping in a RootFileSystemBackend later requires no UI change.
 */
class RexManagerRepository(
    private val context: Context,
    private val backend: FileSystemBackend = NormalFileSystemBackend(),
    private val archiveManager: ArchiveManager = ArchiveManager()
) {

    // ------------------------------------------------------------------ roots

    @Suppress("DEPRECATION")
    fun storageRoots(): List<StorageRoot> {
        val roots = mutableListOf<StorageRoot>()
        roots += StorageRoot("Internal Storage", Environment.getExternalStorageDirectory().absolutePath)
        context.getExternalFilesDir(null)?.let {
            roots += StorageRoot("App Files (external)", it.absolutePath)
        }
        roots += StorageRoot("App Files (private)", context.filesDir.absolutePath)
        return roots
    }

    // ------------------------------------------------------------------ queries

    suspend fun list(path: String) = backend.list(path)

    suspend fun search(root: String, query: String, includeHidden: Boolean) =
        backend.search(
            rootPath = root,
            query = query,
            includeHidden = includeHidden,
            maxDepth = 8,
            maxResults = 500,
            maxVisited = 50_000
        )

    suspend fun storageInfo(path: String): FileOperationResult<StorageInfo> = backend.getStorageInfo(path)

    suspend fun metadata(path: String): FileOperationResult<FileMetadata> = backend.getMetadata(path)

    suspend fun totalSize(path: String): Long? = backend.sizeOf(path).getOrNull()

    // ------------------------------------------------------------------ simple mutations

    suspend fun create(dir: String, name: String, isFolder: Boolean): FileOperationResult<Unit> {
        FileNameUtils.validate(name, emptySet())?.let {
            return FileOperationResult.Failure(FileOperationError(FileErrorType.INVALID_INPUT, it))
        }
        val path = FileNameUtils.join(dir, name)
        return if (isFolder) backend.createDirectory(path) else backend.createFile(path)
    }

    suspend fun rename(path: String, newName: String): FileOperationResult<String> =
        backend.rename(path, newName)

    suspend fun delete(paths: List<String>, report: ProgressReporter): BatchResult {
        var succeeded = 0
        val failures = mutableListOf<ItemFailure>()
        paths.forEachIndexed { index, path ->
            currentCoroutineContext().ensureActive()
            val name = File(path).name
            report(name, index.toFloat() / paths.size)
            when (val r = backend.delete(path)) {
                is FileOperationResult.Success -> succeeded++
                is FileOperationResult.Failure -> failures += ItemFailure(name, r.error)
            }
        }
        report("", 1f)
        return BatchResult(succeeded, 0, failures)
    }

    // ------------------------------------------------------------------ copy / move

    /** Names in [destDir] that would collide with [paths]. */
    suspend fun findConflicts(paths: List<String>, destDir: String, move: Boolean): List<String> =
        paths.mapNotNull { path ->
            val f = File(path)
            when {
                move && f.parent == destDir -> null
                backend.exists(FileNameUtils.join(destDir, f.name)) -> f.name
                else -> null
            }
        }

    suspend fun transfer(
        paths: List<String>,
        destDir: String,
        move: Boolean,
        policy: CollisionPolicy,
        report: ProgressReporter
    ): BatchResult {
        var currentName = ""
        val total = paths.sumOf { backend.sizeOf(it).getOrNull() ?: 0L }
        val tracker = ProgressTracker(total) { fraction -> report(currentName, fraction) }
        var succeeded = 0
        var skipped = 0
        val failures = mutableListOf<ItemFailure>()

        paths.forEachIndexed { index, source ->
            currentCoroutineContext().ensureActive()
            val name = File(source).name
            currentName = name
            report(name, null)

            if (move && File(source).parent == destDir) {
                skipped++
                return@forEachIndexed
            }

            var target = FileNameUtils.join(destDir, name)
            var overwrite = false
            if (backend.exists(target)) {
                when (policy) {
                    CollisionPolicy.REPLACE -> overwrite = true
                    CollisionPolicy.SKIP -> {
                        skipped++
                        return@forEachIndexed
                    }
                    CollisionPolicy.RENAME ->
                        target = FileNameUtils.join(destDir, uniqueName(destDir, name))
                }
            }

            val result = if (move) {
                backend.move(source, target, overwrite, tracker::add)
            } else {
                backend.copy(source, target, overwrite, tracker::add)
            }
            when (result) {
                is FileOperationResult.Success -> succeeded++
                is FileOperationResult.Failure -> failures += ItemFailure(name, result.error)
            }
            if (total <= 0L) report(name, (index + 1f) / paths.size)
        }
        report(currentName, 1f)
        return BatchResult(succeeded, skipped, failures)
    }

    // ------------------------------------------------------------------ hide / unhide

    private fun hiddenTargetName(name: String, hide: Boolean): String? =
        if (hide) {
            if (name.startsWith(".")) null else ".$name"
        } else {
            if (name.startsWith(".")) name.removePrefix(".").takeIf { it.isNotEmpty() } else null
        }

    suspend fun countHideConflicts(paths: List<String>, hide: Boolean): Int =
        paths.count { path ->
            val f = File(path)
            val target = hiddenTargetName(f.name, hide)
            target != null && f.parent != null && backend.exists(FileNameUtils.join(f.parent!!, target))
        }

    /** Hide = rename to ".name". Unhide = remove the leading dot. Never overwrites. */
    suspend fun setHidden(paths: List<String>, hide: Boolean, report: ProgressReporter): BatchResult {
        var succeeded = 0
        var skipped = 0
        val failures = mutableListOf<ItemFailure>()
        paths.forEachIndexed { index, path ->
            currentCoroutineContext().ensureActive()
            val f = File(path)
            val target = hiddenTargetName(f.name, hide)
            val parent = f.parent
            if (target == null || parent == null) {
                skipped++
                return@forEachIndexed
            }
            report(f.name, index.toFloat() / paths.size)
            var finalName = target
            if (backend.exists(FileNameUtils.join(parent, finalName))) {
                finalName = uniqueName(parent, target)
            }
            when (val r = backend.rename(path, finalName)) {
                is FileOperationResult.Success -> succeeded++
                is FileOperationResult.Failure -> failures += ItemFailure(f.name, r.error)
            }
        }
        report("", 1f)
        return BatchResult(succeeded, skipped, failures)
    }

    // ------------------------------------------------------------------ archives

    suspend fun compress(
        paths: List<String>,
        destDir: String,
        baseName: String,
        format: ArchiveFormat,
        report: ProgressReporter
    ): FileOperationResult<String> {
        FileNameUtils.validate(baseName, emptySet())?.let {
            return FileOperationResult.Failure(FileOperationError(FileErrorType.INVALID_INPUT, it))
        }
        val destination = File(destDir, "$baseName.${format.extension}")
        if (backend.exists(destination.path)) {
            return FileOperationResult.Failure(FileOperationError(FileErrorType.DESTINATION_EXISTS))
        }
        val tracker = ProgressTracker { fraction -> report(destination.name, fraction) }
        report(destination.name, null)
        return try {
            archiveManager.compress(format, paths.map { File(it) }, destination) { done, total ->
                tracker.update(done, total)
            }
            FileOperationResult.Success(destination.path)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FileOperationResult.Failure(e.asArchiveError())
        }
    }

    suspend fun extract(
        archivePath: String,
        destPath: String,
        policy: CollisionPolicy,
        report: ProgressReporter
    ): FileOperationResult<Int> {
        val archive = File(archivePath)
        val dest = File(destPath)
        FileNameUtils.validate(dest.name, emptySet())?.let {
            return FileOperationResult.Failure(FileOperationError(FileErrorType.INVALID_INPUT, it))
        }
        if (archiveManager.formatFor(archive.name) == null) {
            return FileOperationResult.Failure(
                FileOperationError(FileErrorType.ARCHIVE_ERROR, "Unsupported archive type.")
            )
        }
        val createdByUs = !backend.exists(dest.path)
        val tracker = ProgressTracker { fraction -> report(archive.name, fraction) }
        report(archive.name, null)
        return try {
            val count = archiveManager.extract(archive, dest, policy) { done, total ->
                tracker.update(done, total)
            }
            FileOperationResult.Success(count)
        } catch (e: CancellationException) {
            if (createdByUs) withContext(NonCancellable + Dispatchers.IO) { dest.deleteRecursively() }
            throw e
        } catch (e: Exception) {
            if (createdByUs) withContext(NonCancellable + Dispatchers.IO) { dest.deleteRecursively() }
            FileOperationResult.Failure(e.asArchiveError())
        }
    }

    // ------------------------------------------------------------------ helpers

    private fun Throwable.asArchiveError(): FileOperationError {
        val error = toFileError()
        return if (error.type == FileErrorType.IO_ERROR || error.type == FileErrorType.UNKNOWN) {
            FileOperationError(
                FileErrorType.ARCHIVE_ERROR,
                "The archive is damaged, encrypted or not supported."
            )
        } else {
            error
        }
    }

    private suspend fun uniqueName(dir: String, name: String): String {
        var n = 1
        var candidate = FileNameUtils.numbered(name, n)
        while (backend.exists(FileNameUtils.join(dir, candidate))) {
            n++
            candidate = FileNameUtils.numbered(name, n)
        }
        return candidate
    }
}
