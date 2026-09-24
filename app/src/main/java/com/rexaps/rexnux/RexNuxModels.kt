package com.rexaps.rexnux

enum class RexNuxDistro(
    val title: String,
    val description: String
) {
    ALPINE("Alpine Linux", "Lightweight and minimal"),
    ARCH("Arch Linux", "Flexible rolling-release system")
}

data class RexNuxUiState(
    val installedDistro: RexNuxDistro? = null,
    val selectedDistro: RexNuxDistro? = null,
    val downloading: Boolean = false,
    val progress: Float = 0f,
    val message: String = "",
    val error: String? = null,
    val showDownloadScreen: Boolean = false,
    val showSettings: Boolean = false,
    val showTerminal: Boolean = false
)
