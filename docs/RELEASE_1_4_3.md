# Space Compass 1.4.3

- Expand the catalogue to 53 regular entries with Ceres, Enceladus, Vega, Deneb, Altair and the Southern Cross. Retain the existing Sun/Moon defaults and saved selections.
- Add Earth-centred distance sorting and a compact per-object Observatory shortcut. Inspect unchecked objects without changing the current target or checked selection; returning preserves the catalogue scroll and search.
- Add optional scenario time acceleration from 1x to 3600x, automatic day rollover and independent restart/active-only/always resumption choices under General > Events. Restart from the configured instant is the new-install default; existing choices are retained.
- Detect close approaches within 1.5 degrees only when both objects are close at the same absolute instant. Show the shared key event in live paths, daily paths, captures and exports. A crossing at different times is not an approach, and an approach is not an eclipse prediction.
- Refine daily-path symbols and spacing: ordinary hourly points indicate rising/falling elevation, key events retain their own symbols and Current retains its target marker and highlighted background.
- Add the 15-minute screensaver delay, shorten its explanation and use the compact delay dropdown. Balance the main object-action spacing and remove excess catalogue info-button margin.
- Make General's natural-height cards pack independently into two landscape columns; correct the system-theme preview and standardize the scenario-speed caption typography.
- Publish graphics pack 1.4 with 29 individual quality-90/method-6 WebP files, including Ceres and Enceladus. The APK pins the exact release, byte lengths, hashes and dimensions. Keep individual downloads, offline cache reuse, placeholders and previous release URLs; do not distribute a duplicate texture ZIP.
- Update in-app news in all twenty languages, documentation, information archive and the signed update index.

## Download

Install `space-compass-1.4.3.apk` over the existing app. Android 8.0 or newer; version code 40, unchanged package and distribution certificate. Do not uninstall or clear app data. The signed update index identifies the exact APK by version, size and SHA-256.

## Graphics and information packages

The APK uses `celestial-textures-v1.4`: 29 individual WebP files, quality 90/method 6, totalling 6,771,736 image bytes. Twenty-eight are 2048 x 1024; the original LV-426 remains 1774 x 887. The previous 27 image files remain byte-identical. New resources are the credited NASA Dawn Ceres archival photograph and Cassini Enceladus cylindrical mosaic. Celestial photographs and maps remain outside the APK and download individually on demand. Credits, manifest and checksums accompany them in their separate graphics release, marked non-latest. Older graphics URLs remain unchanged for earlier APKs. There is no complete texture ZIP in either new release.

`space-compass-information-1.4.3.zip` is an optional snapshot of scientific reference tables, all twenty language catalogues, source documentation and licences. It is not a runtime dependency and contains no device location, private preferences or downloaded ephemeris cache. GitHub's automatically generated source archives are separate.

## Scientific and timing limits

Ceres and Enceladus use validated JPL Horizons windows. Stars use catalogue directions with the existing proper-motion/parallax model. The Southern Cross is a four-star constellation figure and pointing centre, not a single physical object with one distance or mass. Mission predictions and current satellite elements retain their original finite validity; accelerated or arbitrary-date scenarios may reach unavailable data. Missing values are never invented or extrapolated beyond validity.

The time-speed control is off by default. Resumption can restart at the configured instant, advance only while the app is active, or account for elapsed real time while closed. The last mode uses the device wall clock and therefore follows manual clock changes. No background timer, sensor service or wake lock is added. Screensaver/background resource suspension remains in force.

## Verification

Release metadata records local build/JVM tests, Android-test compilation, release lint, twenty-language parity, texture hashes/dimensions, APK signature/alignment, the signed update payload and anonymous public downloads. Device installation and manual checks are reported separately. Connected instrumentation is not claimed unless actually executed.
