# Extraction verification — 2026-10-04

## Completed checks

| Check | Planet Compass | UVIR after removal |
| --- | --- | --- |
| Debug APK build | Passed, version 0.1.0 | Passed, version 1.3.0 |
| JVM unit tests | 331 passed, 0 failures | 564 passed, 0 failures |
| Android instrumentation sources | Compiled | Compiled |
| Android lint | 0 errors, 52 warnings | Not rerun in this extraction |
| Real-device/emulator UI execution | Not performed | Not performed |

The final build tasks were `:app:assembleDebug`, `:app:testDebugUnitTest` and
`:app:compileDebugAndroidTestKotlin`, plus `:app:lintDebug` for Planet Compass.
Builds used the installed Android Studio JDK/SDK and the existing offline
Gradle cache. A workstation-only initialization script resolved cached plugin
artifacts; it is not required by or distributed with this repository.

Regression checks cover celestial mathematics, satellite propagation, rendering
geometry, input contracts, all 20 resource catalogs and extraction boundaries.
The UVIR contract verifies that the logo is again an ordinary Info action and
that no celestial feature code, maps, resource catalogs or dedicated license
assets remain. Planet Compass checks its independent launcher, package,
permissions, textures and license notices.

Two lint errors in inherited code were corrected without changing equations or
layout: explicit control-flow braces in TLE identifier normalization and removal
of an unused constraint-aware container in the trajectory layer.

## Non-blocking lint follow-up

The 52 warnings comprise screen-size API guidance (7), explicit backup-rule
guidance (1), a Compose modifier convention (1), Java indentation suggestions
(3), typography suggestions (20), unused resources (14), KTX suggestions (5)
and a version-catalog suggestion (1). No insecure TLS workaround was introduced.
These warnings are follow-up cleanup, not a claim that release preparation is
complete. Application backup is currently disabled in the manifest.

## Delivery boundaries

- A new local Git repository is initialized independently from UVIR.
- No GitHub remote, release, store listing or production signing was created.
- No installation, uninstall, archive deletion, pairing change or phone test was
  performed. The existing UVIR working-tree changes were preserved.
- The pre-extraction UVIR files are retained in a separate recovery snapshot.
- APKs are development/debug artifacts, not production releases.

Next device checks should follow [DEVICE_TESTING.md](DEVICE_TESTING.md), with
explicit authorization and without clearing either application's data.
