package com.rexaps.rexcoder.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

object Rex {
    // Dark: deep-space navy, neon cyan primary + violet secondary. Still VS Code layout, sci-fi skin.
    val DarkEditorBg = Color(0xFF080C14)
    val DarkSideBar = Color(0xFF0B111C)
    val DarkActivityBar = Color(0xFF05080F)
    val DarkTitleBar = Color(0xFF0A0F1A)
    val DarkTabBar = Color(0xFF0B111C)
    val DarkTabInactive = Color(0xFF0E1522)
    val DarkBorder = Color(0xFF1B2840)
    val DarkSelection = Color(0xFF12294A)
    val DarkSelectionEditor = Color(0xFF1D4A7A)
    val DarkLineHighlight = Color(0xFF0D1727)
    val DarkAccent = Color(0xFF22D3EE)
    val DarkAccent2 = Color(0xFF8B5CF6)
    val DarkField = Color(0xFF0F1827)
    val DarkOverlay = Color(0xFF0C1322)
    val DarkOnAccent = Color(0xFF031018)
    val DarkText = Color(0xFFD5E1F2)
    val DarkTextDim = Color(0xFF7C8BA3)
    val DarkTextBright = Color(0xFFF4F8FD)
    val DarkError = Color(0xFFFB7185)
    val DarkWarning = Color(0xFFFBBF24)
    val DarkModified = Color(0xFFE2C08D)
    val DarkSuccess = Color(0xFF4ADE80)

    val LightEditorBg = Color(0xFFF8FAFC)
    val LightSideBar = Color(0xFFF1F5F9)
    val LightActivityBar = Color(0xFFE2E8F0)
    val LightTitleBar = Color(0xFFEAF2FF)
    val LightTabBar = Color(0xFFF1F5F9)
    val LightTabInactive = Color(0xFFE7EDF5)
    val LightBorder = Color(0xFFD3DCE8)
    val LightSelection = Color(0xFFDCEBFF)
    val LightSelectionEditor = Color(0xFFBBD7FF)
    val LightLineHighlight = Color(0xFFEFF5FC)
    val LightAccent = Color(0xFF0891B2)
    val LightAccent2 = Color(0xFF7C3AED)
    val LightField = Color(0xFFFFFFFF)
    val LightOverlay = Color(0xFFFFFFFF)
    val LightOnAccent = Color(0xFFFFFFFF)
    val LightText = Color(0xFF1E293B)
    val LightTextDim = Color(0xFF64748B)
    val LightTextBright = Color(0xFF0F172A)
    val LightError = Color(0xFFDC2626)
    val LightWarning = Color(0xFFD97706)
    val LightModified = Color(0xFF9A6700)
    val LightSuccess = Color(0xFF15803D)

    // Syntax colors are shared between themes so code keeps a familiar identity.
    val SynKeyword = Color(0xFF569CD6)
    val SynControl = Color(0xFFC586C0)
    val SynString = Color(0xFFCE9178)
    val SynComment = Color(0xFF6A9955)
    val SynNumber = Color(0xFFB5CEA8)
    val SynType = Color(0xFF4EC9B0)
    val SynFunction = Color(0xFFDCDCAA)
    val SynVariable = Color(0xFF9CDCFE)

    var EditorBg: Color = Color.Unspecified
    var SideBar: Color = Color.Unspecified
    var ActivityBar: Color = Color.Unspecified
    var TitleBar: Color = Color.Unspecified
    var TabBar: Color = Color.Unspecified
    var TabInactive: Color = Color.Unspecified
    var Border: Color = Color.Unspecified
    var Selection: Color = Color.Unspecified
    var SelectionEditor: Color = Color.Unspecified
    var LineHighlight: Color = Color.Unspecified
    var Accent: Color = Color.Unspecified
    var Accent2: Color = Color.Unspecified
    var Field: Color = Color.Unspecified
    var Overlay: Color = Color.Unspecified
    var OnAccent: Color = Color.Unspecified
    var Text: Color = Color.Unspecified
    var TextDim: Color = Color.Unspecified
    var TextBright: Color = Color.Unspecified
    var Error: Color = Color.Unspecified
    var Warning: Color = Color.Unspecified
    var Modified: Color = Color.Unspecified
    var Success: Color = Color.Unspecified

    /** Signature cyan -> violet gradient used for highlights, focus lines and primary buttons. */
    fun accentBrush(): Brush = Brush.horizontalGradient(listOf(Accent, Accent2))

    fun apply(colors: ColorScheme) {
        // Editor chrome follows RexAps palettes; only syntax highlighting stays specialized.
        EditorBg = colors.background
        SideBar = colors.surface
        ActivityBar = colors.surfaceVariant
        TitleBar = colors.surface
        TabBar = colors.surface
        TabInactive = colors.surfaceVariant
        Border = colors.outlineVariant
        Selection = colors.primaryContainer
        SelectionEditor = colors.primaryContainer
        LineHighlight = colors.surfaceVariant.copy(alpha = 0.55f)
        Accent = colors.primary
        Accent2 = colors.tertiary
        Field = colors.surfaceVariant
        Overlay = colors.surface
        OnAccent = colors.onPrimary
        Text = colors.onSurface
        TextDim = colors.onSurfaceVariant
        TextBright = colors.onSurface
        Error = colors.error
        Warning = colors.tertiary
        Modified = colors.secondary
        Success = colors.tertiary
    }
}

/** Editor text style for a given font size (sp). Line height scales with the size. */
fun codeStyle(sizeSp: Int): TextStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = sizeSp.sp,
    lineHeight = (sizeSp * 1.55f).sp,
    color = Rex.Text
)

val CodeStyle: TextStyle
    get() = codeStyle(13)

@Composable
fun RexTheme(content: @Composable () -> Unit) {
    Rex.apply(MaterialTheme.colorScheme)
    // Do not nest a separate light/dark MaterialTheme; inherit RexAps colors.
    content()
}
