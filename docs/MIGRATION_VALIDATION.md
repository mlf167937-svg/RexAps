# RexAps GeckoView / RexADM / Release Pipeline Change Notes

## Engine scope

| Module | Rendering / transfer engine | Result |
|---|---|---|
| RexFox | Android WebView -> GeckoView | Migrated; one `GeckoSession` per tab |
| RexAI | Android WebView -> GeckoView | Migrated; one persistent, provider-isolated `GeckoSession` per provider |
| RexADM | `OkHttpRexSmartManager` | Preserved; UI and process-level manager integration added |
| RexChat | Existing WebView implementation | Not changed |
| RexWARP | Existing WebView implementation | Not changed |
| Other modules | Existing implementations | Not intentionally changed |

`android.webkit.URLUtil` remains in RexFox's download helper only as a filename/MIME utility; it is not a WebView renderer.

## GeckoView session design

- One process-wide `GeckoRuntime` is shared by RexFox and RexAI.
- Every RexFox tab owns a separate `GeckoSession`. Normal tabs share the `rexfox-normal` storage context, while private tabs use GeckoView private mode.
- RexAI has a separate persistent GeckoView context for ChatGPT, Claude and Gemini. Provider cookies and local storage are not shared across those contexts.
- Navigation/progress/title/error callbacks are connected to Compose state. A crashed/closed session can be reopened and navigated to its last known URL.
- RexAI uses provider-specific navigation allowlists. Unsupported top-level destinations are opened externally; GeckoView's TLS/certificate validation is not bypassed.
- DOM storage and cookie policy are managed by GeckoView. The old WebView-specific third-party-cookie and DOM-storage controls are no longer represented as active switches. Do Not Track is shown as unsupported by this GeckoView integration rather than falsely applied.

## RexADM

`OkHttpRexSmartManager` remains the download transfer engine. The new Compose dashboard observes its real `downloads` state and exposes category filtering, progress, pause/resume/cancel/retry, open/share, APK installation through Android's package installer, and terminal-item cleanup. Download speed is intentionally not shown because the existing model does not measure it.

A process-level `RexAdmRuntime` shares one manager instance between the internal App Registry route and the external `ACTION_SEND` / `ACTION_VIEW` Activity. `onNewIntent` updates the pending URL flow.

## Release packaging

- Release minification and resource shrinking are enabled with a narrow application ProGuard file.
- Native ABI filters and APK splits target `arm64-v8a` and `armeabi-v7a`; Gradle also produces a Universal APK containing both ARM ABIs.
- GitHub Actions downloads and validates Node.js Mobile `libnode.so` for both supported ARM ABIs before building.
- The Release workflow verifies each APK signature and its exact ABI set, checks that `libnode.so`, `libmozglue.so`, and `libxul.so` exist for every required ABI, and creates `RexAps-arm64-v8a.zip`, `RexAps-armeabi-v7a.zip`, and `RexAps-Universal.zip`.
- The workflow packages `Build.log`, `NodeJS-Download.log`, and `Full-Actions.log` into `Actions.zip` and uploads all ZIPs in the `RexAps-Release-Files` artifact.
- Native GeckoView binaries/assets are not deleted or rewritten. `useLegacyPackaging = false` is enabled, but no APK size reduction is claimed without a measured artifact.
- The GeckoView Nightly 149 dependency pin was retained. The original project explicitly pins AndroidX Core 1.15.0 because the Nightly dependency graph brings Core 1.17.0 while the app is kept at compileSdk 35. Mozilla's GeckoView changelog also records the GeckoView build's move to compileSdk 36 in v139; do not remove the Core compatibility pin or upgrade the GeckoView channel/version without a dependency and AAR-metadata build check.
- Release signing uses a persistent keystore injected from GitHub Actions secrets. The workflow intentionally stops if any required signing secret is missing.

Required Actions secrets:

- `RELEASE_KEYSTORE_BASE64`
- `RELEASE_STORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

See [RELEASE_SIGNING.md](RELEASE_SIGNING.md) for setup instructions.

## Validation performed in the editing environment

- All Android XML resource and manifest files parsed successfully.
- GitHub Actions YAML parsed successfully.
- Static scan confirms RexFox and RexAI no longer instantiate/import `android.webkit.WebView`.
- Static scan confirms the workflow has no `assembleDebug` or Debug artifact upload path.
- Static inspection confirms RexChat and RexWARP source implementations remain present and are not migrated.
- The revised workflow YAML parses successfully, and every inline shell script passes `bash -n`.

## Validation not completed

The editing environment has no Android SDK (`ANDROID_HOME` / `ANDROID_SDK_ROOT` are unset and no `android.jar` is installed). The Gradle wrapper could not download Gradle 8.11.1 because DNS/network resolution for `services.gradle.org` failed. Therefore:

- `:app:assembleRelease` has **not** completed here. The container has no Android SDK (`ANDROID_HOME` / `ANDROID_SDK_ROOT` are unset) and cannot resolve external hosts to download Gradle or Android dependencies.
- No APK, APK signature, dex/native dependency report or before/after APK component-size measurement was produced here. The three named APK ZIPs and `Actions.zip` are created by the GitHub Actions run, not fabricated in this source archive.
- Runtime browser, login, download, installation and regression tests require an Android device/emulator and remain unverified.

The source-side ABI release packaging is configured, but a successful CI build and device testing are still required. APK size must be measured from the first successful CI release artifact; no size reduction is claimed here. The user's stated ~277 MB Debug APK is treated as a supplied baseline, not a newly measured value.
