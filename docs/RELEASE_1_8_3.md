# Space Compass 1.8.3 validation

Version 1.8.3 uses Android version code 69 and the existing distribution certificate.

## Changes

Key Events and Key Heights use keyed lazy rows with per-object session scroll state.
Loading placeholders have separate list state, preserving the loaded list position.
Personal Moments caches formatted timestamps for the current visit. Export resource
profiles and angle formatting are remembered. Covered Observatory model, geometry,
measurement and rotation updates pause while a child list is open; current inputs
resume on return. Celestial computations and persisted selection semantics are preserved.

## Verification scope

The release workflow builds debug/release APKs, runs JVM tests, compiles Android
instrumentation sources and runs release lint. Instrumentation compilation is not
reported as execution. A regression test covers long-list scroll retention across
a temporary placeholder. Final outcomes and artifact checksums are recorded in the
release validation metadata.

Before the version-only bump, the same optimization was measured twice on a Samsung
Galaxy S10e, using the Moon with 77 Key Events and 53 Key Heights and the same swipe
sequence before and after installation. Median Android-reported janky-frame rates
fell from 100% to 3.63% for Events and 92.235% to 3.5% for Heights. The 95th percentile
frame time fell from 87 to 19 ms and 71 to 18 ms respectively. These are specific
device measurements, not a universal frame-rate guarantee. Session scroll return
was verified for both lists. Personal Moments was empty, so a long personal list
was not benchmarked. No Location values, object selections or moments were changed.

## Distribution

The APK, signed update index, information archive, validation metadata and checksums
refer to the same source commit. All twenty news translations are updated. The
existing graphics collection 1.8, individual downloads and integrity pins are unchanged.
No complete texture ZIP or replacement graphics release is created.
