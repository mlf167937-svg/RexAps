package com.rexaps.ui

import android.content.Context

/** App-owned themes; the selected palette never follows the device theme. */
enum class RexThemeOption(val label: String) {
    DARK_BLUE_MOON("Dark Blue Moon"),
    DARK_RED_MOON("Dark Red Moon"),
    DARK_PURPLE_MOON("Dark Purple Moon"),
    DARK_HOLLOW_PURPLE("Dark Holow Purple"),
    LIGHT_BLUE_MOON("Light Blue Moon"),
    LIGHT_RED_MOON("Light Red Moon"),
    LIGHT_PURPLE_MOON("Light Purple Moon"),
    LIGHT_HOLLOW_PURPLE("Light Holow Purple"),
    LIGHT("Daylight"), DARK("Midnight"), AMOLED("AMOLED"),
    LAVENDER("Lavender"), OCEAN("Oceanic"), CYBER("Cyber Pulse"), AURORA("Aurora"),
    COSMIC("Cosmic"), NEBULA("Nebula"), SAPPHIRE("Sapphire"), EMERALD("Emerald"),
    ROSE("Rose Quartz"), SAKURA("Sakura"), SUNSET("Sunset"), GOLD("Gold Luxe"),
    GRAPHITE("Graphite"), OBSIDIAN("Obsidian"), ARCTIC("Arctic"), MINT("Mint"),
    CRIMSON("Crimson"), VIOLET("Electric Violet"), TEAL("Deep Teal"),
    MATRIX("Matrix"), SYNTHWAVE("Synthwave"), GLACIER("Glacier"),
    MOCHA("Mocha"), PEARL("Pearl"), COBALT("Cobalt"), MAGENTA("Magenta"),
    GLASS_BLUE_MOON("Glass Blue Moon"), GLASS_RED_MOON("Glass Red Moon"),
    GLASS_PURPLE_MOON("Glass Purple Moon"), GLASS_HOLLOW_PURPLE("Glass Hollow Purple"),
    GLASS_EMERALD_MOON("Glass Emerald Moon"), GLASS_GOLD_MOON("Glass Gold Moon"),
    DARK_ONYX_MOON("Dark Onyx Moon"), DARK_MIDNIGHT_SILVER("Dark Midnight Silver"),
    LIGHT_PEARL_WHITE("Light Pearl White"), LIGHT_SAKURA_PINK("Light Sakura Pink"),
    NEON_CYAN_MOON("Neon Cyan Moon"), NEON_GREEN_MOON("Neon Green Moon"),
    NEON_PINK_MOON("Neon Pink Moon"), NEON_YELLOW_MOON("Neon Yellow Moon"),
    ASTRAL_GALAXY_MOON("Astral Galaxy Moon"), NEBULA_VOID_MOON("Nebula Void Moon"),
    ECLIPSE_BLACK_MOON("Eclipse Black Moon"), STARLIGHT_SILVER_MOON("Starlight Silver Moon"),
    FROST_ICE_BLUE_MOON("Frost Ice Blue Moon"), INFERNO_MAGMA_RED_MOON("Inferno Magma Red Moon"),
    THUNDER_GOLD_MOON("Thunder Gold Moon"), ABYSSAL_OCEAN_MOON("Abyssal Ocean Moon");

    companion object {
        /** The curated 30-theme catalogue in the product's canonical category/order. */
        val curatedOptions: List<RexThemeOption> = listOf(
            DARK_BLUE_MOON, DARK_RED_MOON, DARK_PURPLE_MOON, DARK_HOLLOW_PURPLE, DARK_ONYX_MOON, DARK_MIDNIGHT_SILVER,
            GLASS_BLUE_MOON, GLASS_RED_MOON, GLASS_PURPLE_MOON, GLASS_HOLLOW_PURPLE, GLASS_EMERALD_MOON, GLASS_GOLD_MOON,
            LIGHT_BLUE_MOON, LIGHT_RED_MOON, LIGHT_PURPLE_MOON, LIGHT_HOLLOW_PURPLE, LIGHT_PEARL_WHITE, LIGHT_SAKURA_PINK,
            NEON_CYAN_MOON, NEON_GREEN_MOON, NEON_PINK_MOON, NEON_YELLOW_MOON,
            ASTRAL_GALAXY_MOON, NEBULA_VOID_MOON, ECLIPSE_BLACK_MOON, STARLIGHT_SILVER_MOON,
            FROST_ICE_BLUE_MOON, INFERNO_MAGMA_RED_MOON, THUNDER_GOLD_MOON, ABYSSAL_OCEAN_MOON
        )
    }
    val swatch: Long get() = RexPaletteSpec.of(this).primary
}

class RexThemeStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val engine = com.rexaps.ui.theme.RexThemeManager(appContext)
    fun load(): RexThemeOption {
        val saved = prefs.getString(KEY_THEME, null)
        val option = if (saved == null || saved == "BLOOD_MOON") RexThemeOption.DARK_BLUE_MOON
            else RexThemeOption.values().firstOrNull { it.name == saved } ?: RexThemeOption.DARK_BLUE_MOON
        if (option in RexThemeOption.curatedOptions) engine.setTheme(option.labelToThemeId())
        return option
    }
    fun save(option: RexThemeOption) {
        prefs.edit().putString(KEY_THEME, option.name).apply()
        if (option in RexThemeOption.curatedOptions) engine.setTheme(option.labelToThemeId())
    }
    private companion object { const val PREFS_NAME = "rexaps_settings"; const val KEY_THEME = "theme_option" }
}
