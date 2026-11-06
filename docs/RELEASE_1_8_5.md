# Space Compass 1.8.5 validation

Version 1.8.5 uses Android version code 71 and the existing distribution certificate.

## Final behaviour

The object island has no removal button. Its arrows have equal visible edge margins,
equal caption insets and unchanged touch slots. The original compass island and its
GPS-details link are retained. Compact portrait uses the existing 40% reticle position
from the top; expanded portrait and landscape retain their centred optical axis.
Only Scenario and Location override the shared export heading. UI text and accessible
labels use consistent loading terminology in all twenty languages. Archive schemas,
extensions, selection behaviour and celestial calculations are unchanged.

Music no longer handles headphone-route removal as permanent audio-focus loss.
Android routes media back to the available output. Background, mute, disabled-music
and genuine focus-loss policies still apply. Physical headphone disconnection could
not be operated remotely and is not claimed as a completed hardware test.

## Verification scope

The workflow builds debug and signed release APKs, runs JVM tests, compiles Android
instrumentation sources, checks twenty-language key/placeholder parity and runs release
lint. Android-test compilation is distinct from execution. Local trial checks on a
Samsung Galaxy S10e verified the object arrows, removal-button absence, restored GPS
link, compact reticle, dedicated export headings and loading accessibility label.
These implementation checks preceded the version/news-only bump. No connected
instrumentation rerun is claimed. Final version, news and installed-APK identity are
recorded separately in release metadata, alongside hosted CI and artifact outcomes.

## Distribution

The APK, signed update index, optional information archive, validation metadata and
checksums refer to the same source commit. News is updated in all twenty languages.
Graphics collection 1.8 and its individual WebP downloads and integrity pins are
unchanged; no new graphics release or complete texture ZIP is needed.
