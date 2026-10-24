package com.rexaps.rexmusic

import kotlinx.coroutines.flow.update

internal fun RexOfflineManager.__split_setStatus_17(key: String, status: DownloadStatus) {
        _state.update { it.copy(downloads = it.downloads + (key to status)) }
        if (::appContext.isInitialized) RexDownloadNotification.showProgress(appContext, key, status)
    }
