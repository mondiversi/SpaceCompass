# Space Compass

[![Android checks](https://github.com/mondiversi/SpaceCompass/actions/workflows/android.yml/badge.svg)](https://github.com/mondiversi/SpaceCompass/actions/workflows/android.yml)

Space Compass is an Android sky compass for the Sun, Moon, planets, satellites,
stars and other celestial objects. Point your phone or tablet, explore daily
paths, inspect reference facts and view textured models. It is an independent
application originally extracted from [UVIR](https://github.com/mondiversi/Uvir).

## Download and install

Download **space-compass-1.1.3.apk** from the
[Space Compass 1.1.3 release](https://github.com/mondiversi/SpaceCompass/releases/tag/v1.1.3).
Android 8.0 or newer is required. Open the APK and follow Android's installation
prompts; allow installation for your browser or file manager if requested.

Install over the existing app to retain settings and selections. **Do not uninstall
or clear app data when updating.** Releases since 0.1.2 check for signed GitHub
updates at startup; you can also use **Info & credits → GitHub repository → Check for
updates**. Download and installation require confirmation. Version 0.1.0 needs
one manual APK update to acquire this feature.

The release includes `SHA256SUMS.txt` and a signed update index. GitHub's source
ZIP/TAR archives and Actions debug artifacts are separate from the signed APK.
See [distribution and signing](docs/GITHUB_RELEASES.md) and
[the 1.1.3 release audit](docs/RELEASE_1_1_3.md).

## Features

- Live azimuth/altitude, compass guidance, named daily paths and satellite passes.
- Thirty ordinary catalog objects: Sun, Moon, planets, natural/artificial satellites,
  comets, Voyager spacecraft, nearby stars, compact objects and an exoplanet.
- Live search by localized name or catalog identifier, combined with type/visibility filters.
- Type/visibility filters and numeric sorting by distance, mass, diameter, pressure,
  gravity and temperature. Missing properties remain unavailable and sort last.
- Selections, active target, filters, sorting, units and interface settings persist.
- Rear-camera overlays with celestial markers and paths, consistent ultrawide/zoom
  steps, pinch and direct level selection; full 360° sky panoramas.
- Exposure-time direction, inclination and field of view in camera photos; previews
  follow selected language/units independently of the export profile.
- Optional celestial reference curves and pole/zenith/geocentre crosses. Guide and
  camera modes start disabled; Sun and Moon are the initial checked objects.
- Manual export to the gallery, a document or sharing; selected-language/unit and
  international profiles, optional labels and position disclosure. Only the general
  place is shown by default, without precise GPS coordinates. No automatic gallery save.
- Day/night OpenStreetMap position details with GPS, altitude, estimated place and weather.
- Scenarios with independently enabled position, date/time, altitude and time zone,
  including past/future skies and weather estimates when available.
- Textured interactive object models, lunar phases, qualified reference data and credits.
- Light/dark themes, animated introduction, phone/tablet and portrait/landscape layouts.
- Twenty interface languages, system-language fallback and regional/unit defaults.
- Signed startup/manual GitHub updates with package, certificate, size and checksum verification.

The application ID is `me.mondiversi.spacecompass`. The current stable version is
**1.1.3**, with Android/update identity `1.1.3` and version code **20**.
This preserves the three-part update protocol used by existing preview installations.
The app has its own preferences and caches and can coexist with UVIR.

Scientific facts carry their qualifications and source notices. Unavailable data
are not replaced with invented values. This is a sky visualization and not a
safety or navigation instrument.

## Build and checks

Open in Android Studio. Install Android SDK Platform 37.0 and Build Tools 36.0.0.
The local build uses the bundled Android Studio JDK 25; CI uses Temurin 25.
Gradle is pinned to 9.5.0, Android Gradle Plugin to 9.3.1 and the Compose compiler
to 2.2.10. The first build needs internet access.

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:assembleRelease :app:lintRelease
```

On Windows use `gradlew.bat`. Debug APKs are written to `app/build/outputs/apk/debug/`.
Release builds enable R8 code and resource optimization. GitHub Actions runs these
checks without the distribution key and retains debug/test reports. Instrumentation
sources are compiled; execution requires separate device/emulator authorization.
Release APKs are aligned and signed locally. Private keys, credentials and local SDK
paths are excluded from the repository and never stored in GitHub Actions.

## Permissions and online data

Location is requested for observer-dependent calculations. Camera access is requested
only when camera mode is enabled. Location/orientation/camera resources follow the
foreground lifecycle. The app has no background location, advertising or analytics.
Android 8/9 asks for legacy storage write access only for an explicit gallery save.
The app never reads other apps' photographs. Update installation uses Android's
installer permission and always requires the user's confirmation.

Sun, Moon and major-planet calculations run locally. Satellite elements and
JPL Horizons ephemerides use bounded HTTPS requests and app-private caches. Catalog
data refresh automatically every ten minutes and on request. Network failure does
not disable valid offline calculations. Unavailable/expired data remain explicit.

Open-Meteo weather uses rounded coordinates. Position details use Android's geocoder,
which may contact its configured service. Maps request OpenStreetMap tiles; night maps
are styled in the app. User-initiated scenario lookups may contact the documented
location/elevation/time-zone providers. These lookups are foreground-only; street
addresses, place names and observer coordinates are excluded from error logs.
See [scenario provider details](docs/OBSERVATION_SIMULATION.md). Review provider terms
before commercial distribution, including the free Open-Meteo endpoint.

## Documentation and license

- [Architecture](ARCHITECTURE.md), [presentation](docs/APP_PRESENTATION.md) and [verification](docs/VERIFICATION.md).
- [Celestial models](docs/CELESTIAL_VIEWER.md), [deep-sky data](docs/DEEP_SKY.md) and [reference data](docs/REFERENCE_DATA_AUDIT.md).
- [Panorama/export](docs/PANORAMA_CAPTURE.md), [camera](docs/CAMERA_VIEW.md), [catalog search](docs/CATALOG_SEARCH.md) and [curved reference labels](docs/CURVED_REFERENCE_LABELS.md).
- [Assets and third-party credits](ASSET_CREDITS.md).

Copyright © 2026 Mondiversi. Application code is licensed under [GPL-3.0](LICENSE).
Third-party SGP4 code and image maps retain their respective licenses/credits;
Astronomy Engine uses MIT, AndroidX uses Apache-2.0 and Leaflet uses BSD-2-Clause.
Launcher/loading artwork and the fictional Easter-egg texture are original assets.

## Data and graphics packages

Release 1.1.3 also provides a reference-information ZIP and the current 12-map
2K WebP graphics ZIP, both with file manifests and credits. They are optional
archives: scientific reference data, all 20 languages, the star atlas and LV-426
are already included in the APK. Planet maps are downloaded individually from
the stable [Celestial texture pack 1.0](https://github.com/mondiversi/SpaceCompass/releases/tag/celestial-textures-v1),
verified and reused offline. The original lower-resolution Io/Europa files remain
available for older app builds. Runtime satellite and JPL data retain their own
refresh schedules and are not frozen into the information archive.

The bundled star atlas follows observer location and date, and weather effects
match the live sky and panoramic capture. Camera, weather effects and reference
curves start disabled; enabled choices and camera zoom are saved. Object models
have compact + and - controls, with the opening size as their minimum zoom.
