# RexAps Theme Engine

## Architecture
- `tokens.kt`: complete shared design-token schema and token validation.
- `engine.kt`: persisted theme selection, live subscribers, preview/commit/cancel, motion preferences.
- `registry.kt`: ordered catalogue of exactly 30 themes; Dark Blue Moon is first-run default.
- `themes/<category>/<theme>.kt`: one complete token configuration per theme, including explicit theme-specific typography, shape, elevation, and motion tokens.
- `descriptions.txt`: theme specifications in the required nine-line format.
- `../motion`: transition presets, motion limits and reduce-motion policy.
- `../background`: low-allocation Canvas ambient renderer and category layer recipes.
- `../components`: token-aware button, card and skeleton primitives.
- `../settings/RexThemeSettings.kt`: searchable categorized theme picker, preview, details, motion level, effect intensity, system reduce-motion preference, reset.

## Add a theme
1. Add a Kotlin file in `themes/<category>/` returning one fully populated `RexDesignTokens`.
2. Add its import and one item to `RexThemeRegistry.all` in `registry.kt`.
3. Add one matching `RexThemeOption` to `RexThemeOption.curatedOptions` and map it through the enum's id conversion if the enum spelling differs from the slug.
4. Add its formatted description to `descriptions.txt`.
5. Run `RexThemeRegistry.validateAll()` and check `RexThemeRegistry.contrastFailures()`.

## Visual customization
Each existing theme now has its own radius scale, typography rhythm, shadow/border weights, motion durations, easing curves, and interaction signature. Keep palette and identity changes inside the existing theme file so IDs and picker behavior remain stable. `motion/transitions.kt` defines category-aware enter/exit motion; token-aware primitives in `../components` provide animated button states and loading shimmer.

## Change global tokens
Edit defaults in `tokens.kt` for global radius, spacing, typography, or motion. Palette-specific values belong in the individual theme file. Keep contrast at WCAG AA (4.5:1 for body text, 3:1 for large text) and rerun the validator after changes.

## Notes
The uploaded archive contains Kotlin source files only and does not include Gradle settings, build scripts, wrapper, or Android manifest. A full Android build cannot be run from this archive alone.
