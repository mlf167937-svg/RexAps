# RexAps Theme Visual Upgrade

## Scope and compatibility
- All 30 existing themes were retained with the same IDs, names, categories, files, and registry ordering.
- No theme was added as a replacement; palette identities were preserved and their visual systems were refined.
- Theme Kotlin files now explicitly define each theme's radii, typography scale/weights, shadows, border weights, blur hints, animation timings, and easing curves.
- `RexThemeSettings` now animates background changes, selection borders, tile scale, expanded detail content, and theme preview feedback while retaining the existing search, category filter, preview, apply, cancel, reset, motion-level, intensity, and reduce-motion controls.
- `RexButton`, `RexCard`, and `RexSkeleton` have richer focus/hover/pressed states, token-driven elevation/shape, and a moving shimmer gradient.
- Module enter/exit transitions now differ by category. The single-Canvas ambient renderer has category-specific motifs for Glass, Dark, Light, Neon, Cosmic, and Elemental themes.
- `descriptions.txt` has been refreshed to document the retained themes and their style signatures.

## Build verification
The input archive contains Kotlin source files only; it does not contain Gradle settings, a Gradle wrapper, Android manifest, resources, or the `.tbs` bundle format/schema. A complete Android build and creation of a genuine `.tbs` artifact cannot be verified from this input alone. Source-level structure, theme count, theme IDs, registry references, token presence, and palette contrast are checked in the accompanying upgrade report.
