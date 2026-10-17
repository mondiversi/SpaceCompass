# Space Compass 1.4.6 maintenance release

Application `me.mondiversi.spacecompass`, version name/display 1.4.6, code 43,
minimum Android API 26. Package, distribution certificate and saved data remain
compatible with earlier versions.

## Scope

Only the morning and afternoon far/near ground colours are lightened. The main
scene and generated panoramas consume the same palette. Continuous solar-height
lighting still controls day/dusk transitions. Horizon haze and the dawn, sunset,
evening and night palettes are unchanged. No celestial, sensor, weather, camera
or time-scenario calculation changes are included.

All twenty in-app news files and existing credits remain unchanged from 1.4.5.
The displayed version is supplied by BuildConfig. The 104-object catalogue and
hidden LV-426 remain unchanged. External graphics pack 1.5 retains its 56 full
WebP images and 56 independent previews, individual downloads, integrity pins,
offline cache and legacy compatibility. No texture ZIP or new image pack is added.

## Verification and distribution

Release checks cover debug/release compilation, 1,222 JVM tests, Android-test
source compilation, release lint, twenty-language key/placeholder parity,
unchanged news, the APK certificate, package/version and 16 KiB alignment.
Connected instrumentation is not executed. The preceding local colour build
was installed and its daytime ground was visually inspected on the phone.
The exact release installation and available UI checks are reported separately
in the attached metadata; no unexecuted real-device test is claimed.

The release includes the signed APK, signed update index, optional scientific
and localization reference-information archive, release metadata and checksums.
The information ZIP is not a runtime image download. Source/tag identity, hosted
checks, anonymous downloads and preservation of older releases are verified
before completion. Install updates without uninstalling or clearing app data.
