package com.rexaps.rexai

import androidx.compose.runtime.Composable

@Composable
internal fun ChatGPTScreen(onBack: () -> Unit) {
    RexAIWebViewScreen(
        provider = RexAIProvider.CHATGPT,
        onBack = onBack
    )
}
