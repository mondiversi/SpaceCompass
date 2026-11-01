# Space Compass 1.7.6 release

Release dated 2026-11-01. Android/display version 1.7.6, version code 62,
application ID `me.mondiversi.spacecompass`, minimum API 26. The existing
distribution certificate and private-data-compatible update path are retained.

## Catalogue data

The saved CATALOG identifier now means increasing solar distance. Default and
alphabetical views use the same solar reference as the explicit solar order;
Earth distance remains a separate numeric choice. Selected-first visit grouping,
missing-last sorting, units and stable ties remain intact. Near-Earth satellite
solar distances retain Earth's catalogue-resolution reference and are marked ≈.

Visible-row satellite warming prefers the validated structured alternative, with
the existing independently validated providers as fallbacks. Shared transport
backoff prevents repeated host outages from delaying every row. Per-request failure
and active-request state distinguish loading, outdated models, an uncovered viewed
date and unavailable quantities without invalidating catalogue worker snapshots.
Current satellite elements are not extrapolated into unrelated simulated dates.
No observer coordinates are sent to the orbital-data providers.

## Interface and gallery

Daily path centers Current on each object's first visit, within actual content
bounds. Returning preserves manual session scroll; time ticks do not recenter it.
Orbit/reference labels use compact clearances, projection-specific placement and
bounded re-entry recovery. The main landscape split is 55% sky / 45% information.
The GPS island shows GPS altitude without duplicating its detailed accuracy.
Normal controls place 0°/90° before Weather; camera controls keep Transparency
before Zoom. Capture uses the regular control size, a fine symbol and a gentle
3.2-second border pulse. Chevron positioning is optically centered.

The English screenshot gallery shows public London demonstrations in portrait
and landscape with main README previews. No personal location, notifications or
private camera frames are used. Unframed native captures and provenance accompany
the presentation cards in [the gallery](screenshots/README.md).

## Verification and compatibility

The release gate includes debug/release builds, all 1,522 JVM tests, Android test-source
compilation, release lint, twenty-language key/placeholder parity, reproducible generated
catalogues/thumbnails, signature/alignment checks and the embedded exact source commit.
Connected instrumentation is not run on the personal phone. Emulator interactions,
phone installation, public source checks and hosted CI are recorded separately in
release metadata; unexecuted device tests are not represented as passing.

Graphics collection 1.8 remains unchanged: 189 individual external WebP images and
189 previews, with inherited URLs, integrity pins, licenses and offline cache
compatibility. No celestial photographs or complete texture archive are added to
the APK. The optional information archive excludes screenshot/texture image binaries
and private device data. Final APK/index checksums describe the exact tagged source.
