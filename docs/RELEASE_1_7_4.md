# Space Compass 1.7.4 release

Release dated 2026-10-31. Android/display version 1.7.4, monotonic version code 60,
application ID `me.mondiversi.spacecompass`, minimum Android API 26.

## Behavior

Key heights owns annual calculation, year selection, export and per-object session
scroll independently of Observatory. The 40% / 15% / 45% table separates moment,
elevation and actionable local date/time. Unnumbered single events and repeated
cycle labels share the same formatter in the screen and exports. Accepted observer
anchors and an activity-retained cache stabilize annual results without changing
the live sky. Incomplete ephemerides do not become annual predictions.

Annual CSV/TXT snapshots freeze the chosen calendar, completed events and accepted
observer. Shared format profiles and position-disclosure policies apply. Applying
an event preserves spatial/resumption settings and pauses the configured instant.

The main scene uses two upper-right control rows. Camera, Weather and 0°/90° are
ordered left to right on the second row, becoming Camera, Transparency and Zoom
in camera mode. Existing circle sizes, stroke weights, saved choices, optical
projection and capture anchor are retained. Settings and Location use shared
compact input/selection fields, excluding specialized catalogue search/filters.
The Location-title bulk shortcut preserves prepared values and resumption policy.

## Verification and distribution

The release workflow verifies debug/release compilation, 1,472 JVM tests, Android
test-source compilation, release lint, twenty-locale key/placeholder parity,
generated catalogue reproducibility and external-image integrity. Connected
instrumentation is not executed. Native phone checks separately verify the new
controls, annual page/action title, navigation and final version/news presentation.

The APK and signed update index retain the distribution identity. Publication
checks match the APK to the exact clean source commit, version/code, size and hash;
native alignment and non-debuggable/test-only flags are verified. GitHub Actions
checks and anonymous public artifact downloads are verified separately. One release
draft is reused by numeric ID, and older releases/assets remain intact.

Graphics collection 1.8 is unchanged: 189 images and 189 previews, individually
pinned to their immutable release resources. No new image or complete texture
ZIP is published. The optional information archive snapshots the released
documentation, scientific reference tables, translations and graphics metadata.
