package com.rexaps.rexgit

import java.io.File
import java.time.LocalDate

class RexGitVersionManager {

    private val nameRegex = Regex("""(versionName\s*=?\s*")([^"]+)(")""")
    private val codeRegex = Regex("""(versionCode\s*=?\s*)(\d+)""")

    fun readVersion(repository: RexGitRepository): RexGitVersion? {
        val file = findGradleFile(File(repository.path)) ?: return null
        val text = file.readText()

        val name = nameRegex.find(text)?.groupValues?.get(2) ?: return null
        val code = codeRegex.find(text)
            ?.groupValues?.get(2)?.toIntOrNull() ?: return null

        return RexGitVersion(name, code)
    }

    fun bump(repository: RexGitRepository): RexGitVersion? {
        val file = findGradleFile(File(repository.path)) ?: return null
        val text = file.readText()

        val oldName = nameRegex.find(text)?.groupValues?.get(2) ?: return null
        val oldCode = codeRegex.find(text)
            ?.groupValues?.get(2)?.toIntOrNull() ?: return null

        val parts = oldName.split(".")
        val oldMajor = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val oldMinor = parts.getOrNull(1)?.toIntOrNull() ?: 0

        var newMinor = oldMinor + 1
        val newMajor: Int

        if (oldMajor == 0) {
            newMajor = LocalDate.now().year % 100
        } else if (newMinor > 99) {
            newMinor = 1
            newMajor = oldMajor + 1
        } else {
            newMajor = oldMajor
        }

        if (newMinor > 99) newMinor = 1

        val newName = "$newMajor.${"%02d".format(newMinor)}"
        // versionCode must always increase
        val newCode = maxOf(newMajor * 100 + newMinor, oldCode + 1)

        var updated = replaceFirstMatch(text, nameRegex) {
            it.groupValues[1] + newName + it.groupValues[3]
        }

        updated = replaceFirstMatch(updated, codeRegex) {
            it.groupValues[1] + newCode
        }

        file.writeText(updated)

        return RexGitVersion(newName, newCode)
    }

    private fun replaceFirstMatch(
        input: String,
        regex: Regex,
        build: (MatchResult) -> String
    ): String {
        val match = regex.find(input) ?: return input
        return input.replaceRange(match.range, build(match))
    }

    private fun findGradleFile(root: File): File? {
        return listOf(
            File(root, "app/build.gradle.kts"),
            File(root, "build.gradle.kts"),
            File(root, "app/build.gradle"),
            File(root, "build.gradle")
        ).firstOrNull { it.isFile }
    }
}
