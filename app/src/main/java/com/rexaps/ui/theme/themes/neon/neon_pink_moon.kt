package com.rexaps.ui.theme.themes.neon

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Neon Pink Moon: subtle VHS tracking line appears on screen changes. */
val neon_pink_moon = RexDesignTokens(
    id = "neon_pink_moon", name = "Neon Pink Moon", category = "Neon",
    primary = argb("FF2BD6"), primaryHover = blend(argb("FF2BD6"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("FF2BD6"), 0xFF000000L, .12f), onPrimary = argb("10131A"),
    secondary = argb("FF5FA2"), accent = argb("FFB0F0"),
    background = argb("0A030A"), backgroundAlt = argb("16081A"),
    surface = argb("FF16081A"), surfaceElevated = argb("FF241728"), surfaceHover = argb("FF2D2131"),
    text = argb("FFE6FA"), textMuted = argb("FFAAB8CC"), textDisabled = argb("FF68758A"),
    border = argb("FF2BD6"), borderStrong = blend(argb("FF2BD6"), argb("FFB0F0"), .45f),
    glow = argb("FF2BD6"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("FF2BD6"), argb("0A030A"), .35f), scrollbar = argb("FF2BD6"),
    opacitySurface = .98f,
    noiseIntensity = .02f,
    gradientPrimary = listOf(argb("FF2BD6"), argb("FF5FA2"), argb("FFB0F0")),
    gradientSecondary = listOf(argb("0A030A"), argb("16081A")),
    gradientBorder = listOf(argb("FF2BD6"), argb("FFB0F0"), argb("FF2BD6")),
    signature = "subtle VHS tracking line appears on screen changes",
    backgroundMotion = "neon ambient: subtle VHS tracking line appears on screen changes",
    transitionMotion = "Neon transition: subtle VHS tracking line appears on screen changes",
    interactionMotion = "theme-colored ripple, 0.97 press scale, 1.5dp lift and accent glow",
    loadingMotion = "neon shimmer and gradient progress with moving highlight",
    notificationMotion = "neon toast with ff2bd6 accent and brief signature pulse"
)
