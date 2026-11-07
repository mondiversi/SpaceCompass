# Space Compass 1.8.6 validation

Version 1.8.6 uses Android version code 72 and the existing distribution certificate.

## Final behaviour

The symmetric time scale includes pause and runs from −86,400× to +86,400×.
Supported legacy archive rates are normalized toward a slower magnitude without
changing direction. Re-enabling Time restores its last configured rate.

Location editing holds the simulated clock and defers applying changes until the
main sky is shown again. The prepared instant survives process restoration. Future
local dates are shown with a running clock and UTC offset above the sky, using theme
orange with a thin grey text outline; hidden controls do not hide the date.

Crossing a local date boundary clears live-track point selections, including when
running backwards. Fixed dated comparison selections are retained. Scene loading
establishes its date baseline before restoring the incoming selections.

Main-sky point bubbles share azimuth/elevation icons and middle-dot separation.
Photo and panorama clock/T fields are drawn on opposite edges without a visible
separator; the internal label boundary remains compatible with existing data.

## Verification scope

The release workflow builds debug and signed release APKs, runs at least 1,699 JVM
tests, compiles Android instrumentation sources, checks twenty-language keys and
placeholders, reproduces generated catalogue resources and runs release lint.
Targeted JVM coverage includes clock migration/holding, deferred editing, future
date display, live-selection rollover, scene import and capture clock columns.
Android-test compilation is distinct from execution: no connected instrumentation
execution is claimed. Final installed APK identity, launch, version and updated
news are recorded separately in release metadata, together with hosted CI results.

## Distribution

Source, APK, signed update index, optional information archive, validation metadata
and checksums refer to the same commit. News is updated in all twenty languages.
Graphics collection 1.8, its individual WebP downloads and all integrity pins are
unchanged; no new graphics release or complete texture ZIP is needed.
