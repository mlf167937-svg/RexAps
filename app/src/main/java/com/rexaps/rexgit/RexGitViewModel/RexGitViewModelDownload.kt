package com.rexaps.rexgit

import java.io.File
import kotlinx.coroutines.flow.update

internal fun RexGitViewModel.__splitImpl_downloadGithubAsset(url: String, rawName: String) {
        if (_state.value.selectedRepository == null) return
        val name = rawName.substringAfterLast('/').replace(Regex("[^A-Za-z0-9._-]"), "_").take(120).ifBlank { "github-download.zip" }
        launchIO {
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
                }
                _state.update { it.copy(message = "Downloaded $name to ${output.parentFile?.absolutePath}") }
            } catch (t: Throwable) {
                _state.update { it.copy(downloadError = t.readableMessage("Download failed")) }
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
