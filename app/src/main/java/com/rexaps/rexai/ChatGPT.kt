package com.rexaps.rexai

import androidx.compose.runtime.Composable

@Composable
fun ChatGPTScreen(holder: RexAIWebStateHolder, onBack: () -> Unit) {
    RexAIWebViewScreen(
        providerId = "chatgpt",
        title = "ChatGPT",
        startUrl = "https://chatgpt.com/",
        holder = holder,
        onBack = onBack
    )
}
