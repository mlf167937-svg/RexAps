package com.rexaps.ui.theme.themes.glass

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Glass Gold Moon: moving gold-foil highlight tracks pointer or tilt. */
val glass_gold_moon = RexDesignTokens(
    id = "glass_gold_moon", name = "Glass Gold Moon", category = "Glass",
    primary = argb("F5C451"), primaryHover = blend(argb("F5C451"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("F5C451"), 0xFF000000L, .12f), onPrimary = argb("10131A"),
    secondary = argb("FFD97A"), accent = argb("FFEBB0"),
    background = argb("17120A"), backgroundAlt = argb("2A200E"),
    surface = argb("FF2A200E"), surfaceElevated = argb("FF372D1C"), surfaceHover = argb("FF3F3626"),
    text = argb("FFF8E6"), textMuted = argb("FFAAB8CC"), textDisabled = argb("FF68758A"),
    border = argb("FFEBB0"), borderStrong = blend(argb("FFEBB0"), argb("FFEBB0"), .45f),
    glow = argb("F5C451"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("F5C451"), argb("17120A"), .35f), scrollbar = argb("FFEBB0"),
    opacitySurface = .08f,
    noiseIntensity = .03f,
    gradientPrimary = listOf(argb("F5C451"), argb("FFD97A"), argb("FFEBB0")),
    gradientSecondary = listOf(argb("17120A"), argb("2A200E")),
    gradientBorder = listOf(argb("FFEBB0"), argb("FFEBB0"), argb("F5C451")),
    signature = "moving gold-foil highlight tracks pointer or tilt",
    backgroundMotion = "glass ambient: moving gold-foil highlight tracks pointer or tilt",
    transitionMotion = "Glass transition: moving gold-foil highlight tracks pointer or tilt",
    interactionMotion = "theme-colored ripple, 0.97 press scale, 1.5dp lift and accent glow",
    loadingMotion = "glass shimmer and gradient progress with moving highlight",
    notificationMotion = "glass toast with f5c451 accent and brief signature pulse"
)
