# Space Compass 1.4.9 release review

Application `me.mondiversi.spacecompass`, version name/display **1.4.9**, Android
version code **46**, minimum API 26. Update with the existing distribution key
and retain app-private settings, selections and caches.

## Object-page navigation

- The object name opens the Observatory without a text underline. The existing
  Observatory and Daily path buttons remain in the main information island.
- Each object page has one destination action after Export, using the standard
  title-bar target, margins, neutral theme colors and 2 dp stroke. No page switch
  track consumes the extra width. Long titles keep the existing fade/marquee.
- The destination glyphs use Export's actual painted dimensions (16.4 × 17.12 dp
  at a 2 dp stroke), including round caps, inside the normal icon viewport.
  Physical screenshot checks compare the monochrome glyph bounds with Export.
- A shared page host retains the inspected object, model saveable state and
  main-scene point selection while moving between pages. Back returns directly
  to the original main screen or object catalogue. Catalogue inspection does
  not add an unchecked body to the scene; unavailable paths remain disabled.
- Model rotation/current-view controls reuse the existing capsule geometry and
  accessible tab semantics independently of the new title-bar destination action.
- Three release-specific news bullets are updated in all twenty languages.
  Scientific values, exports, formatting, privacy choices and credits are retained.

## Packaging and verification

Release checks cover debug/release compilation, JVM tests, Android-test source
compilation, release lint, translation key/placeholder parity, APK identity,
signature, 16 KiB alignment and embedded source revision. Exact results and
available phone checks are recorded in release metadata. Connected instrumentation
is not executed for this release.

The release includes the signed APK, signed update index, optional reference
information archive, metadata and SHA-256 checksums. Source/tag identity, hosted
Android checks and anonymous downloads are verified. The information ZIP is a
reference snapshot and is not used by the running app.

Graphics pack **1.5** remains unchanged: 56 external WebP images and 56 individual
previews, with pinned integrity and offline caches. Previous release URLs remain
valid. No new graphics release, complete texture ZIP or bundled celestial
photograph is required.
