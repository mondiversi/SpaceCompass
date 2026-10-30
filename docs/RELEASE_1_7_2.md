# Space Compass 1.7.2 release audit

Version 1.7.2 uses Android code 58, the existing application package and distribution certificate. It includes the accepted interface work since 1.7.1: centered equal-size Info/logo and Menu controls, compact floating geometry with preserved stroke weight and optical balance, stable catalogue refreshes, revised reticle/panel spacing, an embedded full-width Location map with standard action buttons and estimated weather, language-code ordering and capture-header branding. Capture glyphs are white in the day theme and dark in the night theme; the white outline and pressed animation are preserved.

## Validation boundary

Required checks cover debug/release builds, all JVM tests, instrumentation-source compilation, release lint, twenty-language key/placeholder parity, generated catalogue/identity/thumbnail reproducibility, signing identity, alignment and APK/source-commit agreement. Physical phone checks and hosted CI are reported separately in release metadata; instrumentation compilation is not described as executed UI tests.

During local phone verification, normal/slashed Weather and Labels artwork was balanced without changing glyph size or stroke. The catalogue was recorded for about a minute with filters open and closed: complete rows, counters and bulk-selection styling stayed visible during automatic refreshes. Position-editor edge pulls no longer deform the form, while lower fields remain reachable. Main controls and navigation were checked in portrait/landscape with panels expanded and collapsed. The final release APK receives a separate installation/version/news/theme check.

## Distribution

All six in-app news summaries are refreshed in twenty languages. Graphics collection 1.8 is unchanged: 189 full WebP images and 189 independent previews, totaling 43,663,574 image bytes. All integrity pins, dimensions, attribution, immutable individual downloads and offline caches remain in use. No new graphics release or complete texture ZIP is created. The optional information archive contains reference tables, documentation, licenses and translations, excluding private device data, screenshots, recordings, credentials and celestial photographs. Prior releases and their asset URLs are preserved.
