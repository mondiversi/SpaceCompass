# Space Compass 1.1.0 release audit

Release identity: **1.1.0**, version code **17**, package
`me.mondiversi.spacecompass`, tag `v1.1.0`. Android 8.0/API 26 or newer.
Distribution retains the owner's authorized certificate and existing app data.

## Code and asset changes

Camera Compose binding and Camera2 lifecycle/session ownership are now separate:
`SpaceCompassCameraPreview` is 58 lines and the controller is 375 lines. Camera
options are memoized rather than rebuilt on orientation recomposition. Projection,
zoom policy, device discovery, exposure matching and rendering remain independent.
The largest production Kotlin file is the 541-line screen orchestrator; computations
and network IO remain in their existing bounded producers and caches.

Preview decoding retains bitmap ownership across cancellation and handles decode
failures without losing intermediate images. GPU texture upload/resize recycles
intermediates even on failure. The view-mode thumb animation reads its offset in
placement, avoiding composition on each animation frame.

Bundled assets shrink by **4,450,604 bytes**. Pluto is packaged at the existing
2048 x 1024 renderer limit (597,288 bytes, previously 3,972,946), using quality-95
4:4:4 JPEG. Its measured PSNR against the resized reference is 44.95 dB. The
fictional texture uses lossless WebP (1,901,236 bytes, previously 2,976,182); its
1774 x 887 decoded RGB pixels are identical. Asset credits, coordinate conventions
and all twenty locale catalogs are retained. No duplicate asset is bundled.
Release builds keep R8 code and resource optimization without broad keep rules.

## Localization

All **451 translatable keys** occur exactly once in each of **20 catalogs**.
The resource audit and JVM localization tests check missing/extra keys, empty
values, duplicate keys, format arguments and UTF-8 integrity. All six 1.1.0 news
items are translated and grouped in each `info_news.xml`. Hardcoded interface
text was inspected: remaining literals are brand/credits, scientific identifiers
or input-format examples. Identical short text was reviewed as proper names,
scientific symbols or valid shared terms; no long English sentence is copied into
another language. This is not independent native-speaker proofreading.

## Completed local checks

- Debug and optimized release builds passed.
- **698 JVM tests** passed, with zero failures, errors or skips.
- Android test sources compiled. Connected instrumentation was **not run**.
- Release lint passed with **zero errors**. Remaining warnings are advisory:
  upstream SGP4 formatting, dependency update notices, programmatic view/layout
  patterns, typography, unused development resources and compatible manifest
  conventions. No global suppression or error baseline is added.
- Current files and 2,290 historical text blobs were scanned for private signing
  material, credentials and private/local filenames; no findings.

Existing scoped phone checks cover zoom/pinch/lens handoff, photo field of view,
user-format previews versus export profiles, selection panels, compact controls,
mode switching and orientation. The final signed release installation, hosted CI
and public-download integrity checks are recorded separately during publication.
The release process rebuilds the APK from its exact source commit, verifies its
signature/version/16 KiB alignment and uses a locally signed update index.
Only the signed APK, update index and SHA256SUMS.txt are release download assets.

See [camera checks](CAMERA_VIEW.md), [catalog search](CATALOG_SEARCH.md) and
[distribution](GITHUB_RELEASES.md). Previous release audits remain historical.
