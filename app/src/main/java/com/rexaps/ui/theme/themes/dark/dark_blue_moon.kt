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
    radiusXs = 5, radiusSm = 9, radiusMd = 13, radiusLg = 18, radiusXl = 24,
    blurSm = 7, blurMd = 14, blurLg = 22,
    shadowSm = 2f, shadowMd = 7f, shadowLg = 16f, shadowGlow = 18f,
    borderHairline = 0.6f, borderThin = 1.0f, borderThick = 2.0f,
    fontFamilyUI = "sans-serif", fontFamilyMono = "monospace",
    fontDisplay = 34, fontTitle = 23, fontHeading = 18, fontBody = 14, fontCaption = 12, fontCode = 13,
    weightRegular = 400, weightMedium = 500, weightSemibold = 600, weightBold = 700,
    lineHeightTight = 1.22f, lineHeightNormal = 1.5f, lineHeightLoose = 1.68f,
    motionInstant = 70, motionFast = 145, motionNormal = 245, motionSlow = 380, motionSlower = 560, motionAmbient = 16000,
    easingStandard = "cubic-bezier(0.20,0.00,0.00,1.00)", easingEnter = "cubic-bezier(0.00,0.00,0.20,1.00)", easingExit = "cubic-bezier(0.40,0.00,1.00,1.00)",
    gradientPrimary = listOf(argb("3B82F6"), argb("60A5FA"), argb("93C5FD")),
    gradientSecondary = listOf(argb("050A14"), argb("0D1526")),
    gradientBorder = listOf(argb("1B2A47"), argb("93C5FD"), argb("3B82F6")),
    signature = "moon phases shift slowly during use",
    backgroundMotion = "dark ambient: moon phases shift slowly during use",
    transitionMotion = "Dark transition: moon phases shift slowly during use; use a composed fade/transform entrance with a soft eased settle",
    interactionMotion = "precision blue focus ring, soft hover lift, spring press to 0.965 scale, and a calm radial ripple",
    loadingMotion = "Dark loading: low-contrast metallic sweep over a breathing skeleton, with a short accent-color progress fill",
    notificationMotion = "dark notification in #93c5fd: quick fade-slide, soft surface bloom, then a signature pulse"
)
