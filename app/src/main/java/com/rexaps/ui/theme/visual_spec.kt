package com.rexaps.ui.theme

/** Per-theme material language. Kept separate from palette tokens so a theme has a
 * recognizable composition, not just a different hue. */
enum class RexThemeMaterial { DARK, GLASS, LIGHT, NEON, COSMIC, FROST, FIRE, OCEAN }
enum class RexThemeMotif {
    ORBIT, EMBER, LILAC_RIBBON, VOID_HALO, ONYX_RINGS, SILVER_ARC,
    GLASS_REFRACTION, GLASS_ECLIPSE, GLASS_PRISM, GLASS_VOID, GLASS_LEAF, GLASS_FACET,
    BLUE_RAY, RED_BLADE, PURPLE_WAVE, HOLLOW_RAY, PEARL_SHEEN, SAKURA_PETAL,
    NEON_CIRCUIT, NEON_MATRIX, NEON_PULSE, NEON_SUN,
    GALAXY, NEBULA, ECLIPSE, STARFIELD, ICE_CRYSTAL, MAGMA_FLOW, GOLD_LIGHTNING, OCEAN_WAVE
}

data class RexThemeVisualSpec(
    val material: RexThemeMaterial,
    val motif: RexThemeMotif,
    val surfaceAlpha: Float,
    val borderAlpha: Float,
    val highlightAlpha: Float,
    val glowAlpha: Float,
    val ambientDurationMs: Int,
    val radiusBias: Int = 0
)

/** Exhaustive visual identity map for every item in RexThemeRegistry.all. */
object RexThemeVisuals {
    private val specs = mapOf(
        "dark_blue_moon" to RexThemeVisualSpec(RexThemeMaterial.DARK, RexThemeMotif.ORBIT, .94f, .42f, .16f, .22f, 18000, 0),
        "dark_red_moon" to RexThemeVisualSpec(RexThemeMaterial.DARK, RexThemeMotif.EMBER, .96f, .48f, .12f, .27f, 14500, -1),
        "dark_purple_moon" to RexThemeVisualSpec(RexThemeMaterial.DARK, RexThemeMotif.LILAC_RIBBON, .92f, .44f, .20f, .24f, 20000, 1),
        "dark_hollow_purple" to RexThemeVisualSpec(RexThemeMaterial.DARK, RexThemeMotif.VOID_HALO, .90f, .54f, .13f, .31f, 23000, 3),
        "dark_onyx_moon" to RexThemeVisualSpec(RexThemeMaterial.DARK, RexThemeMotif.ONYX_RINGS, .98f, .34f, .08f, .12f, 26000, -2),
        "dark_midnight_silver" to RexThemeVisualSpec(RexThemeMaterial.DARK, RexThemeMotif.SILVER_ARC, .95f, .38f, .22f, .13f, 22000, 0),
        "glass_blue_moon" to RexThemeVisualSpec(RexThemeMaterial.GLASS, RexThemeMotif.GLASS_REFRACTION, .25f, .58f, .35f, .22f, 19000, 0),
        "glass_red_moon" to RexThemeVisualSpec(RexThemeMaterial.GLASS, RexThemeMotif.GLASS_ECLIPSE, .23f, .62f, .30f, .24f, 18500, -1),
        "glass_purple_moon" to RexThemeVisualSpec(RexThemeMaterial.GLASS, RexThemeMotif.GLASS_PRISM, .22f, .60f, .36f, .24f, 20500, 1),
        "glass_hollow_purple" to RexThemeVisualSpec(RexThemeMaterial.GLASS, RexThemeMotif.GLASS_VOID, .18f, .68f, .24f, .31f, 23000, 4),
        "glass_emerald_moon" to RexThemeVisualSpec(RexThemeMaterial.GLASS, RexThemeMotif.GLASS_LEAF, .24f, .58f, .32f, .20f, 21000, 0),
        "glass_gold_moon" to RexThemeVisualSpec(RexThemeMaterial.GLASS, RexThemeMotif.GLASS_FACET, .25f, .60f, .38f, .20f, 20000, 0),
        "light_blue_moon" to RexThemeVisualSpec(RexThemeMaterial.LIGHT, RexThemeMotif.BLUE_RAY, .98f, .40f, .28f, .10f, 18000, 0),
        "light_red_moon" to RexThemeVisualSpec(RexThemeMaterial.LIGHT, RexThemeMotif.RED_BLADE, .98f, .42f, .22f, .10f, 16000, -1),
        "light_purple_moon" to RexThemeVisualSpec(RexThemeMaterial.LIGHT, RexThemeMotif.PURPLE_WAVE, .97f, .40f, .28f, .12f, 20000, 1),
        "light_hollow_purple" to RexThemeVisualSpec(RexThemeMaterial.LIGHT, RexThemeMotif.HOLLOW_RAY, .96f, .48f, .20f, .14f, 22500, 3),
        "light_pearl_white" to RexThemeVisualSpec(RexThemeMaterial.LIGHT, RexThemeMotif.PEARL_SHEEN, .99f, .28f, .42f, .08f, 24000, 0),
        "light_sakura_pink" to RexThemeVisualSpec(RexThemeMaterial.LIGHT, RexThemeMotif.SAKURA_PETAL, .98f, .36f, .25f, .11f, 17000, 1),
        "neon_cyan_moon" to RexThemeVisualSpec(RexThemeMaterial.NEON, RexThemeMotif.NEON_CIRCUIT, .90f, .68f, .24f, .34f, 13500, 0),
        "neon_green_moon" to RexThemeVisualSpec(RexThemeMaterial.NEON, RexThemeMotif.NEON_MATRIX, .90f, .64f, .18f, .36f, 12500, -1),
        "neon_pink_moon" to RexThemeVisualSpec(RexThemeMaterial.NEON, RexThemeMotif.NEON_PULSE, .91f, .66f, .30f, .34f, 14000, 1),
        "neon_yellow_moon" to RexThemeVisualSpec(RexThemeMaterial.NEON, RexThemeMotif.NEON_SUN, .92f, .65f, .32f, .30f, 15500, 0),
        "astral_galaxy_moon" to RexThemeVisualSpec(RexThemeMaterial.COSMIC, RexThemeMotif.GALAXY, .90f, .44f, .20f, .24f, 26000, 1),
        "nebula_void_moon" to RexThemeVisualSpec(RexThemeMaterial.COSMIC, RexThemeMotif.NEBULA, .88f, .48f, .16f, .29f, 28000, 2),
        "eclipse_black_moon" to RexThemeVisualSpec(RexThemeMaterial.COSMIC, RexThemeMotif.ECLIPSE, .98f, .32f, .12f, .14f, 30000, -2),
        "starlight_silver_moon" to RexThemeVisualSpec(RexThemeMaterial.COSMIC, RexThemeMotif.STARFIELD, .93f, .38f, .30f, .16f, 25000, 0),
        "frost_ice_blue_moon" to RexThemeVisualSpec(RexThemeMaterial.FROST, RexThemeMotif.ICE_CRYSTAL, .88f, .48f, .34f, .17f, 23000, 0),
        "inferno_magma_red_moon" to RexThemeVisualSpec(RexThemeMaterial.FIRE, RexThemeMotif.MAGMA_FLOW, .92f, .50f, .22f, .32f, 13500, -1),
        "thunder_gold_moon" to RexThemeVisualSpec(RexThemeMaterial.NEON, RexThemeMotif.GOLD_LIGHTNING, .92f, .58f, .30f, .31f, 16000, 0),
        "abyssal_ocean_moon" to RexThemeVisualSpec(RexThemeMaterial.OCEAN, RexThemeMotif.OCEAN_WAVE, .90f, .45f, .20f, .22f, 24000, 0)
    )

    fun forTheme(themeId: String): RexThemeVisualSpec = specs[themeId] ?: specs.getValue("dark_blue_moon")
    fun coverage(themeIds: Set<String>): Set<String> = themeIds - specs.keys
}
