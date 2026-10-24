package com.rexaps.rexnux

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

suspend internal fun RexNuxInstaller.installImpl(distro: RexNuxDistro,
    onProgress: suspend (InstallProgress) -> Unit) {
    when (distro) {
        RexNuxDistro.ALPINE -> installAlpine(onProgress)

        RexNuxDistro.ARCH -> {
            throw UnsupportedOperationException(
                "Arch installer belum tersedia."
            )
        }
    }
}

private suspend fun RexNuxInstaller.installAlpine(onProgress: suspend (InstallProgress) -> Unit) {
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
