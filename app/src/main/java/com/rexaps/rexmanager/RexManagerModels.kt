package com.rexaps.rexmanager

import com.rexaps.rexmanager.filesystem.FileOperationError
import java.io.File

data class RexFile(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val isHidden: Boolean,
    val size: Long,
    val lastModified: Long,
    val mimeType: String?,
    val extension: String?
) {
    val parentPath: String? get() = File(path).parent
}

enum class ViewMode { LIST, GRID }

enum class SortMode { NAME, SIZE, MODIFIED, TYPE }

enum class SortOrder { ASCENDING, DESCENDING }

/** How to resolve a name that already exists at the destination. */
enum class CollisionPolicy { REPLACE, SKIP, RENAME }

data class StorageRoot(val name: String, val path: String)

data class Clipboard(val paths: List<String>, val isMove: Boolean)

data class PendingTransfer(val paths: List<String>, val destination: String, val isMove: Boolean)

/** [fraction] == null means indeterminate. */
data class OperationProgress(
    val title: String,
    val currentName: String = "",
    val fraction: Float? = null
)

data class ItemFailure(val name: String, val error: FileOperationError)

data class BatchResult(
    val succeeded: Int,
    val skipped: Int,
    val failures: List<ItemFailure>
)

data class SearchResult(val files: List<RexFile>, val truncated: Boolean)

typealias ProgressReporter = (label: String, fraction: Float?) -> Unit

fun BatchResult.toMessage(verb: String): String {
    val sb = StringBuilder()
    if (succeeded > 0) {
        sb.append("$verb $succeeded item").append(if (succeeded == 1) "" else "s")
    }
    if (skipped > 0) {
        if (sb.isNotEmpty()) sb.append(". ")
        sb.append("$skipped skipped")
    }
    if (failures.isNotEmpty()) {
        if (sb.isNotEmpty()) sb.append(". ")
        val first = failures.first()
        sb.append("${failures.size} failed (${first.name}: ${first.error.message})")
    }
    if (sb.isEmpty()) sb.append("Nothing to do")
    return sb.toString()
}
