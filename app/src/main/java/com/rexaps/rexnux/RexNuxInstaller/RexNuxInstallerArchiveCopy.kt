package com.rexaps.rexnux

import java.io.FileOutputStream

internal fun RexNuxInstaller.copyExactly(input: java.io.InputStream,
    output: FileOutputStream,
    size: Long) {
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

internal fun RexNuxInstaller.skipExactly(input: java.io.InputStream,
    size: Long) {
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
