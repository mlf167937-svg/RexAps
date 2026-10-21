package com.rexaps.ui.theme.themes.light

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Light Red Moon: rounded pink petals drift gently downward. */
val light_red_moon = RexDesignTokens(
    id = "light_red_moon", name = "Light Red Moon", category = "Light",
    primary = argb("DC2626"), primaryHover = blend(argb("DC2626"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("DC2626"), 0xFF000000L, .12f), onPrimary = argb("FFFFFF"),
    secondary = argb("F87171"), accent = argb("FECACA"),
    background = argb("FFF6F6"), backgroundAlt = argb("FFFFFF"),
    surface = argb("FFFFFFFF"), surfaceElevated = argb("FFFFFFFF"), surfaceHover = argb("FFF1F6FF"),
    text = argb("2A0E0E"), textMuted = argb("FF526078"), textDisabled = argb("FF8792A6"),
    border = argb("F6D4D4"), borderStrong = blend(argb("F6D4D4"), argb("FECACA"), .45f),
    glow = argb("DC2626"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("DC2626"), argb("FFF6F6"), .35f), scrollbar = argb("F6D4D4"),
    opacitySurface = .98f,
    noiseIntensity = .01f,
    gradientPrimary = listOf(argb("DC2626"), argb("F87171"), argb("FECACA")),
    gradientSecondary = listOf(argb("FFF6F6"), argb("FFFFFF")),
    gradientBorder = listOf(argb("F6D4D4"), argb("FECACA"), argb("DC2626")),
    signature = "rounded pink petals drift gently downward",
    backgroundMotion = "light ambient: rounded pink petals drift gently downward",
    transitionMotion = "Light transition: rounded pink petals drift gently downward",
    interactionMotion = "theme-colored ripple, 0.97 press scale, 1.5dp lift and accent glow",
    loadingMotion = "light shimmer and gradient progress with moving highlight",
    notificationMotion = "light toast with dc2626 accent and brief signature pulse"
)
