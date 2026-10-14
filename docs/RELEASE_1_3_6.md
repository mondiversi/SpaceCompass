# Space Compass 1.3.6 release

Release dated 2026-10-14. Android/update version 1.3.6, version code 34,
application ID `me.mondiversi.spacecompass`, minimum Android API 26.

## Capture label policy

KEY_SELECTED is displayed immediately below ALL. Its policy includes every captured
object's rise, culmination, minimum, set and current labels together with selected
points. Hourly points with coincident principal events remain eligible. Unselected
hourly captions and non-point annotations are omitted. Existing selected highlights,
legends, angular scales and location disclosure remain independent of label mode.

Panorama and camera renderers use shared path-point and current-point eligibility
methods. Image export variants include the mode's stable key in cache identity.
Fresh settings use KEY_SELECTED; explicit saved modes and legacy on/off choices
retain priority. The option is translated in all twenty interface languages.

## Silent in-app update and verification

All twenty info_news.xml files remain byte-for-byte unchanged from 1.3.5. The app's
version display advances through BuildConfig; previous news and credits remain.
Validation includes debug/release builds, 983 JVM tests, Android-test compilation,
release lint, twenty-language parity/encoding/placeholder checks, a public-source
audit and eighteen pinned WebP image hashes. Native phone checks are recorded
separately. The connected instrumentation suite is not run.

Prior native checks verify label ordering, saved choices and rendered panoramas
in portrait and landscape. Final signed-APK checks verify version and unchanged
in-app news. The same distribution certificate and 16 KiB native alignment are
retained. The signed update index pins package, version, size and SHA-256; the
optional information archive reflects the tagged source. Texture pack 1.1 and
all earlier release assets remain intact.
