package com.rexaps.ui.theme.themes.neon

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Neon Cyan Moon: neon tube lettering lights up in stages. */
val neon_cyan_moon = RexDesignTokens(
    id = "neon_cyan_moon", name = "Neon Cyan Moon", category = "Neon",
    primary = argb("00F0FF"), primaryHover = blend(argb("00F0FF"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("00F0FF"), 0xFF000000L, .12f), onPrimary = argb("10131A"),
    secondary = argb("00B8D4"), accent = argb("7DF9FF"),
    background = argb("03070D"), backgroundAlt = argb("08121C"),
    surface = argb("FF08121C"), surfaceElevated = argb("FF17202A"), surfaceHover = argb("FF212A33"),
    text = argb("E0FCFF"), textMuted = argb("FFAAB8CC"), textDisabled = argb("FF68758A"),
    border = argb("00F0FF"), borderStrong = blend(argb("00F0FF"), argb("7DF9FF"), .45f),
    glow = argb("00F0FF"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("00F0FF"), argb("03070D"), .35f), scrollbar = argb("00F0FF"),
    opacitySurface = .98f,
    noiseIntensity = .02f,
    gradientPrimary = listOf(argb("00F0FF"), argb("00B8D4"), argb("7DF9FF")),
    gradientSecondary = listOf(argb("03070D"), argb("08121C")),
    gradientBorder = listOf(argb("00F0FF"), argb("7DF9FF"), argb("00F0FF")),
    signature = "neon tube lettering lights up in stages",
    backgroundMotion = "neon ambient: neon tube lettering lights up in stages",
    transitionMotion = "Neon transition: neon tube lettering lights up in stages",
    interactionMotion = "theme-colored ripple, 0.97 press scale, 1.5dp lift and accent glow",
    loadingMotion = "neon shimmer and gradient progress with moving highlight",
    notificationMotion = "neon toast with 00f0ff accent and brief signature pulse"
)
