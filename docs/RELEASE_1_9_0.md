# Space Compass 1.9.0 validation

Version 1.9.0 uses Android version code 76 and the existing distribution certificate.

## Behaviour

- Add interactive Globe mode alongside Panorama and Camera, with Earth-centred satellite tracks and apparent directions for distant objects.
- Add axis-locked/free rotation, saved zoom, current-object images, point selection, Antipodes and theme-aware space backgrounds.
- Let live Globe labels follow the active island object. Offer four main-view scopes and all five capture scopes, with the main choice inherited at capture.
- Generate Globe images with frozen geometry, star backgrounds, rear-side labels, privacy-aware location pins and the existing image/table exports.
- Add a saved live-camera angular grid; refine controls, optical alignment and gesture exclusion margins.
- Extend positive and negative simulated time to fifteen days per second; refine Earth screensaver lighting and ocean visibility.
- Update the tutorial and twenty-language News; expand the English portrait/landscape README gallery without personal location data.

## Verification scope

The release builds debug and signed APKs, runs at least 1,738 JVM tests, compiles
instrumentation sources, runs release lint and checks twenty-language key and
placeholder parity. Catalogue and thumbnail generation and resource integrity
are checked independently. Connected instrumentation is not executed.

Local native phone checks have covered Globe labels, both themes, captured stars,
active-island switching, tutorial menus and preserved settings. Release-specific
installation, News, gallery, hosted CI and public artifact checks are recorded in
release metadata. Gallery captures use an isolated emulator with public London
coordinates and English UI; no physical-phone data is copied.

## Distribution

Source, tagged commit, signed APK and update index are verified together. The
existing external graphics collection 1.8 remains immutable; Earth maps are reused
from the existing screensaver. No celestial images or complete texture archive are
added to the APK or application release.
