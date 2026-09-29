package com.rexaps.rextools.ssweb

import com.rexaps.rextools.SsWebResult

data class SsWebUiState(
    val loading: Boolean = false,
    val url: String = "",
    val width: Int = 1080,
    val height: Int = 1920,
    val scale: Int = 2,
    val result: SsWebResult? = null,
    val error: String? = null
)
