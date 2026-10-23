package com.rexaps.rexgit;

import io.noties.prism4j.annotations.PrismBundle;

/**
 * Java marker for Prism4j's annotation processor.
 * Keep this in Java: the generated RexGitGrammarLocator is referenced from Kotlin.
 * Do not add a Kotlin class with the same name, or KAPT will report a duplicate class.
 */
@PrismBundle(
    include = {
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
