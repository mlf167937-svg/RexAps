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
    radiusXs = 9, radiusSm = 13, radiusMd = 18, radiusLg = 24, radiusXl = 30,
    blurSm = 9, blurMd = 18, blurLg = 27,
    shadowSm = 2f, shadowMd = 8f, shadowLg = 18f, shadowGlow = 24f,
    borderHairline = 0.5f, borderThin = 1.0f, borderThick = 2.0f,
    fontFamilyUI = "sans-serif", fontFamilyMono = "monospace",
    fontDisplay = 36, fontTitle = 24, fontHeading = 19, fontBody = 14, fontCaption = 12, fontCode = 13,
    weightRegular = 400, weightMedium = 500, weightSemibold = 600, weightBold = 700,
    lineHeightTight = 1.24f, lineHeightNormal = 1.52f, lineHeightLoose = 1.72f,
    motionInstant = 76, motionFast = 155, motionNormal = 270, motionSlow = 420, motionSlower = 620, motionAmbient = 19000,
    easingStandard = "cubic-bezier(0.16,1.00,0.30,1.00)", easingEnter = "cubic-bezier(0.16,1.00,0.30,1.00)", easingExit = "cubic-bezier(0.40,0.00,1.00,1.00)",
    gradientPrimary = listOf(argb("A67CFF"), argb("3B82F6"), argb("F43F5E")),
    gradientSecondary = listOf(argb("07040F"), argb("120A22")),
    gradientBorder = listOf(argb("2A1650"), argb("F43F5E"), argb("A67CFF")),
    signature = "violet core emits a thin wave after important actions",
    backgroundMotion = "dark ambient: violet core emits a thin wave after important actions",
    transitionMotion = "Dark transition: violet core emits a thin wave after important actions; use a composed fade/transform entrance with a soft eased settle",
    interactionMotion = "violet edge bloom, layered hover lift, spring press scale, and a brief velvet-glow ripple",
    loadingMotion = "Dark loading: low-contrast metallic sweep over a breathing skeleton, with a short accent-color progress fill",
    notificationMotion = "dark notification in #f43f5e: quick fade-slide, soft surface bloom, then a signature pulse"
)
