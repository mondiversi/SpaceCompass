# Space Compass 1.7.9 release audit

Release dated 2026-11-03. Android/display version 1.7.9, version code 65,
application ID `me.mondiversi.spacecompass`, minimum API 26. The existing
distribution certificate and data-preserving installation path are retained.

## Code, architecture and rendering

The edited control code was reviewed against 1.7.8. Presentation-only control
containers are isolated in SpaceCompassMainControlGroups rather than duplicated
across modes. A shared measured vertical axis drives both side columns and hit
exclusions. Camera/shutter placement remains outside the animated information pane.
Row layouts, persisted choices, privacy rules and astronomical calculations are
retained. Off-state icons reuse the same geometry and optical correction.

The shutter pulse remains a draw-phase update, without per-frame layout or scene
recomposition. Its static grey endpoint stays visible when system animations are
disabled. Inactive controls, neutral actions and unavailable controls remain
distinct. Redundant imports and the newly edited callback formatting were cleaned
up; no library upgrades or broad scientific-model refactors are part of this release.

## File sizes and resources

The largest source is the generated, identity-stable celestial model (about 78 kB);
the main scene coordinator is about 57 kB. Generated catalogue chunks remain
separate. No size-driven splitting of generated identifiers is introduced.
The graphics collection remains 1.8: 189 full images and 189 independent thumbnails,
downloaded individually and cached offline. All names, WebP metadata, sizes,
integrity pins, credits and legacy resource bytes are verified. No complete
texture ZIP is produced and no celestial image binaries are added to the APK.
Final APK/archive bytes, hashes and bundled reference-image sizes are recorded
in release-metadata.json and SHA256SUMS.txt rather than estimated here.

## Verification scope

The release gate covers debug/release compilation, all 1,596 JVM tests, Android
test-source compilation, full release lint, twenty-language key/placeholder parity,
generated-catalogue and thumbnail consistency, signing/alignment and the exact
embedded source commit. Lint findings are compared with the previous release;
baseline warnings are distinguished from blocking errors.

Native phone checks verify compact-landscape navigation order, both view-mode
columns, camera above shutter, point/antipode control below Menu and shared corner
midpoints. Rotation, camera and temporary selections are restored. Final-version
news and installation checks are recorded separately. Connected instrumentation
is not executed on the personal phone. Public source, gallery and release downloads
are verified against the committed source and signed package after hosted CI passes.

The information archive remains optional reference/localization material. It
excludes image binaries, private device data, credentials and live caches. The
existing gallery retains its original 1.7.6 capture provenance.
