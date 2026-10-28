# Space Compass 1.5.3 release review

Application `me.mondiversi.spacecompass`, version/display **1.5.3**, code **53**, minimum API 26.
Updates retain the existing distribution certificate, preferences, selections and verified caches.

## Scope

Direct Scenario management and seven independent parameter screens. Effective values remain in
the menu; activation checkboxes stay on the trailing side of child toolbars, while inactive fields
remain visible in grey. Descriptions use the shared settings typography in all twenty languages.

Scenario export always opens. Its optional automatic-values checkbox resets off for each export.
The dialog order is Name, checkbox/description, divider, Data format. Save/Share reject an empty
payload. Complete copies capture one effective position/altitude/zone/instant without mutating the
observer, keep speed/resumption choices, validate missing or unsupported data, and use the existing
canonical version-1 format. Files do not accrue elapsed runtime while archived.

Four persisted paired catalogue filters, selected-object pinning, current-scenario availability,
compact outlined field captions and one day/night category palette for rows and Type choices.
Scientific calculations, catalogue entries and graphics resources remain unchanged.

## Distribution

The signed APK, signed update index, optional reference-information ZIP, release metadata and
checksums are the five application-release assets. The immutable graphics collection remains
`celestial-textures-v1.7`: 184 full WebP images and 184 independent 128×128 previews, quality 90,
method 6, with individual lengths, dimensions and SHA-256 pins. Its logical collection is
43,735,950 bytes including metadata. No new graphics release or complete texture ZIP is needed.

## Verification

Check source publication hygiene, catalogue/thumbnail reproducibility, graphics pins, twenty-language
key and placeholder parity, updated news, debug/release builds, JVM tests, Android-test source
compilation and release lint. Verify the retained certificate, non-debuggable/test-only flags,
16-KiB alignment and exact source revision embedded in the APK. Check the same-key phone update,
hosted checks, public source tree and anonymous release downloads. Connected instrumentation is
not executed without separate authorization; record those categories independently in metadata.
