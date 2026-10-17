# Space Compass 1.4.4 release

Application ID: `me.mondiversi.spacecompass`; version name `1.4.4`; monotonic code `41`.
The distribution uses the existing certificate and preserves app data. Android API
26 remains the minimum and API 37 the compile/target level. See CHANGELOG.md for
the feature summary and CATALOG_100.md/GALAXY_PAIRS.md for scientific routes/limits.

Graphics release `celestial-textures-v1.5` contains 56 full WebP files and 56 separate
128 x 128 WebP previews, quality 90/method 6. All application URLs point at this
release; individual byte lengths, dimensions and SHA-256 values are APK-pinned.
Full image bytes and older pack URLs remain intact. No complete texture ZIP is
distributed. Previews and full images have independent persistent verified caches;
catalogue glyphs do not fetch full images. The original LV-426 is external.

Release validation comprises debug/release builds, the JVM suite, Android-test
source compilation, release lint, twenty-language key/placeholder parity, reproducible
preview generation, per-file integrity and source/credential checks. Connected
instrumentation is not executed. Physical-device checks and hosted CI are reported
separately in release-metadata.json; public downloads are verified against local hashes.

The optional information ZIP contains reference source tables, scientific source
notices, documentation and translations, with a manifest of its entries. It contains
no observer caches, device positions, runtime ephemerides or credentials. The signed
update index identifies the exact final APK; SHA256SUMS.txt covers release assets.

Source, v1.4.4 and graphics tags identify one commit. Publication reuses a release
by numeric ID, checks all authenticated drafts, publishes graphics before the APK
and verifies that older releases/assets remain unchanged.
