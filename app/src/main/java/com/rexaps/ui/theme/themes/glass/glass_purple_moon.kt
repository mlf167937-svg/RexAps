package com.rexaps.ui.theme.themes.glass

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Glass Purple Moon: tiny stars appear around interaction points. */
val glass_purple_moon = RexDesignTokens(
    id = "glass_purple_moon", name = "Glass Purple Moon", category = "Glass",
    primary = argb("9B6BFF"), primaryHover = blend(argb("9B6BFF"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("9B6BFF"), 0xFF000000L, .12f), onPrimary = argb("10131A"),
    secondary = argb("B794FF"), accent = argb("D6C2FF"),
    background = argb("120B22"), backgroundAlt = argb("1E1238"),
    surface = argb("FF1E1238"), surfaceElevated = argb("FF2C2044"), surfaceHover = argb("FF342A4C"),
    text = argb("F3EDFF"), textMuted = argb("FFAAB8CC"), textDisabled = argb("FF68758A"),
    border = argb("D6C2FF"), borderStrong = blend(argb("D6C2FF"), argb("D6C2FF"), .45f),
    glow = argb("9B6BFF"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("9B6BFF"), argb("120B22"), .35f), scrollbar = argb("D6C2FF"),
    opacitySurface = .08f,
    noiseIntensity = .03f,
    gradientPrimary = listOf(argb("9B6BFF"), argb("B794FF"), argb("D6C2FF")),
    gradientSecondary = listOf(argb("120B22"), argb("1E1238")),
    gradientBorder = listOf(argb("D6C2FF"), argb("D6C2FF"), argb("9B6BFF")),
    signature = "tiny stars appear around interaction points",
    backgroundMotion = "glass ambient: tiny stars appear around interaction points",
    transitionMotion = "Glass transition: tiny stars appear around interaction points",
    interactionMotion = "theme-colored ripple, 0.97 press scale, 1.5dp lift and accent glow",
    loadingMotion = "glass shimmer and gradient progress with moving highlight",
    notificationMotion = "glass toast with 9b6bff accent and brief signature pulse"
)
