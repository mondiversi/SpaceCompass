# Space Compass 1.8.8 validation

Version 1.8.8 uses Android version code 74 and the existing distribution certificate.

## Behaviour

- Add remembered virtual-sky zoom from 0.5× to 1× in six steps, defaulting to 1×. Use one perspective for objects, paths, stars, terrain, targets and guide labels.
- Keep camera zoom separate, starting at 0.5× or the widest available lens and respecting device limits and saved choices. Pair zoom with weather in the virtual view and object transparency in camera mode.
- Refine control placement: axis beside the chevron in portrait and compact landscape, below Menu in expanded landscape; selected points above Capture in portrait, beside it in compact landscape and at the sky column's lower-right edge in expanded landscape.
- Default new installations to the phone-length pointing axis (0°); retain existing axis preferences and the real camera's optical axis.
- Let the screensaver Earth be dragged, holding the chosen view until ten idle seconds pass and returning smoothly to the current rotation phase and seasonal orientation. Keep geographic sunlight and the real-position marker aligned in WebGL and the offline fallback.
- Require a double tap anywhere to leave the screensaver. Retain tracking suspension, music and low-rate idle rendering; update its return hint and zoom tutorial in all twenty languages.

## Verification scope

The release workflow builds debug and signed release APKs, runs 1,709 JVM tests,
compiles Android instrumentation sources, checks twenty-language key/placeholder
parity, reproduces generated catalogue and thumbnail resources and runs release lint.
The bundled screensaver controller has 65 deterministic checks for idle deadlines,
smooth phase recovery, repeated input, drag direction and geographic lighting/marker
alignment. Local browser checks covered portrait/landscape in WebGL and software,
with no external requests. Prior native checks covered zoom persistence after a cold
start, camera-mode slots, expanded/compact landscape placement and screensaver gestures.

Instrumentation compilation is distinct from execution; no connected instrumentation
execution is claimed. Final version, news, installed APK identity, hosted CI, public
source and artifact checks are recorded separately in release metadata.

## Distribution

Code, APK, signed update index, optional information archive, metadata and checksums
refer to the same commit. Graphics collection 1.8 and its individual WebP downloads
remain unchanged; no new graphics release or complete texture ZIP is required.
