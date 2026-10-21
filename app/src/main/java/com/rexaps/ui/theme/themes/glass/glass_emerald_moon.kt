package com.rexaps.ui.theme.themes.glass

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Glass Emerald Moon: refracted gemstone light slowly rotates behind panels. */
val glass_emerald_moon = RexDesignTokens(
    id = "glass_emerald_moon", name = "Glass Emerald Moon", category = "Glass",
    primary = argb("2ED8A3"), primaryHover = blend(argb("2ED8A3"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("2ED8A3"), 0xFF000000L, .12f), onPrimary = argb("10131A"),
    secondary = argb("5FE8BE"), accent = argb("A8F5DC"),
    background = argb("07140F"), backgroundAlt = argb("0C2219"),
    surface = argb("FF0C2219"), surfaceElevated = argb("FF1B2F27"), surfaceHover = argb("FF243830"),
    text = argb("E8FFF7"), textMuted = argb("FFAAB8CC"), textDisabled = argb("FF68758A"),
    border = argb("A8F5DC"), borderStrong = blend(argb("A8F5DC"), argb("A8F5DC"), .45f),
    glow = argb("2ED8A3"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("2ED8A3"), argb("07140F"), .35f), scrollbar = argb("A8F5DC"),
    opacitySurface = .08f,
    noiseIntensity = .03f,
    gradientPrimary = listOf(argb("2ED8A3"), argb("5FE8BE"), argb("A8F5DC")),
    gradientSecondary = listOf(argb("07140F"), argb("0C2219")),
    gradientBorder = listOf(argb("A8F5DC"), argb("A8F5DC"), argb("2ED8A3")),
    signature = "refracted gemstone light slowly rotates behind panels",
    backgroundMotion = "glass ambient: refracted gemstone light slowly rotates behind panels",
    transitionMotion = "Glass transition: refracted gemstone light slowly rotates behind panels",
    interactionMotion = "theme-colored ripple, 0.97 press scale, 1.5dp lift and accent glow",
    loadingMotion = "glass shimmer and gradient progress with moving highlight",
    notificationMotion = "glass toast with 2ed8a3 accent and brief signature pulse"
)
