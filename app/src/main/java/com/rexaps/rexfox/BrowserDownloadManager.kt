package com.rexaps.rexfox

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.URLUtil

class BrowserDownloadManager(private val context: Context) {
    private val manager = context.getSystemService(DownloadManager::class.java)

    fun enqueue(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?
    ): Long {
        val filename = URLUtil.guessFileName(url, contentDisposition, mimeType)
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle(filename)
            setDescription("Downloading with RexFox")
            setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            )
            setAllowedOverMetered(true)
            setAllowedOverRoaming(false)
            if (!userAgent.isNullOrBlank()) addRequestHeader("User-Agent", userAgent)
            if (!mimeType.isNullOrBlank()) setMimeType(mimeType)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
        }
        return manager.enqueue(request)
    }
}
