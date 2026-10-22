package com.rexaps.ui.theme.themes.cosmic

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Nebula Void Moon: miniature black hole bends nearby light. */
val nebula_void_moon = RexDesignTokens(
    id = "nebula_void_moon", name = "Nebula Void Moon", category = "Cosmic",
    primary = argb("B24BF3"), primaryHover = blend(argb("B24BF3"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("B24BF3"), 0xFF000000L, .12f), onPrimary = argb("10131A"),
    secondary = argb("FF3D8B"), accent = argb("5EEAD4"),
    background = argb("060211"), backgroundAlt = argb("180A2C"),
    surface = argb("FF180A2C"), surfaceElevated = argb("FF261939"), surfaceHover = argb("FF2F2241"),
    text = argb("F2E9FF"), textMuted = argb("FFAAB8CC"), textDisabled = argb("FF68758A"),
    border = argb("B24BF3"), borderStrong = blend(argb("B24BF3"), argb("5EEAD4"), .45f),
    glow = argb("B24BF3"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("B24BF3"), argb("060211"), .35f), scrollbar = argb("B24BF3"),
    opacitySurface = .08f,
    noiseIntensity = .02f,
    radiusXs = 16, radiusSm = 20, radiusMd = 26, radiusLg = 32, radiusXl = 40,
    blurSm = 14, blurMd = 26, blurLg = 38,
    shadowSm = 3f, shadowMd = 11f, shadowLg = 24f, shadowGlow = 32f,
    borderHairline = 0.5f, borderThin = 1.0f, borderThick = 2.0f,
    fontFamilyUI = "sans-serif-rounded", fontFamilyMono = "monospace",
    fontDisplay = 38, fontTitle = 26, fontHeading = 20, fontBody = 14, fontCaption = 12, fontCode = 13,
    weightRegular = 400, weightMedium = 500, weightSemibold = 600, weightBold = 700,
    lineHeightTight = 1.26f, lineHeightNormal = 1.56f, lineHeightLoose = 1.78f,
    motionInstant = 92, motionFast = 185, motionNormal = 325, motionSlow = 495, motionSlower = 730, motionAmbient = 23500,
    easingStandard = "cubic-bezier(0.16,1.00,0.30,1.00)", easingEnter = "cubic-bezier(0.16,1.00,0.30,1.00)", easingExit = "cubic-bezier(0.40,0.00,1.00,1.00)",
    gradientPrimary = listOf(argb("B24BF3"), argb("FF3D8B"), argb("5EEAD4")),
    gradientSecondary = listOf(argb("060211"), argb("180A2C")),
    gradientBorder = listOf(argb("B24BF3"), argb("5EEAD4"), argb("B24BF3")),
    signature = "miniature black hole bends nearby light",
    backgroundMotion = "cosmic ambient: miniature black hole bends nearby light",
    transitionMotion = "Cosmic transition: miniature black hole bends nearby light; use a composed fade/transform entrance with a soft eased settle",
    interactionMotion = "nebula colors crossfade on hover, the focus ring blooms softly, and taps release like a floating particle",
    loadingMotion = "Cosmic loading: slow aurora shimmer, orbiting progress highlight, and softly staggered star-like item reveals",
    notificationMotion = "cosmic notification in #5eead4: quick fade-slide, soft surface bloom, then a signature pulse"
)
