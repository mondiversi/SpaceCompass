# Verification

## Current development checks — 2026-10-05

| Check | Result |
| --- | --- |
| Debug APK build | Passed, version 0.1.2 |
| Release APK build | Passed, version 0.1.2 |
| Release lint | Completed, 0 errors; remaining findings retained in reports |
| JVM unit tests | 395 passed, 0 failures |
| Android instrumentation sources | Compiled; not executed |
| Phone UI checks | Samsung SM-G970U1, Android 12 / API 31 |
| Tablet UI checks | Pixel Tablet emulator, Android 15 / API 35 |
| GitHub automation | Debug/release builds, JVM tests, instrumentation compilation and release lint |

The current local command is `gradlew.bat :app:assembleDebug
:app:testDebugUnitTest :app:compileDebugAndroidTestKotlin`, using Android Studio's
JDK 25 and the installed Android SDK/cache. Release preparation additionally ran
:app:assembleRelease and :app:lintRelease. The Android UI checks above were
manual checks, not an execution of the instrumentation test suite.

Phone and tablet checks include shared toolbar dimensions, dropdown scaling,
catalog filters/sorting, the filter separator, title-only Information navigation,
point captions and offscreen-arrow colors. Tablet rotation was checked using
synthetic location and orientation. Existing application data was retained during
updates. The verification of one layout does not imply every Android version or
device configuration has been tested.

Source code and GitHub automation are published at
[mondiversi/SpaceCompass](https://github.com/mondiversi/SpaceCompass).
The repository contains no signing key or local SDK configuration. GitHub APK distribution is described in [GITHUB_RELEASES.md](GITHUB_RELEASES.md).
No Play Store submission is part of this release.
The current CI result is available on the repository's Actions page; the local
results above do not stand in for a successful hosted run.

## GitHub updater checks — version 0.1.2

The update feature adds 14 JVM checks covering signature tampering, different
signing keys, downgrade prevention, Android compatibility, restricted HTTPS URLs,
truncated/oversized/checksum-invalid transfers and interrupted-download cleanup.
The twenty-language resource-parity/placeholder checks also passed.

Repository buttons and the real manual update check were verified in landscape
on the Android 15 tablet emulator. The signed 0.1.2 APK was installed as an update
on the Android 12 Samsung phone, retaining data; the phone was locked during the
new Repository UI attempt, so that attempt is not reported as a passed UI check.
The earlier phone checks above describe the previous layout baseline.

The end-to-end download/Android-installation check is performed after the first
public signed index is available and reported in the release validation notes.
The local version-code-1 debug updater fixture used for this check is not a
distribution asset. No connected instrumentation suite was executed.

The following records describe the original extraction baseline. They are
historical and do not supersede the current development checks.

## Historical extraction checks — 2026-10-04

### Extraction results

| Check | Space Compass | UVIR after removal |
| --- | --- | --- |
| Debug APK build | Passed, version 0.1.2 | Passed, version 1.3.0 |
| JVM unit tests | 331 passed, 0 failures | 564 passed, 0 failures |
| Android instrumentation sources | Compiled | Compiled |
| Android lint | 0 errors, 52 warnings | Not rerun in this extraction |
| Real-device/emulator UI execution | Not performed | Not performed |

The final build tasks were `:app:assembleDebug`, `:app:testDebugUnitTest` and
`:app:compileDebugAndroidTestKotlin`, plus `:app:lintDebug` for Space Compass.
Builds used the installed Android Studio JDK/SDK and the existing offline
Gradle cache. A workstation-only initialization script resolved cached plugin
artifacts; it is not required by or distributed with this repository.

Regression checks cover celestial mathematics, satellite propagation, rendering
geometry, input contracts, all 20 resource catalogs and extraction boundaries.
The UVIR contract verifies that the logo is again an ordinary Info action and
that no celestial feature code, maps, resource catalogs or dedicated license
assets remain. Space Compass checks its independent launcher, package,
permissions, textures and license notices.

Two lint errors in inherited code were corrected without changing equations or
layout: explicit control-flow braces in TLE identifier normalization and removal
of an unused constraint-aware container in the trajectory layer.

### Historical lint follow-up

The 52 warnings comprise screen-size API guidance (7), explicit backup-rule
guidance (1), a Compose modifier convention (1), Java indentation suggestions
(3), typography suggestions (20), unused resources (14), KTX suggestions (5)
and a version-catalog suggestion (1). No insecure TLS workaround was introduced.
These warnings are follow-up cleanup, not a claim that release preparation is
complete. Application backup is currently disabled in the manifest.

### Extraction delivery boundaries

- A new local Git repository is initialized independently from UVIR.
- No GitHub remote, release, store listing or production signing was created.
- No installation, uninstall, archive deletion, pairing change or phone test was
  performed. The existing UVIR working-tree changes were preserved.
- The pre-extraction UVIR files are retained in a separate recovery snapshot.
- APKs are development/debug artifacts, not production releases.

Next device checks should follow [DEVICE_TESTING.md](DEVICE_TESTING.md), with
explicit authorization and without clearing either application's data.
