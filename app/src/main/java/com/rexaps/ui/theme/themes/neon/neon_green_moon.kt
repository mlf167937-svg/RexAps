package com.rexaps.ui.theme.themes.neon

import com.rexaps.ui.theme.RexDesignTokens
import com.rexaps.ui.theme.argb
import com.rexaps.ui.theme.blend

/** Neon Green Moon: brief terminal boot sequence opens each module. */
val neon_green_moon = RexDesignTokens(
    id = "neon_green_moon", name = "Neon Green Moon", category = "Neon",
    primary = argb("39FF14"), primaryHover = blend(argb("39FF14"), 0xFFFFFFFFL, .10f),
    primaryPressed = blend(argb("39FF14"), 0xFF000000L, .12f), onPrimary = argb("10131A"),
    secondary = argb("00E676"), accent = argb("B6FF9E"),
    background = argb("030A05"), backgroundAlt = argb("08140B"),
    surface = argb("FF08140B"), surfaceElevated = argb("FF17221A"), surfaceHover = argb("FF212C23"),
    text = argb("E6FFE9"), textMuted = argb("FFAAB8CC"), textDisabled = argb("FF68758A"),
    border = argb("39FF14"), borderStrong = blend(argb("39FF14"), argb("B6FF9E"), .45f),
    glow = argb("39FF14"), overlay = 0x99000000L,
    success = argb("22C55E"), warning = argb("F59E0B"), error = argb("EF4444"), info = argb("38BDF8"),
    selection = blend(argb("39FF14"), argb("030A05"), .35f), scrollbar = argb("39FF14"),
    opacitySurface = .98f,
    noiseIntensity = .02f,
    radiusXs = 3, radiusSm = 5, radiusMd = 8, radiusLg = 12, radiusXl = 16,
    blurSm = 3, blurMd = 7, blurLg = 11,
    shadowSm = 1f, shadowMd = 3f, shadowLg = 9f, shadowGlow = 26f,
    borderHairline = 0.4f, borderThin = 1.0f, borderThick = 2.0f,
    fontFamilyUI = "monospace", fontFamilyMono = "monospace",
    fontDisplay = 30, fontTitle = 21, fontHeading = 17, fontBody = 14, fontCaption = 12, fontCode = 13,
    weightRegular = 400, weightMedium = 500, weightSemibold = 600, weightBold = 700,
    lineHeightTight = 1.18f, lineHeightNormal = 1.44f, lineHeightLoose = 1.6f,
    motionInstant = 42, motionFast = 100, motionNormal = 170, motionSlow = 250, motionSlower = 370, motionAmbient = 11500,
    easingStandard = "cubic-bezier(0.10,0.80,0.20,1.00)", easingEnter = "cubic-bezier(0.10,0.80,0.20,1.00)", easingExit = "cubic-bezier(0.40,0.00,1.00,1.00)",
    gradientPrimary = listOf(argb("39FF14"), argb("00E676"), argb("B6FF9E")),
    gradientSecondary = listOf(argb("030A05"), argb("08140B")),
    gradientBorder = listOf(argb("39FF14"), argb("B6FF9E"), argb("39FF14")),
    signature = "brief terminal boot sequence opens each module",
    backgroundMotion = "neon ambient: brief terminal boot sequence opens each module",
    transitionMotion = "Neon transition: brief terminal boot sequence opens each module; use a composed fade/transform entrance with a soft eased settle",
    interactionMotion = "terminal-green outline flashes into steady glow, crisp hover snap, and a compact press rebound",
    loadingMotion = "Neon loading: high-contrast scan sweep, pulsing tube segment, and a fast animated progress rail",
    notificationMotion = "neon notification in #b6ff9e: quick fade-slide, soft surface bloom, then a signature pulse"
)
