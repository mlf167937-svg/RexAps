package com.rexaps.librex.rexcode.lexer

import com.rexaps.librex.rexcode.*

class RexLexer(private val definition: RexLanguageDefinition) {
    private val word = Regex("[A-Za-z_][A-Za-z0-9_]*")
    private val number = Regex("(?:0[xX][0-9A-Fa-f]+|0[bB][01]+|(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][+-]?\\d+)?)")

    fun tokenize(source: String): List<RexToken> {
        if (source.isEmpty()) return emptyList()
        val out = ArrayList<RexToken>()
        var i = 0
        var line = 0
        var column = 0
        var lineStart = 0

        fun add(type: RexTokenType, start: Int, end: Int, atLine: Int, atColumn: Int) {
            if (end > start) out += RexToken(type, source.substring(start, end), start, end, atLine, atColumn)
        }
        fun advance(end: Int) {
            var p = i
            while (p < end) {
                if (source[p] == '\n') { line++; lineStart = p + 1; column = 0 } else column++
                p++
            }
            i = end
        }
        fun startsAt(value: String): Boolean = value.isNotEmpty() && source.startsWith(value, i)

        while (i < source.length) {
            val start = i; val l = line; val c = column; val ch = source[i]
            val lineComment = definition.lineComments.firstOrNull(::startsAt)
            if (lineComment != null) {
                var e = source.indexOf('\n', i).let { if (it < 0) source.length else it }
                add(if (lineComment == "///" || lineComment == "//!" ) RexTokenType.DOC_COMMENT else RexTokenType.COMMENT, i, e, l, c); advance(e); continue
            }
            val block = definition.blockComments.firstOrNull { startsAt(it.first) }
            if (block != null) {
                val e0 = source.indexOf(block.second, i + block.first.length)
                val e = if (e0 < 0) source.length else e0 + block.second.length
                add(if (block.first == "/**") RexTokenType.DOC_COMMENT else RexTokenType.COMMENT, i, e, l, c); advance(e); continue
            }
            if (ch == '"' || ch == '\'' || ch == '`') {
                val quote = ch; var e = i + 1; var escaped = false
                while (e < source.length) {
                    val q = source[e]
                    if (!escaped && q == quote) { e++; break }
                    escaped = !escaped && q == '\\'
                    if (q != '\\') escaped = false
                    e++
                }
                add(RexTokenType.STRING, i, e, l, c); advance(e); continue
            }
            if (ch.isDigit() || (ch == '.' && i + 1 < source.length && source[i + 1].isDigit())) {
                val m = number.find(source, i)
                if (m != null && m.range.first == i) { add(RexTokenType.NUMBER, i, m.range.last + 1, l, c); advance(m.range.last + 1); continue }
            }
            val wm = word.find(source, i)
            if (wm != null && wm.range.first == i) {
                val text = wm.value
                val type = when {
                    text in definition.keywords -> RexTokenType.KEYWORD
                    text in definition.types -> RexTokenType.TYPE
                    text in definition.literals -> if (text == "true" || text == "false") RexTokenType.BOOLEAN else RexTokenType.NULL
                    text.firstOrNull()?.isUpperCase() == true -> RexTokenType.CLASS
                    source.getOrNull(wm.range.last + 1) == '(' -> RexTokenType.FUNCTION
                    text.all { it.isUpperCase() || it == '_' || it.isDigit() } -> RexTokenType.CONSTANT
                    else -> RexTokenType.VARIABLE
                }
                add(type, i, wm.range.last + 1, l, c); advance(wm.range.last + 1); continue
            }
            val op = definition.operators.sortedByDescending { it.length }.firstOrNull(::startsAt)
            if (op != null) { add(RexTokenType.OPERATOR, i, i + op.length, l, c); advance(i + op.length); continue }
            val type = when (ch) {
                '(', ')', '[', ']', '{', '}' -> RexTokenType.DELIMITER
                ',', ';', ':' -> RexTokenType.PUNCTUATION
                else -> RexTokenType.TEXT
            }
            add(type, start, start + 1, l, c); advance(start + 1)
        }
        return out
    }
}
