# Space Compass 1.3.4 release

Release dated 2026-10-14. Android/update version 1.3.4, version code 32,
application ID `me.mondiversi.spacecompass`, minimum Android API 26.

## Scene continuity and measurement trust

SpaceCompassSceneResumePose stores only the last accepted visual pose. During pause
and recovery it keeps terrain and celestial drawing present without restoring live
compass trust. Fresh readings retain control of measurements, point hit targets and
camera exposure attitudes. Held screen axes are remapped across all four display
rotations; the physical rear-camera axis is preserved. Existing listeners and jobs
still stop in background and during screensaver operation.

The star field retains its last basis while inactive; no new basis calculation is
started. Unchanged drawing values do not resubmit GPU work. Compass notices require
a resumed and visible main page, so suspension does not add a loading warning to
the Android exit animation.

## Navigation and screensaver

The object-selector arrows retain 48 dp targets and 10 by 16 dp glyph layouts.
Their 28 dp press background follows the glyph, with a mirrored 1 dp optical
correction. Existing back/map/collapse chevrons already retain their optical offsets.
The screensaver overlays its return hint at the top centre using cutout-safe margins,
adaptive text sizing and the same label for the whole-cover accessibility action.
Its immersive animation, clock, date, battery and touch-to-dismiss remain intact.

## Verification and distribution

Validation includes debug/release builds, 976 JVM tests, Android-test compilation,
release lint, twenty-language parity/encoding/placeholder checks, source-secret
audit and all eighteen pinned WebP image hashes. Native phone checks are recorded
separately. Connected instrumentation is not run. Functional native checks include
pressed arrows in both orientations, three background/resume cycles and portrait/
landscape screensaver hints. The final APK additionally verifies version and news.

The APK retains the distribution certificate and 16 KiB native alignment. The signed
update index pins package, version, size and SHA-256. The optional information ZIP
reflects the tagged source. Texture pack 1.1, individual URLs/hashes and all older
release assets remain unchanged.
