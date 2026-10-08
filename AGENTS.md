# Space Compass development

- This is an independent Android application, not a UVIR module. Work inside this
  repository. Do not edit the former UVIR repository unless the user explicitly asks.
- Speak Italian with the user. Keep repository documentation, release notes,
  code comments and future GitHub metadata in English.
- Preserve current celestial calculations, phase rendering, trajectories,
  selection, accessible touch targets, RTL and phone/tablet/landscape layouts.
- Keep app data/package/signing distinct from UVIR. Never import sensor credentials,
  pairing services or measurement databases as dependencies.
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
