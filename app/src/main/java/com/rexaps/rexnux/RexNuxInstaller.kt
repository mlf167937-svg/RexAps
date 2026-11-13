package com.rexaps.rexnux

import android.content.Context
import android.os.Build
import android.system.Os
import android.system.OsConstants
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.tukaani.xz.XZInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
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

    data class DistroConfig(
        val distro: RexNuxDistro,
        val url: String,
        val sha256Url: String? = null,
        val sha256Hex: String? = null,
        val archiveType: String // "tar.gz" or "tar.xz"
    )

    companion object {
        private const val TAG = "RexNuxInstaller"

        fun getDistroConfig(distro: RexNuxDistro): DistroConfig {
            return when (distro) {
                RexNuxDistro.ALPINE -> DistroConfig(
                    distro = distro,
                    url = "https://dl-cdn.alpinelinux.org/alpine/v3.21/releases/aarch64/alpine-minirootfs-3.21.0-aarch64.tar.gz",
                    sha256Hex = "f31202c4070c4ef7de9e157e1bd01cb4da3a2150035d74ea5372c5e86f1efac1",
                    archiveType = "tar.gz"
                )
                RexNuxDistro.ARCH -> DistroConfig(
                    distro = distro,
                    url = "https://images.linuxcontainers.org/images/archlinux/current/arm64/default/20261107_04%3A18/rootfs.tar.xz",
                    sha256Hex = "8f97c001dc9e0f873d69e0dc89f52112d8b60af61ba38b8377039a06fba38aa6",
                    archiveType = "tar.xz"
                )
                RexNuxDistro.DEBIAN -> DistroConfig(
                    distro = distro,
                    url = "https://images.linuxcontainers.org/images/debian/bookworm/arm64/default/20261108_05%3A24/rootfs.tar.xz",
                    sha256Hex = "0a2be2b74cada82c35c776ae5daf533fb2a316901b728dfbd98f35b46fb87d85",
                    archiveType = "tar.xz"
                )
                RexNuxDistro.UBUNTU -> DistroConfig(
                    distro = distro,
                    url = "https://images.linuxcontainers.org/images/ubuntu/noble/arm64/default/20261108_07%3A42/rootfs.tar.xz",
                    sha256Hex = "6b067cf0ba536a15c6f059629b7dffc572192dd56c99e73a5b5bd4cc6cd8a976",
                    archiveType = "tar.xz"
                )
                RexNuxDistro.KALI -> DistroConfig(
                    distro = distro,
                    url = "https://images.linuxcontainers.org/images/kali/current/arm64/default/20261108_17%3A14/rootfs.tar.xz",
                    sha256Hex = "8145ab2c8c517e7f4c1cd3d9ead197632fb3c2edd4900c647163c7e80b5eb0f5",
                    archiveType = "tar.xz"
                )
            }
        }
    }

    val rootDir: File
        get() = File(context.filesDir, "rexnux").apply { mkdirs() }

    val downloadsDir: File
        get() = File(rootDir, "downloads").apply { mkdirs() }

    val metadataDir: File
        get() = File(rootDir, "metadata").apply { mkdirs() }

    val runtimeDir: File
        get() = File(rootDir, "runtime").apply { mkdirs() }

    fun distroDir(distro: RexNuxDistro): File {
        return File(rootDir, distro.id)
    }

    fun isDistroInstalled(distro: RexNuxDistro): Boolean {
        val dir = distroDir(distro)
        val shFile = File(dir, "bin/sh")
        val usrShFile = File(dir, "usr/bin/sh")
        return (shFile.exists() || usrShFile.exists()) && File(metadataDir, "${distro.id}.json").exists()
    }

    fun getInstalledDistros(): Set<RexNuxDistro> {
        return RexNuxDistro.entries.filter { isDistroInstalled(it) }.toSet()
    }

    suspend fun install(
        distro: RexNuxDistro,
        onProgress: suspend (InstallProgress) -> Unit
    ) = withContext(Dispatchers.IO) {
        val config = getDistroConfig(distro)
        val ext = if (config.archiveType == "tar.xz") ".tar.xz" else ".tar.gz"
        val archive = File(downloadsDir, "${distro.id}-rootfs$ext")
        val stagingDir = File(rootDir, "${distro.id}_staging")
        val targetDir = distroDir(distro)

        try {
            onProgress(InstallProgress(0.05f, "Menghubungkan ke server ${distro.title}..."))

            // 1. Download rootfs
            download(config.url, archive) { downloaded, total ->
                val ratio = if (total > 0) downloaded.toFloat() / total.toFloat() else 0f
                val mbDownloaded = downloaded / (1024 * 1024)
                val mbTotal = total / (1024 * 1024)
                onProgress(
                    InstallProgress(
                        0.05f + ratio * 0.60f,
                        "Mengunduh ${distro.title}: $mbDownloaded MB / $mbTotal MB (${(ratio * 100).toInt()}%)"
                    )
                )
            }

            // 2. Verifikasi Integritas SHA-256
            onProgress(InstallProgress(0.68f, "Memverifikasi integritas checksum SHA-256..."))
            val expectedSha = config.sha256Hex ?: config.sha256Url?.let { url ->
                downloadText(url).trim().split(Regex("\\s+")).first()
            }

            if (!expectedSha.isNullOrBlank()) {
                val actualSha = sha256(archive)
                if (!expectedSha.equals(actualSha, ignoreCase = true)) {
                    archive.delete()
                    throw IllegalStateException(
                        "Verifikasi SHA-256 gagal! Diharapkan $expectedSha, tetapi diperoleh $actualSha"
                    )
                }
            }

            // 3. Staging directory setup
            onProgress(InstallProgress(0.72f, "Menyiapkan staging filesystem..."))
            if (stagingDir.exists()) stagingDir.deleteRecursively()
            stagingDir.mkdirs()

            // 4. Ekstraksi Rootfs dengan preservasi permissions & symlinks
            onProgress(InstallProgress(0.75f, "Mengekstrak ${distro.title}..."))
            extractArchive(archive, stagingDir, config.archiveType) { processed, total ->
                val ratio = if (total > 0) processed.toFloat() / total.toFloat() else 0f
                onProgress(
                    InstallProgress(
                        0.75f + ratio * 0.20f,
                        "Mengekstrak root filesystem (${(ratio * 100).toInt()}%)..."
                    )
                )
            }

            // 5. Finalisasi struktur Linux & PRoot environment
            onProgress(InstallProgress(0.96f, "Mengonfigurasi /files/home dan DNS resolv.conf..."))
            setupLinuxStructure(stagingDir)

            // 6. Atomic move staging ke direktori final
            if (targetDir.exists()) targetDir.deleteRecursively()
            if (!stagingDir.renameTo(targetDir)) {
                // Fallback copy if rename across mount fails
                stagingDir.copyRecursively(targetDir, overwrite = true)
                stagingDir.deleteRecursively()
            }

            // 7. Simpan metadata
            val size = calculateDirectorySize(targetDir)
            saveMetadata(distro, size)

            // Bersihkan file download
            archive.delete()

            onProgress(InstallProgress(1.0f, "Instalasi ${distro.title} berhasil diselesaikan!"))
        } catch (e: Throwable) {
            Log.e(TAG, "Gagal menginstal ${distro.title}", e)
            stagingDir.deleteRecursively()
            archive.delete()
            throw e
        }
    }

    suspend fun uninstall(distro: RexNuxDistro) = withContext(Dispatchers.IO) {
        val dir = distroDir(distro)
        if (dir.exists()) dir.deleteRecursively()
        File(metadataDir, "${distro.id}.json").delete()
    }

    private fun setupLinuxStructure(rootfsDir: File) {
        // Buat folder files/home sesuai arsitektur RexAps
        val userHome = File(rootfsDir, "files/home")
        userHome.mkdirs()
        try {
            Os.chmod(userHome.absolutePath, 0b111101101) // 0755
        } catch (_: Throwable) {}

        // Buat direktori standar jika belum ada
        listOf("etc", "tmp", "var", "dev", "proc", "sys", "root", "usr/bin").forEach { sub ->
            File(rootfsDir, sub).mkdirs()
        }

        // Atur DNS resolver default agar apt/apk/pacman langsung bisa koneksi internet
        val resolvConf = File(rootfsDir, "etc/resolv.conf")
        try {
            resolvConf.delete() // hapus symlink lama jika ada
            resolvConf.writeText("nameserver 8.8.8.8\nnameserver 1.1.1.1\n")
        } catch (e: Throwable) {
            Log.w(TAG, "Gagal menulis etc/resolv.conf: ${e.message}")
        }

        // Pastikan /tmp permissions
        val tmpDir = File(rootfsDir, "tmp")
        try {
            Os.chmod(tmpDir.absolutePath, 0b111111111) // 0777
        } catch (_: Throwable) {}
    }

    private fun saveMetadata(distro: RexNuxDistro, sizeBytes: Long) {
        val metaFile = File(metadataDir, "${distro.id}.json")
        val json = """
            {
                "distro": "${distro.id}",
                "title": "${distro.title}",
                "installedAt": ${System.currentTimeMillis()},
                "sizeBytes": $sizeBytes,
                "version": "1.0"
            }
        """.trimIndent()
        metaFile.writeText(json)
    }

    fun calculateDirectorySize(dir: File): Long {
        var size = 0L
        dir.walkTopDown().forEach {
            if (it.isFile) size += it.length()
        }
        return size
    }

    fun getFormattedDistroSize(distro: RexNuxDistro): String {
        val dir = distroDir(distro)
        if (!dir.exists()) return "0 MB"
        val bytes = calculateDirectorySize(dir)
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        return if (mb >= 1024.0) {
            "%.2f GB".format(mb / 1024.0)
        } else {
            "%.1f MB".format(mb)
        }
    }

    private suspend fun download(
        url: String,
        destination: File,
        onProgress: suspend (Long, Long) -> Unit
    ) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 20_000
        connection.readTimeout = 40_000
        connection.requestMethod = "GET"
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 RexAps/RexNux")

        try {
            connection.connect()
            var status = connection.responseCode
            var realConn = connection
            if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == 307 || status == 308) {
                val newUrl = connection.getHeaderField("Location")
                realConn = URL(newUrl).openConnection() as HttpURLConnection
                realConn.connectTimeout = 20_000
                realConn.readTimeout = 40_000
                realConn.setRequestProperty("User-Agent", "Mozilla/5.0 RexAps/RexNux")
                realConn.connect()
                status = realConn.responseCode
            }

            if (status !in 200..299) {
                throw IllegalStateException("Download error: HTTP $status")
            }

            val total = realConn.contentLengthLong
            BufferedInputStream(realConn.inputStream).use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(128 * 1024)
                    var downloaded = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        onProgress(downloaded, total)
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun downloadText(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 RexAps/RexNux")
        return try {
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(128 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private suspend fun extractArchive(
        archive: File,
        destination: File,
        type: String,
        onProgress: suspend (Long, Long) -> Unit
    ) {
        val rawInput: InputStream = BufferedInputStream(FileInputStream(archive), 128 * 1024)
        val decompressedStream: InputStream = when (type) {
            "tar.xz" -> XZInputStream(rawInput)
            "tar.gz" -> GzipCompressorInputStream(rawInput, true)
            else -> rawInput
        }

        val tarInput = TarArchiveInputStream(decompressedStream)
        val totalSize = archive.length()
        var processed = 0L
        val hardlinkEntries = mutableListOf<Pair<File, String>>()

        tarInput.use { tar ->
            while (true) {
                val entry: TarArchiveEntry = tar.nextEntry ?: break
                val name = entry.name.removePrefix("./").removePrefix("/")
                if (name.isBlank()) continue

                val target = safeResolve(destination, name)

                if (entry.isDirectory) {
                    target.mkdirs()
                    applyMode(target, entry.mode)
                } else if (entry.isSymbolicLink) {
                    target.parentFile?.mkdirs()
                    if (target.exists() || java.nio.file.Files.isSymbolicLink(target.toPath())) {
                        target.delete()
                    }
                    try {
                        Os.symlink(entry.linkName, target.absolutePath)
                    } catch (e: Throwable) {
                        Log.w(TAG, "Symlink skip ${entry.linkName} -> ${target.path}: ${e.message}")
                    }
                } else if (entry.isLink) {
                    // Simpan untuk resolusi hardlink setelah ekstraksi file selesai
                    hardlinkEntries.add(target to entry.linkName)
                } else {
                    target.parentFile?.mkdirs()
                    FileOutputStream(target).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var remaining = entry.size
                        while (remaining > 0) {
                            val r = tar.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                            if (r == -1) break
                            output.write(buffer, 0, r)
                            remaining -= r
                        }
                    }
                    applyMode(target, entry.mode)
                }

                processed += entry.size + 512
                onProgress(processed, totalSize)
            }
        }

        // Tuntaskan pemrosesan hardlink
        for ((targetFile, linkPath) in hardlinkEntries) {
            val sourceFile = safeResolve(destination, linkPath.removePrefix("./").removePrefix("/"))
            targetFile.parentFile?.mkdirs()
            if (sourceFile.exists()) {
                try {
                    Os.link(sourceFile.absolutePath, targetFile.absolutePath)
                } catch (_: Throwable) {
                    sourceFile.copyTo(targetFile, overwrite = true)
                    if (sourceFile.canExecute()) targetFile.setExecutable(true, false)
                }
            }
        }
    }

    private fun safeResolve(base: File, entryName: String): File {
        val target = File(base, entryName)
        val basePath = base.canonicalPath + File.separator
        val targetPath = target.canonicalPath
        if (targetPath != base.canonicalPath && !targetPath.startsWith(basePath)) {
            throw SecurityException("Path traversal attempt in archive entry: $entryName")
        }
        return target
    }

    private fun applyMode(file: File, mode: Int) {
        if (mode <= 0) return
        try {
            Os.chmod(file.absolutePath, mode and 0b111111111)
        } catch (_: Throwable) {
            // Fallback dengan java File permissions
            val isExec = (mode and 0b001001001) != 0
            if (isExec) file.setExecutable(true, false)
            file.setReadable(true, false)
            file.setWritable(true, true)
        }
    }
}
