package com.rexaps.rexmanager.filesystem

import com.rexaps.rexmanager.archive.RexArchiveException
import java.io.FileNotFoundException
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

enum class FileErrorType {
    PERMISSION_DENIED,
    SOURCE_MISSING,
    DESTINATION_EXISTS,
    NOT_ENOUGH_STORAGE,
    INVALID_INPUT,
    IO_ERROR,
    ARCHIVE_ERROR,
    CANCELLED,
    UNKNOWN
}

data class FileOperationError(val type: FileErrorType, val detail: String? = null) {
    /** Human friendly message. Never contains stack traces. */
    val message: String
        get() = when (type) {
            FileErrorType.PERMISSION_DENIED ->
                "Permission denied. RexManager doesn't have access to this location."
            FileErrorType.SOURCE_MISSING -> "The file or folder no longer exists."
            FileErrorType.DESTINATION_EXISTS -> "An item with the same name already exists."
            FileErrorType.NOT_ENOUGH_STORAGE -> "Not enough storage space."
            FileErrorType.INVALID_INPUT -> detail ?: "Invalid input."
            FileErrorType.IO_ERROR -> "A read/write error occurred. Please try again."
            FileErrorType.ARCHIVE_ERROR -> detail ?: "The archive could not be processed."
            FileErrorType.CANCELLED -> "Operation cancelled."
            FileErrorType.UNKNOWN -> "Something went wrong."
        }
}

/** Internal exception used by backends to signal a typed error. */
class FileOperationException(val error: FileOperationError) : Exception(error.message)

sealed interface FileOperationResult<out T> {
    data class Success<T>(val value: T) : FileOperationResult<T>
    data class Failure(val error: FileOperationError) : FileOperationResult<Nothing>
}

fun <T> FileOperationResult<T>.getOrNull(): T? =
    (this as? FileOperationResult.Success)?.value

private fun String?.looksLikePermissionError(): Boolean {
    val m = this ?: return false
    return m.contains("EACCES") || m.contains("EPERM") ||
        m.contains("Permission denied", ignoreCase = true) ||
        m.contains("Operation not permitted", ignoreCase = true)
}

private fun String?.looksLikeNoSpace(): Boolean {
    val m = this ?: return false
    return m.contains("ENOSPC") || m.contains("No space left", ignoreCase = true)
}

fun Throwable.toFileError(): FileOperationError = when (this) {
    is FileOperationException -> error
    is CancellationException -> FileOperationError(FileErrorType.CANCELLED)
    is SecurityException -> FileOperationError(FileErrorType.PERMISSION_DENIED)
    is FileNotFoundException ->
        if (message.looksLikePermissionError()) FileOperationError(FileErrorType.PERMISSION_DENIED)
        else FileOperationError(FileErrorType.SOURCE_MISSING)
    is RexArchiveException -> FileOperationError(FileErrorType.ARCHIVE_ERROR, message)
    is IllegalArgumentException -> FileOperationError(FileErrorType.INVALID_INPUT, message)
    is IOException -> when {
        message.looksLikeNoSpace() -> FileOperationError(FileErrorType.NOT_ENOUGH_STORAGE)
        message.looksLikePermissionError() -> FileOperationError(FileErrorType.PERMISSION_DENIED)
        else -> FileOperationError(FileErrorType.IO_ERROR)
    }
    else -> FileOperationError(FileErrorType.UNKNOWN)
}
