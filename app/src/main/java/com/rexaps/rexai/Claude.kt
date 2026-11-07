package com.rexaps.rexai

import androidx.compose.runtime.Composable

@Composable
fun ClaudeScreen(holder: RexAIWebStateHolder, onBack: () -> Unit) {
    RexAIGeckoScreen(
        providerId = "claude",
        title = "Claude",
        startUrl = "https://claude.ai/",
        holder = holder,
        onBack = onBack
    )
}
