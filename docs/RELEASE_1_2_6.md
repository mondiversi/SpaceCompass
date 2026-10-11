# Space Compass 1.2.6 release

Release dated 2026-10-11. Android/update version 1.2.6, version code 27,
application ID `me.mondiversi.spacecompass`, minimum Android API 26.

## Changes

The Earth screensaver covers the Android system bars and displays local time,
weekday/date and battery. While visible, a child lifecycle suspends the retained
page's sensors, GPS, camera, data refresh and rendering. Foreground music and the
independent screensaver remain active. Touch or Back returns to the same page and
restores its resources. New settings start music at 20%; saved choices are retained.

Orientation freshness is checked even when callbacks stop. Bounded restart attempts
recover stalled sensor registrations; calibration and possible interference remain
distinct. Inline status actions are offered only when useful; active location search
has no Retry action. Main and capture controls retain their saved choices.

Tapping a sky-path point opens the daily path, centers the selected row within the
real scroll extent and highlights it temporarily. The current row is retained even
when the live object is offscreen. Hourly points run from 0 to 23 and Moon phases
remain informational symbols. A single compact fixed heading places the object name
above its path date/time zone, to the left of the aligned azimuth/elevation columns.
Object facts begin with the object's localized name. Photo and panorama event labels
show frozen minute-resolution T+/T− intervals relative to the capture instant.

Current descriptions and all twenty in-app news summaries describe Space Compass
independently. Scientific and image attribution, copyright and licenses are retained.

## Verification and distribution

Local release checks build debug/release APKs, run 882 JVM regressions, compile
Android instrumentation sources and run release lint. Connected instrumentation is
not claimed as executed. Native phone checks of release identity, news and daily
headers are recorded separately, with earlier feature checks identified by scope.
Translation-key/format-argument parity, map hashes, source exclusion and architecture
sizes are audited. GitHub-hosted checks and anonymous downloads are verified separately.

The aligned, non-debuggable APK retains the distribution certificate and 16 KiB
native-library alignment. Its signed update index contains the exact version code,
APK size and checksum. The updated information archive includes current catalogs
and documentation. The unchanged eighteen-map WebP graphics archive and immutable
celestial texture pack 1.1 retain their hashes and URLs; maps download individually.
Install over the existing app to preserve preferences and cached images.
