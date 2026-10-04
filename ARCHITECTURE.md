# Architecture

## Application boundary

`MainActivity` owns the standalone launcher, system-bar insets, adaptive density,
theme and foreground screen-awake request. It opens `PlanetCompassSunFinderScreen`
directly. The inherited internal SunFinder naming denotes the evolved celestial
compass, not a second application or hidden UVIR entry point.

The application ID, Kotlin namespace, app-private files and preferences are distinct
from UVIR. No parent project, symlink, external source directory or UVIR module
is required at build time. Shared UI helpers were copied and renamed, not linked.

## Code groups

- `PlanetCompassSunFinder*`: lifecycle-scoped location/orientation, sky/ground
  rendering, adaptive layout and environment panels.
- `PlanetCompassCelestial*`: catalog, selection, per-body ephemerides, refresh
  policy, path interaction, thumbnails, data facts and OpenGL model viewer.
- `PlanetCompassMoon*`: continuous lunar phase geometry and shared live marker mask.
- `PlanetCompassIss*`, `PlanetCompassSatellite*`, `PlanetCompassStarlink*`, `sgp4`:
  validated satellite elements and SGP4 propagation.
- `PlanetCompassHorizonsEphemeris`: validated JPL ephemerides and motion parsing.
- Copied presentation helpers: accessible controls, dropdowns, bounded dialogs,
  scrollbars, numeric/date formatting, title overflow and screen scaling.
- `PlanetCompassErrorLog`: independent bounded app-private error/crash log.

Astronomy calculations and downloads remain off the UI thread according to the
existing refresh policy. Orientation updates project cached paths; they must
not regenerate orbital calculations, decode textures or dismiss open dialogs.
Moon live/edge markers share one current phase per UTC minute. The textured
viewer and all asset credits are described in `docs/CELESTIAL_VIEWER.md`.

## Testing boundary

Unit tests cover celestial calculations, geometry, selection, formatting,
localization and source-level UI/security contracts. Ported instrumentation tests
use synthetic location and orientation rather than recording a user's position.
They need a device or emulator; compilation alone is not execution.

Keep future settings, release signing/updater, persisted object selection and
onboarding within this application's boundary. Do not reintroduce UVIR sensor
credentials, database migrations or hardware services into this project.
