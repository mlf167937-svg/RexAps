package com.rexaps.rexgit

import java.io.File
import kotlinx.coroutines.flow.update

internal fun RexGitViewModel.__splitImpl_downloadGithubAsset(url: String, rawName: String) {
        if (_state.value.selectedRepository == null) return
        val sanitizedName = rawName.substringAfterLast('/')
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .take(120)
        val name = sanitizedName.takeUnless { it.isBlank() || it == "." || it == ".." }
            ?: "github-download.zip"
        launchIO {
            val notificationKey = "artifact:$name:${System.currentTimeMillis()}"
            var lastNotificationPercent: Int? = null
            var lastNotificationAt = 0L
            RexGitOperationNotifications.progress(
                getApplication(), notificationKey, "GitHub download: $name", "Preparing download…", null
            )
            _state.update {
                it.copy(
                    downloadingArtifact = name,
                    downloadedBytes = 0L,
                    downloadTotalBytes = null,
                    downloadError = null
                )
            }
            try {
                val auth = try { externalGithubAuth() } catch (_: Throwable) { null }
                val downloadDir = RexGitStorage.artifactsDirectory()
                val requested = File(downloadDir, name)
                val output = if (!requested.exists()) requested else {
                    val base = requested.nameWithoutExtension
                    val extension = requested.extension.takeIf { it.isNotBlank() }?.let { ".$it" }.orEmpty()
                    var index = 2
                    var candidate = File(downloadDir, "$base-$index$extension")
                    while (candidate.exists()) { index++; candidate = File(downloadDir, "$base-$index$extension") }
                    candidate
                }
                val accept = if (url.contains("/releases/assets/")) "application/octet-stream" else "application/vnd.github+json"
                github.download(url, auth?.token, output, accept) { received, total ->
                    _state.update { cur ->
                        cur.copy(
                            downloadedBytes = received,
                            downloadTotalBytes = total
                        )
                    }
                    val percent = total?.takeIf { it > 0L }
                        ?.let { ((received * 100L) / it).toInt().coerceIn(0, 100) }
                    val now = System.currentTimeMillis()
                    if (percent != null && percent != lastNotificationPercent &&
                        (percent == 0 || percent == 100 || percent % 5 == 0)
                    ) {
                        lastNotificationPercent = percent
                        lastNotificationAt = now
                        RexGitOperationNotifications.progress(
                            getApplication(), notificationKey, "GitHub download: $name",
                            "${formatDownloadBytes(received)} / ${total?.let(::formatDownloadBytes) ?: "?"}", percent
                        )
                    } else if (now - lastNotificationAt >= 1_500L) {
                        lastNotificationAt = now
                        RexGitOperationNotifications.progress(
                            getApplication(), notificationKey, "GitHub download: $name",
                            formatDownloadBytes(received), percent
                        )
                    }
                }
                _state.update { it.copy(message = "Downloaded $name to ${output.parentFile?.absolutePath}") }
                RexGitOperationNotifications.completed(
                    getApplication(), notificationKey, "GitHub download complete: $name",
                    "Saved to Download/RexAps/Github/Artefak/"
                )
            } catch (t: Throwable) {
                val message = t.readableMessage("Download failed")
                _state.update { it.copy(downloadError = message) }
                RexGitOperationNotifications.failed(
                    getApplication(), notificationKey, "GitHub download failed: $name", message
                )
            } finally {
                _state.update {
                    it.copy(
                        downloadingArtifact = null,
                        downloadedBytes = 0L,
                        downloadTotalBytes = null
                    )
                }
            }
        }
    }

private fun formatDownloadBytes(bytes: Long): String = when {
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024L -> "${bytes / 1024L} KB"
    else -> String.format(java.util.Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
}
