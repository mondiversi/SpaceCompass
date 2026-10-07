# Curved celestial-reference labels

Equator, tropic and polar-circle names now follow their actual projected lines,
as do their declination values and sparse numeric repeats. Pole, zenith and
Earth-centre point captions retain their horizontal boxes beside the crosses.
The same renderer is used by virtual/camera live views and panorama/photo captures.
No reference vectors, celestial calculations, coordinates, defaults or catalog
selection behavior changed.

The curve captions use the same whole-string Canvas text-on-path painter as orbit
names. Native shaping, offset-baseline arc-length advances, upright direction,
outline and per-reference colors are preserved. Text sits 12 scaled pixels from
the line. Glyphs are not positioned by individual character rotation or crowded
onto the unshifted path. Existing checks reject tight bends and compressed ends.

Candidate baselines remain on a single actual visible projected run; clipped and
wrapped fragments are never connected to make space for a word. Their bounds
respect scene edges, the capture caption and reserved objects/controls/axes.
Point captions get priority. Curve names try a bounded set of actual anchors at
24/22/20 scaled font sizes, then sparse degree repeats reserve separate positions.
An illegible/no-space caption is omitted rather than forced over other text.
The live layer paints guide geometry behind bodies and caption text exactly once
in front; panoramas/photos defer the shaped caption pass until after orbit paths.

Existing translated resources and number-format preferences are reused. Export
snapshots still determine selected versus international language/format profile.
The guide switch controls all reference geometry; the capture Labels switch hides
all curve/point captions while retaining guide lines and cross markers.

## Validation

Both debug/release compilation and Android-test source compilation pass. All 669
JVM tests pass, including six new layout regressions for alternative free anchors,
header/edge/collision exclusion, readable reversed/vertical runs, curved glyph
span, disconnected fragments, all five circles at both hemispheres/all four
panorama centers, and camera lens/roll fragments. Existing orbit text regressions
also pass. No connected instrumentation was executed. All twenty language catalogs
retain their 447 matching translated keys; no new string resources were added.

Manual Samsung SM-G970U1 / Android 12 portrait checks verified curved Italian
Capricorn text in the live virtual view, colored equator/tropics/Antarctic captions
and degrees in the panorama, horizontal zenith/south-pole point captions, Labels
off/on suppression/restoration, and curved Capricorn text plus a horizontal
Earth-centre label in a real-camera photograph. The main view was restored with
camera disabled and previous checked objects, active body and guide/label choices.
No gallery save, document write, sharing, data clearing or GitHub publication.
Full evidence/scope are in the local curved-guides-validation.json artifact.
