# Space Compass 1.3.5 release

Release dated 2026-10-14. Android/update version 1.3.5, version code 33,
application ID `me.mondiversi.spacecompass`, minimum Android API 26.

## Relative time and scene input

The shared PointCountdown formatter renders elapsed and remaining time as compact
hours/minutes, for example T−3h27m and T+0h5m. It continues to use absolute instants,
whole minutes, locale-aware digits and existing sign rules. Ordinary clock times
and time-zone offsets remain unchanged. Live callouts, daily paths, frozen capture
labels and CSV/TXT exports use this same formatter.

The scene hit detector excludes both a tap inside the main header and a target
whose centre is covered by that header. Live thumbnails use the same eligibility
check. Expanded headers and the measured floating landscape header are protected;
decorative orbital drawing remains intact. Blank-surface input shields protect
the object and compass/position islands without adding inert accessibility actions.
Child navigation, details links and scroll gestures retain priority. The full-scene
backdrop already renders covered paths and markers with interaction disabled.

## Verification and distribution

Validation includes debug/release builds, 979 JVM tests, Android-test compilation,
release lint, twenty-language parity/encoding/placeholder checks, source-secret
audit and all eighteen pinned WebP image hashes. Native phone checks are recorded
separately. The connected instrumentation suite is not run. Existing local native
checks cover the countdown, expanded/floating headers and island actions in both
orientations. The final signed release verifies version and translated news.

The APK retains the distribution certificate and 16 KiB native alignment. Its signed
update index pins package, version, size and SHA-256. The optional information ZIP
reflects the tagged source. Texture pack 1.1, individual URLs/hashes and all older
release assets remain unchanged.
