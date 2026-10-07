package com.rexaps.librex.rexcode

import com.rexaps.librex.rexcode.languages.*

object RexLanguageRegistry {
    private val definitions: List<RexLanguageDefinition> = listOf(
        JavaScriptSyntax, TypeScriptSyntax, JavaSyntax, KotlinSyntax, PythonSyntax,
        HtmlSyntax, CssSyntax, JsonSyntax, XmlSyntax, CSyntax, CppSyntax, CSharpSyntax,
        GoSyntax, RustSyntax, PhpSyntax, SqlSyntax, ShellSyntax, YamlSyntax, MarkdownSyntax,
        DartSyntax, SwiftSyntax, RubySyntax, JsxSyntax, TsxSyntax, VueSyntax, GradleSyntax,
        KtsSyntax, TomlSyntax, EnvSyntax, GitignoreSyntax, DockerfileSyntax, BatchSyntax,
        RSyntax, LuaSyntax, ScssSyntax, SassSyntax, LessSyntax, IniSyntax, TextSyntax
    )

    private val byLanguage = definitions.associateBy { it.language }
    private val byExtension = definitions.flatMap { d -> d.extensions.map { it.lowercase() to d } }.toMap()
    private val byFileName = definitions.flatMap { d -> d.fileNames.map { it.lowercase() to d } }.toMap()

    fun all(): List<RexLanguageDefinition> = definitions
    fun get(language: RexLanguage): RexLanguageDefinition = byLanguage[language] ?: TextSyntax
    fun findByExtension(extension: String): RexLanguageDefinition = byExtension[extension.removePrefix(".").lowercase()] ?: TextSyntax
    fun findByFileName(name: String): RexLanguageDefinition = byFileName[name.lowercase()] ?: findByExtension(name.substringAfterLast('.', ""))
}
