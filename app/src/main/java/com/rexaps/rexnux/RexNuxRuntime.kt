package com.rexaps.rexnux

import android.content.Context
import java.io.File

class RexNuxRuntime(
    private val context: Context
) {
    private val rootDir =
        File(context.filesDir, "rexnux")

    private val alpineDir =
        File(rootDir, "alpine")

    private val runtimeDir =
        File(rootDir, "runtime")

    private val prootFile =
        File(runtimeDir, "proot")

    private fun copyAsset(
        name: String,
        target: File
    ) {
        target.parentFile?.mkdirs()

        context.assets.open(name).use { input ->
            target.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        target.setReadable(true, false)
        target.setWritable(true, false)
        target.setExecutable(true, false)
    }

    private fun prepareRuntime() {
        runtimeDir.mkdirs()

        if (!prootFile.exists()) {
            copyAsset(
                "rexnux/proot",
                prootFile
            )
        } else {
            prootFile.setExecutable(true, false)
        }
    }

    fun isAlpineInstalled(): Boolean {
        return File(
            alpineDir,
            "bin/sh"
        ).isFile
    }

    fun isRuntimeInstalled(): Boolean {
        return prootFile.isFile &&
            prootFile.canExecute()
    }

    fun startAlpine(): Process {
        if (!isAlpineInstalled()) {
            error(
                "Alpine Linux belum terinstall."
            )
        }

        prepareRuntime()

        if (!isRuntimeInstalled()) {
            error(
                "PRoot runtime tidak tersedia."
            )
        }

        val command = listOf(
            prootFile.absolutePath,

            "--root-id",
            "--link2symlink",

            "-r",
            alpineDir.absolutePath,

            "-b",
            "/dev",

            "-b",
            "/proc",

            "-b",
            "/sys",

            "-b",
            "/sdcard",

            "-w",
            "/root",

            "/bin/sh",
            "-l"
        )

        return ProcessBuilder(command)
            .directory(alpineDir)
            .redirectErrorStream(true)
            .apply {
                environment()["HOME"] = "/root"
                environment()["USER"] = "root"
                environment()["SHELL"] = "/bin/sh"
                environment()["TERM"] = "xterm-256color"
                environment()["LANG"] = "C.UTF-8"
                environment()["PATH"] =
                    "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
            }
            .start()
    }
}
