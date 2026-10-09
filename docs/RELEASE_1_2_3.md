# Space Compass 1.2.3

Version 1.2.3, code 24, package me.mondiversi.spacecompass and tag v1.2.3.
Android 8.0/API 26 or newer. This maintenance release preserves all twenty 1.2.2
in-app news catalogs byte for byte. Existing preferences, selections, caches,
zoom/pointing reference and the shared distribution certificate are retained.

Compass notices distinguish fresh magnetic anomalies, calibration, low accuracy
and stale/recovering samples. Field tolerance, sample freshness and recovery
thresholds are unchanged. All notices/actions share theme-aware orange; their
localized texts have no trailing full stops or ellipses. Data/retry retains priority.
Six pure JVM regressions cover classification, precedence and the existing limits.

The pointing control uses bold localized 0°/90° digits and horizontal/vertical
bars, with optical alignment inside the existing accessible circle. Capture uses
a panorama glyph in the virtual scene and the existing shutter in camera mode.
The translated export dialog is titled Export image; saved export profiles remain.

No dependencies, Android permissions or runtime packages are added. R8 code/resource
optimization remains enabled. Image pack celestial-textures-v1.1 stays pinned to
its eighteen verified individual WebP assets and unchanged optional ZIP. Current
translated resources, references, documentation and license notices are included
in the refreshed information archive; user/GPS/network caches are excluded.

The signed update index identifies code 24 and the exact APK URL, byte count and
SHA-256. Release validation covers debug/release builds, 795 JVM tests, Android
test-source compilation, release lint, twenty-language key/placeholder parity,
source privacy review, image pins and 16 KiB APK/native alignment. Actual device
checks and hosted Android checks are reported separately in release-metadata.json.
The full connected instrumentation suite is not run on the personal phone.
