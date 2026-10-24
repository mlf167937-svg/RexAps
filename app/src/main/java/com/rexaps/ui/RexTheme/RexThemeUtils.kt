package com.rexaps.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

internal fun RexPaletteSpec.toColorScheme(): ColorScheme = if (dark) {
    darkColorScheme(
        primary = Color(primary), onPrimary = Color(onPrimary),
        primaryContainer = Color(container), onPrimaryContainer = Color(onContainer),
        secondary = Color(tertiary), onSecondary = Color(onPrimary),
        tertiary = Color(tertiary), background = Color(background), onBackground = Color(text),
        surface = Color(card), onSurface = Color(text), surfaceVariant = Color(card),
        onSurfaceVariant = Color(muted), outline = Color(outline), outlineVariant = Color(outline),
        surfaceContainerLowest = Color(background), surfaceContainerLow = Color(card),
        surfaceContainer = Color(card), surfaceContainerHigh = Color(card), surfaceContainerHighest = Color(outline),
        error = Color(0xFFFF6B75), onError = Color(0xFF330008),
        errorContainer = Color(0xFF57151D), onErrorContainer = Color(0xFFFFDDE0)
    )
} else {
    lightColorScheme(
        primary = Color(primary), onPrimary = Color(onPrimary),
        primaryContainer = Color(container), onPrimaryContainer = Color(onContainer),
        secondary = Color(tertiary), onSecondary = Color(text),
        tertiary = Color(tertiary), background = Color(background), onBackground = Color(text),
        surface = Color(card), onSurface = Color(text), surfaceVariant = Color(card),
        onSurfaceVariant = Color(muted), outline = Color(outline), outlineVariant = Color(outline),
        surfaceContainerLowest = Color(background), surfaceContainerLow = Color(card),
        surfaceContainer = Color(card), surfaceContainerHigh = Color(container), surfaceContainerHighest = Color(container),
        error = Color(0xFFB42335), onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFE1E4), onErrorContainer = Color(0xFF721321)
    )
}

fun RexThemeOption.colorScheme(): ColorScheme = RexThemeCache.get(this)
