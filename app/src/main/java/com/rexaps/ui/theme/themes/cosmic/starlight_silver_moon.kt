package com.rexaps.ui.theme.themes.cosmic

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Starlight Silver Moon: constellation lines connect nearby UI elements on hover. */
val starlight_silver_moon = RexDesignTokens(
    id = "starlight_silver_moon", name = "Starlight Silver Moon", category = "Cosmic",
    primary = argb("D8E2F5"), primaryHover = blend(argb("D8E2F5"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("D8E2F5"), 0xFF000000L, .12f), onPrimary = argb("10131A"),
    secondary = argb("9FB3D9"), accent = argb("FFFFFF"),
    background = argb("05070E"), backgroundAlt = argb("171B26"),
    surface = argb("FF171B26"), surfaceElevated = argb("FF252933"), surfaceHover = argb("FF2E323C"),
    text = argb("F2F6FF"), textMuted = argb("FFAAB8CC"), textDisabled = argb("FF68758A"),
    border = argb("D8E2F5"), borderStrong = blend(argb("D8E2F5"), argb("FFFFFF"), .45f),
    glow = argb("D8E2F5"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("D8E2F5"), argb("05070E"), .35f), scrollbar = argb("D8E2F5"),
    opacitySurface = .08f,
    noiseIntensity = .02f,
    gradientPrimary = listOf(argb("D8E2F5"), argb("9FB3D9"), argb("FFFFFF")),
    gradientSecondary = listOf(argb("05070E"), argb("171B26")),
    gradientBorder = listOf(argb("D8E2F5"), argb("FFFFFF"), argb("D8E2F5")),
    signature = "constellation lines connect nearby UI elements on hover",
    backgroundMotion = "cosmic ambient: constellation lines connect nearby UI elements on hover",
    transitionMotion = "Cosmic transition: constellation lines connect nearby UI elements on hover",
    interactionMotion = "theme-colored ripple, 0.97 press scale, 1.5dp lift and accent glow",
    loadingMotion = "cosmic shimmer and gradient progress with moving highlight",
    notificationMotion = "cosmic toast with d8e2f5 accent and brief signature pulse"
)
