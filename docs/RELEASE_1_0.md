# Space Compass 1.0 release audit

This is the first stable release. The interface displays **1.0**; Android,
GitHub tag/APK filename and the signed update index use **1.0.0**, version code **15**.
The full identity preserves compatibility with preview update clients, which require
three-part semantic versions and an exact APK/index version match.

## Source and package review

- Production Kotlin is split into 190 files; the largest has 529 lines. Calculation,
  geometry, policy/formatting, network producers, sensors and UI/rendering remain separate.
  No external UVIR source or module is required.
- Code/resource optimization is enabled for release through AGP 9.3 R8. No blanket
  keep rules disable optimization. Dependency/platform consumer rules remain in effect.
- Cached paths and remote owners survive child-page navigation. Texture decoding is
  sampled/capped to GPU limits, GPU-upload bitmaps are recycled, and thumbnails are bounded.
- GL shader handles are resolved once after each context link instead of performing
  thirteen driver lookups every frame. The native map split layout reuses its parameters.
- Original credited maps remain at their source resolution. There are no duplicate
  asset files. The signed optimized APK is approximately **13.3 MiB**, down from **20.0 MiB**
  for the last unoptimized development APK (about one third smaller).
- Working files and existing Git history were scanned for private signing material,
  credentials, embedded URL passwords and local/private filenames: no findings.
  Captures, user GPS data, credentials, keys and local build artifacts are excluded
  from the public repository. Signing stays local.

## Compatibility and localization

- Minimum API 26/Android 8.0; target/compile API 37. All bundled 64-bit native libraries
  satisfy 16 KiB ELF load alignment, and release APK ZIP alignment is checked.
- Altitude-only scenarios clear vertical accuracy through `LocationCompat`, avoiding
  an API 33 method on Android 8–12 while retaining real horizontal accuracy.
- AndroidX ExifInterface 1.4.2 writes capture time/subseconds and explicit UTC offsets
  on every supported version. It retains credits and the existing GPS-disclosure policy.
- Configuration/resources are observed in Compose; language changes update preference
  examples, camera error callbacks and the hidden-object notification.
- An Android 13+ monochrome launcher resource supports themed icons while keeping the
  existing full-color artwork on other configurations.
- Twenty catalogs each contain the same **447 translatable string keys**, with no
  duplicates, missing/extra entries, empty values, malformed UTF-8 or placeholder mismatch.
  The six 1.0 release-summary items are translated in all twenty catalogs.
- Identical-English short strings were reviewed: proper/catalog names, scientific symbols
  and valid identical terms are intentional; no copied long English sentences remain.
  This resource/semantic review is not a claim of independent native-speaker proofreading
  for every language.

## Executed checks

The following local tasks passed:

```text
:app:assembleDebug
:app:testDebugUnitTest
:app:compileDebugAndroidTestKotlin
:app:assembleRelease
:app:lintRelease
```

**669 JVM tests** passed with zero failures, errors or skips. Release lint has **zero
errors**. Remaining advisory warnings concern programmatic view constructors, bundled
third-party formatting, compatible layout/configuration APIs, style suggestions,
unused development resources and newer available tool/library versions. Leaflet needs
JavaScript; its code is bundled and map requests remain constrained to the documented
provider. No global warning suppression or error baseline was added. Android test
sources compiled; connected instrumentation was **not executed**.

The optimized signed app was installed over the existing Samsung Android 12 app with
the authorized distribution certificate and no uninstall/data clear. Observed checks:

- Main compass and retained Sun/Moon selection with Sun as the active target.
- Info & news displays 1.0 and all six release-summary items; repository and licence
  islands retain their two actions and copyright/source credits.
- Thirty ordinary objects; populated ISS/Starlink rows in the catalog.
- Textured Sun view with phase/orientation controls and formatted physical data.
- Dark OpenStreetMap position details with GPS/accuracy, altitude, place and weather table.
- Portrait panorama generation and export dialog, all three actions, and the retained
  international-format selection. Gallery saving and sharing were not invoked.
- A real manual GitHub check reports **No update available**, rather than offering an
  older preview or reinstalling the same version.

Broader sensor, scenario, geometry, RTL, camera photo and export behavior is covered by
the existing tests and the scoped development checks in [VERIFICATION.md](VERIFICATION.md).
Physical compass accuracy still depends on the device's measurements and surroundings;
remote data retain their provider/epoch constraints rather than fabricated fallbacks.

The final APK is rebuilt from the tagged source commit, aligned, signed and its exact
installed bytes verified. Publication includes the signed update index and SHA256SUMS;
public download checks and hosted Android checks gate release publication. Artifact
hashes are recorded alongside the release assets, without embedding private credentials.

References: [R8 release optimization](https://developer.android.com/topic/performance/app-optimization/enable-app-optimization),
[LocationCompat](https://developer.android.com/reference/androidx/core/location/LocationCompat),
[AndroidX EXIF](https://developer.android.com/jetpack/androidx/releases/exifinterface).
