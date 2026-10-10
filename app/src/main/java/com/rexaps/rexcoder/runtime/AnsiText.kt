package com.rexaps.rexcoder.runtime

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

/** State SGR (warna/gaya) yang terbawa dari satu baris ke baris berikutnya. */
data class AnsiStyle(
    val fg: Color? = null,
    val bg: Color? = null,
    val bold: Boolean = false,
    val dim: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strike: Boolean = false,
    val inverse: Boolean = false
)

/**
 * Parser ANSI/VT100 ringan untuk terminal RexCoder.
 * Mendukung: 16 warna, 256 warna, truecolor (24-bit), bold, dim, italic, underline,
 * strike, inverse, dan reset. Sequence lain (CSI non-warna, OSC, charset) dibuang
 * supaya tidak muncul sebagai kotak "□[?2004h".
 */
object AnsiText {
    private const val ESC = '\u001B'

    /** 16 warna: index 0-7 normal, 8-15 bright. */
    val DarkPalette: List<Color> = listOf(
        Color(0xFF3B4252), Color(0xFFF2777A), Color(0xFF4ADE80), Color(0xFFFBBF24),
        Color(0xFF60A5FA), Color(0xFFC084FC), Color(0xFF22D3EE), Color(0xFFD5E1F2),
        Color(0xFF7C8BA3), Color(0xFFFB7185), Color(0xFF86EFAC), Color(0xFFFDE047),
        Color(0xFF93C5FD), Color(0xFFD8B4FE), Color(0xFF67E8F9), Color(0xFFF4F8FD)
    )

    val LightPalette: List<Color> = listOf(
        Color(0xFF1E293B), Color(0xFFDC2626), Color(0xFF15803D), Color(0xFFB45309),
        Color(0xFF1D4ED8), Color(0xFF7E22CE), Color(0xFF0E7490), Color(0xFF64748B),
        Color(0xFF475569), Color(0xFFEF4444), Color(0xFF16A34A), Color(0xFFD97706),
        Color(0xFF2563EB), Color(0xFF9333EA), Color(0xFF0891B2), Color(0xFF334155)
    )

    /** Render semua baris; style SGR dibawa antar baris. */
    fun render(lines: List<String>, dark: Boolean, defaultFg: Color, defaultBg: Color): List<AnnotatedString> {
        val pal = if (dark) DarkPalette else LightPalette
        var state = AnsiStyle()
        val out = ArrayList<AnnotatedString>(lines.size)
        for (line in lines) {
            val b = AnnotatedString.Builder()
            state = renderLine(line, state, pal, defaultFg, defaultBg, b)
            out.add(b.toAnnotatedString())
        }
        return out
    }

    /**
     * Index awal escape sequence yang terpotong di akhir [s] (belum lengkap), atau -1.
     * Dipakai untuk menahan potongan sequence sampai chunk berikutnya datang.
     */
    fun incompleteEscapeStart(s: String): Int {
        val i = s.lastIndexOf(ESC)
        if (i < 0) return -1
        if (s.length - i > 256) return -1
        if (i == s.lastIndex) return i
        return when (s[i + 1]) {
            '[' -> {
                var j = i + 2
                while (j < s.length && s[j].code in 0x20..0x3F) j++
                if (j >= s.length) i else -1
            }
            ']' -> if (s.indexOf('\u0007', i) >= 0 || s.indexOf("\u001B\\", i + 2) >= 0) -1 else i
            '(', ')', '*', '+', '#' -> if (i + 2 >= s.length) i else -1
            else -> -1
        }
    }

    private fun renderLine(
        line: String, start: AnsiStyle, pal: List<Color>,
        dfg: Color, dbg: Color, b: AnnotatedString.Builder
    ): AnsiStyle {
        var st = start
        val seg = StringBuilder()
        fun flush() {
            if (seg.isEmpty()) return
            val span = spanFor(st, dfg, dbg)
            if (span == null) b.append(seg.toString()) else b.withStyle(span) { append(seg.toString()) }
            seg.setLength(0)
        }

        var i = 0
        val n = line.length
        while (i < n) {
            val c = line[i]
            if (c == ESC) {
                if (i + 1 >= n) { i = n; continue }
                when (line[i + 1]) {
                    '[' -> {
                        var j = i + 2
                        while (j < n && line[j].code in 0x30..0x3F) j++
                        val paramEnd = j
                        while (j < n && line[j].code in 0x20..0x2F) j++
                        if (j >= n) { i = n; continue }
                        val params = line.substring(i + 2, paramEnd)
                        val hasIntermediate = j > paramEnd
                        val privatePrefix = params.isNotEmpty() && params[0] in "<=>?"
                        if (line[j] == 'm' && !hasIntermediate && !privatePrefix) {
                            flush()
                            st = applySgr(st, params, pal)
                        }
                        i = j + 1
                    }
                    ']' -> {
                        var j = i + 2
                        var end = -1
                        while (j < n) {
                            if (line[j] == '\u0007') { end = j + 1; break }
                            if (line[j] == ESC && j + 1 < n && line[j + 1] == '\\') { end = j + 2; break }
                            j++
                        }
                        i = if (end < 0) n else end
                    }
                    '(', ')', '*', '+', '#' -> i += 3
                    else -> i += 2
                }
                continue
            }
            when {
                c == '\t' -> seg.append("    ")
                c.code < 0x20 || c.code == 0x7F -> {}
                else -> seg.append(c)
            }
            i++
        }
        flush()
        return st
    }

    private fun spanFor(s: AnsiStyle, dfg: Color, dbg: Color): SpanStyle? {
        if (s == AnsiStyle()) return null
        var fg = s.fg
        var bg = s.bg
        if (s.inverse) {
            val f = fg ?: dfg
            val g = bg ?: dbg
            fg = g
            bg = f
        }
        if (s.dim) fg = (fg ?: dfg).copy(alpha = 0.6f)
        val deco = mutableListOf<TextDecoration>()
        if (s.underline) deco.add(TextDecoration.Underline)
        if (s.strike) deco.add(TextDecoration.LineThrough)
        return SpanStyle(
            color = fg ?: Color.Unspecified,
            background = bg ?: Color.Unspecified,
            fontWeight = if (s.bold) FontWeight.Bold else null,
            fontStyle = if (s.italic) FontStyle.Italic else null,
            textDecoration = if (deco.isNotEmpty()) TextDecoration.combine(deco) else null
        )
    }

    private fun applySgr(cur: AnsiStyle, raw: String, pal: List<Color>): AnsiStyle {
        val p = if (raw.isEmpty()) listOf(0) else raw.split(';', ':').map { it.toIntOrNull() ?: 0 }
        var s = cur
        var i = 0
        while (i < p.size) {
            when (val code = p[i]) {
                0 -> s = AnsiStyle()
                1 -> s = s.copy(bold = true)
                2 -> s = s.copy(dim = true)
                3 -> s = s.copy(italic = true)
                4 -> s = s.copy(underline = true)
                7 -> s = s.copy(inverse = true)
                9 -> s = s.copy(strike = true)
                22 -> s = s.copy(bold = false, dim = false)
                23 -> s = s.copy(italic = false)
                24 -> s = s.copy(underline = false)
                27 -> s = s.copy(inverse = false)
                29 -> s = s.copy(strike = false)
                in 30..37 -> s = s.copy(fg = pal[code - 30])
                39 -> s = s.copy(fg = null)
                in 40..47 -> s = s.copy(bg = pal[code - 40])
                49 -> s = s.copy(bg = null)
                in 90..97 -> s = s.copy(fg = pal[code - 90 + 8])
                in 100..107 -> s = s.copy(bg = pal[code - 100 + 8])
                38, 48 -> {
                    val mode = p.getOrNull(i + 1)
                    var color: Color? = null
                    if (mode == 5) {
                        color = p.getOrNull(i + 2)?.let { color256(it.coerceIn(0, 255), pal) }
                        i += 2
                    } else if (mode == 2) {
                        if (i + 4 < p.size) {
                            color = Color(
                                p[i + 2].coerceIn(0, 255),
                                p[i + 3].coerceIn(0, 255),
                                p[i + 4].coerceIn(0, 255)
                            )
                        }
                        i += 4
                    }
                    if (color != null) s = if (code == 38) s.copy(fg = color) else s.copy(bg = color)
                }
            }
            i++
        }
        return s
    }

    private val cube = intArrayOf(0, 95, 135, 175, 215, 255)

    private fun color256(n: Int, pal: List<Color>): Color = when {
        n < 16 -> pal[n]
        n < 232 -> {
            val k = n - 16
            Color(cube[k / 36], cube[(k / 6) % 6], cube[k % 6])
        }
        else -> {
            val g = 8 + (n - 232) * 10
            Color(g, g, g)
        }
    }
}
