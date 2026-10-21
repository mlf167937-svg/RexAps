package com.rexaps.ui.theme.themes.glass

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Glass Hollow Purple: blue and red orbs merge into a pulsing violet core. */
val glass_hollow_purple = RexDesignTokens(
    id = "glass_hollow_purple", name = "Glass Hollow Purple", category = "Glass",
    primary = argb("B06FF2"), primaryHover = blend(argb("B06FF2"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("B06FF2"), 0xFF000000L, .12f), onPrimary = argb("10131A"),
    secondary = argb("4F8CFF"), accent = argb("FF3B6B"),
    background = argb("0C0716"), backgroundAlt = argb("170A2E"),
    surface = argb("FF170A2E"), surfaceElevated = argb("FF25193B"), surfaceHover = argb("FF2E2243"),
    text = argb("F1EAFF"), textMuted = argb("FFAAB8CC"), textDisabled = argb("FF68758A"),
    border = argb("B06FF2"), borderStrong = blend(argb("B06FF2"), argb("FF3B6B"), .45f),
    glow = argb("B06FF2"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("B06FF2"), argb("0C0716"), .35f), scrollbar = argb("B06FF2"),
    opacitySurface = .08f,
    noiseIntensity = .03f,
    gradientPrimary = listOf(argb("B06FF2"), argb("4F8CFF"), argb("FF3B6B")),
    gradientSecondary = listOf(argb("0C0716"), argb("170A2E")),
    gradientBorder = listOf(argb("B06FF2"), argb("FF3B6B"), argb("B06FF2")),
    signature = "blue and red orbs merge into a pulsing violet core",
    backgroundMotion = "glass ambient: blue and red orbs merge into a pulsing violet core",
    transitionMotion = "Glass transition: blue and red orbs merge into a pulsing violet core",
    interactionMotion = "theme-colored ripple, 0.97 press scale, 1.5dp lift and accent glow",
    loadingMotion = "glass shimmer and gradient progress with moving highlight",
    notificationMotion = "glass toast with 8a2be2 accent and brief signature pulse"
)
