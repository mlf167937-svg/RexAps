package com.rexaps.ui

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

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
    internal val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    internal val engine = com.rexaps.ui.theme.RexThemeManager(appContext)
    fun load(): RexThemeOption {
        return this.__splitImpl_load()
    }
    fun save(option: RexThemeOption) {
        return this.__splitImpl_save(option)
    }
    private companion object { const val PREFS_NAME = "rexaps_settings"; const val KEY_THEME = "theme_option" }
}

data class RexPaletteSpec(
    val dark: Boolean, val primary: Long, val onPrimary: Long, val container: Long,
    val onContainer: Long, val tertiary: Long, val background: Long, val card: Long,
    val muted: Long, val outline: Long, val text: Long
) {
    companion object {
        fun of(option: RexThemeOption): RexPaletteSpec {
        return this.__splitImpl_of(option)
    }
        private fun p(d:Boolean, a:Long, on:Long, c:Long, oc:Long, t:Long, bg:Long, card:Long, muted:Long, outline:Long, text:Long) =
            RexPaletteSpec(d,a,on,c,oc,t,bg,card,muted,outline,text)
        internal val palettes: Map<RexThemeOption, RexPaletteSpec> = mapOf(
            // Four curated dark recommendations. Dark Blue Moon is the first-run default.
            RexThemeOption.DARK_BLUE_MOON to p(true,0xFF69A7FF,0xFF06152D,0xFF173967,0xFFDCEBFF,0xFF65DDF5,0xFF070D18,0xFF101B2B,0xFF9DB4D2,0xFF263B57,0xFFF1F6FF),
            RexThemeOption.DARK_RED_MOON to p(true,0xFFFF4568,0xFF23050C,0xFF5F1827,0xFFFFDCE3,0xFFFF9CB0,0xFF11070B,0xFF1E1016,0xFFCDA4AE,0xFF59303C,0xFFFFF2F5),
            RexThemeOption.DARK_PURPLE_MOON to p(true,0xFFB99BFF,0xFF20103A,0xFF43266D,0xFFF0E5FF,0xFF8BE3FF,0xFF100918,0xFF1E142B,0xFFB7A6C9,0xFF3F2B55,0xFFF8F2FF),
            RexThemeOption.DARK_HOLLOW_PURPLE to p(true,0xFFCF8BFF,0xFF250A3D,0xFF4B1A70,0xFFFFE3FF,0xFF8B75FF,0xFF0B0612,0xFF180D24,0xFFB8A2C8,0xFF412552,0xFFFFF4FF),
            // Matching light variants keep each curated Moon theme available in both appearances.
            RexThemeOption.LIGHT_BLUE_MOON to p(false,0xFF245DB8,0xFFFFFFFF,0xFFDCE9FF,0xFF153D7B,0xFF087E9A,0xFFF4F8FF,0xFFFFFFFF,0xFF526782,0xFFD6E1F2,0xFF101C30),
            RexThemeOption.LIGHT_RED_MOON to p(false,0xFFB42343,0xFFFFFFFF,0xFFFFE0E7,0xFF7A1830,0xFF9C2948,0xFFFFF5F7,0xFFFFFFFF,0xFF79515B,0xFFF0D5DC,0xFF2B1018),
            RexThemeOption.LIGHT_PURPLE_MOON to p(false,0xFF7042B8,0xFFFFFFFF,0xFFEDE2FF,0xFF4A267F,0xFF087EAD,0xFFFAF7FF,0xFFFFFFFF,0xFF6E617E,0xFFE5D9F4,0xFF241536),
            RexThemeOption.LIGHT_HOLLOW_PURPLE to p(false,0xFF7828A8,0xFFFFFFFF,0xFFF2DFFF,0xFF591B7D,0xFF5C48C7,0xFFFCF7FF,0xFFFFFFFF,0xFF74627F,0xFFE9D8F2,0xFF291237),
            RexThemeOption.LIGHT to p(false,0xFF635BDB,0xFFFFFFFF,0xFFE9E7FF,0xFF302B83,0xFF0E9F9A,0xFFF6F7FB,0xFFFFFFFF,0xFF626A7A,0xFFE2E5EF,0xFF171A26),
            RexThemeOption.DARK to p(true,0xFF9B8CFF,0xFF17132D,0xFF30285D,0xFFE9E4FF,0xFF64D8CB,0xFF101116,0xFF191B23,0xFFA4A8B8,0xFF30323D,0xFFF3F4FA),
            RexThemeOption.AMOLED to p(true,0xFFB4A5FF,0xFF120D25,0xFF251D40,0xFFE8E1FF,0xFF52E0D0,0xFF000000,0xFF0D0D12,0xFFA7A4B6,0xFF292633,0xFFF7F6FF),
            RexThemeOption.LAVENDER to p(false,0xFF7650D8,0xFFFFFFFF,0xFFECE4FF,0xFF39246F,0xFFB26DFF,0xFFF8F5FF,0xFFFFFFFF,0xFF716B82,0xFFE7DDF8,0xFF20172F),
            RexThemeOption.OCEAN to p(true,0xFF50C8F5,0xFF052A3B,0xFF123E54,0xFFD9F5FF,0xFF6EE7D8,0xFF071723,0xFF102737,0xFF92B1C4,0xFF244357,0xFFEAF7FC),
            RexThemeOption.CYBER to p(true,0xFF57F3FF,0xFF00262B,0xFF12363B,0xFFD7FDFF,0xFFFF4BD8,0xFF080B12,0xFF121724,0xFF9BA9BC,0xFF263747,0xFFF0FAFF),
            RexThemeOption.AURORA to p(true,0xFF72E6B1,0xFF062B20,0xFF164737,0xFFD8FFED,0xFF9D8CFF,0xFF0B1518,0xFF142226,0xFF9BB5B0,0xFF29413F,0xFFECF8F4),
            RexThemeOption.COSMIC to p(true,0xFFA58BFF,0xFF1C1040,0xFF33245D,0xFFEAE0FF,0xFF58C7FF,0xFF0C0B1B,0xFF18162D,0xFFA29BBF,0xFF302A4C,0xFFF2EEFF),
            RexThemeOption.NEBULA to p(true,0xFFE879F9,0xFF340A3C,0xFF4B1D53,0xFFFFDDFC,0xFF8B9BFF,0xFF140A1C,0xFF24132E,0xFFB7A0BE,0xFF45294F,0xFFFFF0FF),
            RexThemeOption.SAPPHIRE to p(true,0xFF6EA8FF,0xFF071F48,0xFF17376A,0xFFDDEBFF,0xFF68D9FF,0xFF081326,0xFF101F38,0xFF9CAFCB,0xFF253B5D,0xFFEDF4FF),
            RexThemeOption.EMERALD to p(true,0xFF4DDBA5,0xFF052D20,0xFF124B37,0xFFD4FFEC,0xFF6EDBC8,0xFF081610,0xFF12231A,0xFF9CB8AA,0xFF294638,0xFFECF8F1),
            RexThemeOption.ROSE to p(false,0xFFB52F5D,0xFFFFFFFF,0xFFFFE1EB,0xFF60213A,0xFF9C6BFF,0xFFFFF6F9,0xFFFFFFFF,0xFF795663,0xFFF2DCE5,0xFF2C1720),
            RexThemeOption.SAKURA to p(false,0xFFB72A70,0xFFFFFFFF,0xFFFFE1F0,0xFF64234A,0xFF9F8BFF,0xFFFFF7FB,0xFFFFFFFF,0xFF795568,0xFFF2DCE8,0xFF2B1723),
            RexThemeOption.SUNSET to p(true,0xFFFF986B,0xFF3D1608,0xFF57291A,0xFFFFE2D2,0xFFFFC15C,0xFF1B1110,0xFF2B1B19,0xFFC0A095,0xFF49302B,0xFFFFF0E8),
            RexThemeOption.GOLD to p(true,0xFFFFD477,0xFF332000,0xFF4A3512,0xFFFFEDC1,0xFFFFAB70,0xFF15120B,0xFF242017,0xFFB5AA92,0xFF403726,0xFFFFF7E8),
            RexThemeOption.GRAPHITE to p(true,0xFFB5C0D1,0xFF17202D,0xFF2B3544,0xFFE5EDF8,0xFF7DD3FC,0xFF101319,0xFF1A2029,0xFFA0AAB8,0xFF303946,0xFFF1F4F8),
            RexThemeOption.OBSIDIAN to p(true,0xFFE0E4EF,0xFF171923,0xFF303442,0xFFF3F5FF,0xFF9D8BFF,0xFF090A0E,0xFF13151B,0xFF969BA9,0xFF292C36,0xFFF2F3F8),
            RexThemeOption.ARCTIC to p(false,0xFF116A9A,0xFFFFFFFF,0xFFD9F1FF,0xFF123E5B,0xFF14A6A0,0xFFF3FAFE,0xFFFFFFFF,0xFF5D7585,0xFFDCEAF2,0xFF132630),
            RexThemeOption.MINT to p(false,0xFF11705F,0xFFFFFFFF,0xFFD2F6ED,0xFF10463C,0xFF5BBE98,0xFFF3FBF7,0xFFFFFFFF,0xFF4B685F,0xFFD9EBE4,0xFF132821),
            RexThemeOption.CRIMSON to p(true,0xFFFF6B7D,0xFF3D0711,0xFF571522,0xFFFFE0E5,0xFFFFAD75,0xFF18090D,0xFF28131A,0xFFB99AA1,0xFF48252E,0xFFFFF0F2),
            RexThemeOption.VIOLET to p(true,0xFF9F83FF,0xFF1D1042,0xFF382666,0xFFECE3FF,0xFF5FD8FF,0xFF100B20,0xFF1C1532,0xFFA69BBE,0xFF34294F,0xFFF5F0FF),
            RexThemeOption.TEAL to p(true,0xFF50D5CE,0xFF06312F,0xFF154A48,0xFFD9FFFC,0xFF7DBBFF,0xFF081718,0xFF122526,0xFF9BB7B6,0xFF294343,0xFFECFAF9),
            RexThemeOption.MATRIX to p(true,0xFF6BFF8B,0xFF03250A,0xFF10421A,0xFFD6FFDE,0xFFB4FF54,0xFF050D08,0xFF0D1A11,0xFF8DB99A,0xFF23432B,0xFFE9FFEE),
            RexThemeOption.SYNTHWAVE to p(true,0xFFFF72D2,0xFF3C0B35,0xFF57194D,0xFFFFDDF5,0xFF62D7FF,0xFF12091F,0xFF21132F,0xFFB9A0C1,0xFF42264E,0xFFFFF0FD),
            RexThemeOption.GLACIER to p(false,0xFF2B62B7,0xFFFFFFFF,0xFFDDEAFF,0xFF193F79,0xFF55BFCB,0xFFF4F8FF,0xFFFFFFFF,0xFF52657E,0xFFDDE6F4,0xFF172235),
            RexThemeOption.MOCHA to p(true,0xFFD7A77B,0xFF2F1B0C,0xFF49301D,0xFFFFE7D1,0xFFB6C8A0,0xFF17110D,0xFF251C16,0xFFB3A08E,0xFF3F3025,0xFFF6E9DD),
            RexThemeOption.PEARL to p(false,0xFF5F6882,0xFFFFFFFF,0xFFE7EAF3,0xFF2D354A,0xFF9C82E8,0xFFF8F9FC,0xFFFFFFFF,0xFF5F6678,0xFFE4E7EF,0xFF202432),
            RexThemeOption.COBALT to p(true,0xFF5889FF,0xFF071B4A,0xFF18366D,0xFFDBE7FF,0xFF9B83FF,0xFF080F20,0xFF111D35,0xFF9AA9C8,0xFF26385A,0xFFEEF3FF),
            RexThemeOption.MAGENTA to p(true,0xFFFF70B8,0xFF3B0A25,0xFF57183B,0xFFFFDDEC,0xFFB18CFF,0xFF160914,0xFF251323,0xFFBBA0B5,0xFF43263E,0xFFFFF0F8)
        )
    }
}

internal fun RexThemeOption.labelToThemeId(): String = this.__splitImpl_labelToThemeId()

internal fun RexPaletteSpec.toColorScheme(): ColorScheme = this.__splitImpl_toColorScheme()

private object RexThemeCache {
    private val cache = LinkedHashMap<String, ColorScheme>(8, 0.75f, true)
    @Synchronized fun get(option: RexThemeOption): ColorScheme {
        val key = option.name
        cache[key]?.let { return it }
        val result = RexPaletteSpec.of(option).toColorScheme()
        cache[key] = result
        if (cache.size > 8) cache.remove(cache.keys.first())
        return result
    }
}

fun RexThemeOption.colorScheme(): ColorScheme = RexThemeCache.get(this)

private val baseTypography = Typography()
internal val rexTypography = Typography(
    displayLarge=baseTypography.displayLarge.copy(fontWeight=FontWeight.Bold, letterSpacing=(-1.0).sp),
    headlineLarge=baseTypography.headlineLarge.copy(fontWeight=FontWeight.Bold, letterSpacing=(-0.65).sp),
    headlineMedium=baseTypography.headlineMedium.copy(fontWeight=FontWeight.Bold, letterSpacing=(-0.4).sp),
    headlineSmall=baseTypography.headlineSmall.copy(fontWeight=FontWeight.SemiBold, letterSpacing=(-0.25).sp),
    titleLarge=baseTypography.titleLarge.copy(fontWeight=FontWeight.SemiBold, letterSpacing=(-0.2).sp),
    titleMedium=baseTypography.titleMedium.copy(fontWeight=FontWeight.SemiBold),
    titleSmall=baseTypography.titleSmall.copy(fontWeight=FontWeight.SemiBold),
    bodyLarge=baseTypography.bodyLarge.copy(lineHeight=24.sp), bodyMedium=baseTypography.bodyMedium.copy(lineHeight=21.sp),
    bodySmall=baseTypography.bodySmall.copy(lineHeight=18.sp), labelLarge=baseTypography.labelLarge.copy(fontWeight=FontWeight.SemiBold),
    labelMedium=baseTypography.labelMedium.copy(fontWeight=FontWeight.Medium), labelSmall=baseTypography.labelSmall.copy(fontWeight=FontWeight.Medium)
)

@Composable fun RexTheme(option: RexThemeOption, content: @Composable () -> Unit) {
        return __splitImpl_RexTheme(option, content)
    }
