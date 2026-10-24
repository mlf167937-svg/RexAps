package com.rexaps.rexnux

import android.content.Context
import java.io.File
import java.io.FileOutputStream

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
    ) = this.__split_install_1(distro, onProgress)

    internal suspend fun installAlpine(
        onProgress: suspend (InstallProgress) -> Unit
    ) = this.__split_installAlpine_2(onProgress)

    internal suspend fun download(
        url: String,
        destination: File,
        onProgress: suspend (Long, Long) -> Unit
    ) = this.__split_download_3(url, destination, onProgress)

    internal fun downloadText(
        url: String
    ): String = this.__split_downloadText_4(url)

    internal fun sha256(
        file: File
    ): String = this.__split_sha256_5(file)

    internal suspend fun extractTarGz(
        archive: File,
        destination: File,
        onProgress: suspend (Long, Long) -> Unit
    ) = this.__split_extractTarGz_6(archive, destination, onProgress)

    internal fun safeResolve(
        base: File,
        entryName: String
    ): File = this.__split_safeResolve_7(base, entryName)

    internal fun readFully(
        input: java.io.InputStream,
        buffer: ByteArray
    ): Boolean = this.__split_readFully_8(input, buffer)

    internal fun readString(
        buffer: ByteArray,
        offset: Int,
        length: Int
    ): String = this.__split_readString_9(buffer, offset, length)

    internal fun readOctal(
        buffer: ByteArray,
        offset: Int,
        length: Int
    ): Long = this.__split_readOctal_10(buffer, offset, length)

    internal fun copyExactly(
        input: java.io.InputStream,
        output: FileOutputStream,
        size: Long
    ) = this.__split_copyExactly_11(input, output, size)

    internal fun skipExactly(
        input: java.io.InputStream,
        size: Long
    ) = this.__split_skipExactly_12(input, size)
}
