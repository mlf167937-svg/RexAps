package com.rexaps.rexmonitor.booster

import android.os.Build
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

/**
 * Root bersifat OPSIONAL. Deteksi hanya memeriksa file; akses `su` hanya diminta saat
 * pengguna menekan tombol, dan hanya menjalankan perintah baca-saja yang tetap (whitelist):
 * `id` dan `head -n 1 /proc/stat`.
 */
class RexRootChecker {
    private var process: Process? = null
    private var writer: BufferedWriter? = null
    private var reader: BufferedReader? = null

    fun isLikelyRooted(): Boolean {
        val paths = listOf(
            "/system/bin/su", "/system/xbin/su", "/sbin/su", "/system/su",
            "/system/app/Superuser.apk", "/data/adb/magisk", "/system/sd/xbin/su"
        )
        return paths.any { runCatching { File(it).exists() }.getOrDefault(false) } ||
            Build.TAGS?.contains("test-keys") == true
    }

    /** Meminta akses su. true hanya jika benar-benar mendapat uid=0. Blocking: panggil dari IO. */
    @Synchronized
    fun requestSu(): Boolean {
        close()
        return try {
            val p = ProcessBuilder("su").redirectErrorStream(true).start()
            process = p
            writer = BufferedWriter(OutputStreamWriter(p.outputStream))
            reader = BufferedReader(InputStreamReader(p.inputStream))
            writer!!.write("id\n")
            writer!!.flush()
            val ok = reader!!.readLine()?.contains("uid=0") == true
            if (!ok) close()
            ok
        } catch (_: Throwable) {
            close()
            false
        }
    }

    /** Baris pertama /proc/stat via su yang sudah disetujui; null bila tidak tersedia. */
    @Synchronized
    fun readProcStatLine(): String? {
        val p = process ?: return null
        if (!p.isAlive) {
            close()
            return null
        }
        return try {
            writer?.write("head -n 1 /proc/stat\n")
            writer?.flush()
            reader?.readLine()?.takeIf { it.startsWith("cpu ") }
        } catch (_: Throwable) {
            close()
            null
        }
    }

    @Synchronized
    fun close() {
        runCatching { writer?.write("exit\n"); writer?.flush() }
        runCatching { process?.destroy() }
        process = null
        writer = null
        reader = null
    }
}
