# Space Compass 1.5.0 release review

Application `me.mondiversi.spacecompass`, version name/display **1.5.0**, code **50**, minimum API 26.
Install over the existing app using the same distribution certificate; no data reset or uninstall is needed.

## Catalogue and graphics

999 selectable entries include all 88 constellations.
Descriptions, names, source metadata and twenty-language resources stay in the APK.
See [sources and scientific limits](CATALOG_999.md) and [canonical provenance](../catalogue/README.md).
Unmeasured distances, uncertain masses and expired/unsupported ephemerides are not fabricated.
Historical missions retain real coverage windows; exoplanets trace the unresolved host direction.
Constellations render linked stars rather than an arbitrarily rotated generic cross.

Stable graphics pack 1.6 contains 174 full WebP files and 174 128×128 previews, quality 90/method 6.
Full maps total 38,001,332 bytes; previews 749,914 bytes. Most maps are 2048×1024; existing illustrations retain their original source dimensions.
The APK pins every individual resource to that release, preserving old bytes, aliases, caches and URLs.
No complete graphics ZIP is distributed. The optional information ZIP is a reference snapshot, not a runtime package.

## Interface

The panorama icon has a central crest, prominent points and dashed horizon. Shutter blades form a closed aperture.
Object-island action gaps are slightly wider. Observatory and Daily path circles use a light neutral fill and dark
glyphs at night; daytime styling, geometry and touch targets remain intact. Info explains terrestrial orientation
references and the limits of ordinary smartphones in space, in all twenty languages. Three news summaries are refreshed.

## Verification

Release validation covers reproducible catalogue resources/previews, translation key/placeholder parity,
graphics checksums and pins, debug/release builds, JVM tests, Android-test source compilation and release lint.
Signed APK validation checks package/version, certificate, non-debuggable/test-only flags, 16 KiB alignment
and the exact embedded source revision. Device checks, public downloads, source archive comparison and hosted
Android checks are recorded separately in release metadata. Connected UI instrumentation is not executed.
The graphics release is verified before the matching APK is published; duplicate drafts and old assets are checked.
