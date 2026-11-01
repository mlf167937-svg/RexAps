package com.rexaps.rexai

import androidx.compose.runtime.Composable

@Composable
fun ClaudeScreen(holder: RexAIWebStateHolder, onBack: () -> Unit) {
    RexAIWebViewScreen(
        providerId = "claude",
        title = "Claude",
        startUrl = "https://claude.ai/",
        holder = holder,
        onBack = onBack
    )
}
