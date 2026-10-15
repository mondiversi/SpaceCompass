# Space Compass 1.3.7 release

Release dated 2026-10-15. Android/update version 1.3.7, version code 35,
application ID `me.mondiversi.spacecompass`, minimum Android API 26.

## Changes

- Separate the custom date and time into independently enabled sections, alongside position, altitude and time zone. Add current-date/current-time shortcuts and an optional advancing clock whose offset survives app/device restarts.
- Add a Current scenario summary with localized values and an approximate place beneath coordinates, plus an orange active-section counter on the main scenario icon.
- Apply matching altitude and time zone from the selected position together, with validated metadata, section-specific activation/change/deactivation notices, automatic saving and consistent expanding-card feedback.
- Keep enabled Earth-reference names visible across reticle crossings and in both camera/panorama captures, regardless of object-label policy.
- Add a last-known-position marker to the screensaver globe; correct map handedness and eastward rotation, and vary the Sun-facing seasonal perspective continuously with the effective date, including advancing scenarios.
- Keep screensaver coordinates in session memory, pause lookup/sensor/GPS work, share globe/marker transforms across WebGL and the software fallback, and preserve the established illustrative lighting and accelerated spin.
- Refresh in-app news in all twenty languages. Preserve credits, saved preferences and selections, signing identity, immutable image pack 1.1 and previous release assets.

## Architecture and compatibility

ObserverClock and ClockEditing own effective-time resolution and editor actions;
Draft validates enabled fields and metadata before committing a plan. The versioned
15-field plan remains compatible with earlier 5/7/11/13-field preferences. Disabled
scenario values remain available for reuse. Advancing clocks persist an offset,
not per-tick preference writes. Summary/badge/section notices consume saved state.
No system GPS or clock is changed. The existing foreground geocoder resolves the
approximate summary place only for a valid enabled saved position.

The screensaver retains the last valid effective coordinates in session memory.
Its seasonal orientation uses the effective observation instant, existing Astronomy
Engine solar longitude and mean obliquity of date. Both shader and software sphere
use the same inverse texture transform as the forward marker projection. One
minute updates refresh seasonal orientation, without waking GPS or sensors. The
Sun-facing seasonal perspective does not turn illustrative local lighting or the
accelerated spin into a live satellite image. Legacy fallback rasterizes at most
once per second and handles unavailable WebGL/context loss/reduced motion.

## Verification and distribution

Debug/release compilation, 1,061 JVM tests, Android-test compilation, release lint,
twenty-language resource parity/encoding/format checks, source privacy audit and
eighteen pinned WebP hash checks are required before publication. Connected
instrumentation is not run. Native phone checks of scenario summary, reference
captions, seasonal screensaver and final release/news are recorded separately.
GPU/software seasonal raster tests cover both hemispheres and four seasons,
map handedness, spin, marker projection, fallback and no-network operation.

The same distribution certificate and 16 KiB native alignment are retained.
The update index pins package, version, size and SHA-256. The optional information
archive matches the tagged source; the immutable graphics pack remains 1.1.
Private signing material, device coordinates and UI captures are not published.
