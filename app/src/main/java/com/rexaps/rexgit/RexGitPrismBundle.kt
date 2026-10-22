package com.rexaps.rexgit

import io.noties.prism4j.annotations.PrismBundle

/**
 * Generates RexGitGrammarLocator via the Prism4j annotation processor.
 * The app module needs the Prism4j bundler configured as KAPT (see build notes).
 */
@PrismBundle(
    includes = ["clike", "java", "javascript", "json", "kotlin", "markup", "css", "yaml", "python", "markdown", "sql", "go", "swift"],
    grammarLocatorClassName = ".RexGitGrammarLocator"
)
class RexGitPrismBundle
