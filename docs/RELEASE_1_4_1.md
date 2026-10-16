# Space Compass 1.4.1

- Show bold object headings and optional event names in focused point callouts, with wrapping titles, balanced clock/countdown and angle rows, and matching light translucency on the caption and reminder button.
- Improve borderless count badges with bold white numbers, exact day/night scenario accents and a subtly lighter daytime blue.
- Place a plain remove-object X between navigation and Observatory, with balanced visual gaps, a narrower horizontal hit region and the existing confirmation.
- Bound weather requests and show a localized Retry action when relevant data are unavailable; preserve location/orbital status priority.
- Move LV-426 out of the APK. All 20 celestial images use external quality-90 WebP files from texture pack 1.2.1, including the Roadster photograph. Verify individual downloads and reuse matching legacy caches.
- Refresh news in all twenty languages and publish consistent graphics/reference archives. Preserve scientific calculations, saved settings, selections and distribution identity.

## Download

Android 8.0 or newer. Install space-compass-1.4.1.apk over the existing app. Version code 38; same distribution certificate and signed update index.

## Graphics

This APK points to celestial-textures-v1.2.1. Its 20 WebP images include LV-426, use quality 90/method 6 and retain the reviewed image bytes. Nineteen are 2048 x 1024; LV-426 remains 1774 x 887. The app downloads only individual requested files. The complete ZIP, manifest, credits and SHA256SUMS describe the same set. Existing valid caches and earlier release URLs are preserved.

## Verification

Debug/release builds, JVM tests, Android-test compilation, release lint, translation parity, texture hashes, source review, APK signature and signed update-index checks are recorded in release-metadata.json. Device installation is reported separately; connected instrumentation is not executed.

## Audit limits

No connected instrumentation suite or real reminder creation is performed by the release pipeline. Physical installation and hosted CI results are recorded separately. Private signing configuration, GPS caches and device captures are excluded from published files.
