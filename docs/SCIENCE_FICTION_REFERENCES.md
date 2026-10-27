# Fictional direction references

The optional Science fiction filter contains eleven existing/new catalogue entries. Ten entries
are added to the previous 999-object catalogue, for 1009 selectable identities. The old fictional
entry keeps its saved identity. Default checks remain Sun and Moon. All new enum identities are
appended after the measured catalogue to preserve saved selections and existing orbit colours.

## Overlapping type filters

Science fiction is an additional filter membership: its six planets also appear under
Planets, its three moons under Natural satellites, and its two ships under Spacecraft.
Selecting several types takes their union without duplicate rows or inflated result counts.
Each category count includes its members; All remains the 1009 unique selectable objects.
Search, horizon visibility, checked-object priority and saved filters retain their existing
behaviour. The primary scientific category remains Science fiction, so filter membership
cannot introduce measured distances or physical assumptions for fictional objects.

## Reference model

- Arrakis uses Canopus; Caladan uses Delta Pavonis; Giedi Prime uses 36 Ophiuchi B.
  The associations come from Frank Herbert's *Dune*, Terminology of the Imperium:
  https://andrejjh.github.io/duneToI.html
- The Alien moons, parent gas giant and Nostromo use the Zeta-2 Reticuli direction.
  This is a fictional setting reference, not spacecraft telemetry. The screenplay and
  licensed-book references are retained in catalogue/science-fiction.json.
- Pandora and Polyphemus use the unresolved Alpha Centauri direction; official descriptions:
  https://www.avatar.com/pandorapedia/pandora and https://www.avatar.com/pandorapedia/polyphemus
- Vulcan uses 40 Eridani A, the fictional association described by NASA:
  https://science.nasa.gov/universe/exoplanets/vulcan-may-not-be-real-but-spocks-home-system-is/
- Discovery One uses Jupiter's calculated direction as its destination reference in the
  1968 film, not a reconstructed spacecraft orbit. The original novel's Saturn destination
  is deliberately not mixed with the film continuity. Source: AFI film catalogue,
  https://catalog.afi.com/Catalog/moviedetails/23399

SIMBAD host-star identifiers, ICRS/J2000 coordinates, proper motions and query provenance
are retained in catalogue/science-fiction.json. Existing stellar and planetary calculations
produce the observer/date-dependent azimuth, elevation and daily path. A fictional body's
measured range remains unavailable. Presentation may now show an approximate distance
derived from the named real reference, explicitly marked with ≈ and a provenance note.
This estimated range is available in the catalogue, compass readout, Observatory, travel
comparison and Observatory exports; it never changes the measured-orbit calculation.
Unknown physical properties stay absent. These entries do not generate angular approach
events with their reference or other objects. They do remain selectable, searchable,
filterable and available in live/camera views, paths and exports. Details and exports
identify the universe, reference and illustrative limitations in all twenty languages.

## Graphics

Graphics pack 1.7 adds ten original AI-assisted illustrations and ten independent
previews. Full images: WebP quality 90/method 6 at 2048 x 1024. Previews: 128 x 128,
same quality/method. Planet/moon maps wrap onto illustrative globes; both spacecraft remain
flat images, with full silhouettes in their previews. Rings are illustrative, not measured.
The APK contains no new celestial image. Manifests, dimensions, independent hashes,
sources and credits are external. Pack 1.6 files, URLs, aliases and cached bytes are
unchanged. New individual URLs target pack 1.7, published and verified before the matching 1.5.1 APK. No complete graphics ZIP.
Source prompts are recorded in the new manifest. Original PNGs are retained in the local
work folder; they are not bundled or published. No film frames or official image assets
were copied for these illustrations. Existing third-party credits remain intact.

Reproduce translations and search aliases with scripts/GenerateScienceFictionResources.py;
append --check for verification. The measured catalogue generator preserves the separate,
append-only fictional identities. GenerateCelestialThumbnails.py --pack
textures/celestial-textures-v1.7 --check verifies every preview against its full resource.
