# Space Compass 1.7.7 release

Release dated 2026-11-02. Android/display version 1.7.7, version code 63,
application ID `me.mondiversi.spacecompass`, minimum API 26. The existing
distribution certificate and data-preserving installation path are retained.

## Main-view interaction

The menu tutorial dims the scene and explains individual controls, including a
read-only camera demonstration. Its bounded, scrollable explanation card stays
between controls. Visual highlights follow compact icon/text bounds independently
of larger touch targets. Close with the menu’s X or Back; there is no triple-tap
exit or selection-clearing gesture. A welcome is shown once, including the first
start after updating an installation that has not seen it.

Empty-sky taps toggle the remembered reticle and hover information. Points and
controls retain their own actions. The GPS details link is clickable without
making all compass readings navigation targets. Capture labels are independent.

## Time windows and antipodes

The remembered neutral interval control switches between calendar-day (0–24,
default) and rolling next-24-hour (+24h) paths. Shared interval boundaries propagate
through ephemerides, satellite passes, Daily path, capture labels and table exports.
Snapshots freeze the capture’s time window rather than recomputing it during export.

Antipodes transform selected directions by azimuth +180° and inverted elevation.
They are derived viewing directions, not additional celestial bodies or physical
orbital positions. Tap the dedicated button to toggle them; hold to clear real
selections, with haptic feedback. Visibility starts off and resets when selections
become empty. Captures and their CSV/readable tables include visible antipodes;
Daily path and its exports exclude them. No alarms are attached to derived points.

Current objects have a single trajectory-coloured border, with the appropriate
above/below-horizon tint. Selection overrides it with the theme accent, including
offscreen indicators. The border remains legible when images are translucent.

## Verification and distribution

The release gate covers debug/release compilation, all 1,584 JVM tests, Android
test-source compilation, release lint, twenty-language key/placeholder parity,
reproducible generated catalogues/thumbnails, signature/alignment and the APK’s
embedded exact source commit. Connected instrumentation is not executed on the
personal phone. Installation and native phone checks are recorded separately in
release metadata, alongside hosted CI and anonymous public-download verification.

Graphics collection 1.8 remains unchanged: 189 external full images and 189
independent thumbnails. Individual download URLs, SHA-256 pins, credits, legacy
versions and offline caches remain compatible. No external celestial images are
embedded in the APK and no complete graphics ZIP is produced. The optional
information archive contains reference/localization material, excluding image
binaries, live caches and private device data. Existing public demonstration
screenshots are retained with their original capture provenance.
