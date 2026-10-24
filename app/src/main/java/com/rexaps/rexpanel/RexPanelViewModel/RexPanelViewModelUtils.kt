package com.rexaps.rexpanel

internal fun monCmd(elevate: Boolean): String =
    MON_CMD.replace("@@ELEV@@", if (elevate) "1" else "0")

/**
 * CPU dari keluaran `top`
 * Android toybox, procps Linux, busybox.
 *
 * Dipakai saat /proc/stat tidak bisa dibaca.
 */
internal fun parseTopCpu(t: String): Float? {
    TOP_TOYBOX.find(t)?.let {
        val tot = it.groupValues[1].toFloat()
        val idle = it.groupValues[2].toFloat()

        if (tot > 0f) {
            return ((tot - idle) / tot).coerceIn(0f, 1f)
        }
    }

    TOP_LINUX.find(t)?.let {
        return (
            1f -
                it.groupValues[1]
                    .replace(',', '.')
                    .toFloat() / 100f
            ).coerceIn(0f, 1f)
    }

    TOP_BUSY.find(t)?.let {
        return (
            1f -
                it.groupValues[1].toFloat() / 100f
            ).coerceIn(0f, 1f)
    }

    return null
}
