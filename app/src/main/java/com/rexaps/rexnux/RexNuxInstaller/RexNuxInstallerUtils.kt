package com.rexaps.rexnux

import java.io.File

internal fun RexNuxInstaller.__split_safeResolve_7(
        base: File,
        entryName: String
    ): File {
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
