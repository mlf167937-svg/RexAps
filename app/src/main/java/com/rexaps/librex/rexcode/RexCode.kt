package com.rexaps.librex.rexcode

import com.rexaps.librex.rexcode.detection.RexLanguageDetector
import com.rexaps.librex.rexcode.lexer.RexLexer

object RexCode {
    fun language(fileName: String): RexLanguage = RexLanguageDetector.detect(fileName)
    fun definition(language: RexLanguage): RexLanguageDefinition = RexLanguageRegistry.get(language)
    fun tokenize(source: String, language: RexLanguage): List<RexToken> = RexLexer(definition(language)).tokenize(source)
    fun highlight(source: String, language: RexLanguage): RexSyntaxResult = RexSyntaxResult(language, tokenize(source, language))
    fun highlight(source: String, fileName: String): RexSyntaxResult = highlight(source, language(fileName))
}
