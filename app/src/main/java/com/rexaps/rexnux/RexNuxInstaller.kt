package com.rexaps.rexnux

import android.content.Context
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.zip.GZIPInputStream

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

    private val rootDir: File
        get() = File(context.filesDir, "rexnux")

    private val alpineDir: File
        get() = File(rootDir, "alpine")

    private val downloadsDir: File
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

    private suspend fun installAlpine(
        onProgress: suspend (InstallProgress) -> Unit
    ) {
        rootDir.mkdirs()
        downloadsDir.mkdirs()

        val archive = File(
            downloadsDir,
            "alpine-$ALPINE_VERSION-aarch64.tar.gz"
        )

        onProgress(
            InstallProgress(
                0f,
                "Downloading Alpine Linux..."
            )
        )

        download(
            url = ALPINE_URL,
            destination = archive
        ) { downloaded, total ->

            val progress =
                if (total > 0) {
                    downloaded.toFloat() / total.toFloat()
                } else {
                    0f
                }

            onProgress(
                InstallProgress(
                    progress * 0.7f,
                    "Downloading Alpine... ${
                        (progress * 100).toInt()
                    }%"
                )
            )
        }

        onProgress(
            InstallProgress(
                0.72f,
                "Verifying Alpine..."
            )
        )

        val expectedSha = downloadText(
            ALPINE_SHA256_URL
        ).trim()
            .split(Regex("\\s+"))
            .first()

        val actualSha = sha256(archive)

        if (!expectedSha.equals(actualSha, ignoreCase = true)) {
            archive.delete()

            throw IllegalStateException(
                "SHA-256 verification failed."
            )
        }

        onProgress(
            InstallProgress(
                0.78f,
                "Preparing filesystem..."
            )
        )

        if (alpineDir.exists()) {
            alpineDir.deleteRecursively()
        }

        alpineDir.mkdirs()

        onProgress(
            InstallProgress(
                0.8f,
                "Extracting Alpine..."
            )
        )

        extractTarGz(
            archive,
            alpineDir
        ) { extracted, estimated ->

            val progress =
                if (estimated > 0) {
                    extracted.toFloat() /
                            estimated.toFloat()
                } else {
                    0f
                }

            onProgress(
                InstallProgress(
                    0.8f + progress * 0.19f,
                    "Extracting Alpine..."
                )
            )
        }

        archive.delete()

        onProgress(
            InstallProgress(
                1f,
                "Alpine installation complete."
            )
        )
    }

    private fun download(
        url: String,
        destination: File,
        onProgress: (Long, Long) -> Unit
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

    private fun downloadText(
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

    private fun sha256(
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

    private fun extractTarGz(
        archive: File,
        destination: File,
        onProgress: (Long, Long) -> Unit
    ) {
        /*
         * Minirootfs berukuran kecil, jadi untuk tahap awal
         * kita menggunakan ukuran archive sebagai indikator
         * progress extraction.
         *
         * TAR header:
         * 0..99   = filename
         * 100..107 = mode
         * 108..115 = uid
         * 116..123 = gid
         * 124..135 = size
         * 136..147 = mtime
         * 148..155 = checksum
         * 156 = typeflag
         */
        GZIPInputStream(
            BufferedInputStream(
                archive.inputStream()
            )
        ).use { input ->

            var processed = 0L

            while (true) {
                val header = ByteArray(512)

                if (!readFully(input, header)) {
                    break
                }

                if (header.all { it.toInt() == 0 }) {
                    break
                }

                val name = readString(
                    header,
                    0,
                    100
                )

                val size = readOctal(
                    header,
                    124,
                    12
                )

                val type = header[156].toInt()
                    .toChar()

                val target =
                    safeResolve(
                        destination,
                        name
                    )

                when (type) {

                    '5' -> {
                        target.mkdirs()
                    }

                    '0', '\u0000' -> {
                        target.parentFile?.mkdirs()

                        FileOutputStream(target)
                            .use { output ->

                                copyExactly(
                                    input,
                                    output,
                                    size
                                )
                            }

                        target.setReadable(
                            true,
                            false
                        )

                        target.setWritable(
                            true,
                            true
                        )
                    }

                    '2' -> {
                        /*
                         * Symlink support.
                         *
                         * Android filesystem permissions differ
                         * from normal Linux, therefore this is kept
                         * conservative for the first implementation.
                         */
                    }

                    else -> {
                        skipExactly(
                            input,
                            size
                        )
                    }
                }

                val padding =
                    (512 - (size % 512)) % 512

                skipExactly(
                    input,
                    padding
                )

                processed += 512 + size + padding

                onProgress(
                    processed,
                    archive.length()
                )
            }
        }
    }

    private fun safeResolve(
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

    private fun readFully(
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

    private fun readString(
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

    private fun readOctal(
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

    private fun copyExactly(
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

    private fun skipExactly(
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
