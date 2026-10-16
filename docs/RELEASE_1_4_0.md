# Space Compass 1.4.0 release

Release dated 2026-10-16. Android/update version 1.4.0, code 37, package
`me.mondiversi.spacecompass`, minimum Android API 26. The distribution certificate
is unchanged; install over the current app without clearing data.

- Navigate a steadier sky with adjustable Low, Balanced and High fluidity. Balanced is now the default; saved choices are retained. Gyroscope-assisted stabilization reduces stationary and handheld jitter without freezing real motion or changing geographic north.
- Send future orbital points to Android Clock or Calendar from the focused sky callout and daily path. The Events setting can hide the main-screen alert button; Android owns reminder confirmation and scheduling.
- Add Tesla Roadster (2018), with guarded JPL heliocentric ephemerides, reference facts, aliases and a downloadable 2K WebP archival photograph. It is a heliocentric payload, not an Earth satellite.
- Highlight the current daily-path row consistently, retain lunar-phase icons and key-event emphasis, and keep unavailable properties explicit.
- Remove an object directly from the main card after confirmation. Improve object-name space, optical arrow alignment and matching full palette colors in names and photo/panorama legends.
- Keep focused point callouts above celestial icons without the repeated above/below jump; center the alert button vertically beside the callout.
- Refine shared icons, confirmation buttons, export file headings and the selected-object label option. Hide the unavailable Display section while retaining its implementation.
- Refresh all twenty in-app news summaries. Preserve settings, selected objects/points, scenarios, scientific calculations and the existing distribution certificate.

## Verification and distribution

Required gates: debug/release builds, JVM tests, Android instrumentation compilation,
release lint, all twenty language catalogs with key/placeholder parity, source
publication review, 19 pinned WebP images, signature/non-debuggable manifest and
16 KiB native alignment. Native phone checks are reported separately; connected
instrumentation is not executed. No real alarm/calendar entry is created by testing.

The release metadata records exact results, APK size/hash, archives and source commit.
The signed index pins this APK and code 37. Optional information and graphics ZIPs
carry per-file hashes and matching source identifiers. The 19-image archive adds
the previously authorized Roadster asset; original individual maps and old archives
remain unchanged. Runtime downloads stay individual and hash-verified.

News summaries are updated in all twenty languages. Scientific/image credits and
licenses are preserved. Observer coordinates, private UI captures, caches, signing
files and local machine configuration are excluded from publication.
