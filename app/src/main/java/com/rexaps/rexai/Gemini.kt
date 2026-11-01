package com.rexaps.rexai

import androidx.compose.runtime.Composable

@Composable
fun GeminiScreen(holder: RexAIWebStateHolder, onBack: () -> Unit) {
    RexAIWebViewScreen(
        providerId = "gemini",
        title = "Gemini",
        startUrl = "https://gemini.google.com/",
        holder = holder,
        onBack = onBack
    )
}
