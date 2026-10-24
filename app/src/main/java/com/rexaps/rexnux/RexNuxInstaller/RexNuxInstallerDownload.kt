package com.rexaps.rexnux

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal suspend fun RexNuxInstaller.__splitImpl_installAlpine(
        onProgress: suspend (RexNuxInstaller.InstallProgress) -> Unit
    ) {
        withContext(Dispatchers.IO) {

            rootDir.mkdirs()
            downloadsDir.mkdirs()

            val archive = File(
                downloadsDir,
                "alpine-${RexNuxInstaller.ALPINE_VERSION}-aarch64.tar.gz"
            )

            onProgress(
                RexNuxInstaller.InstallProgress(
                    0f,
                    "Downloading Alpine Linux..."
                )
            )

            download(
                url = RexNuxInstaller.ALPINE_URL,
                destination = archive
            ) { downloaded, total ->

                val progress =
                    if (total > 0) {
                        downloaded.toFloat() / total.toFloat()
                    } else {
                        0f
                    }

                onProgress(
                    RexNuxInstaller.InstallProgress(
                        progress * 0.7f,
                        "Downloading Alpine... ${
                            (progress * 100).toInt()
                        }%"
                    )
                )
            }

            onProgress(
                RexNuxInstaller.InstallProgress(
                    0.72f,
                    "Verifying Alpine..."
                )
            )

            val expectedSha = downloadText(
                RexNuxInstaller.ALPINE_SHA256_URL
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
                RexNuxInstaller.InstallProgress(
                    0.78f,
                    "Preparing filesystem..."
                )
            )

            if (alpineDir.exists()) {
                alpineDir.deleteRecursively()
            }

            alpineDir.mkdirs()

            onProgress(
                RexNuxInstaller.InstallProgress(
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
                    RexNuxInstaller.InstallProgress(
                        0.8f + progress * 0.19f,
                        "Extracting Alpine..."
                    )
                )
            }

            archive.delete()

            onProgress(
                RexNuxInstaller.InstallProgress(
                    1f,
                    "Alpine installation complete."
                )
            )
        }
    }
