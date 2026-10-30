# Space Compass

[![Android checks](https://github.com/mondiversi/SpaceCompass/actions/workflows/android.yml/badge.svg)](https://github.com/mondiversi/SpaceCompass/actions/workflows/android.yml)

Space Compass is an Android sky compass for the Sun, Moon, planets, satellites,
stars and other celestial objects. Point your phone or tablet, explore daily
paths, inspect reference facts and view textured models. Space Compass is a standalone application.

## Download and install

Download **space-compass-1.7.2.apk** from the
[Space Compass 1.7.2 release](https://github.com/mondiversi/SpaceCompass/releases/tag/v1.7.2).
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
[the 1.7.2 release audit](docs/RELEASE_1_7_2.md).

## Features

- A remembered main-view reticle/callout toggle, independent of capture labels. Generated celestial symbols use translucent dark backing for contrast.
- Catalogue search, filtering, sorting and optional facet counts run off the UI thread. Ready previews remain visible while optional row loading waits for scrolling to settle; initial loading uses a centered theme-aware spinner.
- Live azimuth/altitude, compass guidance, named daily paths and satellite passes. Pin multiple path points and manage the same selections in the daily path. Reticle-focused callouts show clock and T± side by side. Current positions and lunar phases remain available.
- 9,999 catalogue objects: Sun, Moon, planets, natural/artificial satellites,
  comets, Voyager spacecraft, nearby stars, nebulae, star clusters, compact objects and exoplanets.
- Live search by localized name or catalog identifier, combined with four paired filters: type, visibility, origin and pointing availability. Selected objects remain pinned first and visible through filters/search; numeric or alphabetical sorting still applies within each group. Object rows and Type choices use neutral theme backgrounds. A funnel identifies filters; up/down arrows identify sorting. The compact search field centers its text, hint and cursor.
- Type/visibility filters and numeric sorting by Sun-centred or Earth-centred distance, mass, diameter, pressure,
  gravity and temperature. Missing properties remain unavailable and sort last.
- Favorites are independent of scene selection: mark them with the Observatory heart and filter the catalogue with its heart. Favorite checkboxes use the shared day/night pink accent, while their check still controls scene selection. Favorites, filters, selected objects and the active target persist.
- Newly checked objects become the active target without jumping to the top during the catalogue visit. Returning from Observatory keeps list position; returning to the main scene and reopening the catalogue refreshes selected-first grouping. An open filter panel stays fixed while the search/value header retains quick-return scrolling.
- Filter-option counters combine favorites, search and the other active facets, including retained checked-object exceptions. Favorites-only remains a strict independent scope. Sorting, units and interface settings persist.
- Send future path points to Android Clock or Calendar with a visible handoff; control the main-view alert button under General → Events.
- Follow Tesla Roadster (2018) using JPL heliocentric ephemerides and a verified archival photograph.
- Steadier sensor-driven motion combines short gyroscope increments with the measured magnetic/gravity reference, and pauses with the screensaver/background lifecycle.
- Rear-camera overlays with celestial markers and paths, consistent ultrawide/zoom
  steps, pinch and direct level selection; full 360° sky panoramas.
  Zoom sits beside capture; a saved camera-only transparency control fades complete object glyphs to 50%.
  Photos and every export variant retain the opacity recorded at the shutter tap.
- Exposure-time direction, inclination and field of view in camera photos; previews
  follow selected language/units independently of the export profile.
- Optional celestial reference curves and celestial-pole, zenith and nadir crosses. Guide and
  camera modes start disabled; Sun and Moon are the initial checked objects.
- Save or share images, readable TXT tables and CSV spreadsheets from captures, daily paths and the Observatory,
  using one remembered selected-language/unit or international profile shared with Location exports.
  Filenames follow the chosen profile, with hyphens within fields and underscores between fields.
  Special/selected points are chronological.
  Location disclosure applies to every output: captures inherit the preview; daily paths start
  with area only. Hidden location uses UTC. No automatic gallery save.
- Day/night OpenStreetMap position details with GPS, altitude, estimated place and weather.
- Location management with independently enabled position, altitude, time zone, date and time;
  optional −3600× to +3600× time speed with a central 0× pause and restart/active-only/always resumption.
  Open it directly from the main scene. Compact independent cards show effective current values;
  customized values are bold. The leading icon/checkbox/title target toggles a parameter; values,
  the chevron and other card space open its editor. Child forms stay fully visible and editable, with activation controlled only by the management menu.
  Prepared inactive values are saved without applying them. Resumption has an always-checked, disabled grey
  checkbox and still offers its three policies.
  The position editor embeds a square-cornered full-width map, paired coordinates, consistent action buttons and estimated weather. Normal scrolling remains available without stretching content away from the native map.
  Natural-height cards pack into up to two readable columns in landscape.
  Save, share and import named `.spacecompass` location files with optional scene objects and the active target.
  Without observer or speed overrides, export captures the current position, altitude and time zone only;
  date and time stay live at the imported place, and the recipient's resumption preference is retained.
  Missing fixes remain explicit. Including an empty object selection clears the recipient's scene;
  omitting objects leaves it alone. Favorites and catalogue filters are not transferred.
  Readable fields and suggested filenames follow the shared export profile; import values stay canonical.
  Configured temporal locations retain speed and resumption without accruing time while archived.
- Read object names in the theme text color, with an outlined split-color dot: above-horizon orbit color on top and below-horizon color underneath. The dot remains visible beside shortened long names. Photo/panorama legends and image exports use the same text-sized dots, with outlines matching the lettering. Button counters use larger solid theme-accent badges with light/dark bold digits.
- Tap the object name to open the catalogue. Checked objects remain first in the chosen order and visible despite filters/search. The island keeps Observatory and Daily path shortcuts. Observatory offers Favorite and Export; Daily path retains Export.
- Science fiction is an additional type-filter membership, with ten new clearly qualified illustrative references. Their real reference directions and approximate ranges do not imply measured fictional orbits or physical properties.
- Compare estimated travel times at everyday, light-speed and fictional warp speeds through Warp 9.999. The selected speed persists; compact duration symbols and unit preferences apply.
- Observatory with textured interactive models, lunar phases, classified reference tables,
  qualified core temperatures, spatial context and credits. Export data and current/rotation images.
- General settings combine light/dark themes, adjustable sky fluidity, event controls and original ambient music synthesized continuously offline. Balanced fluidity is the default; previously saved levels are retained.
  Music starts enabled at 20% volume, with saved on/off and ten-percent volume steps. Settings-save confirmations are deferred until leaving a modified settings page.
- Music cards reveal their controls with 300 ms animations. Location child forms stay fully editable independently of their activation in the management menu.
- Full-screen animated Earth screensaver with local time, date and battery, enabled after five minutes by default. It shows the last known real GPS position, with a dimmed grey marker on the far side, on a correctly oriented globe with eastward visual spin. Seasons and geographic day/night illumination follow the real UTC Sun independently of scenarios. It pauses live sensors, GPS, camera and data refresh until dismissed.
- Solar-height-driven dawn/dusk lighting shared by sky, ground, weather and panoramic captures.
- Translucent main islands with paths and objects drawn behind them, a reticle centred in the free sky area and a 50/50 landscape split.
- Titled photo/panorama previews with opaque theme-matched toolbars and top-aligned controls, object-colour legends and larger wrapping photo metadata on a separate opaque black band.
- White reticle, existing-outline object focus/selection, a lighter daytime blue and stable bottom-right landscape capture. Equal-size Info/logo and Menu controls stay centered between the upper corner controls; the menu opens from its own anchor. The lower-left row keeps panel collapse and pointing-axis selection. Capture symbols are white by day and dark at night.
- Stable ground, orbits and star background when returning from background; optically centred object arrows and a clear screensaver return hint. Focused callouts cover celestial icons without jumping; split-color keys beside object names and in capture legends show both horizon treatments.
- Compact T+/T− hour/minute offsets shared by views and exports; orbit points behind translucent title/information overlays remain visible without being selectable.
- Capture labels can show only key events and selected points across all objects; this is the default for fresh settings. Existing saved label choices remain intact.
- Enabled Earth-reference names remain visible through reticle crossings and all capture-label policies.
- Daily paths use compact, uniformly thin reminder bells with more room for data. Daily paths pair key-event names with compact symbols and bold their complete rows, including clock/T± and angles. Culmination uses + consistently in the live sky, captures and image exports.
- Time-coincident close-approach key events in paths, captures and exports; these are angular alignments, not eclipse predictions.
- Open Observatory from a catalogue thumbnail or name; only the checkbox changes scene selection. Keep passive data values separate from both actions. Includes a 15-minute screensaver delay and independently packed General cards in landscape.
- Animated introduction, phone/tablet and portrait/landscape layouts.
- Twenty interface languages, system-language fallback and regional/unit defaults.
- Signed startup/manual GitHub updates with package, certificate, size and checksum verification.

The application ID is `me.mondiversi.spacecompass`. The current stable version is
**1.7.2**, with Android/update identity `1.7.2` and version code **58**.
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
data refresh silently every ten minutes and when models or observer settings change. Network failure does
not disable valid offline calculations. Unavailable/expired data remain explicit.

Open-Meteo weather uses rounded coordinates. Position details use Android's geocoder,
which may contact its configured service. Maps request OpenStreetMap tiles; night maps
are styled in the app. User-initiated Location lookups may contact the documented
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
Launcher/loading artwork and original illustrations are project assets.

## Data and graphics packages

Release 1.7.2 includes **9,999 selectable objects**, and an optional reference-information archive.
All descriptive data and translations remain inside the APK. Scientific limits and provenance are in the
[catalogue sources](docs/CATALOG_9999.md) and [canonical source catalogue](catalogue/README.md).

The [graphics collection 1.8](https://github.com/mondiversi/SpaceCompass/releases/tag/celestial-textures-v1.8)
contains **189 full images and 189 independent 128×128 previews**, all WebP quality 90/method 6.
Most full images are 2048×1024; existing illustrations retain their original source dimensions.
Catalogue/live/captured glyphs request lightweight previews. The Observatory and image exports
fetch full resources when needed; lunar phase stays dynamic.
The complete logical image collection is **43.66 MB (41.64 MiB; 43,663,574 bytes)**, excluding metadata.
The incremental 1.8 release adds five photographs and five previews: **444,858 image bytes**, plus index, credits and checksums. Unchanged files continue to download from immutable packs 1.6 and 1.7;
the manifest and APK pin every resource to its exact version, size, SHA-256 and dimensions.
Downloads are individual, with persistent offline caches and reusable verified legacy filenames.
Missing photographs keep appropriate rendered symbols without disabling pointing.
The APK contains scientific data, all twenty languages, the star atlas and separate Earth screensaver maps,
without celestial photographs or complete texture ZIPs. Older releases and their resource URLs remain available.
