# Space Compass 1.4.9 release review

Application `me.mondiversi.spacecompass`, version name/display **1.4.9**, Android
version code **47**, minimum API 26. This refresh replaces the existing release's
files, using the same distribution key, release ID and download URLs. Code 47
makes it detectable by the updater for original 1.4.9/code-46 installations.
Settings, selections and offline caches are retained on installation.

## Catalogue and object navigation

- Checked objects appear first in the selected sort order. Unchecked matches
  retain that same sorting in their following group. Missing sort values remain
  last within each group, stable ties retain catalogue order, and hidden objects
  are available only while explicitly revealed/checked.
- Checked objects bypass type, horizon and search exclusions. Existing filters
  remain unchanged; an unchecked excluded object disappears again. Select all
  operates on the displayed list without discovering hidden objects.
- Tapping the object name in the main island opens the catalogue. The removal X
  and confirmation flow are removed, reclaiming name space. Observatory and Daily
  path island actions remain available.
- Both object-page title bars retain Export without reciprocal navigation
  shortcuts. Back returns directly to the entry screen. Catalogue-only inspection
  does not select an object. Observatory inspection no longer calculates an unused
  daily path solely to support the removed shortcut.
- Model controls, the current-view/rotation capsule, selected path points,
  science, exports, privacy choices and credits are preserved.
- Three concise in-app news items are updated in all twenty interface languages.

## Packaging and verification

Checks cover debug/release compilation, JVM tests, Android-test source compilation,
release lint, translation key/placeholder parity, APK identity, signing certificate,
16 KiB alignment and embedded source revision. Available physical-device checks
and hosted Android checks are recorded separately in release metadata. Connected
instrumentation is not executed.

The existing release's signed APK, signed update index, optional reference
information ZIP, metadata and checksums are replaced only after local verification.
Original assets are backed up before replacement. Source/tag identity and anonymous
downloads are verified. There is no new release or duplicate draft.

Graphics pack **1.5** remains unchanged: 56 external WebP images and 56 previews,
with pinned integrity, individual downloads and offline caches. Prior releases
and resource URLs remain valid. No graphics ZIP or new bundled celestial photo
is required. The information ZIP is a reference snapshot, not a runtime package.
