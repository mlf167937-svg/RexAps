package com.rexaps.rexgit

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight

/** Lightweight syntax highlighting. No parser/compiler/dependency required. */
enum class RexGitLanguage {
    HTML, JSON, JAVASCRIPT, KOTLIN, JAVA, CSS, XML, YAML, PLAIN
}

data class RexGitSyntaxColors(
    val keyword: Color,
    val string: Color,
    val number: Color,
    val comment: Color,
    val type: Color,
    val property: Color,
    val tag: Color,
    val punctuation: Color
)

object RexGitSyntax {
    fun detect(path: String): RexGitLanguage {
        return when (path.substringAfterLast('.', "").lowercase()) {
            "html", "htm" -> RexGitLanguage.HTML
            "json" -> RexGitLanguage.JSON
            "js", "mjs", "cjs" -> RexGitLanguage.JAVASCRIPT
            "kt", "kts" -> RexGitLanguage.KOTLIN
            "java" -> RexGitLanguage.JAVA
            "css" -> RexGitLanguage.CSS
            "xml" -> RexGitLanguage.XML
            "yml", "yaml" -> RexGitLanguage.YAML
            else -> RexGitLanguage.PLAIN
        }
    }

    fun label(language: RexGitLanguage): String = when (language) {
        RexGitLanguage.HTML -> "HTML"
        RexGitLanguage.JSON -> "JSON"
        RexGitLanguage.JAVASCRIPT -> "JavaScript"
        RexGitLanguage.KOTLIN -> "Kotlin"
        RexGitLanguage.JAVA -> "Java"
        RexGitLanguage.CSS -> "CSS"
        RexGitLanguage.XML -> "XML"
        RexGitLanguage.YAML -> "YAML"
        RexGitLanguage.PLAIN -> "Plain text"
    }

    fun highlight(
        text: String,
        language: RexGitLanguage,
        colors: RexGitSyntaxColors
    ): AnnotatedString {
        if (language == RexGitLanguage.PLAIN || text.isEmpty()) return AnnotatedString(text)

        val builder = AnnotatedString.Builder(text)
        val keywords = keywords(language)
        var i = 0

        fun span(start: Int, end: Int, color: Color, bold: Boolean = false) {
            if (end > start) {
                builder.addStyle(
                    SpanStyle(
                        color = color,
                        fontWeight = if (bold) FontWeight.SemiBold else null
                    ),
                    start,
                    end
                )
            }
        }

        while (i < text.length) {
            val c = text[i]

            // Line/block comments.
            if ((c == '/' && i + 1 < text.length && text[i + 1] == '/') ||
                (c == '#' && language == RexGitLanguage.YAML)
            ) {
                val end = text.indexOf('\n', i).let { if (it < 0) text.length else it }
                span(i, end, colors.comment)
                i = end
                continue
            }
            if (c == '/' && i + 1 < text.length && text[i + 1] == '*') {
                val end0 = text.indexOf("*/", i + 2)
                val end = if (end0 < 0) text.length else end0 + 2
                span(i, end, colors.comment)
                i = end
                continue
            }
            if ((language == RexGitLanguage.HTML || language == RexGitLanguage.XML) &&
                text.startsWith("<!--", i)
            ) {
                val end0 = text.indexOf("-->", i + 4)
                val end = if (end0 < 0) text.length else end0 + 3
                span(i, end, colors.comment)
                i = end
                continue
            }

            // Quoted strings. JSON object keys and YAML keys get property color.
            if (c == '\"' || c == '\'') {
                val quote = c
                var j = i + 1
                var escaped = false
                while (j < text.length) {
                    val ch = text[j]
                    if (ch == quote && !escaped) {
                        j++
                        break
                    }
                    escaped = ch == '\\' && !escaped
                    if (ch != '\\') escaped = false
                    j++
                }
                val next = text.substring(j).takeWhile { it.isWhitespace() }.firstOrNull()
                val property = next == ':' &&
                    (language == RexGitLanguage.JSON || language == RexGitLanguage.YAML || language == RexGitLanguage.CSS)
                span(i, j, if (property) colors.property else colors.string)
                i = j
                continue
            }

            // HTML/XML tag names and attributes.
            if ((language == RexGitLanguage.HTML || language == RexGitLanguage.XML) && c == '<') {
                var j = i + 1
                if (j < text.length && text[j] == '/') j++
                while (j < text.length && text[j].isWhitespace()) j++
                val tagStart = j
                while (j < text.length && (text[j].isLetterOrDigit() || text[j] == ':' || text[j] == '-' || text[j] == '_')) j++
                if (j > tagStart) span(tagStart, j, colors.tag, bold = true)
                i = maxOf(j, i + 1)
                continue
            }

            // Numbers.
            if (c.isDigit() && (i == 0 || !text[i - 1].isLetterOrDigit() && text[i - 1] != '_')) {
                var j = i + 1
                while (j < text.length && (text[j].isDigit() || text[j] == '.' || text[j] == '_')) j++
                span(i, j, colors.number)
                i = j
                continue
            }

            // Identifiers / keywords / properties.
            if (c.isLetter() || c == '_' || c == '$') {
                var j = i + 1
                while (j < text.length && (text[j].isLetterOrDigit() || text[j] == '_' || text[j] == '$' || text[j] == '-')) j++
                val word = text.substring(i, j)
                val after = text.substring(j).dropWhile { it.isWhitespace() }.firstOrNull()
                when {
                    word in keywords -> span(i, j, colors.keyword, bold = true)
                    word in typeNames(language) -> span(i, j, colors.type)
                    after == ':' && (language == RexGitLanguage.CSS || language == RexGitLanguage.YAML) -> span(i, j, colors.property)
                    language == RexGitLanguage.HTML || language == RexGitLanguage.XML -> span(i, j, colors.property)
                }
                i = j
                continue
            }

            if (c in "{}[]():;,.") span(i, i + 1, colors.punctuation)
            i++
        }

        return builder.toAnnotatedString()
    }

    private fun keywords(language: RexGitLanguage): Set<String> = when (language) {
        RexGitLanguage.KOTLIN -> setOf("class", "object", "fun", "val", "var", "if", "else", "when", "for", "while", "do", "return", "in", "is", "as", "try", "catch", "finally", "throw", "import", "package", "private", "public", "protected", "internal", "open", "override", "data", "sealed", "enum", "interface", "abstract", "companion", "const", "lateinit", "suspend", "inline", "reified", "null", "true", "false")
        RexGitLanguage.JAVA -> setOf("class", "interface", "enum", "extends", "implements", "public", "private", "protected", "static", "final", "abstract", "void", "new", "return", "if", "else", "for", "while", "do", "switch", "case", "break", "continue", "try", "catch", "finally", "throw", "throws", "import", "package", "this", "super", "true", "false", "null", "synchronized")
        RexGitLanguage.JAVASCRIPT -> setOf("const", "let", "var", "function", "class", "extends", "new", "return", "if", "else", "for", "while", "do", "switch", "case", "break", "continue", "try", "catch", "finally", "throw", "import", "export", "from", "async", "await", "typeof", "instanceof", "in", "of", "this", "true", "false", "null", "undefined")
        RexGitLanguage.JSON -> setOf("true", "false", "null")
        RexGitLanguage.CSS -> setOf("@media", "@font-face", "@import", "important")
        RexGitLanguage.HTML, RexGitLanguage.XML -> setOf("DOCTYPE", "true", "false")
        RexGitLanguage.YAML -> setOf("true", "false", "null", "yes", "no", "on", "off")
        RexGitLanguage.PLAIN -> emptySet()
    }

    private fun typeNames(language: RexGitLanguage): Set<String> = when (language) {
        RexGitLanguage.KOTLIN -> setOf("String", "Int", "Long", "Float", "Double", "Boolean", "Unit", "Any", "Nothing", "List", "Set", "Map", "MutableList", "MutableMap", "Array")
        RexGitLanguage.JAVA -> setOf("String", "int", "long", "float", "double", "boolean", "byte", "short", "char", "void", "Object", "Integer", "Long", "Boolean", "List", "Map", "Set")
        RexGitLanguage.JAVASCRIPT -> setOf("Array", "Object", "String", "Number", "Boolean", "Promise", "Date", "RegExp", "Map", "Set")
        else -> emptySet()
    }
}
