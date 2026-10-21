package com.rexaps.ui.theme.themes.glass

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Glass Blue Moon: moonlight reflection sweeps across cards. */
val glass_blue_moon = RexDesignTokens(
    id = "glass_blue_moon", name = "Glass Blue Moon", category = "Glass",
    primary = argb("4F8CFF"), primaryHover = blend(argb("4F8CFF"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("4F8CFF"), 0xFF000000L, .12f), onPrimary = argb("10131A"),
    secondary = argb("7AA7FF"), accent = argb("A5C8FF"),
    background = argb("0B1220"), backgroundAlt = argb("101C36"),
    surface = argb("FF101C36"), surfaceElevated = argb("FF1E2A42"), surfaceHover = argb("FF28334A"),
    text = argb("EAF2FF"), textMuted = argb("FFAAB8CC"), textDisabled = argb("FF68758A"),
    border = argb("A5C8FF"), borderStrong = blend(argb("A5C8FF"), argb("A5C8FF"), .45f),
    glow = argb("4F8CFF"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("4F8CFF"), argb("0B1220"), .35f), scrollbar = argb("A5C8FF"),
    opacitySurface = .08f,
    noiseIntensity = .03f,
    gradientPrimary = listOf(argb("4F8CFF"), argb("7AA7FF"), argb("A5C8FF")),
    gradientSecondary = listOf(argb("0B1220"), argb("101C36")),
    gradientBorder = listOf(argb("A5C8FF"), argb("A5C8FF"), argb("4F8CFF")),
    signature = "moonlight reflection sweeps across cards",
    backgroundMotion = "glass ambient: moonlight reflection sweeps across cards",
    transitionMotion = "Glass transition: moonlight reflection sweeps across cards",
    interactionMotion = "theme-colored ripple, 0.97 press scale, 1.5dp lift and accent glow",
    loadingMotion = "glass shimmer and gradient progress with moving highlight",
    notificationMotion = "glass toast with 4f8cff accent and brief signature pulse"
)
