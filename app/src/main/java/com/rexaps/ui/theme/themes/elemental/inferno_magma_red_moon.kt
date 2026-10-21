package com.rexaps.ui.theme.themes.elemental

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Inferno Magma Red Moon: lava fissures glow in a slow breathing rhythm. */
val inferno_magma_red_moon = RexDesignTokens(
    id = "inferno_magma_red_moon", name = "Inferno Magma Red Moon", category = "Elemental",
    primary = argb("FF3B1F"), primaryHover = blend(argb("FF3B1F"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("FF3B1F"), 0xFF000000L, .12f), onPrimary = argb("10131A"),
    secondary = argb("FF7A1A"), accent = argb("FFB347"),
    background = argb("120505"), backgroundAlt = argb("1F0A07"),
    surface = argb("FF1F0A07"), surfaceElevated = argb("FF2C1916"), surfaceHover = argb("FF352220"),
    text = argb("FFEFE6"), textMuted = argb("FFAAB8CC"), textDisabled = argb("FF68758A"),
    border = argb("FF7A1A"), borderStrong = blend(argb("FF7A1A"), argb("FFB347"), .45f),
    glow = argb("FF3B1F"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("FF3B1F"), argb("120505"), .35f), scrollbar = argb("FF7A1A"),
    opacitySurface = .08f,
    noiseIntensity = .02f,
    gradientPrimary = listOf(argb("FF3B1F"), argb("FF7A1A"), argb("FFB347")),
    gradientSecondary = listOf(argb("120505"), argb("1F0A07")),
    gradientBorder = listOf(argb("FF7A1A"), argb("FFB347"), argb("FF3B1F")),
    signature = "lava fissures glow in a slow breathing rhythm",
    backgroundMotion = "elemental ambient: lava fissures glow in a slow breathing rhythm",
    transitionMotion = "Elemental transition: lava fissures glow in a slow breathing rhythm",
    interactionMotion = "theme-colored ripple, 0.97 press scale, 1.5dp lift and accent glow",
    loadingMotion = "elemental shimmer and gradient progress with moving highlight",
    notificationMotion = "elemental toast with ff3b1f accent and brief signature pulse"
)
