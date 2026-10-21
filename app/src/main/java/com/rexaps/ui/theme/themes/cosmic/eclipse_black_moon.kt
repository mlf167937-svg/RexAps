package com.rexaps.ui.theme.themes.cosmic

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Eclipse Black Moon: slow eclipse cycle closes and opens around the logo. */
val eclipse_black_moon = RexDesignTokens(
    id = "eclipse_black_moon", name = "Eclipse Black Moon", category = "Cosmic",
    primary = argb("FFB020"), primaryHover = blend(argb("FFB020"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("FFB020"), 0xFF000000L, .12f), onPrimary = argb("10131A"),
    secondary = argb("FF6A00"), accent = argb("FFE08A"),
    background = argb("000000"), backgroundAlt = argb("0A0A0A"),
    surface = argb("FF0A0A0A"), surfaceElevated = argb("FF191919"), surfaceHover = argb("FF222222"),
    text = argb("F6F1E6"), textMuted = argb("FFAAB8CC"), textDisabled = argb("FF68758A"),
    border = argb("FFB020"), borderStrong = blend(argb("FFB020"), argb("FFE08A"), .45f),
    glow = argb("FFB020"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("FFB020"), argb("000000"), .35f), scrollbar = argb("FFB020"),
    opacitySurface = .08f,
    noiseIntensity = .02f,
    gradientPrimary = listOf(argb("FFB020"), argb("FF6A00"), argb("FFE08A")),
    gradientSecondary = listOf(argb("000000"), argb("0A0A0A")),
    gradientBorder = listOf(argb("FFB020"), argb("FFE08A"), argb("FFB020")),
    signature = "slow eclipse cycle closes and opens around the logo",
    backgroundMotion = "cosmic ambient: slow eclipse cycle closes and opens around the logo",
    transitionMotion = "Cosmic transition: slow eclipse cycle closes and opens around the logo",
    interactionMotion = "theme-colored ripple, 0.97 press scale, 1.5dp lift and accent glow",
    loadingMotion = "cosmic shimmer and gradient progress with moving highlight",
    notificationMotion = "cosmic toast with ffb020 accent and brief signature pulse"
)
