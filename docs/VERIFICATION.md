# Verification

## Current development checks — 2026-10-05

| Check | Result |
| --- | --- |
| Debug APK build | Passed, version 0.1.8 |
| Release APK build | Passed, version 0.1.8 |
| Release lint | Completed, 0 errors; remaining findings retained in reports |
| JVM unit tests | 425 passed, 0 failures |
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

## Dynamic mass display — version 0.1.8

Seven new JVM tests cover both sides of the exact 10-solar-mass threshold in kg
and M☉ references, independence from category and pound choice, Sun conversion,
lower stellar/compact masses, converted uncertainties/qualifiers, binary totals
and separate components, unchanged high-mass references and invalid/missing data.
All 425 JVM tests passed, together with debug/release builds, instrumentation-source
compilation and release lint (zero errors). The instrumentation suite was not run.
Complete translatable-string parity passed for all twenty interface languages;
recent news and the plain estimate/model note are localized.

The signed candidate was checked on the Android 15 tablet: Sun kg/lb in portrait
and landscape, pound preference surviving process restart, Alpha Centauri A+B
and both resolved components in pounds with uncertainties, and Rigel retaining
solar masses with the pound choice. Original automatic mass units, free rotation
and Sun/Moon selection were restored. Final distribution installation is recorded
in the release notes. No new phone UI pass is implied by these tablet checks.

## Atmospheric pressure — version 0.1.7

Nine new JVM tests cover exact bar/Pa/psi conversion, regional defaults and explicit
pressure independence, saved preference observation/reload, invalid data omission,
published pressure scales, supported/unsupported bodies, qualified upper/night
limits, negative exponents and localized numeric conventions. All 418 JVM tests,
debug/release builds, instrumentation-source compilation and release lint passed;
lint reported zero errors. The instrumentation suite was not executed.

All twenty interface languages have complete translatable-string parity, including
nine new pressure/mass-density labels and the recent-news summary. The pressure
island follows mass/density and uses the shared island/radio/example layout.
Atmospheric/exosphere reference data and exclusions are documented in
[ATMOSPHERIC_PRESSURE.md](ATMOSPHERIC_PRESSURE.md).

The signed candidate was checked on the Android 15 tablet using synthetic GPS:
Moon pressure conversions to bar, Pa and psi; retained kg/m³ density; process
restart preserving PSI and active Moon; missing Sun pressure row; pressure radio
choices in light/dark themes and portrait/landscape. System pressure, original
theme, free rotation and active Sun were restored. The final distribution package
is rebuilt from the committed source; final phone/tablet installation is recorded
in the release notes. No new phone UI result is implied by tablet checks.

## Info and news — version 0.1.6

The renamed page and three-item recent-news summary are localized in all twenty
languages, with matching resource keys and version placeholders. The news island
follows the description/Earth-use note and precedes GitHub/credits; it uses the
existing app island component and UVIR's two-sparkle glyph.
Debug/release builds, all 409 JVM tests, instrumentation-source compilation and
release lint passed. No new test mirroring this reversible text/layout change
was added. The instrumentation suite was not executed. Focused signed-APK UI
checks are recorded in the release notes after installation.

## Mass units — version 0.1.5

All 409 JVM tests passed, including eight new checks for exact kg/lb conversion,
automatic regional defaults, explicit mass/length independence, presentation
preference reload, all available nonstellar mass readouts, unchanged solar masses,
invalid/unknown values and all four mass/volume combinations for density.
The twenty interface languages have complete translatable-string parity; the
new island follows distances/speeds and includes an icon, description and kg/lb examples.
Debug/release builds, instrumentation-source compilation and release lint passed.
The instrumentation suite was not executed. Focused signed-APK device checks are
recorded in the release notes after installation.

## Units and viewer controls — version 0.1.4

Distances, astronomical distances and speed share one persisted selection:
System, meters/kilometers or feet/miles. Regression checks cover migration of
older choices, precedence of the unified setting, device-region defaults,
temperature/coordinate independence and nearby/astronomical/physical-size output.
Older explicit distance choices take precedence, followed by nearby length then
speed; no other preferences or celestial selections are cleared.
Twenty-language string parity passed, including the renamed Units page.

Viewer tabs use an opaque light fill with a dark symbol when selected, versus
the existing dark fill and white symbol when inactive. Tests require at least
4.5:1 contrast for the glyph and between active/inactive fills, including presses.
Sky action buttons and the existing 48 dp touch targets remain covered by their
regression checks. Signed APK/device checks are recorded in release notes.

## Repository presentation — version 0.1.3

The repository island contains the project URL followed by **Open repository**
and **Check for updates**. The package identifier, explanatory sentence and
persistent check-result line were removed. Update results still use the localized
temporary notice or update dialog. The unused explanatory resource was removed
consistently from all twenty languages; existing parity checks passed.
Signed APK installation and device UI results are recorded in this version's
GitHub release validation notes.

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
