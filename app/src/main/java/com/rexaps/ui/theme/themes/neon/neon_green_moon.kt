package com.rexaps.ui.theme.themes.neon

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Neon Green Moon: brief terminal boot sequence opens each module. */
val neon_green_moon = RexDesignTokens(
    id = "neon_green_moon", name = "Neon Green Moon", category = "Neon",
    primary = argb("39FF14"), primaryHover = blend(argb("39FF14"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("39FF14"), 0xFF000000L, .12f), onPrimary = argb("10131A"),
    secondary = argb("00E676"), accent = argb("B6FF9E"),
    background = argb("030A05"), backgroundAlt = argb("08140B"),
    surface = argb("FF08140B"), surfaceElevated = argb("FF17221A"), surfaceHover = argb("FF212C23"),
    text = argb("E6FFE9"), textMuted = argb("FFAAB8CC"), textDisabled = argb("FF68758A"),
    border = argb("39FF14"), borderStrong = blend(argb("39FF14"), argb("B6FF9E"), .45f),
    glow = argb("39FF14"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("39FF14"), argb("030A05"), .35f), scrollbar = argb("39FF14"),
    opacitySurface = .98f,
    noiseIntensity = .02f,
    gradientPrimary = listOf(argb("39FF14"), argb("00E676"), argb("B6FF9E")),
    gradientSecondary = listOf(argb("030A05"), argb("08140B")),
    gradientBorder = listOf(argb("39FF14"), argb("B6FF9E"), argb("39FF14")),
    signature = "brief terminal boot sequence opens each module",
    backgroundMotion = "neon ambient: brief terminal boot sequence opens each module",
    transitionMotion = "Neon transition: brief terminal boot sequence opens each module",
    interactionMotion = "theme-colored ripple, 0.97 press scale, 1.5dp lift and accent glow",
    loadingMotion = "neon shimmer and gradient progress with moving highlight",
    notificationMotion = "neon toast with 39ff14 accent and brief signature pulse"
)
