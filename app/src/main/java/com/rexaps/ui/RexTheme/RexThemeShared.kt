package com.rexaps.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

internal fun RexPaletteSpec.__splitImpl_toColorScheme(): ColorScheme = if (dark) {
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

internal fun RexPaletteSpec.Companion.__splitImpl_of(option: RexThemeOption): RexPaletteSpec {
            if (option in RexThemeOption.curatedOptions) {
                val t = com.rexaps.ui.theme.RexThemeRegistry.get(option.labelToThemeId())
                val container = com.rexaps.ui.theme.blend(t.primary, t.background, .42f)
                val darkForeground = 0xFF10131AL
                val lightForeground = 0xFFFFFFFFL
                val onPrimary = if (com.rexaps.ui.theme.RexThemeRegistry.contrastRatio(darkForeground, t.primary) >=
                    com.rexaps.ui.theme.RexThemeRegistry.contrastRatio(lightForeground, t.primary)) darkForeground else lightForeground
                val onContainer = if (com.rexaps.ui.theme.RexThemeRegistry.contrastRatio(darkForeground, container) >=
                    com.rexaps.ui.theme.RexThemeRegistry.contrastRatio(lightForeground, container)) darkForeground else lightForeground
                return RexPaletteSpec(
                    dark = t.category != "Light", primary = t.primary, onPrimary = onPrimary,
                    container = container, onContainer = onContainer,
                    tertiary = t.secondary, background = t.background,
                    card = t.surface, muted = t.textMuted, outline = t.border, text = t.text
                )
            }
            return palettes[option] ?: palettes.getValue(RexThemeOption.DARK_BLUE_MOON)
        }

internal fun RexThemeOption.__splitImpl_labelToThemeId(): String = when(this) {
    RexThemeOption.ASTRAL_GALAXY_MOON -> "astral_galaxy_moon"
    RexThemeOption.NEBULA_VOID_MOON -> "nebula_void_moon"
    RexThemeOption.ECLIPSE_BLACK_MOON -> "eclipse_black_moon"
    RexThemeOption.STARLIGHT_SILVER_MOON -> "starlight_silver_moon"
    RexThemeOption.FROST_ICE_BLUE_MOON -> "frost_ice_blue_moon"
    RexThemeOption.INFERNO_MAGMA_RED_MOON -> "inferno_magma_red_moon"
    RexThemeOption.THUNDER_GOLD_MOON -> "thunder_gold_moon"
    RexThemeOption.ABYSSAL_OCEAN_MOON -> "abyssal_ocean_moon"
    else -> name.lowercase()
}

internal fun RexThemeStore.__splitImpl_load(): RexThemeOption {
        val saved = prefs.getString(KEY_THEME, null)
        val option = if (saved == null || saved == "BLOOD_MOON") RexThemeOption.DARK_BLUE_MOON
            else RexThemeOption.values().firstOrNull { it.name == saved } ?: RexThemeOption.DARK_BLUE_MOON
        if (option in RexThemeOption.curatedOptions) engine.setTheme(option.labelToThemeId())
        return option
    }
