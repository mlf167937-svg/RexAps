package com.rexaps.rexmanager.filesystem

data class FileMetadata(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val isHidden: Boolean,
    val size: Long,
    val lastModified: Long,
    val mimeType: String?,
    val extension: String?,
    val canRead: Boolean,
    val canWrite: Boolean,
    val canExecute: Boolean,
    val itemCount: Int?,
    val isSymlink: Boolean
) {
    val parentPath: String get() = path.substringBeforeLast('/', "")
}

data class StorageInfo(val total: Long, val free: Long) {
    val used: Long get() = (total - free).coerceAtLeast(0L)
    val usedFraction: Float
        get() = if (total <= 0L) 0f else (used.toFloat() / total.toFloat()).coerceIn(0f, 1f)
}
