package com.rexaps.rexnux

import android.content.Context
import java.io.File

class RexNuxInstaller(
    private val context: Context
) {

    data class InstallProgress(
        val progress: Float,
        val message: String
    )

    companion object {
        internal const val ALPINE_VERSION = "3.24.2"

        internal const val ALPINE_URL =
            "https://dl-cdn.alpinelinux.org/alpine/v3.24/releases/aarch64/" +
                    "alpine-minirootfs-$ALPINE_VERSION-aarch64.tar.gz"

        internal const val ALPINE_SHA256_URL =
            "$ALPINE_URL.sha256"
    }

    internal val rootDir: File
        get() = File(context.filesDir, "rexnux")

    internal val alpineDir: File
        get() = File(rootDir, "alpine")

    internal val downloadsDir: File
        get() = File(rootDir, "downloads")

    suspend fun install(
        distro: RexNuxDistro,
        onProgress: suspend (InstallProgress) -> Unit
    ) = installImpl(distro, onProgress)
}
