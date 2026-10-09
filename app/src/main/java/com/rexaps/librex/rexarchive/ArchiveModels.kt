package com.rexaps.librex.rexarchive

import android.net.Uri
import java.io.File

/** Formats implemented by this version. Unsupported formats are still detectable where signatures are known. */
enum class ArchiveFormat(val extensions: Set<String>, val canCreate: Boolean, val canExtract: Boolean) {
    ZIP(setOf("zip"), true, true),
    TAR(setOf("tar"), true, true),
    TAR_GZIP(setOf("tar.gz", "tgz"), true, true),
    TAR_XZ(setOf("tar.xz", "txz"), true, true),
    TAR_BZIP2(setOf("tar.bz2", "tbz2", "tbz"), true, true),
    GZIP(setOf("gz"), true, true),
    BZIP2(setOf("bz2"), true, true),
    XZ(setOf("xz"), true, true),
    SEVEN_ZIP(setOf("7z"), true, true),
    RAR(setOf("rar"), false, true),
    PDF(setOf("pdf"), false, false),
    UNKNOWN(emptySet(), false, false);

    companion object {
        fun fromFileName(name: String): ArchiveFormat {
            val n = name.lowercase()
            return entries.firstOrNull { f -> f.extensions.any { n.endsWith(".$it") } } ?: UNKNOWN
        }
    }
}

data class ArchiveCapabilities(
    val canDetect: Boolean = true,
    val canList: Boolean,
    val canExtract: Boolean,
    val canExtractSelected: Boolean,
    val canCreate: Boolean,
    val canUpdate: Boolean = false,
    val supportsEncryption: Boolean = false,
    val supportsStreaming: Boolean = true,
    val supportsMultiVolume: Boolean = false
)

data class ArchiveEntry(
    /** Stable ID for this listing session; names are not guaranteed unique in hostile archives. */
    val id: Int,
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long? = null,
    val compressedSize: Long? = null,
    val lastModifiedMillis: Long? = null,
    val crc: Long? = null,
    val isEncrypted: Boolean? = null,
    val isSymbolicLink: Boolean = false
)

enum class ArchiveOperation { DETECT, LIST, CREATE, EXTRACT }
enum class OverwritePolicy { SKIP, OVERWRITE, RENAME, FAIL }
enum class SymlinkPolicy { REJECT, SKIP }

data class ArchiveProgress(
    val operation: ArchiveOperation,
    val currentEntry: String? = null,
    val entriesCompleted: Long = 0,
    val totalEntries: Long? = null,
    val bytesProcessed: Long = 0,
    val totalBytes: Long? = null,
    val fraction: Float? = null
)

data class ArchiveLimits(
    val maxEntries: Long = 100_000,
    val maxEntryBytes: Long = 2L * 1024 * 1024 * 1024,
    val maxTotalBytes: Long = 8L * 1024 * 1024 * 1024,
    val maxPathLength: Int = 4_096,
    val maxCompressionRatio: Double = 10_000.0
) {
    init {
        require(maxEntries > 0 && maxEntryBytes > 0 && maxTotalBytes > 0)
        require(maxPathLength > 0 && maxCompressionRatio >= 1.0)
    }
}

data class ArchiveOptions(
    val overwritePolicy: OverwritePolicy = OverwritePolicy.SKIP,
    val symlinkPolicy: SymlinkPolicy = SymlinkPolicy.REJECT,
    val limits: ArchiveLimits = ArchiveLimits(),
    val includeRootDirectory: Boolean = true,
    val includeHiddenFiles: Boolean = true,
    val compressionLevel: Int = 6,
    val bufferSize: Int = 64 * 1024,
    val verifyCrc: Boolean = true
) {
    init { require(compressionLevel in 0..9); require(bufferSize in 4 * 1024..1024 * 1024) }
}

sealed class ArchiveError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class UnsupportedFormat(format: ArchiveFormat) : ArchiveError("Unsupported archive format: $format")
    class InvalidArchive(message: String, cause: Throwable? = null) : ArchiveError(message, cause)
    class UnsafePath(path: String) : ArchiveError("Unsafe archive entry path: $path")
    class LimitExceeded(message: String) : ArchiveError(message)
    class EntryNotFound(name: String) : ArchiveError("Archive entry not found: $name")
    class DestinationExists(path: String) : ArchiveError("Destination already exists: $path")
    class DestinationUnavailable(path: String, cause: Throwable? = null) : ArchiveError("Destination unavailable: $path", cause)
    class BackendUnavailable(format: ArchiveFormat) : ArchiveError("No compatible backend available for $format")
    class Io(message: String, cause: Throwable? = null) : ArchiveError(message, cause)
}

data class ArchiveSource(val file: File? = null, val uri: Uri? = null, val displayName: String? = null) {
    init { require((file != null) xor (uri != null)) { "Provide exactly one of file or uri" } }
    val name: String get() = displayName ?: file?.name ?: uri?.lastPathSegment ?: "archive"
    companion object { fun file(file: File) = ArchiveSource(file = file); fun uri(uri: Uri, displayName: String? = null) = ArchiveSource(uri = uri, displayName = displayName) }
}

data class ArchiveDestination(val file: File) 
