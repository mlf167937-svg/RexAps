package com.rexaps.ui.theme.themes.dark

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Dark Onyx Moon: white ink spreads subtly from the tap point. */
val dark_onyx_moon = RexDesignTokens(
    id = "dark_onyx_moon", name = "Dark Onyx Moon", category = "Dark",
    primary = argb("E5E7EB"), primaryHover = blend(argb("E5E7EB"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("E5E7EB"), 0xFF000000L, .12f), onPrimary = argb("10131A"),
    secondary = argb("9CA3AF"), accent = argb("FFFFFF"),
    background = argb("000000"), backgroundAlt = argb("0B0B0C"),
    surface = argb("FF0B0B0C"), surfaceElevated = argb("FF1A1A1B"), surfaceHover = argb("FF232324"),
    text = argb("F5F5F5"), textMuted = argb("FFAAB8CC"), textDisabled = argb("FF68758A"),
    border = argb("222224"), borderStrong = blend(argb("222224"), argb("FFFFFF"), .45f),
    glow = argb("E5E7EB"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("E5E7EB"), argb("000000"), .35f), scrollbar = argb("222224"),
    opacitySurface = .98f,
    noiseIntensity = .02f,
    gradientPrimary = listOf(argb("E5E7EB"), argb("9CA3AF"), argb("FFFFFF")),
    gradientSecondary = listOf(argb("000000"), argb("0B0B0C")),
    gradientBorder = listOf(argb("222224"), argb("FFFFFF"), argb("E5E7EB")),
    signature = "white ink spreads subtly from the tap point",
    backgroundMotion = "dark ambient: white ink spreads subtly from the tap point",
    transitionMotion = "Dark transition: white ink spreads subtly from the tap point",
    interactionMotion = "theme-colored ripple, 0.97 press scale, 1.5dp lift and accent glow",
    loadingMotion = "dark shimmer and gradient progress with moving highlight",
    notificationMotion = "dark toast with e5e7eb accent and brief signature pulse"
)
