package com.rexaps.rexgit

import java.io.File

class RexGitVersionManager {

    fun readVersion(repository: RexGitRepository): RexGitVersion? {
        val gradleFile = findGradleFile(File(repository.path))
            ?: return null

        val text = gradleFile.readText()

        val versionName = Regex(
            """versionName\s*=\s*"([^"]+)""""
        )
            .find(text)
            ?.groupValues
            ?.getOrNull(1)
            ?: return null

        val versionCode = Regex(
            """versionCode\s*=\s*(\d+)"""
        )
            .find(text)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
            ?: return null

        return RexGitVersion(
            versionName = versionName,
            versionCode = versionCode
        )
    }

    fun bump(
        repository: RexGitRepository
    ): RexGitVersion? {

        val gradleFile = findGradleFile(
            File(repository.path)
        ) ?: return null

        val text = gradleFile.readText()

        val nameMatch = Regex(
            """versionName\s*=\s*"([^"]+)""""
        ).find(text) ?: return null

        val codeMatch = Regex(
            """versionCode\s*=\s*(\d+)"""
        ).find(text) ?: return null

        val oldName = nameMatch.groupValues[1]
        val oldCode = codeMatch.groupValues[1].toInt()

        val parts = oldName.split(".")

        val major = parts
            .getOrNull(0)
            ?.toIntOrNull()
            ?: 0

        val minor = parts
            .getOrNull(1)
            ?.toIntOrNull()
            ?: 0

        var newMajor = major
        var newMinor = minor + 1

        if (newMinor > 99) {
            newMinor = 1
            newMajor++
        }

        val newName = "$newMajor.${"%02d".format(newMinor)}"

        val newCode = if (newMajor > 0) {
            "$newMajor${"%02d".format(newMinor)}"
                .toInt()
        } else {
            oldCode + 1
        }

        var updated = text.replace(
            nameMatch.value,
            """versionName = "$newName""""
        )

        updated = updated.replace(
            codeMatch.value,
            "versionCode = $newCode"
        )

        gradleFile.writeText(updated)

        return RexGitVersion(
            versionName = newName,
            versionCode = newCode
        )
    }

    private fun findGradleFile(root: File): File? {

        val candidates = listOf(
            File(root, "app/build.gradle.kts"),
            File(root, "build.gradle.kts"),
            File(root, "app/build.gradle"),
            File(root, "build.gradle")
        )

        return candidates.firstOrNull { it.isFile }
    }
}
