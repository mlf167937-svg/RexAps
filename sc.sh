#!/usr/bin/env bash
# Remove stale pre-refactor speed-test files that conflict with the shared
# RexWarpSpeedTestComponents.kt and the Portrait/Landscape layout wrappers.
# Run from the Android project root (the directory containing app/).
set -euo pipefail

SOURCE_DIR="app/src/main/java/com/rexaps/rexwarp/ui"
if [[ ! -d "$SOURCE_DIR" ]]; then
  echo "ERROR: tidak menemukan '$SOURCE_DIR'. Jalankan script dari root project Android." >&2
  exit 2
fi

legacy_files=(
  "RexWarpSpeedTestScreenPotriat.kt"
  "RexWarpSpeedTestScreenPotriat/RexWarpSpeedTestScreenPotriatContent.kt"
  "RexWarpSpeedTestScreenPotriat/RexWarpSpeedTestScreenPotriatContent2.kt"
  "RexWarpSpeedTestScreenLandscape/RexWarpSpeedTestScreenLandscapeContent.kt"
  "RexWarpSpeedTestScreenLandscape/RexWarpSpeedTestScreenLandscapeContent2.kt"
)

existing=()
for relative in "${legacy_files[@]}"; do
  if [[ -f "$SOURCE_DIR/$relative" ]]; then
    existing+=("$relative")
  fi
done

if (( ${#existing[@]} == 0 )); then
  echo "Tidak ada file legacy duplikat yang ditemukan. Tidak ada perubahan."
  exit 0
fi

stamp="$(date +%Y%m%d-%H%M%S)"
backup=".backup-speedtest-legacy-${stamp}.tar.gz"
# Save only the files scheduled for deletion so the change is reversible.
tar -czf "$backup" -C "$SOURCE_DIR" "${existing[@]}"
echo "Backup dibuat: $backup"

for relative in "${existing[@]}"; do
  rm -- "$SOURCE_DIR/$relative"
  echo "Dihapus: $SOURCE_DIR/$relative"
done

# Remove the misspelled legacy folder only if it is now empty.
rmdir "$SOURCE_DIR/RexWarpSpeedTestScreenPotriat" 2>/dev/null || true

echo
echo "Selesai. Layout baru dan RexWarpSpeedTestComponents.kt tetap dipertahankan."
echo "Lanjutkan dengan: ./gradlew :app:compileDebugKotlin"

