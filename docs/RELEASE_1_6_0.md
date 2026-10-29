# Space Compass 1.6.0 release review

Application `me.mondiversi.spacecompass`, version/display **1.6.0**, Android code **55**, minimum API 26.

## Scope

Persistent Observatory favorites and a catalogue heart filter remain independent of scene checks.
Favorite checkboxes use the shared day/night pink color. Catalogue grouping is retained for the visit,
new checks update the active object, open filters remain visible, and every option count combines the
actual favorite/search/filter scope with retained selected-object exceptions. Refresh stays automatic.

Location management has compact independent cards, effective values, bold customization and six
activation controls synchronized with their child pages. Resumption is always editable without an
activation checkbox. Optional celestial scene selections and spatial-only automatic exports preserve
the documented version-1 import semantics. News is refreshed in all twenty interface languages.

## Graphics

Reuse immutable `celestial-textures-v1.7`: 184 full WebP images and 184 independent previews.
The logical collection remains 43,735,950 bytes. No image, preview, manifest, download pin or URL
changes; no complete graphics ZIP is created. Verify all individual pins and public availability.

## Verification and distribution

Run debug/release compilation, the complete JVM suite, Android-test source compilation, release lint,
resource-key/placeholder parity and catalogue/graphics reproducibility checks. The transfer matrix covers
1,536 combinations of independent flags, temporal policies, speed and export profiles, plus automatic
spatial exports, canonical units, optional objects, invalid-file rejection and legacy compatibility.
Record real-device UI actions separately from compiled instrumentation; connected instrumentation is
not executed without separate authorization. Preserve the existing installation and distribution key.

Verify the exact committed source embedded in the signed APK, hosted Android checks, anonymous source
tree equality, update signature, archive pins and public downloads. Publish exactly one v1.6.0 release
with the APK, signed index, optional information archive, verification metadata and checksums. Keep
every older application/graphics release and download URL available. The public metadata records
actual build/test/device outcomes rather than treating an unavailable UI check as a pass.
