package com.rexaps.rexpanel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

/** Emulator terminal mini: grid karakter, kursor, warna ANSI, hapus baris/layar. */
class TermBuffer(initCols: Int = 80, initRows: Int = 24) {
    var cols = initCols
        private set
    var rows = initRows
        private set
    var curR = 0
        private set
    var curC = 0
        private set
    var version by mutableIntStateOf(0)
        private set

    private var chars = Array(initRows) { CharArray(initCols) { ' ' } }
    private var fgs = Array(initRows) { IntArray(initCols) }
    private var fg = 0
    private var bold = false
    private var state = 0
    private val params = StringBuilder()

    fun reset() {
        chars = Array(rows) { CharArray(cols) { ' ' } }
        fgs = Array(rows) { IntArray(cols) }
        curR = 0; curC = 0; fg = 0; bold = false; state = 0
        version++
    }

    fun feed(s: String) {
        for (ch in s) put(ch)
        version++
    }

    fun resize(newCols: Int, newRows: Int) {
        if (newCols < 2 || newRows < 2 || (newCols == cols && newRows == rows)) return
        val drop = if (curR >= newRows) curR - newRows + 1 else 0
        val nc = Array(newRows) { CharArray(newCols) { ' ' } }
        val nf = Array(newRows) { IntArray(newCols) }
        val w = minOf(cols, newCols)
        for (r in 0 until newRows) {
            val src = r + drop
            if (src < rows) {
                System.arraycopy(chars[src], 0, nc[r], 0, w)
                System.arraycopy(fgs[src], 0, nf[r], 0, w)
            }
        }
        chars = nc; fgs = nf; cols = newCols; rows = newRows
        curR = (curR - drop).coerceIn(0, rows - 1)
        curC = curC.coerceIn(0, cols - 1)
        version++
    }

    fun rowText(version: Int, r: Int, defColor: Color, cursorBg: Color?, cursorFg: Color): AnnotatedString {
        if (r !in 0 until rows) return AnnotatedString("")
        val row = chars[r]
        val f = fgs[r]
        return buildAnnotatedString {
            var start = 0
            while (start < cols) {
                var end = start + 1
                while (end < cols && f[end] == f[start]) end++
                val color = if (f[start] == 0) defColor else Color(f[start])
                withStyle(SpanStyle(color = color)) { append(String(row, start, end - start)) }
                start = end
            }
            if (cursorBg != null && r == curR) {
                val c = curC.coerceIn(0, cols - 1)
                addStyle(SpanStyle(background = cursorBg, color = cursorFg), c, c + 1)
            }
        }
    }

    /* ------------------------------ parser ------------------------------ */

    private fun put(ch: Char) {
        when (state) {
            0 -> when (ch) {
                '\u001B' -> state = 1
                '\r' -> curC = 0
                '\n' -> lineFeed()
                '\b' -> if (curC > 0) curC--
                '\t' -> curC = minOf(cols - 1, (curC / 8 + 1) * 8)
                else -> if (ch >= ' ' && ch != '\u007F') putChar(ch)
            }
            1 -> when (ch) {
                '[' -> { params.clear(); state = 2 }
                ']' -> state = 3
                '(', ')' -> state = 4
                else -> state = 0
            }
            2 -> if (ch.code in 0x20..0x3F) params.append(ch) else { csi(ch); state = 0 }
            3 -> when (ch) {
                '\u0007' -> state = 0
                '\u001B' -> state = 5
            }
            else -> state = 0
        }
    }

    private fun putChar(ch: Char) {
        if (curC >= cols) { curC = 0; lineFeed() }
        chars[curR][curC] = ch
        fgs[curR][curC] = fg
        curC++
    }

    private fun lineFeed() {
        if (curR == rows - 1) scrollUp() else curR++
    }

    private fun scrollUp() {
        for (i in 0 until rows - 1) { chars[i] = chars[i + 1]; fgs[i] = fgs[i + 1] }
        chars[rows - 1] = CharArray(cols) { ' ' }
        fgs[rows - 1] = IntArray(cols)
    }

    private fun clearRange(r: Int, from: Int, to: Int) {
        for (c in from.coerceAtLeast(0) until to.coerceAtMost(cols)) { chars[r][c] = ' '; fgs[r][c] = 0 }
    }

    private fun csi(cmd: Char) {
        val raw = params.toString()
        if (raw.startsWith("?") || raw.startsWith(">")) return
        val p = raw.split(';').map { it.toIntOrNull() }
        fun cnt() = maxOf(1, p.getOrNull(0) ?: 1)
        when (cmd) {
            'A' -> curR = maxOf(0, curR - cnt())
            'B' -> curR = minOf(rows - 1, curR + cnt())
            'C' -> curC = minOf(cols - 1, curC + cnt())
            'D' -> curC = maxOf(0, curC - cnt())
            'G' -> curC = (cnt() - 1).coerceIn(0, cols - 1)
            'd' -> curR = (cnt() - 1).coerceIn(0, rows - 1)
            'H', 'f' -> {
                curR = (maxOf(1, p.getOrNull(0) ?: 1) - 1).coerceIn(0, rows - 1)
                curC = (maxOf(1, p.getOrNull(1) ?: 1) - 1).coerceIn(0, cols - 1)
            }
            'J' -> when (p.getOrNull(0) ?: 0) {
                0 -> { clearRange(curR, curC, cols); for (r in curR + 1 until rows) clearRange(r, 0, cols) }
                1 -> { for (r in 0 until curR) clearRange(r, 0, cols); clearRange(curR, 0, curC + 1) }
                else -> for (r in 0 until rows) clearRange(r, 0, cols)
            }
            'K' -> when (p.getOrNull(0) ?: 0) {
                0 -> clearRange(curR, curC, cols)
                1 -> clearRange(curR, 0, curC + 1)
                else -> clearRange(curR, 0, cols)
            }
            'X' -> clearRange(curR, curC, minOf(cols, curC + cnt()))
            'P' -> {
                val n = cnt(); val row = chars[curR]; val f = fgs[curR]
                for (i in curC until cols) {
                    val src = i + n
                    row[i] = if (src < cols) row[src] else ' '
                    f[i] = if (src < cols) f[src] else 0
                }
            }
            '@' -> {
                val n = cnt(); val row = chars[curR]; val f = fgs[curR]
                for (i in cols - 1 downTo curC) {
                    val src = i - n
                    row[i] = if (src >= curC) row[src] else ' '
                    f[i] = if (src >= curC) f[src] else 0
                }
            }
            'L' -> {
                val n = cnt()
                for (i in rows - 1 downTo curR) {
                    val src = i - n
                    if (src >= curR) { chars[i] = chars[src]; fgs[i] = fgs[src] }
                    else { chars[i] = CharArray(cols) { ' ' }; fgs[i] = IntArray(cols) }
                }
            }
            'M' -> {
                val n = cnt()
                for (i in curR until rows) {
                    val src = i + n
                    if (src < rows) { chars[i] = chars[src]; fgs[i] = fgs[src] }
                    else { chars[i] = CharArray(cols) { ' ' }; fgs[i] = IntArray(cols) }
                }
            }
            'm' -> sgr(p)
        }
    }

    private fun sgr(p: List<Int?>) {
        val list = p.map { it ?: 0 }
        var i = 0
        while (i < list.size) {
            when (val c = list[i]) {
                0 -> { fg = 0; bold = false }
                1 -> bold = true
                22 -> bold = false
                in 30..37 -> fg = PALETTE[c - 30 + if (bold) 8 else 0]
                39 -> fg = 0
                in 90..97 -> fg = PALETTE[c - 90 + 8]
                38 -> {
                    if (list.getOrNull(i + 1) == 5 && i + 2 < list.size) {
                        fg = xterm(list[i + 2]); i += 2
                    } else if (list.getOrNull(i + 1) == 2 && i + 4 < list.size) {
                        fg = rgb(list[i + 2], list[i + 3], list[i + 4]); i += 4
                    }
                }
            }
            i++
        }
    }

    private companion object {
        fun rgb(r: Int, g: Int, b: Int): Int =
            (0xFF shl 24) or (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)

        val PALETTE: IntArray = listOf(
            0xFF45475A, 0xFFF38BA8, 0xFFA6E3A1, 0xFFF9E2AF,
            0xFF89B4FA, 0xFFF5C2E7, 0xFF94E2D5, 0xFFBAC2DE,
            0xFF585B70, 0xFFF38BA8, 0xFFA6E3A1, 0xFFF9E2AF,
            0xFF89B4FA, 0xFFF5C2E7, 0xFF94E2D5, 0xFFA6ADC8
        ).map { it.toInt() }.toIntArray()

        fun xterm(n0: Int): Int {
            val n = n0.coerceIn(0, 255)
            return when {
                n < 16 -> PALETTE[n]
                n < 232 -> {
                    val v = n - 16
                    fun lv(x: Int) = if (x == 0) 0 else 55 + x * 40
                    rgb(lv(v / 36), lv(v / 6 % 6), lv(v % 6))
                }
                else -> { val g = 8 + (n - 232) * 10; rgb(g, g, g) }
            }
        }
    }
}
