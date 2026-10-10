package com.rexaps.librex.rexsmartmanager

import kotlinx.coroutines.flow.StateFlow

/** Shared download-engine contract. UI and browser integrations should depend on this interface. */
interface RexSmartManager {
    val downloads: StateFlow<List<DownloadProgress>>
    fun enqueue(request: DownloadRequest): DownloadId
    fun pause(id: DownloadId)
    fun resume(id: DownloadId)
    fun cancel(id: DownloadId)
    fun openActionRequired(id: DownloadId, message: String)
}
