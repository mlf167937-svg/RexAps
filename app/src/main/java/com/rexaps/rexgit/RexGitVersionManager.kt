package com.rexaps.rexgit

import java.io.File
import java.time.LocalDate

class RexGitVersionManager {

    fun readVersion(
        repository: RexGitRepository
    ): RexGitVersion? {

        val file =
            findGradleFile(
                File(repository.path)
            ) ?: return null

        val text =
            file.readText()

        val name =
            Regex(
                """versionName\s*=\s*"([^"]+)""""
            )
                .find(text)
                ?.groupValues
                ?.getOrNull(1)
                ?: return null

        val code =
            Regex(
                """versionCode\s*=\s*(\d+)"""
            )
                .find(text)
                ?.groupValues
                ?.getOrNull(1)
                ?.toIntOrNull()
                ?: return null

        return RexGitVersion(
            name,
            code
        )
    }

    fun bump(
        repository: RexGitRepository
    ): RexGitVersion? {

        val file =
            findGradleFile(
                File(repository.path)
            ) ?: return null

        val text =
            file.readText()

        val nameMatch =
            Regex(
                """versionName\s*=\s*"([^"]+)""""
            ).find(text)
                ?: return null

        val codeMatch =
            Regex(
                """versionCode\s*=\s*(\d+)"""
            ).find(text)
                ?: return null

        val oldName =
            nameMatch.groupValues[1]

        val oldCode =
            codeMatch
                .groupValues[1]
                .toInt()

        val parts =
            oldName.split(".")

        val oldMajor =
            parts
                .getOrNull(0)
                ?.toIntOrNull()
                ?: 0

        val oldMinor =
            parts
                .getOrNull(1)
                ?.toIntOrNull()
                ?: 0

        val newMajor: Int
        var newMinor = oldMinor + 1

        if (oldMajor == 0) {
            newMajor =
                LocalDate.now()
                    .year
                    .rem(100)
        } else {
            if (newMinor > 99) {
                newMinor = 1
                newMajor = oldMajor + 1
            } else {
                newMajor = oldMajor
            }
        }

        if (newMinor > 99) {
            newMinor = 1
        }

        val newName =
            "$newMajor.${"%02d".format(newMinor)}"

        val newCode =
            if (oldMajor == 0) {
                newMajor * 100 + newMinor
            } else {
                newMajor * 100 + newMinor
            }

        var updated =
            text.replace(
                nameMatch.value,
                """versionName = "$newName""""
            )

        updated =
            updated.replace(
                codeMatch.value,
                "versionCode = $newCode"
            )

        file.writeText(updated)

        return RexGitVersion(
            newName,
            newCode
        )
    }

    private fun findGradleFile(
        root: File
    ): File? {

        val candidates =
            listOf(
                File(
                    root,
                    "app/build.gradle.kts"
                ),
                File(
                    root,
                    "build.gradle.kts"
                ),
                File(
                    root,
                    "app/build.gradle"
                ),
                File(
                    root,
                    "build.gradle"
                )
            )

        return candidates
            .firstOrNull { it.isFile }
    }
}
