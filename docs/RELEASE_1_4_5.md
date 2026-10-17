# Space Compass 1.4.5 maintenance release

Application `me.mondiversi.spacecompass`, version name/display 1.4.5, code 42,
minimum Android API 26. The package and distribution certificate are unchanged.

## Scope

The scenario Time slider supports twelve rates in each direction, from −3600×
to +3600×, with a paused 0× exactly in the centre. Standard slider semantics,
discrete positions and physical direction are retained across RTL languages.
The readout separates direction, signed multiplier and simulated duration per
real second. Re-enabling Time starts at +1×; direction/rate edits preserve the
reached instant. Midnight can move into the previous or next civil day.

Restart, active-only and always resumption remain independent. Timeline bounds
and provider validity checks remain authoritative; unavailable ephemerides or
weather are never invented. Accelerated consumers refresh their clock identity
when changing rate or direction. Captures keep their frozen observation time.

All twenty in-app news files and existing credits remain unchanged from 1.4.4.
BuildConfig supplies the new version in Info & credits. Graphics pack 1.5 is
reused unchanged, including 56 full WebP images and 56 small previews. Resources
remain external, individually downloaded and verified; no texture ZIP is added.

## Verification and distribution

Release checks cover debug/release compilation, 1,222 JVM tests, Android-test
source compilation, release lint, translation keys/placeholders in twenty
languages, unchanged news, APK signing identity, 16 KiB alignment and the signed
update payload. Connected instrumentation is not executed. Real-phone checks
cover central pause, reverse/forward equivalence and rotation; temporary Time
and rotation settings are restored. Install updates without clearing app data.

The release contains the signed APK, signed update index, reference-information
archive, release metadata and SHA256SUMS.txt. The information archive is a
scientific/localization reference snapshot, not a runtime download or image pack.
Final hashes, test counts, hosted checks and installation status accompany the
release in its metadata. Previous releases and graphics URLs are preserved.
