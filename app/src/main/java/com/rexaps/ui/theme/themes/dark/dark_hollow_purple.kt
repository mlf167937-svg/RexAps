package com.rexaps.ui.theme.themes.dark

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Dark Hollow Purple: violet core emits a thin wave after important actions. */
val dark_hollow_purple = RexDesignTokens(
    id = "dark_hollow_purple", name = "Dark Hollow Purple", category = "Dark",
    primary = argb("A67CFF"), primaryHover = blend(argb("A67CFF"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("A67CFF"), 0xFF000000L, .12f), onPrimary = argb("10131A"),
    secondary = argb("3B82F6"), accent = argb("F43F5E"),
    background = argb("07040F"), backgroundAlt = argb("120A22"),
    surface = argb("FF120A22"), surfaceElevated = argb("FF20192F"), surfaceHover = argb("FF2A2238"),
    text = argb("EEE8FF"), textMuted = argb("FFAAB8CC"), textDisabled = argb("FF68758A"),
    border = argb("2A1650"), borderStrong = blend(argb("2A1650"), argb("F43F5E"), .45f),
    glow = argb("A67CFF"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("A67CFF"), argb("07040F"), .35f), scrollbar = argb("2A1650"),
    opacitySurface = .98f,
    noiseIntensity = .02f,
    gradientPrimary = listOf(argb("A67CFF"), argb("3B82F6"), argb("F43F5E")),
    gradientSecondary = listOf(argb("07040F"), argb("120A22")),
    gradientBorder = listOf(argb("2A1650"), argb("F43F5E"), argb("A67CFF")),
    signature = "violet core emits a thin wave after important actions",
    backgroundMotion = "dark ambient: violet core emits a thin wave after important actions",
    transitionMotion = "Dark transition: violet core emits a thin wave after important actions",
    interactionMotion = "theme-colored ripple, 0.97 press scale, 1.5dp lift and accent glow",
    loadingMotion = "dark shimmer and gradient progress with moving highlight",
    notificationMotion = "dark toast with 7c3aed accent and brief signature pulse"
)
