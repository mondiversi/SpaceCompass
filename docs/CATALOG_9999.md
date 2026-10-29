# 9,999-object catalogue — Space Compass 1.7.0

This expansion adds 8,990 identities to the released 1,009-object catalogue. It ships in Space Compass 1.7.0 with versioned external graphics collection 1.8.

| Primary family | Total |
|---|---:|
| PLANET | 7 |
| DWARF_PLANET | 100 |
| NATURAL_SATELLITE | 200 |
| ASTEROID | 1000 |
| COMET | 300 |
| STAR | 3061 |
| EXOPLANET | 1500 |
| BLACK_HOLE | 120 |
| NEUTRON_STAR | 250 |
| GALAXY | 1600 |
| NEBULA | 900 |
| STAR_CLUSTER | 700 |
| CONSTELLATION | 88 |
| SPACE_PROBE | 60 |
| ARTIFICIAL_SATELLITE | 100 |
| SPACE_STATION | 2 |
| SCIENCE_FICTION | 11 |

The 100 dwarf-planet entries include explicitly labelled trans-Neptunian candidates; this is not a claim that the IAU recognizes 100 dwarf planets. All 88 constellation patterns and the eleven existing fictional references remain distinct and qualified. Overlapping physical/fiction filters do not increase the unique total.

## Scientific models

- Bright Hipparcos stars: HYG v4.1 ICRS/J2000 coordinates, tangent-plane proper motion, available distance, spectral class and visual magnitude. The unknown-distance sentinel is rejected.
- Confirmed exoplanets: NASA Exoplanet Archive `pscomppars`; point at the unresolved host. Mass/radius limits are not treated as detections; equilibrium temperature is labelled as a model.
- Extended objects: OpenNGC v20260501; additional Lynds dark clouds and pulsars use SIMBAD astrometry. Unknown distances remain unknown, rather than becoming one AU.
- Nuclear black holes: the author's van den Bosch (2016) compilation, with measured masses, distance and nuclear direction. Upper limits and globular-cluster rows are rejected. Host galaxy and nucleus are separate objects, and both can be found.
- Solar-system bodies and probes: individually identified JPL Horizons targets. Apparent Earth-centred ICRF states use UTC and light-time/aberration correction; observer parallax is applied in the app. Motion uses the stated primary, separately from apparent pointing. No extrapolation beyond delivered samples. Historical missions may have no present-day trajectory.
- Artificial satellites: unique NORAD/COSPAR identity, validated TLE checksums, inclination/mean-motion/radius/speed envelopes, SGP4 propagation and freshness checks. Dead but still orbiting satellites are not represented as operational missions.

## Compatibility and scale

`catalogue/body-identities.json` is append-only. The 1,010 released identifiers (including the retired Earth-centre ID) retain their exact names and ordinals. The registry replaces the JVM enum initializer, whose method-size limit cannot safely hold ten thousand objects. Preferences and portable Location files continue to use stable names. The retired ID stays outside the 9,999 selectable objects.

Generated identity/data chunks remain bounded. Catalogue batches share one Earth state, observer origin and horizontal rotation; constellation patterns retain their individual stars. Background calculations are cancellable. Search normalization is indexed per locale. Lazy rows preserve selected-first grouping, favorites and filter semantics. Only selected/observed/visible remote targets are warmed automatically; each keeps its independent offline cache and retry policy. Merely opening the app does not request thousands of unseen targets.

The portable Location format remains version 1. Its bounded input/output limit is now 1 MiB so a scene containing all 9,999 IDs can round-trip with its active object. Older small files remain valid. Very large all-object files can exceed the former version's 128 KiB input limit.

Texts and twenty locale resources remain in the APK. Proper scientific identifiers stay international; shared classifications, limitations and translated proper names use the existing language system. Graphics are separate: pack 1.8 is published with 1.7.0, inherits all released bytes and exact URLs, and adds five reviewed photographs plus previews. Missing photographs use the existing symbolic renderer. New resources use individually pinned versioned URLs; older APK resource URLs are preserved.

## Reproduction and verification

Run `scripts/GenerateBodyIdentities.py`, `scripts/GenerateExpandedCatalog.py`, and `scripts/GenerateExpandedCatalogResources.py` (all accept `--check`). Canonical records preserve per-object source URLs; `sources-9999.json` pins downloaded source extracts. JPL validation records and compressed test-only vectors document actual target identity, dates and samples. All new satellites have test-only TLE fixtures. Neither research fixtures nor graphics are copied into application assets.

## Verified source snapshot

All 1,414 added JPL targets were individually validated with 49 hourly apparent vectors. Eighteen require historical dates; 1,396 cover the 28–30 October 2026 verification window. All 75 added NORAD satellites have separately validated TLE fixtures. Exact windows and response hashes are in `catalogue/horizons-validation-9999.json`; compressed vectors are test resources only. Persistent galaxy/nucleus coincidences and unresolved planets at the same explicitly identified host are excluded from close-approach events.

## Release verification

See [the 1.7.0 release audit](RELEASE_1_7_0.md) for build, localization, device and distribution checks.
