package com.rexaps.ui.theme.themes.light

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Light Purple Moon: pastel stars twinkle faintly. */
val light_purple_moon = RexDesignTokens(
    id = "light_purple_moon", name = "Light Purple Moon", category = "Light",
    primary = argb("7C3AED"), primaryHover = blend(argb("7C3AED"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("7C3AED"), 0xFF000000L, .12f), onPrimary = argb("FFFFFF"),
    secondary = argb("A78BFA"), accent = argb("DDD6FE"),
    background = argb("F9F6FF"), backgroundAlt = argb("FFFFFF"),
    surface = argb("FFFFFFFF"), surfaceElevated = argb("FFFFFFFF"), surfaceHover = argb("FFF1F6FF"),
    text = argb("1E1033"), textMuted = argb("FF526078"), textDisabled = argb("FF8792A6"),
    border = argb("E4DAFA"), borderStrong = blend(argb("E4DAFA"), argb("DDD6FE"), .45f),
    glow = argb("7C3AED"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("7C3AED"), argb("F9F6FF"), .35f), scrollbar = argb("E4DAFA"),
    opacitySurface = .98f,
    noiseIntensity = .01f,
    gradientPrimary = listOf(argb("7C3AED"), argb("A78BFA"), argb("DDD6FE")),
    gradientSecondary = listOf(argb("F9F6FF"), argb("FFFFFF")),
    gradientBorder = listOf(argb("E4DAFA"), argb("DDD6FE"), argb("7C3AED")),
    signature = "pastel stars twinkle faintly",
    backgroundMotion = "light ambient: pastel stars twinkle faintly",
    transitionMotion = "Light transition: pastel stars twinkle faintly",
    interactionMotion = "theme-colored ripple, 0.97 press scale, 1.5dp lift and accent glow",
    loadingMotion = "light shimmer and gradient progress with moving highlight",
    notificationMotion = "light toast with 7c3aed accent and brief signature pulse"
)
