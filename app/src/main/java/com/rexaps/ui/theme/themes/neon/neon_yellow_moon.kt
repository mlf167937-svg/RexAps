package com.rexaps.ui.theme.themes.neon

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Neon Yellow Moon: electric current travels along panel dividers. */
val neon_yellow_moon = RexDesignTokens(
    id = "neon_yellow_moon", name = "Neon Yellow Moon", category = "Neon",
    primary = argb("FFEE00"), primaryHover = blend(argb("FFEE00"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("FFEE00"), 0xFF000000L, .12f), onPrimary = argb("10131A"),
    secondary = argb("FFC400"), accent = argb("FFF59D"),
    background = argb("0A0900"), backgroundAlt = argb("15130A"),
    surface = argb("FF15130A"), surfaceElevated = argb("FF232119"), surfaceHover = argb("FF2C2B22"),
    text = argb("FFFBD6"), textMuted = argb("FFAAB8CC"), textDisabled = argb("FF68758A"),
    border = argb("FFEE00"), borderStrong = blend(argb("FFEE00"), argb("FFF59D"), .45f),
    glow = argb("FFEE00"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("FFEE00"), argb("0A0900"), .35f), scrollbar = argb("FFEE00"),
    opacitySurface = .98f,
    noiseIntensity = .02f,
    gradientPrimary = listOf(argb("FFEE00"), argb("FFC400"), argb("FFF59D")),
    gradientSecondary = listOf(argb("0A0900"), argb("15130A")),
    gradientBorder = listOf(argb("FFEE00"), argb("FFF59D"), argb("FFEE00")),
    signature = "electric current travels along panel dividers",
    backgroundMotion = "neon ambient: electric current travels along panel dividers",
    transitionMotion = "Neon transition: electric current travels along panel dividers",
    interactionMotion = "theme-colored ripple, 0.97 press scale, 1.5dp lift and accent glow",
    loadingMotion = "neon shimmer and gradient progress with moving highlight",
    notificationMotion = "neon toast with ffee00 accent and brief signature pulse"
)
