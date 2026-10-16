# Space Compass

[![Android checks](https://github.com/mondiversi/SpaceCompass/actions/workflows/android.yml/badge.svg)](https://github.com/mondiversi/SpaceCompass/actions/workflows/android.yml)

Space Compass is an Android sky compass for the Sun, Moon, planets, satellites,
stars and other celestial objects. Point your phone or tablet, explore daily
paths, inspect reference facts and view textured models. Space Compass is a standalone application.

## Download and install

Download **space-compass-1.4.1.apk** from the
[Space Compass 1.4.1 release](https://github.com/mondiversi/SpaceCompass/releases/tag/v1.4.1).
Android 8.0 or newer is required. Open the APK and follow Android's installation
prompts; allow installation for your browser or file manager if requested.

Install over the existing app to retain settings and selections. **Do not uninstall
or clear app data when updating.** Releases since 0.1.2 check for signed GitHub
updates at startup; you can also use **Info & credits â†’ GitHub repository â†’ Check for
updates**. Download and installation require confirmation. Version 0.1.0 needs
one manual APK update to acquire this feature.

The release includes `SHA256SUMS.txt` and a signed update index. GitHub's source
ZIP/TAR archives and Actions debug artifacts are separate from the signed APK.
See [distribution and signing](docs/GITHUB_RELEASES.md) and
[the 1.4.1 release audit](docs/RELEASE_1_4_1.md).

## Features

- Live azimuth/altitude, compass guidance, named daily paths and satellite passes. Pin multiple path points and manage the same selections in the daily path. Reticle-focused callouts show clock and TÂ± side by side. Current positions and lunar phases remain available.
- Thirty-six ordinary catalog objects: Sun, Moon, planets, natural/artificial satellites,
  comets, Voyager spacecraft, nearby stars, nebulae, star clusters, compact objects and an exoplanet.
- Live search by localized name or catalog identifier, combined with type/visibility filters.
- Type/visibility filters and numeric sorting by distance, mass, diameter, pressure,
  gravity and temperature. Missing properties remain unavailable and sort last.
- Selections, active target, filters, sorting, units and interface settings persist.
- Send future path points to Android Clock or Calendar with a visible handoff; control the main-view alert button under General â†’ Events.
- Follow Tesla Roadster (2018) using JPL heliocentric ephemerides and a verified archival photograph.
- Steadier sensor-driven motion combines short gyroscope increments with the measured magnetic/gravity reference, and pauses with the screensaver/background lifecycle.
- Rear-camera overlays with celestial markers and paths, consistent ultrawide/zoom
  steps, pinch and direct level selection; full 360Â° sky panoramas.
- Exposure-time direction, inclination and field of view in camera photos; previews
  follow selected language/units independently of the export profile.
- Optional celestial reference curves and pole/zenith/geocentre crosses. Guide and
  camera modes start disabled; Sun and Moon are the initial checked objects.
- Save or share images, CSV and readable TXT from captures, daily paths and the Observatory,
  using selected-language/unit or international profiles. Special/selected points are chronological.
  Location disclosure applies to every output: captures inherit the preview; daily paths start
  with area only. Hidden location uses UTC. No automatic gallery save.
- Day/night OpenStreetMap position details with GPS, altitude, estimated place and weather.
- Custom scenarios with independently enabled position, altitude, time zone, date and time;
  an optional advancing clock preserves its real-time offset across restarts. The current-scenario
  summary shows saved overrides and an approximate place, with an active-section badge.
- Observatory with textured interactive models, lunar phases, classified reference tables,
  qualified core temperatures, spatial context and credits. Export data and current/rotation images.
- General settings combine light/dark themes, adjustable sky fluidity, event controls and original ambient music synthesized continuously offline. Balanced fluidity is the default; previously saved levels are retained.
  Music starts enabled at 20% volume, with saved on/off and ten-percent volume steps.
- Music and scenario cards reveal their controls with 300 ms animations; disabled cards retain only their headings.
- Full-screen animated Earth screensaver with local time, date and battery, enabled after five minutes by default. It shows the last known observer position on a correctly oriented globe with eastward spin and a Sun-facing seasonal perspective based on the effective scenario date. It pauses live sensors, GPS, camera and data refresh until dismissed.
- Solar-height-driven dawn/dusk lighting shared by sky, ground, weather and panoramic captures.
- Translucent main islands with paths and objects drawn behind them, a reticle centred in the free sky area and a 55/45 landscape split.
- Titled photo/panorama previews with opaque theme-matched toolbars and top-aligned controls, object-colour legends and larger wrapping photo metadata on a separate opaque black band.
- White reticle, existing-outline object focus/selection, a lighter daytime blue and stable bottom-right landscape capture. Collapsed landscape panels use a floating central title bar.
- Stable ground, orbits and star background when returning from background; optically centred object arrows and a clear screensaver return hint. Focused callouts cover celestial icons without jumping; object names and capture legends use the full identity color. Remove a checked object directly from its main card after confirmation.
- Compact T+/Tâˆ’ hour/minute offsets shared by views and exports; orbit points behind translucent title/information overlays remain visible without being selectable.
- Capture labels can show only key events and selected points across all objects; this is the default for fresh settings. Existing saved label choices remain intact.
- Enabled Earth-reference names remain visible through reticle crossings and all capture-label policies.
- Daily paths pair key-event names with compact symbols and bold their complete rows, including clock/TÂ± and angles. Culmination uses + consistently in the live sky, captures and image exports.
- Animated introduction, phone/tablet and portrait/landscape layouts.
- Twenty interface languages, system-language fallback and regional/unit defaults.
- Signed startup/manual GitHub updates with package, certificate, size and checksum verification.

The application ID is `me.mondiversi.spacecompass`. The current stable version is
**1.4.1**, with Android/update identity `1.4.1` and version code **38**.
This preserves the three-part update protocol used by existing preview installations.
The app stores its preferences and caches privately.

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

Copyright Â© 2026 Mondiversi. Application code is licensed under [GPL-3.0](LICENSE).
Third-party SGP4 code and image maps retain their respective licenses/credits;
Astronomy Engine uses MIT, AndroidX uses Apache-2.0 and Leaflet uses BSD-2-Clause.
Launcher/loading artwork and the fictional Easter-egg texture are original assets.

## Data and graphics packages

Release 1.4.1 includes a reference-information archive and the complete 20-image
graphics archive. The app downloads individual maps, archival photographs and LV-426
from [Celestial texture pack 1.2.1](https://github.com/mondiversi/SpaceCompass/releases/tag/celestial-textures-v1.2.1).
Every celestial image is WebP at quality 90/method 6; nineteen are 2048 x 1024,
and LV-426 retains its original 1774 x 887 pixels. The APK includes scientific data,
all twenty languages, the star atlas and the separate Earth screensaver maps.
Complete graphics ZIPs are optional; they are never downloaded by the app. Verified
images are cached for offline use and matching legacy caches are reused. Older
graphics releases and individual URLs remain available for previous APKs.
