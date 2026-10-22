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
    radiusXs = 10, radiusSm = 14, radiusMd = 19, radiusLg = 25, radiusXl = 32,
    blurSm = 9, blurMd = 18, blurLg = 27,
    shadowSm = 2f, shadowMd = 7f, shadowLg = 16f, shadowGlow = 16f,
    borderHairline = 0.4f, borderThin = 1.0f, borderThick = 1.5f,
    fontFamilyUI = "sans-serif-light", fontFamilyMono = "monospace",
    fontDisplay = 35, fontTitle = 24, fontHeading = 19, fontBody = 14, fontCaption = 12, fontCode = 13,
    weightRegular = 300, weightMedium = 400, weightSemibold = 600, weightBold = 700,
    lineHeightTight = 1.22f, lineHeightNormal = 1.5f, lineHeightLoose = 1.7f,
    motionInstant = 80, motionFast = 160, motionNormal = 285, motionSlow = 435, motionSlower = 635, motionAmbient = 20500,
    easingStandard = "cubic-bezier(0.16,1.00,0.30,1.00)", easingEnter = "cubic-bezier(0.16,1.00,0.30,1.00)", easingExit = "cubic-bezier(0.40,0.00,1.00,1.00)",
    gradientPrimary = listOf(argb("D8E2F5"), argb("9FB3D9"), argb("FFFFFF")),
    gradientSecondary = listOf(argb("05070E"), argb("171B26")),
    gradientBorder = listOf(argb("D8E2F5"), argb("FFFFFF"), argb("D8E2F5")),
    signature = "constellation lines connect nearby UI elements on hover",
    backgroundMotion = "cosmic ambient: constellation lines connect nearby UI elements on hover",
    transitionMotion = "Cosmic transition: constellation lines connect nearby UI elements on hover; use a composed fade/transform entrance with a soft eased settle",
    interactionMotion = "fine star-point focus sparkle, quiet elevation, and a silver ripple fading without a flash",
    loadingMotion = "Cosmic loading: slow aurora shimmer, orbiting progress highlight, and softly staggered star-like item reveals",
    notificationMotion = "cosmic notification in #ffffff: quick fade-slide, soft surface bloom, then a signature pulse"
)
