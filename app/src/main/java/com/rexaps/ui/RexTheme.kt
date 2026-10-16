package com.rexaps.ui

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
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

/** 30 curated themes: premium gradients, futuristic darks and carefully tuned light palettes. */
enum class RexThemeOption(val label: String) {
    SYSTEM("Otomatis"), NEON_NOVA("Neon Nova"), CYBER_PULSE("Cyber Pulse"), COSMIC_VOID("Cosmic Void"),
    GALAXY_DRIVE("Galaxy Drive"), AURORA_FLUX("Aurora Flux"), DEEP_OCEAN("Deep Ocean"), ARCTIC_GLASS("Arctic Glass"),
    ROYAL_VIOLET("Royal Violet"), LAVENDER_DREAM("Lavender Dream"), PLASMA_PINK("Plasma Pink"), SAKURA_GLOW("Sakura Glow"),
    CRIMSON_EDGE("Crimson Edge"), SOLAR_FLARE("Solar Flare"), GOLDEN_HOUR("Golden Hour"), EMERALD_MATRIX("Emerald Matrix"),
    JADE_EMPIRE("Jade Empire"), TURQUOISE_WAVE("Turquoise Wave"), SYNTHWAVE("Synthwave"), VAPORWAVE("Vaporwave"),
    OBSIDIAN("Obsidian"), TITANIUM("Titanium"), MIDNIGHT_BLUE("Midnight Blue"), GLACIER_MINT("Glacier Mint"),
    PEARL_LUXE("Pearl Luxe"), ROSE_GOLD("Rose Gold"), AMETHYST("Amethyst"), COBALT_STORM("Cobalt Storm"),
    LIME_VOLT("Lime Volt"), CORAL_REEF("Coral Reef");

    val swatch: Long get() = RexPaletteSpec.of(this).primary
}

class RexThemeStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("rexaps_settings", Context.MODE_PRIVATE)
    fun load(): RexThemeOption {
        val saved = prefs.getString("theme_option", null)
        return RexThemeOption.values().firstOrNull { it.name == saved } ?: RexThemeOption.NEON_NOVA
    }
    fun save(option: RexThemeOption) { prefs.edit().putString("theme_option", option.name).apply() }
}

data class RexPaletteSpec(
    val dark: Boolean, val primary: Long, val onPrimary: Long, val container: Long,
    val onContainer: Long, val tertiary: Long, val background: Long, val card: Long,
    val muted: Long, val outline: Long, val text: Long
) {
    companion object {
        private fun p(d:Boolean, a:Long, on:Long, c:Long, oc:Long, t:Long, bg:Long, card:Long, muted:Long, line:Long, text:Long) =
            RexPaletteSpec(d,a,on,c,oc,t,bg,card,muted,line,text)
        private val palettes = mapOf(
            RexThemeOption.SYSTEM to p(true,0xFF8B5CF6,0xFFFFFFFF,0xFF30205E,0xFFEDE4FF,0xFF22D3EE,0xFF090817,0xFF141127,0xFFAAA4C7,0xFF30264E,0xFFF7F4FF),
            RexThemeOption.NEON_NOVA to p(true,0xFF7C3AED,0xFFFFFFFF,0xFF35126D,0xFFE9D5FF,0xFF22D3EE,0xFF08071A,0xFF12112F,0xFFA6A5D7,0xFF30266C,0xFFF7F5FF),
            RexThemeOption.CYBER_PULSE to p(true,0xFF00E5FF,0xFF001B28,0xFF06384A,0xFFC8F8FF,0xFFFF36D7,0xFF050B16,0xFF0D1A2B,0xFF8BA8C5,0xFF1C3A58,0xFFEAFBFF),
            RexThemeOption.COSMIC_VOID to p(true,0xFFB58AFF,0xFF21103D,0xFF35204F,0xFFF0E5FF,0xFFFC71B9,0xFF090610,0xFF171020,0xFFAA9BB8,0xFF33243F,0xFFF8F1FF),
            RexThemeOption.GALAXY_DRIVE to p(true,0xFF6366F1,0xFFFFFFFF,0xFF292A75,0xFFE0E7FF,0xFFEC4899,0xFF080B20,0xFF131936,0xFF9DA8D4,0xFF29335D,0xFFF0F4FF),
            RexThemeOption.AURORA_FLUX to p(true,0xFF34D399,0xFF04271E,0xFF124936,0xFFD1FAE5,0xFF818CF8,0xFF061410,0xFF10231D,0xFF91B7A9,0xFF24483A,0xFFEAFFF6),
            RexThemeOption.DEEP_OCEAN to p(true,0xFF38BDF8,0xFF06243A,0xFF123E5B,0xFFDDF5FF,0xFF2DD4BF,0xFF06121F,0xFF0D2234,0xFF8EABBF,0xFF1C3D53,0xFFEAF7FF),
            RexThemeOption.ARCTIC_GLASS to p(false,0xFF2563EB,0xFFFFFFFF,0xFFDCEBFF,0xFF12346A,0xFF06B6D4,0xFFF2F8FF,0xFFFFFFFF,0xFF647994,0xFFD5E4F6,0xFF10233D),
            RexThemeOption.ROYAL_VIOLET to p(true,0xFFA78BFA,0xFF241044,0xFF372364,0xFFF0E9FF,0xFF60A5FA,0xFF10091F,0xFF1D1432,0xFFB1A4CC,0xFF39284F,0xFFF7F1FF),
            RexThemeOption.LAVENDER_DREAM to p(false,0xFF8057F5,0xFFFFFFFF,0xFFE8DFFF,0xFF29145E,0xFFBA7BFF,0xFFF7F3FF,0xFFFFFFFF,0xFF766B91,0xFFE2D8F5,0xFF211638),
            RexThemeOption.PLASMA_PINK to p(true,0xFFFF4FD8,0xFF39002C,0xFF5A1450,0xFFFFDDF7,0xFF8B5CF6,0xFF150817,0xFF25112A,0xFFB99AB8,0xFF482344,0xFFFFF0FC),
            RexThemeOption.SAKURA_GLOW to p(false,0xFFEC4899,0xFFFFFFFF,0xFFFFD8EA,0xFF581338,0xFFA78BFA,0xFFFFF5FA,0xFFFFFFFF,0xFF906D80,0xFFF4D8E5,0xFF321423),
            RexThemeOption.CRIMSON_EDGE to p(true,0xFFFF5268,0xFF39030B,0xFF57121D,0xFFFFE0E4,0xFFFF9A66,0xFF13070B,0xFF241016,0xFFB99AA0,0xFF48232D,0xFFFFF0F2),
            RexThemeOption.SOLAR_FLARE to p(true,0xFFFF9F43,0xFF3C1A00,0xFF5A2C0A,0xFFFFE7CC,0xFFFFD166,0xFF160B06,0xFF28170D,0xFFC1A18A,0xFF493020,0xFFFFF4E8),
            RexThemeOption.GOLDEN_HOUR to p(false,0xFFB7791F,0xFFFFFFFF,0xFFF8E5BD,0xFF49300B,0xFFDBA84A,0xFFFFFAF0,0xFFFFFFFF,0xFF897653,0xFFEDE0C6,0xFF30240F),
            RexThemeOption.EMERALD_MATRIX to p(true,0xFF22C55E,0xFF002B12,0xFF104523,0xFFD0FBE0,0xFF00E5A8,0xFF050F0A,0xFF0D1E14,0xFF8AB49B,0xFF1D3B29,0xFFE8FFF0),
            RexThemeOption.JADE_EMPIRE to p(false,0xFF0F9F78,0xFFFFFFFF,0xFFC9F3E5,0xFF063C2E,0xFF6EE7B7,0xFFF0FBF7,0xFFFFFFFF,0xFF5D8175,0xFFD2EAE1,0xFF102B24),
            RexThemeOption.TURQUOISE_WAVE to p(false,0xFF0891B2,0xFFFFFFFF,0xFFCFFAFE,0xFF064E5B,0xFF2DD4BF,0xFFF0FCFD,0xFFFFFFFF,0xFF557D83,0xFFD1EBEE,0xFF10282C),
            RexThemeOption.SYNTHWAVE to p(true,0xFFFF4FCB,0xFF35002B,0xFF51104A,0xFFFFD9F5,0xFF27D7FF,0xFF10071D,0xFF1E1033,0xFFAA91BD,0xFF39214E,0xFFFFF0FF),
            RexThemeOption.VAPORWAVE to p(true,0xFF67E8F9,0xFF083344,0xFF164E63,0xFFCFFAFE,0xFFF0ABFC,0xFF0C1024,0xFF171B35,0xFFA0B2D0,0xFF2C365B,0xFFF0F9FF),
            RexThemeOption.OBSIDIAN to p(true,0xFFB7A7FF,0xFF21164A,0xFF28243C,0xFFEDE9FE,0xFF6EE7F9,0xFF07080D,0xFF12141D,0xFF989CAB,0xFF292D3B,0xFFF2F3FA),
            RexThemeOption.TITANIUM to p(true,0xFFA8B4C8,0xFF141D2A,0xFF2C3A4D,0xFFE6EDF7,0xFF7DD3FC,0xFF0A1019,0xFF151E2B,0xFF9AA9BC,0xFF2A394A,0xFFF0F5FC),
            RexThemeOption.MIDNIGHT_BLUE to p(true,0xFF4F8CFF,0xFF001A48,0xFF10366F,0xFFD8E7FF,0xFF38BDF8,0xFF050B19,0xFF0D1830,0xFF8D9FBE,0xFF20365B,0xFFEAF1FF),
            RexThemeOption.GLACIER_MINT to p(false,0xFF0D9488,0xFFFFFFFF,0xFFCCFBF1,0xFF064E49,0xFF60A5FA,0xFFF0FDFA,0xFFFFFFFF,0xFF5D817D,0xFFD1ECE7,0xFF102B29),
            RexThemeOption.PEARL_LUXE to p(false,0xFF7C6FE8,0xFFFFFFFF,0xFFE8E5FF,0xFF30286D,0xFFBA8BFF,0xFFF8F7FF,0xFFFFFFFF,0xFF77758F,0xFFE3E0F4,0xFF211F38),
            RexThemeOption.ROSE_GOLD to p(false,0xFFB76E79,0xFFFFFFFF,0xFFF7DDE0,0xFF572832,0xFFD6A07A,0xFFFFF7F5,0xFFFFFFFF,0xFF8C7070,0xFFF0DCD8,0xFF302020),
            RexThemeOption.AMETHYST to p(true,0xFFC084FC,0xFF321052,0xFF4C1D75,0xFFF3E8FF,0xFF818CF8,0xFF10081B,0xFF20112F,0xFFB09BC4,0xFF3A2650,0xFFF8F0FF),
            RexThemeOption.COBALT_STORM to p(true,0xFF3B82F6,0xFFFFFFFF,0xFF163B80,0xFFDBEAFE,0xFF22D3EE,0xFF050A17,0xFF0E1830,0xFF91A5C8,0xFF20365C,0xFFEAF2FF),
            RexThemeOption.LIME_VOLT to p(true,0xFFA3E635,0xFF1B2D00,0xFF344D0C,0xFFECFCCB,0xFF22D3EE,0xFF0B1005,0xFF171F0C,0xFFA3B58B,0xFF303F1D,0xFFF4FFE6),
            RexThemeOption.CORAL_REEF to p(false,0xFFEA6548,0xFFFFFFFF,0xFFFFDDD5,0xFF632315,0xFFFFA58B,0xFFFFF7F3,0xFFFFFFFF,0xFF8D7167,0xFFF3DED6,0xFF321A14)
        )
        fun of(option: RexThemeOption): RexPaletteSpec = palettes[option] ?: palettes.getValue(RexThemeOption.NEON_NOVA)
    }
}

private fun RexPaletteSpec.toColorScheme(): ColorScheme = if (dark) {
    darkColorScheme(
        primary = Color(primary), onPrimary = Color(onPrimary), primaryContainer = Color(container),
        onPrimaryContainer = Color(onContainer), secondary = Color(muted), tertiary = Color(tertiary),
        background = Color(background), onBackground = Color(text), surface = Color(card), onSurface = Color(text),
        surfaceVariant = Color(card), onSurfaceVariant = Color(muted), outline = Color(outline), outlineVariant = Color(outline)
    )
} else {
    lightColorScheme(
        primary = Color(primary), onPrimary = Color(onPrimary), primaryContainer = Color(container),
        onPrimaryContainer = Color(onContainer), secondary = Color(muted), tertiary = Color(tertiary),
        background = Color(background), onBackground = Color(text), surface = Color(card), onSurface = Color(text),
        surfaceVariant = Color(card), onSurfaceVariant = Color(muted), outline = Color(outline), outlineVariant = Color(outline)
    )
}

private object RexThemeCache {
    private val cache = LinkedHashMap<String, ColorScheme>(8, 0.75f, true)
    @Synchronized fun get(option: RexThemeOption, systemDark: Boolean): ColorScheme {
        val actual = if (option == RexThemeOption.SYSTEM) {
            if (systemDark) RexThemeOption.OBSIDIAN else RexThemeOption.PEARL_LUXE
        } else option
        cache[actual.name]?.let { return it }
        val built = RexPaletteSpec.of(actual).toColorScheme()
        cache[actual.name] = built
        if (cache.size > 8) cache.remove(cache.keys.first())
        return built
    }
}

fun RexThemeOption.colorScheme(systemDark: Boolean): ColorScheme = RexThemeCache.get(this, systemDark)

private val baseTypography = Typography()
private val RexTypography = Typography(
    headlineLarge = baseTypography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.8).sp),
    headlineMedium = baseTypography.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineSmall = baseTypography.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    titleLarge = baseTypography.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
    titleMedium = baseTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = baseTypography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    bodyMedium = baseTypography.bodyMedium.copy(lineHeight = 21.sp),
    bodySmall = baseTypography.bodySmall.copy(lineHeight = 18.sp),
    labelLarge = baseTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    labelMedium = baseTypography.labelMedium.copy(fontWeight = FontWeight.Medium),
    labelSmall = baseTypography.labelSmall.copy(fontWeight = FontWeight.Medium)
)

@Composable
fun RexTheme(option: RexThemeOption, content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val scheme = remember(option, systemDark) { option.colorScheme(systemDark) }
    MaterialTheme(colorScheme = scheme, typography = RexTypography, content = content)
}
