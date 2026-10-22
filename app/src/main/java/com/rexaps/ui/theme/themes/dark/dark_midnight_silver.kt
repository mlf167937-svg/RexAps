package com.rexaps.ui.theme.themes.dark

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Dark Midnight Silver: brushed-metal reflection follows scrolling. */
val dark_midnight_silver = RexDesignTokens(
    id = "dark_midnight_silver", name = "Dark Midnight Silver", category = "Dark",
    primary = argb("C0C8D8"), primaryHover = blend(argb("C0C8D8"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("C0C8D8"), 0xFF000000L, .12f), onPrimary = argb("10131A"),
    secondary = argb("8F9BB3"), accent = argb("E3E8F2"),
    background = argb("070A10"), backgroundAlt = argb("111622"),
    surface = argb("FF111622"), surfaceElevated = argb("FF1F242F"), surfaceHover = argb("FF292D38"),
    text = argb("EDF1F8"), textMuted = argb("FFAAB8CC"), textDisabled = argb("FF68758A"),
    border = argb("232B3D"), borderStrong = blend(argb("232B3D"), argb("E3E8F2"), .45f),
    glow = argb("C0C8D8"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("C0C8D8"), argb("070A10"), .35f), scrollbar = argb("232B3D"),
    opacitySurface = .98f,
    noiseIntensity = .02f,
    radiusXs = 3, radiusSm = 7, radiusMd = 11, radiusLg = 15, radiusXl = 20,
    blurSm = 5, blurMd = 10, blurLg = 16,
    shadowSm = 1f, shadowMd = 4f, shadowLg = 10f, shadowGlow = 8f,
    borderHairline = 0.4f, borderThin = 0.8f, borderThick = 1.5f,
    fontFamilyUI = "sans-serif-medium", fontFamilyMono = "monospace",
    fontDisplay = 30, fontTitle = 21, fontHeading = 17, fontBody = 14, fontCaption = 12, fontCode = 13,
    weightRegular = 400, weightMedium = 500, weightSemibold = 600, weightBold = 700,
    lineHeightTight = 1.2f, lineHeightNormal = 1.46f, lineHeightLoose = 1.62f,
    motionInstant = 60, motionFast = 125, motionNormal = 210, motionSlow = 330, motionSlower = 480, motionAmbient = 14000,
    easingStandard = "cubic-bezier(0.20,0.00,0.00,1.00)", easingEnter = "cubic-bezier(0.00,0.00,0.20,1.00)", easingExit = "cubic-bezier(0.40,0.00,1.00,1.00)",
    gradientPrimary = listOf(argb("C0C8D8"), argb("8F9BB3"), argb("E3E8F2")),
    gradientSecondary = listOf(argb("070A10"), argb("111622")),
    gradientBorder = listOf(argb("232B3D"), argb("E3E8F2"), argb("C0C8D8")),
    signature = "brushed-metal reflection follows scrolling",
    backgroundMotion = "dark ambient: brushed-metal reflection follows scrolling",
    transitionMotion = "Dark transition: brushed-metal reflection follows scrolling; use a composed fade/transform entrance with a soft eased settle",
    interactionMotion = "crisp silver outline on focus, restrained 1dp hover lift, and a tight tactile press",
    loadingMotion = "Dark loading: low-contrast metallic sweep over a breathing skeleton, with a short accent-color progress fill",
    notificationMotion = "dark notification in #e3e8f2: quick fade-slide, soft surface bloom, then a signature pulse"
)
