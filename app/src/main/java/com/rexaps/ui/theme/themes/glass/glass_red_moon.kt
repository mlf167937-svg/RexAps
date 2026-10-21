package com.rexaps.ui.theme.themes.glass

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Glass Red Moon: red eclipse passes behind the header every 30 seconds. */
val glass_red_moon = RexDesignTokens(
    id = "glass_red_moon", name = "Glass Red Moon", category = "Glass",
    primary = argb("FF4D6D"), primaryHover = blend(argb("FF4D6D"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("FF4D6D"), 0xFF000000L, .12f), onPrimary = argb("10131A"),
    secondary = argb("FF7A90"), accent = argb("FFB3C1"),
    background = argb("1A0A10"), backgroundAlt = argb("2A0F1A"),
    surface = argb("FF2A0F1A"), surfaceElevated = argb("FF371D28"), surfaceHover = argb("FF3F2731"),
    text = argb("FFEFF2"), textMuted = argb("FFAAB8CC"), textDisabled = argb("FF68758A"),
    border = argb("FFB3C1"), borderStrong = blend(argb("FFB3C1"), argb("FFB3C1"), .45f),
    glow = argb("FF4D6D"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("FF4D6D"), argb("1A0A10"), .35f), scrollbar = argb("FFB3C1"),
    opacitySurface = .08f,
    noiseIntensity = .03f,
    gradientPrimary = listOf(argb("FF4D6D"), argb("FF7A90"), argb("FFB3C1")),
    gradientSecondary = listOf(argb("1A0A10"), argb("2A0F1A")),
    gradientBorder = listOf(argb("FFB3C1"), argb("FFB3C1"), argb("FF4D6D")),
    signature = "red eclipse passes behind the header every 30 seconds",
    backgroundMotion = "glass ambient: red eclipse passes behind the header every 30 seconds",
    transitionMotion = "Glass transition: red eclipse passes behind the header every 30 seconds",
    interactionMotion = "theme-colored ripple, 0.97 press scale, 1.5dp lift and accent glow",
    loadingMotion = "glass shimmer and gradient progress with moving highlight",
    notificationMotion = "glass toast with ff4d6d accent and brief signature pulse"
)
