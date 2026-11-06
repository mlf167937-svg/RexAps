package com.rexaps.rexgit

import android.provider.OpenableColumns
import androidx.lifecycle.viewModelScope
import android.net.Uri
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal fun RexGitViewModel.__splitImpl_publishRelease(
    tagName: String,
    releaseName: String,
    body: String,
    draft: Boolean,
    prerelease: Boolean,
    assetUri: Uri?
) {
    val tag = tagName.trim()
    if (tag.isBlank()) {
        _state.update { it.copy(releasePublishError = "Tag wajib diisi.", releasePublishMessage = null) }
        return
    }
    if (tag.length > 120 || tag.any { it.isWhitespace() }) {
        _state.update { it.copy(releasePublishError = "Tag maksimal 120 karakter dan tidak boleh mengandung spasi.", releasePublishMessage = null) }
        return
    }
    if (_state.value.isPublishingRelease) return

    _state.update {
        it.copy(
            isPublishingRelease = true,
            releasePublishError = null,
            releasePublishMessage = null
        )
    }
    viewModelScope.launch(Dispatchers.IO) {
        var tempAsset: File? = null
        try {
            val repository = _state.value.selectedRepository
                ?: throw IllegalStateException("Pilih repository terlebih dahulu.")
            val slug = repositorySlug(repository)
                ?: throw IllegalStateException("Remote repository harus mengarah ke GitHub (owner/repository).")
            val auth = externalGithubAuth(false) ?: throw missingAuthError()
            val branch = _state.value.repositoryMetadata?.defaultBranch
                ?.takeIf { it.isNotBlank() } ?: repository.branch.ifBlank { "main" }
            val created = github.createRelease(
                token = auth.token,
                owner = slug.first,
                repository = slug.second,
                tagName = tag,
                releaseName = releaseName.trim().ifBlank { tag },
                body = body,
                draft = draft,
                prerelease = prerelease,
                targetCommitish = branch
            )

            var uploadWarning: String? = null
            if (assetUri != null) {
                try {
                    val resolver = getApplication<android.app.Application>().contentResolver
                    val displayName = resolver.query(assetUri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                        ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
                        ?.takeIf { it.isNotBlank() }
                        ?: assetUri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
                        ?: "release-asset.bin"
                    val safeName = displayName.replace(Regex("[\\/\\\\]"), "_").take(180)
                    tempAsset = File.createTempFile("rexgit-release-", ".asset", getApplication<android.app.Application>().cacheDir)
                    resolver.openInputStream(assetUri)?.use { input ->
                        tempAsset!!.outputStream().buffered().use { output -> input.copyTo(output, 64 * 1024) }
                    } ?: throw IllegalStateException("Tidak dapat membaca file asset yang dipilih.")
                    val mime = resolver.getType(assetUri).orEmpty().ifBlank {
                        if (safeName.endsWith(".apk", true)) "application/vnd.android.package-archive" else "application/octet-stream"
                    }
                    github.uploadReleaseAsset(
                        token = auth.token,
                        uploadUrlTemplate = created.uploadUrl,
                        file = tempAsset!!,
                        fileName = safeName,
                        contentType = mime
                    )
                } catch (t: Throwable) {
                    uploadWarning = "Release berhasil dibuat, tetapi asset gagal diunggah: ${t.readableMessage("Upload failed")}."
                }
            }

            val latest = runCatching { github.listReleases(auth.token, slug.first, slug.second) }
                .getOrElse { listOf(created) }
            _state.update {
                it.copy(
                    releases = latest,
                    releasesError = null,
                    releasePublishMessage = if (uploadWarning != null) {
                        "Release ${created.tagName} berhasil dibuat, tetapi asset belum berhasil dilampirkan."
                    } else {
                        "Release ${created.tagName} berhasil dibuat${if (assetUri != null) " dan asset berhasil diunggah" else ""}."
                    },
                    releasePublishError = uploadWarning
                )
            }
        } catch (t: Throwable) {
            _state.update { it.copy(releasePublishError = t.readableMessage("Gagal membuat GitHub Release")) }
        } finally {
            tempAsset?.delete()
            _state.update { it.copy(isPublishingRelease = false) }
        }
    }
}
