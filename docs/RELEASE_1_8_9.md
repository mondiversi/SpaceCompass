# Space Compass 1.8.9 validation

Version 1.8.9 uses Android version code 75 and the existing distribution certificate.

## Behaviour

- Clarify the zoom tutorial: two-finger zoom works in both the simulated sky and the real camera view. Remove the unnecessary explanation about separate saved choices.
- Keep the disabled 0°/90° control highlighted and explainable in the camera tutorial without activating the real camera or changing the saved pointing axis.
- Keep the tutorial explanation clear of the axis control in landscape, including the collapsed-island layout.
- Place 0°/90° immediately beside the chevron when landscape islands are visible; keep it below Menu when the islands are collapsed. Apply the same layout to the tutorial.
- Update tutorial text and in-app News in all twenty languages; keep astronomical calculations, saved selections and the graphics collection unchanged.

## Verification scope

The release workflow builds debug and signed release APKs, runs 1,710 JVM tests,
compiles Android instrumentation sources, checks twenty-language key/placeholder
parity, reproduces generated catalogue and thumbnail resources and runs release lint.
The tutorial layout regression checks that the explanation leaves reading room
without covering an axis control below Menu. Previous local phone checks covered
visible/collapsed landscape islands, camera tutorial targets and both zoom explanations,
with real camera/axis preferences, selections and rotation retained.

Instrumentation compilation is distinct from execution; no connected instrumentation
execution is claimed. Final installed APK identity, version and News, hosted CI,
public source and artifact checks are recorded separately in release metadata.

## Distribution

Code, APK, signed update index, optional information archive, metadata and checksums
refer to the same commit. Graphics collection 1.8 and its individual WebP downloads
remain unchanged; no new graphics release or complete texture ZIP is required.
