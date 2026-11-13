package com.rexaps.rexnux

enum class RexNuxDistro(
    val id: String,
    val title: String,
    val description: String,
    val defaultShell: String = "/bin/sh",
    val packageManager: String = "apk"
) {
    ARCH(
        id = "arch",
        title = "Arch Linux ARM64",
        description = "Flexible rolling-release system dengan pacman",
        defaultShell = "/bin/bash",
        packageManager = "pacman"
    ),
    ALPINE(
        id = "alpine",
        title = "Alpine Linux ARM64",
        description = "Minimalis, cepat, dan ringan berbasis musl libc & apk",
        defaultShell = "/bin/sh",
        packageManager = "apk"
    ),
    DEBIAN(
        id = "debian",
        title = "Debian 12 (Bookworm) ARM64",
        description = "Sangat stabil dengan ekosistem APT & dpkg yang luas",
        defaultShell = "/bin/bash",
        packageManager = "apt"
    ),
    UBUNTU(
        id = "ubuntu",
        title = "Ubuntu 24.04 LTS (Noble) ARM64",
        description = "Distro populer dengan repositori komprehensif & apt",
        defaultShell = "/bin/bash",
        packageManager = "apt"
    ),
    KALI(
        id = "kali",
        title = "Kali Linux ARM64",
        description = "Platform keamanan siber, analisis, dan penetrasi",
        defaultShell = "/bin/bash",
        packageManager = "apt"
    )
}

data class DistroMetadata(
    val distroId: String,
    val installedAt: Long = System.currentTimeMillis(),
    val sizeBytes: Long = 0L,
    val version: String = ""
)

data class RexNuxUiState(
    val installedDistros: Set<RexNuxDistro> = emptySet(),
    val activeDistro: RexNuxDistro? = null,
    val selectedDistro: RexNuxDistro? = null,
    val downloading: Boolean = false,
    val progress: Float = 0f,
    val message: String = "",
    val error: String? = null,
    val showDownloadScreen: Boolean = false,
    val showSettings: Boolean = false,
    val showTerminal: Boolean = false,
    val distroSizes: Map<RexNuxDistro, String> = emptyMap()
)
