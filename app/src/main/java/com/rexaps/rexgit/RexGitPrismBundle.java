package com.rexaps.rexgit;

import io.noties.prism4j.annotations.PrismBundle;

/**
 * Generates RexGitGrammarLocator for Markwon syntax highlighting.
 * Kept in Java so Prism4j's annotation processor receives the includes array
 * directly instead of relying on Kotlin KAPT stubs for this annotation.
 */
@PrismBundle(
    includes = {
        "clike",
        "java",
        "javascript",
        "json",
        "kotlin",
        "markup",
        "css",
        "yaml",
        "python",
        "markdown",
        "sql",
        "go",
        "swift"
    },
    grammarLocatorClassName = ".RexGitGrammarLocator"
)
public final class RexGitPrismBundle {
    private RexGitPrismBundle() {
        // Annotation-only marker class.
    }
}
