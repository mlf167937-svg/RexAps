package com.rexaps.rexnux

import android.content.Context
import android.os.Build
import android.system.Os
import android.util.Log
import java.io.File

class RexNuxRuntime(
    private val context: Context
) {
    companion object {
        private const val TAG = "RexNuxRuntime"
    }

    private val rootDir = File(context.filesDir, "rexnux")
    private val runtimeDir = File(rootDir, "runtime")
    private val prootFile = File(runtimeDir, "proot")
    private val libtallocFile = File(runtimeDir, "libtalloc.so.2")

    private fun copyAsset(name: String, target: File) {
        target.parentFile?.mkdirs()
        context.assets.open(name).use { input ->
            target.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        target.setReadable(true, false)
        target.setWritable(true, false)
        target.setExecutable(true, false)
        try {
            Os.chmod(target.absolutePath, 0b111101101) // 0755
        } catch (_: Throwable) {}
    }

    fun prepareRuntime() {
        runtimeDir.mkdirs()
        if (!prootFile.exists()) {
            copyAsset("rexnux/proot", prootFile)
        } else {
            prootFile.setExecutable(true, false)
            try {
                Os.chmod(prootFile.absolutePath, 0b111101101)
            } catch (_: Throwable) {}
        }

        // Pastikan libtalloc.so.2 tersedia di folder runtime
        if (!libtallocFile.exists()) {
            try {
                copyAsset("rexnux/libtalloc.so.2", libtallocFile)
            } catch (e: Throwable) {
                Log.w(TAG, "libtalloc.so.2 asset not found in assets: ${e.message}")
            }
        }
    }

    fun isDistroInstalled(distro: RexNuxDistro): Boolean {
        val distroDir = File(rootDir, distro.id)
        val sh1 = File(distroDir, "bin/sh")
        val sh2 = File(distroDir, "usr/bin/sh")
        val sh3 = File(distroDir, "bin/bash")
        return (sh1.exists() || sh2.exists() || sh3.exists())
    }

    fun isRuntimeInstalled(): Boolean {
        prepareRuntime()
        return prootFile.isFile && prootFile.canExecute()
    }

    fun startDistro(distro: RexNuxDistro): Process {
        val distroDir = File(rootDir, distro.id)
        if (!isDistroInstalled(distro)) {
            error("Distro ${distro.title} belum terinstall. Silakan instal terlebih dahulu.")
        }
        prepareRuntime()
        if (!isRuntimeInstalled()) {
            error("PRoot binary tidak dapat dieksekusi di perangkat ini.")
        }

        // Tentukan shell yang tersedia di rootfs
        val shellCandidate = listOf(
            distro.defaultShell,
            "/bin/bash",
            "/usr/bin/bash",
            "/bin/sh",
            "/usr/bin/sh"
        ).firstOrNull { File(distroDir, it.removePrefix("/")).exists() } ?: "/bin/sh"

        val userHome = File(distroDir, "files/home").apply { mkdirs() }
        val tmpDir = File(distroDir, "tmp").apply { mkdirs() }

        // Bindings yang aman dan kompatibel dengan SELinux / sandbox Android
        val command = mutableListOf(
            prootFile.absolutePath,
            "--root-id",
            "--link2symlink",
            "-0", // simulate root UID 0
            "-r", distroDir.absolutePath,
            "-b", "/dev",
            "-b", "/proc",
            "-b", "/sys"
        )

        // Bind /sdcard jika tersedia
        val sdcard = File("/sdcard")
        if (sdcard.exists() && sdcard.canRead()) {
            command.addAll(listOf("-b", "/sdcard"))
        }

        // Working directory dan shell
        command.addAll(
            listOf(
                "-w", "/files/home",
                shellCandidate,
                "-l"
            )
        )

        val pb = ProcessBuilder(command)
            .directory(distroDir)
            .redirectErrorStream(true)

        val env = pb.environment()
        // Mengarahkan LD_LIBRARY_PATH runtime agar PRoot menemukan libtalloc.so.2
        val currentLd = env["LD_LIBRARY_PATH"]
        env["LD_LIBRARY_PATH"] = listOfNotNull(
            runtimeDir.absolutePath,
            context.applicationInfo.nativeLibraryDir,
            currentLd
        ).joinToString(":")

        env["HOME"] = "/files/home"
        env["USER"] = "root"
        env["SHELL"] = shellCandidate
        env["TERM"] = "xterm-256color"
        env["LANG"] = "C.UTF-8"
        env["PATH"] = "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
        env["PROOT_TMP_DIR"] = tmpDir.absolutePath

        Log.i(TAG, "Launching PRoot command: ${command.joinToString(" ")}")
        return pb.start()
    }
}
