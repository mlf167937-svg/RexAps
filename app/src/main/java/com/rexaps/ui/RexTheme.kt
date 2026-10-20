package com.rexaps.ui

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** 30 curated app-owned themes. The selected palette never follows the device theme. */
enum class RexThemeOption(val label: String) {
    BLOOD_MOON("Dark Blood Moon"), LIGHT("Daylight"), DARK("Midnight"), AMOLED("AMOLED"),
    LAVENDER("Lavender"), OCEAN("Oceanic"), CYBER("Cyber Pulse"), AURORA("Aurora"),
    COSMIC("Cosmic"), NEBULA("Nebula"), SAPPHIRE("Sapphire"), EMERALD("Emerald"),
    ROSE("Rose Quartz"), SAKURA("Sakura"), SUNSET("Sunset"), GOLD("Gold Luxe"),
    GRAPHITE("Graphite"), OBSIDIAN("Obsidian"), ARCTIC("Arctic"), MINT("Mint"),
    CRIMSON("Crimson"), VIOLET("Electric Violet"), TEAL("Deep Teal"),
    MATRIX("Matrix"), SYNTHWAVE("Synthwave"), GLACIER("Glacier"),
    MOCHA("Mocha"), PEARL("Pearl"), COBALT("Cobalt"), MAGENTA("Magenta");

    val swatch: Long get() = RexPaletteSpec.of(this).primary
}

class RexThemeStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    fun load(): RexThemeOption = prefs.getString(KEY_THEME, null)?.let { saved ->
        RexThemeOption.values().firstOrNull { it.name == saved }
    } ?: RexThemeOption.BLOOD_MOON
    fun save(option: RexThemeOption) { prefs.edit().putString(KEY_THEME, option.name).apply() }
    private companion object { const val PREFS_NAME = "rexaps_settings"; const val KEY_THEME = "theme_option" }
}

data class RexPaletteSpec(
    val dark: Boolean, val primary: Long, val onPrimary: Long, val container: Long,
    val onContainer: Long, val tertiary: Long, val background: Long, val card: Long,
    val muted: Long, val outline: Long, val text: Long
) {
    companion object {
        fun of(option: RexThemeOption): RexPaletteSpec = palettes[option] ?: palettes.getValue(RexThemeOption.BLOOD_MOON)
        private fun p(d:Boolean, a:Long, on:Long, c:Long, oc:Long, t:Long, bg:Long, card:Long, muted:Long, outline:Long, text:Long) =
            RexPaletteSpec(d,a,on,c,oc,t,bg,card,muted,outline,text)
        private val palettes: Map<RexThemeOption, RexPaletteSpec> = mapOf(
            RexThemeOption.BLOOD_MOON to p(true,0xFFFF4568,0xFF23050C,0xFF5F1827,0xFFFFDCE3,0xFFFF9CB0,0xFF11070B,0xFF1E1016,0xFFCDA4AE,0xFF59303C,0xFFFFF2F5),
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

private fun RexPaletteSpec.toColorScheme(): ColorScheme = if (dark) {
    darkColorScheme(
        primary = Color(primary), onPrimary = Color(onPrimary),
        primaryContainer = Color(container), onPrimaryContainer = Color(onContainer),
        secondary = Color(tertiary), onSecondary = Color(onPrimary),
        tertiary = Color(tertiary), background = Color(background), onBackground = Color(text),
        surface = Color(card), onSurface = Color(text), surfaceVariant = Color(card),
        onSurfaceVariant = Color(muted), outline = Color(outline), outlineVariant = Color(outline),
        surfaceContainerLowest = Color(background), surfaceContainerLow = Color(card),
        surfaceContainer = Color(card), surfaceContainerHigh = Color(card), surfaceContainerHighest = Color(outline),
        error = Color(0xFFFF6B75), onError = Color(0xFF330008),
        errorContainer = Color(0xFF57151D), onErrorContainer = Color(0xFFFFDDE0)
    )
} else {
    lightColorScheme(
        primary = Color(primary), onPrimary = Color(onPrimary),
        primaryContainer = Color(container), onPrimaryContainer = Color(onContainer),
        secondary = Color(tertiary), onSecondary = Color(text),
        tertiary = Color(tertiary), background = Color(background), onBackground = Color(text),
        surface = Color(card), onSurface = Color(text), surfaceVariant = Color(card),
        onSurfaceVariant = Color(muted), outline = Color(outline), outlineVariant = Color(outline),
        surfaceContainerLowest = Color(background), surfaceContainerLow = Color(card),
        surfaceContainer = Color(card), surfaceContainerHigh = Color(container), surfaceContainerHighest = Color(container),
        error = Color(0xFFB42335), onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFE1E4), onErrorContainer = Color(0xFF721321)
    )
}

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
private val rexTypography = Typography(
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
    val scheme = remember(option) { option.colorScheme() }
    MaterialTheme(colorScheme=scheme, typography=rexTypography, content=content)
}
