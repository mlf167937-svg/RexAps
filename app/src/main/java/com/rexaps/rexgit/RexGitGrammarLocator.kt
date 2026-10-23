package com.rexaps.rexgit

import io.noties.prism4j.GrammarLocator
import io.noties.prism4j.Prism4j
import java.util.Locale
import java.util.regex.Pattern

/**
 * Kotlin-only Prism4j grammar locator.
 *
 * This deliberately avoids a source annotation processor: language grammars are
 * defined here, so KAPT does not need to generate a Java RexGitGrammarLocator
 * from a Prism bundle annotation. The highlighter is intentionally lightweight,
 * with plain Markdown rendering as a safe fallback for unrecognized languages.
 */
internal class RexGitGrammarLocator : GrammarLocator {

    private val supported = setOf(
        "clike", "java", "javascript", "json", "kotlin", "markup", "css",
        "yaml", "python", "markdown", "sql", "go", "swift", "html", "xml",
        "js", "mjs", "cjs", "ts", "tsx", "typescript", "kt", "kts", "py", "yml", "sh", "bash", "shell"
    )

    override fun languages(): Set<String> = supported

    override fun grammar(prism4j: Prism4j, language: String): Prism4j.Grammar? {
        val normalized = language.lowercase(Locale.ROOT)
        val canonical = when (normalized) {
            "js", "mjs", "cjs", "ts", "tsx", "typescript" -> "javascript"
            "kt", "kts" -> "kotlin"
            "html", "xml", "svg", "xhtml" -> "markup"
            "yml" -> "yaml"
            "py" -> "python"
            "sh", "bash", "shell" -> "clike"
            else -> normalized
        }
        return when (canonical) {
            "json" -> jsonGrammar()
            "markup" -> markupGrammar()
            "css" -> cssGrammar()
            "yaml" -> yamlGrammar()
            "markdown" -> markdownGrammar()
            "python" -> codeGrammar(
                "python",
                keywords = PYTHON_KEYWORDS,
                lineComment = "#[^\\r\\n]*"
            )
            "sql" -> codeGrammar(
                "sql",
                keywords = SQL_KEYWORDS,
                lineComment = "--[^\\r\\n]*",
                blockComment = "/\\*[\\s\\S]*?\\*/"
            )
            "java", "kotlin", "javascript", "go", "swift", "clike" ->
                codeGrammar(
                    canonical,
                    keywords = languageKeywords(canonical),
                    lineComment = "//[^\\r\\n]*",
                    blockComment = "/\\*[\\s\\S]*?\\*/"
                )
            else -> null
        }
    }

    private fun jsonGrammar(): Prism4j.Grammar = grammar(
        "json",
        token("property", "\\\"(?:\\\\.|[^\\\"\\\\])*\\\"(?=\\s*:)"),
        token("string", "\\\"(?:\\\\.|[^\\\"\\\\])*\\\""),
        token("number", "(?<![A-Za-z_])(?:-?0[xX][0-9A-Fa-f]+|-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)"),
        token("boolean", "\\b(?:true|false|null)\\b"),
        token("punctuation", "[{}\\[\\],:]"),
        token("operator", "-")
    )

    private fun markupGrammar(): Prism4j.Grammar = grammar(
        "markup",
        token("comment", "<!--[\\s\\S]*?-->"),
        token("doctype", "<!DOCTYPE[^>]*>"),
        token("tag", "</?[A-Za-z][A-Za-z0-9:_-]*(?:\\s+[^<>]*?)?\\s*/?>"),
        token("entity", "&(?:#x?[0-9A-Fa-f]+|[A-Za-z][A-Za-z0-9]+);")
    )

    private fun cssGrammar(): Prism4j.Grammar = grammar(
        "css",
        token("comment", "/\\*[\\s\\S]*?\\*/"),
        token("string", "\\\"(?:\\\\.|[^\\\"\\\\])*\\\"|'(?:\\\\.|[^'\\\\])*'"),
        token("atrule", "@[A-Za-z-]+"),
        token("property", "(?m)^[ \\t]*[-A-Za-z_][A-Za-z0-9_-]*(?=\\s*:)"),
        token("selector", "[.#]?[A-Za-z_][A-Za-z0-9_-]*(?=\\s*\\{)"),
        token("number", "(?<![A-Za-z_-])\\d+(?:\\.\\d+)?(?:px|em|rem|%|vh|vw|s|ms)?"),
        token("important", "!important"),
        token("punctuation", "[{}();:,]")
    )

    private fun yamlGrammar(): Prism4j.Grammar = grammar(
        "yaml",
        token("comment", "(?m)^[ \\t]*#[^\\r\\n]*"),
        token("key", "(?m)^[ \\t]*[A-Za-z_][A-Za-z0-9_.-]*(?=\\s*:)"),
        token("string", "\\\"(?:\\\\.|[^\\\"\\\\])*\\\"|'(?:\\\\.|[^'\\\\])*'"),
        token("boolean", "\\b(?:true|false|yes|no|null|~)\\b"),
        token("number", "(?<![A-Za-z_])[-+]?\\d+(?:\\.\\d+)?"),
        token("punctuation", "[\\[\\]{}:,|>&*!%@`-]")
    )

    private fun markdownGrammar(): Prism4j.Grammar = grammar(
        "markdown",
        token("code", "`{1,3}[^`\\r\\n]*`{1,3}"),
        token("title", "(?m)^#{1,6}\\s+[^\\r\\n]+"),
        token("bold", "\\*\\*[^*]+\\*\\*|__[^_]+__"),
        token("italic", "\\*[^*]+\\*|_[^_]+_"),
        token("url", "\\[[^]]+\\]\\([^ )]+\\)"),
        token("blockquote", "(?m)^>[^\\r\\n]*"),
        token("list", "(?m)^\\s*(?:[-*+] |\\d+\\. )")
    )

    private fun codeGrammar(
        name: String,
        keywords: Set<String>,
        lineComment: String? = null,
        blockComment: String? = null
    ): Prism4j.Grammar {
        val tokens = mutableListOf<Prism4j.Token>()
        // Comments are deliberately tokenized before keywords and punctuation.
        blockComment?.let { tokens += token("comment", it) }
        lineComment?.let { tokens += token("comment", it) }
        tokens += token("string", "\\\"(?:\\\\.|[^\\\"\\\\])*\\\"|'(?:\\\\.|[^'\\\\])*'")
        tokens += token("keyword", keywordRegex(keywords))
        tokens += token("boolean", "\\b(?:true|false|null|undefined|nil|None|True|False)\\b")
        tokens += token("number", "(?<![A-Za-z_])(?:0[xX][0-9A-Fa-f]+|\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)")
        tokens += token("class-name", "\\b[A-Z][A-Za-z0-9_]*\\b")
        tokens += token("function", "\\b[A-Za-z_][A-Za-z0-9_]*(?=\\s*\\()")
        tokens += token("punctuation", "[{}\\[\\]();,.]")
        tokens += token("operator", "(?:===?|!==?|=>|->|\\+\\+|--|[+*/%=!<>|&^~-])")
        return Prism4j.grammar(name, *tokens.toTypedArray())
    }

    private fun languageKeywords(language: String): Set<String> = when (language) {
        "java" -> JAVA_KEYWORDS
        "kotlin" -> KOTLIN_KEYWORDS
        "javascript" -> JAVASCRIPT_KEYWORDS
        "go" -> GO_KEYWORDS
        "swift" -> SWIFT_KEYWORDS
        else -> CLIKE_KEYWORDS
    }

    private fun keywordRegex(words: Set<String>): String =
        "\\b(?:" + words.sorted().joinToString("|") { Pattern.quote(it) } + ")\\b"

    private fun token(name: String, regex: String): Prism4j.Token =
        Prism4j.token(name, Prism4j.pattern(Pattern.compile(regex)))

    private fun grammar(name: String, vararg tokens: Prism4j.Token): Prism4j.Grammar =
        Prism4j.grammar(name, *tokens)

    private companion object {
        val CLIKE_KEYWORDS = setOf(
            "abstract", "as", "async", "await", "break", "case", "catch", "class",
            "const", "continue", "default", "do", "else", "enum", "export", "extends",
            "final", "finally", "for", "from", "function", "if", "implements", "import",
            "in", "instanceof", "interface", "let", "namespace", "new", "override",
            "package", "private", "protected", "public", "return", "static", "super",
            "switch", "this", "throw", "try", "typeof", "var", "void", "while", "with",
            "yield"
        )
        val JAVA_KEYWORDS = CLIKE_KEYWORDS + setOf(
            "boolean", "byte", "char", "double", "float", "int", "long", "short",
            "synchronized", "throws", "transient", "volatile"
        )
        val KOTLIN_KEYWORDS = JAVA_KEYWORDS + setOf(
            "data", "fun", "val", "var", "when", "object", "companion", "suspend",
            "internal", "reified", "sealed", "lateinit", "by", "is", "in", "out",
            "where", "typealias", "it"
        )
        val JAVASCRIPT_KEYWORDS = CLIKE_KEYWORDS + setOf(
            "delete", "debugger", "of", "get", "set", "null", "undefined"
        )
        val PYTHON_KEYWORDS = setOf(
            "and", "as", "assert", "async", "await", "break", "class", "continue",
            "def", "del", "elif", "else", "except", "finally", "for", "from", "global",
            "if", "import", "in", "is", "lambda", "nonlocal", "not", "or", "pass",
            "raise", "return", "try", "while", "with", "yield", "match", "case"
        )
        val SQL_KEYWORDS = setOf(
            "all", "alter", "and", "as", "asc", "begin", "by", "case", "check", "column",
            "commit", "create", "delete", "desc", "distinct", "drop", "else", "end",
            "exists", "from", "group", "having", "index", "insert", "into", "join", "key",
            "left", "limit", "not", "null", "on", "or", "order", "primary", "references",
            "right", "rollback", "select", "set", "table", "then", "union", "unique",
            "update", "values", "when", "where"
        )
        val GO_KEYWORDS = CLIKE_KEYWORDS + setOf(
            "chan", "defer", "fallthrough", "go", "map", "range", "select", "struct",
            "type", "bool", "byte", "complex64", "complex128", "error", "float32",
            "float64", "int", "int8", "int16", "int32", "int64", "rune", "string", "uint"
        )
        val SWIFT_KEYWORDS = CLIKE_KEYWORDS + setOf(
            "deinit", "didSet", "get", "guard", "init", "inout", "lazy", "mutating",
            "nil", "protocol", "repeat", "set", "some", "subscript", "typealias", "where"
        )
    }
}
