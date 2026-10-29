# Space Compass 1.7.0 release audit

Version 1.7.0 uses Android version code 56 and the existing package and distribution certificate. It expands the catalogue to 9,999 selectable objects, publishes incremental external graphics 1.8 and refreshes all six news summaries in twenty languages.

Stable identities retain prior selections, favorites and saved Location objects. Generated catalogue chunks, locale resources, source provenance and test-only ephemeris/TLE fixtures are reproducible. Scientific and translated reference text stays in the APK; celestial photographs and independent thumbnails remain external.

Location activation belongs only to the management menu. All child forms stay visible and editable; valid inactive values are retained without turning on overrides. Resumption shows a permanently checked, disabled grey checkbox. Compact cards preserve touch targets and landscape columns.

The external collection contains 189 full images and 189 128×128 previews, totalling 43,663,574 binary bytes. Five new photographic pairs add 444,858 binary bytes. Quality 90/method 6, dimensions, source credits, naming and SHA-256 pins are verified. Inherited resources keep immutable versioned URLs; no full graphics ZIP is created. Missing photography uses symbolic rendering.

Required checks cover debug/release compilation, JVM tests, Android instrumentation-source compilation and release lint. Instrumentation execution is distinct and is not claimed. Final signed APK size/hash, package and certificate checks, installation, hosted CI and published-download verification are recorded in the release metadata rather than guessed before the final build.

The optional information ZIP is a source/reference/localization snapshot, not a runtime package. It contains no live device coordinates, private preferences, credentials, cache entries or celestial image files. The signed update index pins the final APK version, size and checksum. Older application and graphics releases remain unchanged.
