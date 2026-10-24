package com.rexaps.rexnux

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

suspend internal fun RexNuxInstaller.__split_install_1(
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

internal suspend fun RexNuxInstaller.__split_installAlpine_2(
        onProgress: suspend (InstallProgress) -> Unit
    ) {
        withContext(Dispatchers.IO) {

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
    }

internal suspend fun RexNuxInstaller.__split_download_3(
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

internal fun RexNuxInstaller.__split_downloadText_4(
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
