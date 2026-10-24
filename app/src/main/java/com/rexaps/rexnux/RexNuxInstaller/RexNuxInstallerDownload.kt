package com.rexaps.rexnux

import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

internal suspend fun RexNuxInstaller.download(url: String,
    destination: File,
    onProgress: suspend (Long, Long) -> Unit) {
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

internal fun RexNuxInstaller.downloadText(url: String): String {
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

internal fun RexNuxInstaller.sha256(file: File): String {
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
