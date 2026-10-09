# Space Compass 1.2.2

Version 1.2.2, code 23, package me.mondiversi.spacecompass and tag v1.2.2.
Android 8.0/API 26 or newer. This is a regular release: the twenty in-app news
summaries describe this version. Installation preserves existing preferences,
selected objects, image caches and the shared UVIR distribution certificate.

Quiet Orbit is an original 96-second stereo meditative loop, distributed as a
350,618-byte offline Ogg under the project GPL-3.0 license. Soft start/loop edges,
smoothed gain, Android audio focus and foreground-only playback keep it unobtrusive.
It starts enabled at 20% where no saved choice exists; saved disabled preferences
are applied before the first foreground callback. The volume slider uses 10% steps.
There is no new playback dependency, service, permission or audio network request.

General replaces the visible Appearance label in twenty languages and has a compact
cog glyph. The music card follows Theme. Music and four Custom scenario toggles
share a 300 ms top-anchored expand/collapse/fade, including all body spacing and
dividers. Exiting controls become inert immediately. Disabled music shows only its
heading and clears uncommitted previews. Sensor calculations are preserved; native
pointing icons now use straight optical-axis and diagonal phone-length arrows.

Image pack celestial-textures-v1.1 stays pinned to the same individual files and
verified hashes/dimensions. The optional complete graphics archive is reused byte
for byte. The information archive is refreshed with the current translated strings,
reference tables, documentation and licence notices. Runtime/user caches are excluded.

The signed update index carries code 23 and the exact APK URL, size and SHA-256.
Local debug/release builds, 789 JVM tests, Android test-source compilation, release
lint, translation/placeholder parity, public-source review and 16 KiB alignment
form the release checks. Device UI/audio checks and hosted Android checks are
reported separately in release-metadata.json; the full connected instrumentation
suite is not run on the personal phone. Public downloads are checked after publication.
