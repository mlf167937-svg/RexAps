package com.rexaps.ui.theme.themes.dark

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Dark Purple Moon: fine constellation lines draw into empty areas. */
val dark_purple_moon = RexDesignTokens(
    id = "dark_purple_moon", name = "Dark Purple Moon", category = "Dark",
    primary = argb("8B5CF6"), primaryHover = blend(argb("8B5CF6"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("8B5CF6"), 0xFF000000L, .12f), onPrimary = argb("FFFFFF"),
    secondary = argb("A78BFA"), accent = argb("C4B5FD"),
    background = argb("0A0614"), backgroundAlt = argb("150E26"),
    surface = argb("FF150E26"), surfaceElevated = argb("FF231C33"), surfaceHover = argb("FF2C263C"),
    text = argb("EFE9FF"), textMuted = argb("FFAAB8CC"), textDisabled = argb("FF68758A"),
    border = argb("2C1F4D"), borderStrong = blend(argb("2C1F4D"), argb("C4B5FD"), .45f),
    glow = argb("8B5CF6"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("8B5CF6"), argb("0A0614"), .35f), scrollbar = argb("2C1F4D"),
    opacitySurface = .98f,
    noiseIntensity = .02f,
    gradientPrimary = listOf(argb("8B5CF6"), argb("A78BFA"), argb("C4B5FD")),
    gradientSecondary = listOf(argb("0A0614"), argb("150E26")),
    gradientBorder = listOf(argb("2C1F4D"), argb("C4B5FD"), argb("8B5CF6")),
    signature = "fine constellation lines draw into empty areas",
    backgroundMotion = "dark ambient: fine constellation lines draw into empty areas",
    transitionMotion = "Dark transition: fine constellation lines draw into empty areas",
    interactionMotion = "theme-colored ripple, 0.97 press scale, 1.5dp lift and accent glow",
    loadingMotion = "dark shimmer and gradient progress with moving highlight",
    notificationMotion = "dark toast with 8b5cf6 accent and brief signature pulse"
)
