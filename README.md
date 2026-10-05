# Space Compass

[![Android checks](https://github.com/mondiversi/SpaceCompass/actions/workflows/android.yml/badge.svg)](https://github.com/mondiversi/SpaceCompass/actions/workflows/android.yml)

An Android celestial compass for locating planets, stars, spacecraft and other
celestial objects. Point your phone or tablet, explore daily sky trajectories,
inspect reference facts and view textured models. Space Compass is an independent
application extracted from [UVIR](https://github.com/mondiversi/Uvir).

## Download and install

Space Compass is distributed on GitHub. Open the
[Releases page](https://github.com/mondiversi/SpaceCompass/releases) and download
`space-compass-0.1.0.apk` from the **Assets** section of the v0.1.0 preview release.
Android 8.0 or newer is required. Open the downloaded APK and follow Android's
installation prompts. If requested, allow installation for the browser or file
manager you used to open the file.

Updates are manual: download the APK from a newer release and install it over
the existing app to keep your settings and selections. There is no need to
uninstall the app. The ZIP/TAR source archives and Actions debug builds are not
the signed release APK.

The download includes a `SHA256SUMS.txt` file for checking its integrity.
See [GitHub release and signing notes](docs/GITHUB_RELEASES.md).

## Features

- Live azimuth and altitude, compass guidance, daily paths and satellite passes.
- A catalog of 29 objects, including the Sun, Moon, planets, natural and artificial
  satellites, nearby stars, compact objects, an exoplanet and Voyager spacecraft.
- Multiple-object selection, type/visibility filters and name/solar-distance sorting.
- Saved settings, checked objects, active target, filters and sorting across restarts.
- Interactive 3D models, lunar phases, reference facts and source-specific credits.
- Phone/tablet layouts, portrait/landscape, light/dark appearance, animated introduction
  and accessible controls, including right-to-left layouts.
- Twenty interface languages, system-language fallback and regional/unit settings.

The application ID is `me.mondiversi.spacecompass`; the current development
version is `0.1.0`. Android 8.0 (API 26) or newer is required.
The app can coexist with UVIR and has its own preferences, logs and data caches.

This is a recreational/scientific visualization, not a navigation, irradiance or
safety instrument. Unknown scientific properties remain explicitly unavailable.

## Build and checks

Open the project in Android Studio. Install Android SDK Platform 37.0 and Build
Tools 36.0.0. The local build uses the bundled Android Studio JDK 25; the GitHub
workflow uses Temurin 25. Gradle is pinned to 9.5.0, Android Gradle Plugin to 9.3.1
and the Compose compiler to 2.2.10. The first build needs internet access.

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin
```

On Windows, use `gradlew.bat`. The debug APK is written to
`app/build/outputs/apk/debug/`. GitHub Actions runs these same checks and retains
the debug APK and JVM test reports as development artifacts. Instrumentation
sources are compiled; executing them requires a device or emulator.

GitHub release APKs are built with the release variant and signed locally.
Signing keys, local SDK paths and private configuration are excluded from the
repository and are not stored in GitHub Actions. Automated debug artifacts are
for development; published release APKs are listed separately under Releases.

## Permissions and online data

Location is requested at runtime for observer-dependent positions. Orientation
and location listeners run only in the foreground. The app has no camera access,
background location, advertising or analytics. Denied or missing location is
shown explicitly instead of invented.

Sun, Moon and major-planet calculations run locally. Satellite elements and
Sedna/Voyager ephemerides use bounded HTTPS requests and app-private caches.
Weather uses a rounded location cell with Open-Meteo. Network failure does not
disable valid offline calculations. Review provider terms before commercial
distribution, especially the free Open-Meteo endpoint.

## Project documentation

- [Architecture](ARCHITECTURE.md) and [presentation](docs/APP_PRESENTATION.md).
- [Scientific and rendering notes](docs/CELESTIAL_VIEWER.md),
  [deep-sky models](docs/DEEP_SKY.md) and [reference-data audit](docs/REFERENCE_DATA_AUDIT.md).
- [Verification](docs/VERIFICATION.md), [device checks](docs/DEVICE_TESTING.md)
  and [original extraction](docs/EXTRACTION.md).
- [Assets and third-party credits](ASSET_CREDITS.md).

## License

Copyright © 2026 Mondiversi. The application code is licensed under
[GPL-3.0](LICENSE). Bundled third-party SGP4 code and image maps retain their
respective licenses and credits. Astronomy Engine is licensed under MIT.
The Space Compass launcher and loading artwork are original vector assets.
