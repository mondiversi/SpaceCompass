# Space Compass 1.4.7 maintenance release

Application `me.mondiversi.spacecompass`, version name/display 1.4.7, code 44,
minimum Android API 26. Package, distribution certificate and saved data remain
compatible with earlier versions.

## Scope

Refine the main object island: smaller navigation/removal glyphs, compact gaps,
optically centred telescope artwork, and dark circular observatory/daily-path
controls with white glyphs in both themes and no perimeter borders. The removal
confirmation remains in place. Floating icon strokes match the zoom controls;
title-bar actions retain their separate 2 dp design. Catalogue toolbar actions
use the shared spacing. The daytime blue is refined; the night accent is unchanged.
No celestial, sensor, weather, camera or time-scenario calculations are changed.

The 2026-10-21 rebuild also applies UVIR-style soft Material press feedback to
outlined settings, repository, observer-confirmation and export actions. Neutral
buttons stay neutral, blue buttons stay blue, and text/icons do not invert color.
Thin action glyphs are 25 percent stronger without changing their viewports or
shared toolbar assets; solid repository-logo cutouts remain intact. Resting
surfaces, disabled colors, dimensions and accessible touch targets are preserved.

All twenty in-app news files and existing credits remain unchanged from 1.4.6.
The displayed version is supplied by BuildConfig. The 104-object catalogue and
hidden LV-426 remain unchanged. External graphics pack 1.5 retains its 56 full
WebP images and 56 independent previews, individual downloads, integrity pins,
offline cache and legacy compatibility. No texture ZIP or new image pack is added.

## Verification and distribution

Release checks cover debug/release compilation, 1,223 JVM tests, Android-test
source compilation, release lint, twenty-language key/placeholder parity,
unchanged news, the APK certificate, package/version and 16 KiB alignment.
Connected instrumentation is not executed. The preceding local visual builds
were installed and their controls inspected on the phone. The exact release
installation and available UI checks are reported separately in the attached
metadata; no unexecuted real-device test is claimed.

The release includes the signed APK, signed update index, optional scientific
and localization reference-information archive, release metadata and checksums.
The information ZIP is not a runtime image download. Source/tag identity, hosted
checks, anonymous downloads and preservation of older releases are verified
before completion. Install updates without uninstalling or clearing app data.

## Same-version rebuild

At the owner's explicit request, this revision retains version 1.4.7 and code 44.
It replaces the existing release assets and signed update index, rather than
creating another release. The existing application tag is updated with an
exact-reference lease to keep public source archives aligned with the APK.
Earlier releases and immutable graphics pack 1.5 are not changed. Installations
already on code 44 do not receive an automatic update offer; download the APK
again and install it over the existing app to obtain the revised build.

Soft press feedback was manually inspected on the physical phone in light and
dark themes using the local pre-commit preview. Presses were cancelled without
opening repository links, starting downloads or saving observations, and the
original theme was restored. The new isolated button UI tests are compiled but
not run as connected instrumentation; exact rebuilt-APK installation checks are
reported separately in release metadata.
