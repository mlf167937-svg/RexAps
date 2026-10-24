package com.rexaps.rexnux

import android.system.Os
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.GZIPInputStream

internal suspend fun RexNuxInstaller.extractTarGz(archive: File,
    destination: File,
    onProgress: suspend (Long, Long) -> Unit) {
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
     * 157..256 = linkname (for symlinks/hardlinks)
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

                '1' -> {
                    /*
                     * Hardlink. The linkname field points to
                     * another path already extracted inside
                     * this archive. We fall back to a copy of
                     * that file since Android/Java has no
                     * portable hardlink API exposed here.
                     */
                    val linkName = readString(
                        header,
                        157,
                        100
                    )

                    val linkedFile =
                        safeResolve(
                            destination,
                            linkName
                        )

                    target.parentFile?.mkdirs()

                    if (linkedFile.exists()) {
                        linkedFile.copyTo(
                            target,
                            overwrite = true
                        )

                        target.setReadable(true, false)
                        target.setWritable(true, true)
                        target.setExecutable(
                            linkedFile.canExecute(),
                            false
                        )
                    }
                }

                '2' -> {
                    /*
                     * Symlink. Alpine's minirootfs relies
                     * heavily on these (busybox applets),
                     * so this must actually create a real
                     * symlink or the rootfs is unusable.
                     */
                    val linkTarget = readString(
                        header,
                        157,
                        100
                    )

                    target.parentFile?.mkdirs()

                    if (target.exists() ||
                        java.nio.file.Files.isSymbolicLink(
                            target.toPath()
                        )
                    ) {
                        target.delete()
                    }

                    try {
                        Os.symlink(
                            linkTarget,
                            target.absolutePath
                        )
                    } catch (e: Exception) {
                        /*
                         * Non-fatal: skip broken symlink
                         * entries rather than aborting the
                         * whole install.
                         */
                    }
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

private fun RexNuxInstaller.safeResolve(base: File,
    entryName: String): File {
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

private fun RexNuxInstaller.readFully(input: java.io.InputStream,
    buffer: ByteArray): Boolean {
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

private fun RexNuxInstaller.readString(buffer: ByteArray,
    offset: Int,
    length: Int): String {
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

private fun RexNuxInstaller.readOctal(buffer: ByteArray,
    offset: Int,
    length: Int): Long {
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
