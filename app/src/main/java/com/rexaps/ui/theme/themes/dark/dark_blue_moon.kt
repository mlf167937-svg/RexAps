package com.rexaps.ui.theme.themes.dark

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Dark Blue Moon: moon phases shift slowly during use. */
val dark_blue_moon = RexDesignTokens(
    id = "dark_blue_moon", name = "Dark Blue Moon", category = "Dark",
    primary = argb("3B82F6"), primaryHover = blend(argb("3B82F6"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("3B82F6"), 0xFF000000L, .12f), onPrimary = argb("10131A"),
    secondary = argb("60A5FA"), accent = argb("93C5FD"),
    background = argb("050A14"), backgroundAlt = argb("0D1526"),
    surface = argb("FF0D1526"), surfaceElevated = argb("FF1C2333"), surfaceHover = argb("FF252C3C"),
    text = argb("E6EEFB"), textMuted = argb("FFAAB8CC"), textDisabled = argb("FF68758A"),
    border = argb("1B2A47"), borderStrong = blend(argb("1B2A47"), argb("93C5FD"), .45f),
    glow = argb("3B82F6"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("3B82F6"), argb("050A14"), .35f), scrollbar = argb("1B2A47"),
    opacitySurface = .98f,
    noiseIntensity = .02f,
    gradientPrimary = listOf(argb("3B82F6"), argb("60A5FA"), argb("93C5FD")),
    gradientSecondary = listOf(argb("050A14"), argb("0D1526")),
    gradientBorder = listOf(argb("1B2A47"), argb("93C5FD"), argb("3B82F6")),
    signature = "moon phases shift slowly during use",
    backgroundMotion = "dark ambient: moon phases shift slowly during use",
    transitionMotion = "Dark transition: moon phases shift slowly during use",
    interactionMotion = "theme-colored ripple, 0.97 press scale, 1.5dp lift and accent glow",
    loadingMotion = "dark shimmer and gradient progress with moving highlight",
    notificationMotion = "dark toast with 3b82f6 accent and brief signature pulse"
)
