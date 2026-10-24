package com.rexaps.rexnux

internal fun RexNuxInstaller.__split_readFully_8(
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

internal fun RexNuxInstaller.__split_readString_9(
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

internal fun RexNuxInstaller.__split_readOctal_10(
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
