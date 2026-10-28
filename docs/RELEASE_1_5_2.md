# Space Compass 1.5.2 release review

Application `me.mondiversi.spacecompass`, version/display **1.5.2**, code **52**, minimum API 26.
Update with the existing distribution certificate to retain settings, selections and caches.

## Scope

Shutter-side camera zoom and remembered camera-only 50% object translucency. The new
half-dotted glyph is slashed when off; photographs retain their exposure-time opacity
for every later preview/export variant. Camera activation still starts off on cold launch.
One shared export-format preference, profile-aware structured filenames and portable
scenario files preserve canonical import values. Current scenario includes its resumption
policy. The Earth screensaver uses real GPS and UTC geographic daylight, with a dimmed
far-side marker, unchanged maps and idle-only solar updates.

## Distribution and graphics

Graphics collection 1.7 remains immutable: 184 full WebP images and 184 independent
128×128 previews, quality 90/method 6. Its complete logical collection is 43,735,950
bytes including metadata. Individual resource URLs, hashes, dimensions and verified
offline caches remain compatible. No new graphics assets or complete texture ZIP is added.
The APK retains the existing bundled star atlas and screensaver maps, and the signed
update index pins the exact new APK. The optional information ZIP contains reference
text, translations, credits and graphics metadata rather than a runtime data download.

## Verification

Validate twenty-language key/placeholder parity and refreshed news, catalogue and
thumbnail reproducibility, graphics hashes and pins, clean source, debug/release builds,
JVM tests, Android-test source compilation and release lint. Connected instrumentation
is not executed without separate authorization. Verify the distribution certificate,
non-debuggable/test-only flags, 16-KiB alignment, source revision embedded in the APK,
same-key phone update, hosted checks and anonymous downloads before publication is complete.
Record exact results in the release metadata and workspace validation records.
