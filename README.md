# Planet Compass

An independent Android celestial compass extracted from UVIR. Point the phone
to locate selected celestial objects, inspect their trajectories and explore
textured body models. This is a recreational/scientific visualization, not a
navigation, irradiance or safety instrument.

## Current foundation

- Direct launch into the compass, with Sun and Moon initially selected.
- Multiple-object selection, live pointing, daily paths and satellite passes.
- Planet/Moon textures, lunar phases, interactive 3D views, zoom and reference facts.
- Adaptive phone/tablet layouts, portrait/landscape, light/dark system themes and RTL.
- The existing localized feature resources and regression tests are retained.
- Independent application ID, preferences, diagnostic log and ephemeris cache.

The package is `me.mondiversi.planetcompass`, version `0.1.0` (development).
It can coexist with UVIR. It contains no UVIR hardware pairing, Bluetooth/USB/MQTT
connection managers, measurement database or sensor firmware.

## Build

Open this directory in Android Studio. Install Android SDK 37 and use a compatible
JDK (the bundled Android Studio JDK works). Gradle is pinned to 9.5.0; Android
Gradle Plugin to 9.3.1; Compose compiler to 2.2.10. The first build needs internet
access to download dependencies.

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin
```

On Windows, use `gradlew.bat`. The APK is produced under
`app/build/outputs/apk/debug/`. No release signing key is included; configuring
production signing is a separate release step. This repository has no GitHub
remote yet and has not been published.

## Permissions and online data

Location is requested at runtime for observer-dependent positions. Phone
orientation/location listeners run only in the foreground. No camera access,
background location, advertising or analytics is included. Approximate location
works; denied/missing locations are shown explicitly instead of invented.

Sun/Moon/major-planet calculations run locally. Satellite elements and Sedna/
Voyager ephemerides use bounded HTTPS requests and independent app-private caches.
Weather uses a rounded location cell with Open-Meteo. Network failure is logged
locally and must not disable valid offline calculations. Review provider terms
before commercial distribution, especially the free Open-Meteo endpoint.

See [architecture](ARCHITECTURE.md), [scientific and rendering notes](docs/CELESTIAL_VIEWER.md),
[extraction notes](docs/EXTRACTION.md) and [asset credits](ASSET_CREDITS.md).
The completed checks and remaining release work are recorded in
[verification](docs/VERIFICATION.md).

## License

Code retains the UVIR project's GPL-3.0 license. Bundled third-party SGP4 source
and image maps retain their own licenses/credits. The launcher vector is a new
placeholder identity for this independent project, not the UVIR logo.
