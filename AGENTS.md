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
- Do not weaken HTTPS certificate/hostname validation. The additional public CA is
  restricted to the exact JPL host and must not become a global trust override.
- No GitHub remote, publication, store release or production signing is authorized
  merely by routine local development.
