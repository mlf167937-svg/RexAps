package com.rexaps.librex.rexsmartmanager

import java.util.UUID

typealias DownloadId = String

data class DownloadRequest(
    val url: String,
    val fileName: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val allowResume: Boolean = true
)

enum class DownloadStatus {
    QUEUED, DOWNLOADING, PAUSED, ACTION_REQUIRED, COMPLETED, FAILED, CANCELLED
}

data class DownloadProgress(
    val id: DownloadId,
    val url: String,
    val fileName: String,
    val status: DownloadStatus,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long? = null,
    val message: String? = null,
    val filePath: String? = null
) {
    val percent: Int?
        get() = totalBytes?.takeIf { it > 0L }
            ?.let { ((downloadedBytes * 100L) / it).toInt().coerceIn(0, 100) }
}

internal fun newDownloadId(): DownloadId = UUID.randomUUID().toString()
