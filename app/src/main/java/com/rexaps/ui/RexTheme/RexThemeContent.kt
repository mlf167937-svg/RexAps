package com.rexaps.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable internal fun __splitImpl_RexTheme(option: RexThemeOption, content: @Composable () -> Unit) {
    val scheme = remember(option) { option.colorScheme() }
    MaterialTheme(colorScheme=scheme, typography=rexTypography, content=content)
}
