# Space Compass 1.4.8 release review

Application `me.mondiversi.spacecompass`, version name/display **1.4.8**, Android
version code **45**, minimum API 26. Update with the existing distribution key
and retain app-private settings, selections and caches.

## Export interface

- Photo and panorama file choices appear as Image (JPG), Readable table (TXT),
  then Spreadsheet (CSV). The generated batch follows the same relative order.
- Daily paths put Readable table before Spreadsheet. The Observatory keeps
  Current image and Rotation image first, followed by the same TXT/CSV order.
- The CSV option is called Spreadsheet in all twenty supported languages.
  Existing file keys, extensions, MIME types, CSV schemas, captured times,
  selected/key events and scientific values are preserved.
- Formatting profiles, location disclosure, initial unchecked file choices,
  save/share delivery and returning to the retained source view are unchanged.
- Info & credits shows three release-specific news bullets in all twenty
  languages and the new version. Existing scientific/image credits are retained.

## Packaging and checks

Release verification covers debug/release compilation, JVM tests, Android-test
source compilation, release lint, localization key/placeholder parity, APK
signature/package/version, 16 KiB alignment and the embedded source revision.
Exact results and available physical-phone checks are recorded in release
metadata. Connected instrumentation is not run as part of this release.

The release includes the signed APK, signed update index, optional reference
information archive, release metadata and SHA-256 checksums. Source/tag identity,
hosted checks and anonymous asset downloads are checked before completion.
The information ZIP is not a runtime texture download.

Graphics pack **1.5** remains unchanged: 56 external full WebP images and 56
independent previews, downloaded individually with pinned integrity and offline
caches. Earlier release URLs remain available; no complete texture ZIP or new
graphics release is needed. No celestial photographs are added to the APK.
