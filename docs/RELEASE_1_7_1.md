# Space Compass 1.7.1 release audit

Version 1.7.1 uses Android code 57, the existing package and the existing distribution certificate.
The release adds a saved live reticle/callout toggle, higher-contrast generated symbols, asynchronous
catalogue presentation, on-demand one-pass facet counts and deferred optional row loading. A centered
circular initial-loading indicator follows the day/night theme. The 9,999-object catalogue, scientific
models, saved checks/favorites/filters and capture-label policies retain their established behavior.

## Physical-device comparison

Two matched release variants, differing only in the catalogue optimizations, were compared on a
Samsung SM-G970U1 running Android 12. Six warmed-cache rounds used the same catalogue-opening,
filter-opening, eight-swipe scrolling and three-character search gestures, ordered optimized → previous
→ optimized. Android gfxinfo counters were reset for each phase. Phone temperature stayed within
30.2–31.3 °C; normal background services and network responses were not disabled.

During scrolling, the median of the per-run median render times fell from 42 to about 30 ms; the
aggregate share of frames missing Android's deadline fell from 92.19% to 59.59%. The longest opening
frame histogram buckets were 250–300 ms previously and 57–65 ms after optimization. Search-frame
95th-percentile buckets improved from 93–105 ms to 31–44 ms. These are small-sample render timings
on one phone, not complete-task durations, universal frame-rate claims or a promise of no remaining jank.
The optimized APK was restored after the comparison. No connected instrumentation was executed.

## Distribution and checks

Refresh six news summaries and validate all translatable keys/placeholders in twenty languages.
Required checks cover debug/release builds, JVM tests, Android instrumentation-source compilation,
release lint, signing identity, alignment and the exact committed source embedded in the APK.
Physical checks, hosted CI and public downloads are recorded separately in the release metadata.

Reuse unchanged graphics collection 1.8: 189 full WebP images and 189 independent previews, totalling
43,663,574 binary bytes. Existing integrity pins, attribution, individual URLs and verified offline caches
remain intact. No new graphics release or complete texture ZIP is created. The optional information
archive is a reference/localization snapshot, not a runtime package; it excludes personal device data,
screenshots, performance recordings, caches, credentials and celestial photographs.
