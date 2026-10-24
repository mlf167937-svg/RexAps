package com.rexaps.rexnux

import android.content.Context
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class RexNuxInstaller(
    private val context: Context
) {

    data class InstallProgress(
        val progress: Float,
        val message: String
    )

    companion object {
        private const val ALPINE_VERSION = "3.24.2"

        private const val ALPINE_URL =
            "https://dl-cdn.alpinelinux.org/alpine/v3.24/releases/aarch64/" +
                    "alpine-minirootfs-$ALPINE_VERSION-aarch64.tar.gz"

        private const val ALPINE_SHA256_URL =
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
    ) {
        when (distro) {
            RexNuxDistro.ALPINE -> installAlpine(onProgress)

            RexNuxDistro.ARCH -> {
                throw UnsupportedOperationException(
                    "Arch installer belum tersedia."
                )
            }
        }
    }

    internal suspend fun installAlpine(
        onProgress: suspend (InstallProgress) -> Unit
    ) {
        return this.__splitImpl_installAlpine(onProgress)
    }

    internal suspend fun download(
        url: String,
        destination: File,
        onProgress: suspend (Long, Long) -> Unit
    ) {
        val connection =
            URL(url).openConnection() as HttpURLConnection

        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.requestMethod = "GET"

        try {
            connection.connect()

            if (connection.responseCode !in 200..299) {
                throw IllegalStateException(
                    "Download failed: HTTP ${connection.responseCode}"
                )
            }

            val total = connection.contentLengthLong

            BufferedInputStream(
                connection.inputStream
            ).use { input ->

                FileOutputStream(destination).use { output ->

                    val buffer = ByteArray(64 * 1024)

                    var downloaded = 0L

                    while (true) {
                        val read = input.read(buffer)

                        if (read == -1) {
                            break
                        }

                        output.write(
                            buffer,
                            0,
                            read
                        )

                        downloaded += read

                        onProgress(
                            downloaded,
                            total
                        )
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    internal fun downloadText(
        url: String
    ): String {
        val connection =
            URL(url).openConnection() as HttpURLConnection

        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000

        return try {
            connection.inputStream
                .bufferedReader()
                .use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    internal fun sha256(
        file: File
    ): String {
        val digest =
            MessageDigest.getInstance("SHA-256")

        file.inputStream().use { input ->

            val buffer = ByteArray(64 * 1024)

            while (true) {
                val read = input.read(buffer)

                if (read == -1) {
                    break
                }

                digest.update(
                    buffer,
                    0,
                    read
                )
            }
        }

        return digest.digest()
            .joinToString("") {
                "%02x".format(it)
            }
    }

    internal suspend fun extractTarGz(
        archive: File,
        destination: File,
        onProgress: suspend (Long, Long) -> Unit
    ) {
        return this.__splitImpl_extractTarGz(archive, destination, onProgress)
    }

    internal fun safeResolve(
        base: File,
        entryName: String
    ): File {
        val target =
            File(base, entryName)

        val basePath =
            base.canonicalPath + File.separator

        val targetPath =
            target.canonicalPath

        if (
            targetPath != base.canonicalPath &&
            !targetPath.startsWith(basePath)
        ) {
            throw SecurityException(
                "Invalid archive entry: $entryName"
            )
        }

        return target
    }

    internal fun readFully(
        input: java.io.InputStream,
        buffer: ByteArray
    ): Boolean {
        var offset = 0

        while (offset < buffer.size) {
            val read =
                input.read(
                    buffer,
                    offset,
                    buffer.size - offset
                )

            if (read == -1) {
                return offset != 0
            }

            offset += read
        }

        return true
    }

    internal fun readString(
        buffer: ByteArray,
        offset: Int,
        length: Int
    ): String {
        var end = offset

        while (
            end < offset + length &&
            buffer[end].toInt() != 0
        ) {
            end++
        }

        return String(
            buffer,
            offset,
            end - offset,
            Charsets.UTF_8
        )
    }

    internal fun readOctal(
        buffer: ByteArray,
        offset: Int,
        length: Int
    ): Long {
        val value = readString(
            buffer,
            offset,
            length
        ).trim()

        if (value.isEmpty()) {
            return 0L
        }

        return value.toLongOrNull(8)
            ?: 0L
    }

    internal fun copyExactly(
        input: java.io.InputStream,
        output: FileOutputStream,
        size: Long
    ) {
        var remaining = size

        val buffer = ByteArray(64 * 1024)

        while (remaining > 0) {
            val read = input.read(
                buffer,
                0,
                minOf(
                    buffer.size.toLong(),
                    remaining
                ).toInt()
            )

            if (read == -1) {
                throw IllegalStateException(
                    "Unexpected end of archive."
                )
            }

            output.write(
                buffer,
                0,
                read
            )

            remaining -= read
        }
    }

    internal fun skipExactly(
        input: java.io.InputStream,
        size: Long
    ) {
        var remaining = size

        while (remaining > 0) {
            val skipped =
                input.skip(remaining)

            if (skipped <= 0) {
                if (input.read() == -1) {
                    throw IllegalStateException(
                        "Unexpected end of archive."
                    )
                }

                remaining--
            } else {
                remaining -= skipped
            }
        }
    }
}
