package com.rexaps.rexai

import androidx.compose.runtime.Composable

@Composable
internal fun ClaudeScreen(onBack: () -> Unit) {
    RexAIWebViewScreen(
        provider = RexAIProvider.CLAUDE,
        onBack = onBack
    )
}
