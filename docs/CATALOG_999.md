# 999-object catalogue

999 selectable entries: 894 additions and all 105 previous identities.
The retired Earth-centre identifier remains readable for compatibility but is not selectable.
Default selection remains Sun and Moon. No hidden title hold or reveal state is required.

## Families

7 planets; 30 dwarf planets/candidates; 80 natural satellites plus the fictional LV-426;
70 asteroids; 40 comets/interstellar objects; 250 stars; 60 exoplanets; 30 black holes/compact
candidates; 30 neutron stars/pulsars; 100 galaxies; 90 nebulae; 70 clusters; all 88 constellations;
26 probes; 25 artificial satellites; 2 stations.

## Implementation and limits

The enum is append-only, preserving prior preference identifiers and colours. A lazily initialized
typed registry uses O(1) lookups and generated chunks; no remote descriptive-data dependency exists.
Sources: [catalogue provenance](../catalogue/README.md). Reproduce generated sources/resources with
`scripts/GenerateExpandedCatalog.py --check` and `scripts/GenerateExpandedCatalogResources.py --check`.

- Catalogue coordinates include proper motion, precession/nutation, annual aberration, observer location,
  sidereal time and parallax where a distance is adopted. Direction-only records do not acquire a fictitious
  one-AU distance; they show missing distances and sort those values last. They remain trackable offline.
- Exoplanets trace their unresolved host direction. A galaxy or nebula traces its adopted centre, not an
  invented resolved component. Constellations project every linked star, not one generic icon in the sky.
- Historical missions only track within Horizons' actual coverage. TLE objects require fresh elements
  and cannot be projected arbitrarily far into a past/future scenario. Unknown parameters stay absent.
- Only checked objects participate in approach calculations. A unit-vector spatial grid reduces the live
  pair scan; precise separations remain authoritative. Galactic centres and their own nuclei do not create
  fake conjunctions. Distant fixed pairs get a conservative screening margin before daily refinement.
- Catalogue rows are lazy and science computations run off the UI thread. Network requests are serial,
  cached and selected targets pre-empt background warming. Selecting hundreds of simultaneous paths still
  entails real computation and visual crowding, especially on older devices; no tracking results are invented
  or silently truncated to make that case seem instantaneous.

## Graphics, stable pack 1.6

174 full WebP resources plus 174 independent 128×128 previews. Full images total 38,001,332 bytes;
previews total 749,914 bytes (38,751,246 bytes together, before manifests/credits). All are WebP
quality 90, method 6. Published image bytes are retained. Most full images are 2048×1024;
LV-426 retains its earlier 1774×887 source. 118 new photographic resources were accepted after
identity/credit and visual review. Others use code-rendered symbols rather than invented photographs.

Names: `{category}_{stable_catalogue_identifier}.webp`; thumbnail: `thumb_{identical_name}.webp`.
See [naming](../textures/celestial-textures-v1.6/NAMING.md) and
[credits/processing](../textures/celestial-textures-v1.6/CREDITS.md).
No texture ZIP and no celestial photos in the APK. Previews and full maps download individually on demand.
SHA-256, byte-length and dimension pins remain independent. A strict alias map reuses verified old caches.

Pack 1.6 is distributed as stable individual assets. Application 1.5.0 pins every canonical full image
and preview to that exact release. Older published releases/URLs and verified cache aliases remain intact.
The application release follows public verification of every graphics asset. No complete texture ZIP is distributed.
