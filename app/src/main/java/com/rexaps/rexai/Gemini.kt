package com.rexaps.rexai

import androidx.compose.runtime.Composable

@Composable
internal fun GeminiScreen(onBack: () -> Unit) {
    RexAIWebViewScreen(
        provider = RexAIProvider.GEMINI,
        onBack = onBack
    )
}
