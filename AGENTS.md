# Space Compass development

- This is an independent Android application, not a UVIR module. Work inside this
  repository. Do not edit the former UVIR repository unless the user explicitly asks.
- Speak Italian with the user. Keep repository documentation, release notes,
  code comments and future GitHub metadata in English.
- Preserve current celestial calculations, phase rendering, trajectories,
  selection, accessible touch targets, RTL and phone/tablet/landscape layouts.
- Keep app data, package and release workflow independent from UVIR. The owner
  intentionally retains the same distribution key for both apps, outside Git in
  `~/Dropbox/Mondiversi/UvirSigning`. Resolve it with
  `java scripts/SignUpdateIndex.java --keystore-path`; see `docs/UPDATES.md` for
  the shared private `signing.properties` and optional overrides. Release APKs
  and update indices use that configuration automatically on a new trusted PC.
  Never generate a replacement key for an update
  or import UVIR sensor credentials, pairing services or measurement databases.
- Use `gradlew.bat :app:assembleDebug :app:testDebugUnitTest
  :app:compileDebugAndroidTestKotlin` on Windows. Connected instrumentation needs
  explicit device authorization; never uninstall or clear app data to fix an install.
- Report compilation, JVM tests, instrumentation compilation and real-device checks
  separately. Do not describe unexecuted UI tests as passing.
- Preserve image/scientific credits, the GPL license and SGP4 license/notice.
- Distribute all new or replaced planetary and other celestial surface/cloud maps
  as WebP. The reviewed current baseline is lossy WebP quality 90, method 6,
  at 2048 x 1024. Preserve attribution and orientation; update verified sizes,
  dimensions and SHA-256 pins if a derivative changes. Keep existing legacy
  release assets/URLs needed by older APKs; compatibility copies are not the
  format to use for new maps.
- Do not weaken HTTPS certificate/hostname validation. The additional public CA is
  restricted to the exact JPL host and must not become a global trust override.
- No GitHub remote, publication, store release or production signing is authorized
  merely by routine local development.
- Upload code or releases to GitHub only when the user explicitly requests that
  publication. Keep routine fixes, builds and device installations local.
- For distributions after 1.4.1, do not create or upload complete celestial texture
  ZIPs to either the application or graphics release. Publish individual WebP files,
  their manifest, credits and checksums in the versioned graphics release. Preserve
  existing published archives and URLs. This does not remove information archives.
- Reuse a release draft by its persisted numeric ID or by searching the authenticated
  releases list, including drafts. A tag-only lookup can omit drafts. Check for
  duplicate drafts before creating a release and after publishing it; never create
  a second release when the intended draft already exists.
- Keep future settings descriptions on the shared `SpaceCompassSettingsDescription`
  style. Match the reviewed UVIR typography, margins and indentation; do not
  introduce one-off font sizes or spacing.
