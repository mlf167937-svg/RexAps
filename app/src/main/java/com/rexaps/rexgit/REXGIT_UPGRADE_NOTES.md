# RexGit source upgrade notes

## Changes in this bundle

- Fixed GitHub Actions workflow-list parsing. `GET /repos/{owner}/{repo}/actions/workflows` returns a JSON object envelope with a `workflows` array; RexGit previously tried to parse the complete object as a top-level `JSONArray`.
- Added defensive parsing for GitHub collection envelopes and a clear error if a successful API response has an unexpected shape. Workflow runs and per-run artifacts use the same helper.
- Removed the Prism4j annotation-bundle marker entirely and replaced the generated `RexGitGrammarLocator` dependency with a Kotlin `GrammarLocator` implementation. This avoids the earlier `No languages are specified to be included`, duplicate-class, and wrong-annotation-member failure path while preserving a lightweight grammar for common code blocks. No hand-authored Java or generated locator is required.
- Added a shared RexGit Material 3 color system with distinct, system-following light and dark palettes, accessible surface variants, and a consistent blue developer-tool accent.
- Fixed editor Search/Next so the first result is not skipped after entering a new query.
- Added actionable messaging for push authentication/permission failures and non-fast-forward rejections; no force-push or silent reset is attempted.
- Preserved current editor, Git operation, Markdown, repository, release, and screen entry point files rather than replacing APIs with mock behavior.

## Important compatibility notes

This archive contains only the RexGit source files. It does **not** include the application/module Gradle files, version catalog, manifest, resources, test source sets, wrapper, or parent-module contracts. Therefore dependency versions, KAPT configuration, duplicate dependency exclusions, manifest integration, and full app build cannot be verified or safely rewritten from this archive.

Prism syntax highlighting now constructs lightweight grammars in `RexGitGrammarLocator.kt` using the Prism4j core API. Since no `@PrismBundle` annotation is used, the Prism4j bundler annotation processor is no longer needed for RexGit. The host Gradle file was not in the uploaded archive; if it explicitly adds `prism4j-bundler` only for RexGit, remove that KAPT/annotation-processor dependency in the host project. Keep the `io.noties:prism4j` core dependency used by Markwon.

## Verification record

- PASS: source archive inspected and unpacked.
- PASS: Actions parsing code now reads the `workflows` array from the response object envelope and guards against unexpected response shapes.
- PASS: only `.kt` code source files remain in the RexGit module source directory; the old `RexGitPrismBundle.java` and Kotlin marker were removed.
- PASS: `RexGitGrammarLocator.kt` compiled with `kotlinc` against an API-shaped Prism4j stub; each defined grammar was instantiated and its regex patterns compiled in that check. This is a targeted source/regex check, not a substitute for compiling against the app's resolved Prism4j artifact.
- PASS: ZIP structure and key source invariants checked after packaging.
- NOT RUN: Android/Compose app compilation, full dependency resolution, unit tests, emulator screenshots, and live GitHub Actions requests. The uploaded source-only archive lacks Gradle configuration, and those results must not be inferred from static source checks.

## Host-project verification

In the complete Android project, run the configured equivalent of:

    ./gradlew clean :app:assembleDebug
    ./gradlew :app:testDebugUnitTest

Then open a repository with at least one Actions workflow. Confirm that the Actions tab shows the returned workflow count, run list, run status, and artifact list. A token needs access to the repository and sufficient Actions read permission for private repositories.
