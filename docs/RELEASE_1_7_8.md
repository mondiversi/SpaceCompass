# Space Compass 1.7.8 release

Release dated 2026-11-02. Android/display version 1.7.8, version code 64,
application ID `me.mondiversi.spacecompass`, minimum API 26. The distribution
certificate and data-preserving installation path are unchanged.

## Portable Location points

The optional celestial-selection section now includes original selected orbit
points and the active object. Imports validate the complete payload before
changing settings and restore pins into the shared selection owner. Imported
points have no cached path; live projections and Daily path rows resolve a
compatible path when needed. Derived antipodes are not serialized or enabled.
Explicit empty point lists clear the previous pins, while older Location files
without the optional extension preserve the current selection. Retired or
unchecked object references are ignored. Time, geometry, size and duplicate
checks reject malformed known-point data without partially applying it.

## Antipodes and tutorial

The button uses the original selected-point symbol when antipodes are hidden,
and an equally sized opposite-points symbol when they are visible. Its badge is
neutral: dark with light digits by day, light with dark digits by night. Short
toggles have no vibration; holding still clears selected points with feedback.
Projected antipodes no longer contain a dark disk. Normal sky, camera, photo
and panorama renderers preserve the scene inside the symbol.

Tutorial taps do not alter antipodes or real selections. Camera demonstration
changes only tutorial controls and never opens CameraX. Tutorial titles and
accessibility labels distinguish selected points from selected points with
their antipodes. Android notifications use the same clear distinction.

## Verification and distribution

The release gate includes debug/release compilation, 1,596 JVM tests, Android
test-source compilation, release lint, twenty-language key/placeholder parity,
generated-catalogue and thumbnail consistency, signing/alignment and the APK's
exact embedded source commit. Connected instrumentation is not executed on the
personal phone. Final installation, hosted CI and public asset/source checks
are recorded separately in release metadata without claiming unexecuted UI tests.

Graphics collection 1.8 is unchanged: 189 external images and 189 independent
thumbnails. Individual downloads, integrity pins, credits, legacy URLs and offline
caches remain compatible. No new texture ZIP or embedded celestial images are
distributed. The optional information archive contains reference and localization
material, excluding image binaries, device data, credentials and live caches.
The public demonstration gallery keeps its original 1.7.6 capture provenance.
