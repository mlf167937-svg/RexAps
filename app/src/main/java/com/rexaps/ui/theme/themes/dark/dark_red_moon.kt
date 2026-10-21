package com.rexaps.ui.theme.themes.dark

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Dark Red Moon: main emblem has a restrained slow heartbeat pulse. */
val dark_red_moon = RexDesignTokens(
    id = "dark_red_moon", name = "Dark Red Moon", category = "Dark",
    primary = argb("EF4444"), primaryHover = blend(argb("EF4444"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("EF4444"), 0xFF000000L, .12f), onPrimary = argb("10131A"),
    secondary = argb("F87171"), accent = argb("FCA5A5"),
    background = argb("0F0506"), backgroundAlt = argb("1A0A0C"),
    surface = argb("FF1A0A0C"), surfaceElevated = argb("FF28191B"), surfaceHover = argb("FF312224"),
    text = argb("FBEAEA"), textMuted = argb("FFAAB8CC"), textDisabled = argb("FF68758A"),
    border = argb("3A1518"), borderStrong = blend(argb("3A1518"), argb("FCA5A5"), .45f),
    glow = argb("EF4444"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("EF4444"), argb("0F0506"), .35f), scrollbar = argb("3A1518"),
    opacitySurface = .98f,
    noiseIntensity = .02f,
    gradientPrimary = listOf(argb("EF4444"), argb("F87171"), argb("FCA5A5")),
    gradientSecondary = listOf(argb("0F0506"), argb("1A0A0C")),
    gradientBorder = listOf(argb("3A1518"), argb("FCA5A5"), argb("EF4444")),
    signature = "main emblem has a restrained slow heartbeat pulse",
    backgroundMotion = "dark ambient: main emblem has a restrained slow heartbeat pulse",
    transitionMotion = "Dark transition: main emblem has a restrained slow heartbeat pulse",
    interactionMotion = "theme-colored ripple, 0.97 press scale, 1.5dp lift and accent glow",
    loadingMotion = "dark shimmer and gradient progress with moving highlight",
    notificationMotion = "dark toast with ef4444 accent and brief signature pulse"
)
