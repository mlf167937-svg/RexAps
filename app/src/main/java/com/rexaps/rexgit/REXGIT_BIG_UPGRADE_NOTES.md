# RexGit — Big Upgrade (source package)

This is a source-only update. The uploaded ZIP contained Kotlin files but no Gradle wrapper/build scripts, AndroidManifest.xml, version catalog, or project-level dependency setup. A reproducible Android build could therefore not be run here. Copy the Kotlin source files into the same package in the real app and apply the dependency instructions below before building.

## Previous RexGit behavior included in this package

- Standard Git Push works without enabling GitDev. GitDev is optional for metadata/version/folder behavior.
- Commit types include Change, Fix, Add, Upgrade, Update, Testing, and Delete.
- `token.env` supports as many numbered account entries as needed with `Account-1-Username`, `Account-1-Token`, `Account-2-Username`, `Account-2-Token`, etc.
- Account-owned repositories are stored under `Download/RexAps/Github/<GitHub-username>/<Repo-name>/`; global clones use `Download/RexAps/Github/Global/<Owner>/<Repo-name>/`.
- Repository scanning intentionally ignores the old flat `Github/<Repo-name>/` layout.
- GitHub search supports repository keyword search and clone into the global owner/repository structure.

## Changes in this source pass

- README preview is rendered by Markwon using Android-native text rendering rather than a WebView. This avoids executing README HTML/JavaScript.
- GFM extensions included: tables, task lists, strikethrough, linkification, fenced-code syntax highlighting, light/dark syntax themes, and visible language labels for recognized fenced-code languages.
- HTML handling includes the Markdown renderer's built-in handlers (for example `strong`, headings, links, images, lists, `br`, `sub`, `sup`) and a restricted alignment handler for `p`, `div`, and `center`. The custom style handler recognizes only `text-align`; it does not run CSS or scripts. HTML `<img>` uses Markwon's image subsystem; `androidsvg` support is needed for SVG Shields badges.
- Remote images have loading placeholders. Relative image paths are resolved to GitHub raw content based on the repository URL/current branch where possible.
- Relative links to existing repository files open them in RexGit's file editor, with a canonical-path check to prevent path traversal. External `http`, `https`, and `mailto` links are opened externally; unsafe schemes such as `javascript:` and `file:` are ignored. Heading-anchor auto-scroll is not yet implemented.
- README preview shows repository avatar, description, stars, forks, license, language, visibility, default/current branch, last-updated metadata, tags, copy-clone-URL, and a GitHub page shortcut when metadata is available.
- Markdown preview can follow the app theme or override to light/dark.
- Actions uses GitHub REST endpoints for repository workflows, latest workflow runs, per-run artifacts, and logs ZIP download links. Runs show status/conclusion, event, branch, commit SHA/message, actor, created/updated timestamps and approximate run duration. Each run links to the official GitHub run page. The current loader fetches artifacts for the eight newest run entries to avoid excessive startup requests.
- Release display shows release tags, pre-release/draft status, Markdown release notes, and asset links.
- Actions logs ZIPs, run artifacts, and release assets download in the app to `Download/RexAps/Github/Artefak/` if shared Download storage is writable. The UI shows byte progress; existing filenames are preserved by creating a numbered filename for collisions.
- Actions and Release loading errors are tracked separately. Actions errors recommend checking the selected account/repository access and GitHub token permissions; a button opens the account selection dialog.
- The `Artefak` directory is excluded from the repository-owner scan.
- Existing Code tab editor syntax highlighting/search/copy features remain in place.

## Gradle dependencies required

Add these to the app module's `build.gradle.kts` dependencies. Keep versions consistent with the Android/Kotlin versions already used by the project.

```kotlin
implementation("io.noties.markwon:core:4.6.2")
implementation("io.noties.markwon:html:4.6.2")
implementation("io.noties.markwon:image:4.6.2")
implementation("io.noties.markwon:ext-tables:4.6.2")
implementation("io.noties.markwon:ext-tasklist:4.6.2")
implementation("io.noties.markwon:ext-strikethrough:4.6.2")
implementation("io.noties.markwon:linkify:4.6.2")
implementation("io.noties.markwon:syntax-highlight:4.6.2")
implementation("io.noties:prism4j:2.0.0")
implementation("com.caverock:androidsvg-aar:1.4")
compileOnly("io.noties:prism4j-bundler:2.0.0")
kapt("io.noties:prism4j-bundler:2.0.0")
```

The `kapt(...)` line requires the Kotlin KAPT plugin in the app module, for example `id("org.jetbrains.kotlin.kapt")` inside the Gradle `plugins` block. The annotation-processor-backed `RexGitGrammarLocator` is generated from `RexGitPrismBundle.kt`; do not remove that file while keeping the syntax-highlight import. If this project cannot enable KAPT, remove the `SyntaxHighlightPlugin` setup and bundle annotation, or use a generated/hand-written Prism grammar locator instead.

The app manifest must allow Internet access with `android.permission.INTERNET`. GitHub API requests and downloads require working network access and appropriate credentials. GitHub docs describe workflow-runs and artifacts API permission requirements; private repositories may return `404` when inaccessible to the selected token.

## Known limits / validation

- This renderer is GitHub-style, not pixel-identical to GitHub's web renderer. Some GitHub-specific alerts, embedded media, custom components, and anchor auto-scroll may differ or be unsupported.
- Code language labels and syntax highlighting apply to recognized fenced blocks. The Code tab keeps its existing editor search/highlighting/copy tools; README text selection/copy behavior still depends on Android `TextView` selection behavior.
- Downloads are streamed with byte-progress reporting and partial files are deleted on download errors, but full runtime verification (private repo, signed redirects, artifact expiry, Android scoped storage, and network errors) remains necessary.
- Full Android/Compose compilation and UI testing were not possible because this upload lacked build scripts, manifest, and dependency configuration. Treat this as a source implementation to integrate and test, not a verified APK.

## Security notes

- Never place real GitHub tokens in source files or share them in chat.
- README HTML is rendered as native spans, not executed in a WebView. Custom style handling only reads a restricted `text-align` value.
- External link handling permits only `http`, `https`, and `mailto`; other URI schemes are ignored.
