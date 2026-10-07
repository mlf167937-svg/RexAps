package com.rexaps.librex.rexcode

data class RexSyntaxResult(
    val language: RexLanguage,
    val tokens: List<RexToken>
)

interface RexLanguageDefinition {
    val language: RexLanguage
    val extensions: Set<String>
    val fileNames: Set<String> get() = emptySet()
    val keywords: Set<String> get() = emptySet()
    val types: Set<String> get() = emptySet()
    val literals: Set<String> get() = setOf("true", "false", "null", "nil", "None", "undefined")
    val lineComments: List<String> get() = emptyList()
    val blockComments: List<Pair<String, String>> get() = emptyList()
    val operators: Set<String> get() = emptySet()
}
