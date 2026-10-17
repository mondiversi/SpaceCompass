# One hundred ordinary catalogue targets (Space Compass 1.4.4)

The catalogue grows from 53 to 100 ordinary selectable objects. The retired Earth
centre stays excluded and hidden LV-426 remains separate (101 only after reveal).
Existing enum identifiers, selections, preferences and relative presentation order
are preserved. Scientific families are inserted next to their parent or related targets.
Explicit distance orders continue to use their actual numeric sort fields.

## Added targets

- **Natural satellites:** GANYMEDE, CALLISTO, PHOBOS, DEIMOS, RHEA, IAPETUS, TITANIA, MIRANDA, TRITON, CHARON.
- **Small bodies and dwarf planets:** ERIS, HAUMEA, MAKEMAKE, VESTA, BENNU, RYUGU, HALE_BOPP, ENCKE.
- **Stars:** ARCTURUS, CANOPUS, CAPELLA, ANTARES, ALDEBARAN, SPICA, FOMALHAUT, ACHERNAR, HADAR, SIGMA_OCTANTIS.
- **Deep-sky objects:** M13, OMEGA_CENTAURI, M57, M27, M1, CARINA_NEBULA, HORSEHEAD_NEBULA, LARGE_MAGELLANIC_CLOUD, SMALL_MAGELLANIC_CLOUD, M51.
- **Spacecraft:** BEPICOLOMBO, SOLAR_ORBITER, LUCY, PSYCHE.
- **Compact objects and exoplanets:** CYGNUS_X1, V404_CYGNI, VELA_PULSAR, TOI_700_D, KEPLER_186_F.

## Scientific routes and limits

- **22 moving targets:** identity-pinned NASA/JPL Horizons geocentric ICRF apparent
  vectors (light time and stellar aberration), UTC input, local topocentric correction
  and normal daily-path calculations. Geometric states retain an explicit origin: the Sun for small bodies/craft and
  each primary planet for the new moons. Heliocentric catalogue distances never
  interpret a planet-centred moon vector as a Sun-centred vector. Only centre-based requests leave the phone;
  GPS is not sent to JPL. Spacecraft coverage remains limited to published mission
  ephemerides. Missing/out-of-window samples return unavailable, never extrapolation.
- **10 new moons:** JPL satellite physical table GM, volume-equivalent radius,
  density and synchronous reference periods. Parent-relative speeds use direct JPL planet-centred vectors and the
  derivative of Hermite position interpolation. Their independent cache names
  and strict origin checks prevent heliocentric/planet-centred cache confusion. Ten direct
  parent-centred JPL velocity fixtures independently test the half-hour midpoint;
  ordinary linear velocity interpolation would damp Phobos speed by about 8%.
  Caronte/Charon no longer depends on subtracting a local Pluto-system approximation.
- **25 distant targets:** ICRS/J2000 catalogue directions, approximate reference
  distances, proper motion where adopted, annual parallax/aberration and the existing
  horizontal-coordinate transform. These work offline. Extended objects represent
  catalogue centres, not resolved trajectories of their gas/stars. The new Galaxy
  filter classifies LMC, SMC and M51 independently of nebulae and stars, with correct
  galaxy membership. Cygnus X-1/V404 follow unresolved binary directions.
- **TOI-700 d / Kepler-186 f:** NASA Exoplanet Archive default reference radii,
  periods and host coordinates/distances; no guessed masses, surface maps,
  atmospheric pressure or resolved 3-D planetary orbits. A localized viewer note
  explains host-star pointing. No habitability is asserted from these parameters.
- **Sigma Octantis:** localized note explains that it is faint and near, not on,
  the south celestial pole; Crux remains a more recognizable orientation pattern.
- Unknown/unadopted physical data remain `null`/a dash. Catalogue magnitude,
  spectral type and geometry must not be confused with live telemetry. Rounded
  reference periods and physical estimates do not drive JPL pointing.

Coordinates, adopted numbers and source links are indexed in
[CATALOG_100_SOURCES.json](CATALOG_100_SOURCES.json). JPL reference tables in
`app/src/test/resources/catalog-100` were fetched for 2026-10-17 12:30 UTC at the
synthetic observer 45 N, 9 E, 100 m; they contain no device position.

## Graphics and compatibility

Graphics pack `celestial-textures-v1.5` contains 25 new archived photographs
plus the 29 unchanged previous images. Every new resource is an individual WebP,
quality 90, method 6, 2048 x 1024, with proportional letterboxing, source/credit,
byte length, dimensions and SHA-256 pin. An archive photo is shown flat, not
wrapped on an invented planetary sphere. The viewer identifies archived imagery.
Other new objects retain the appropriate star, comet, craft, diffuse-target,
black-hole, pulsar or neutral-world placeholder. No celestial photo is added to APK assets.

The published v1.5 graphics release contains every full image and preview, with
individual APK-pinned URLs. Full image bytes, legacy caches and older release URLs
are preserved. A complete texture ZIP is not distributed. The subsequent galaxy
extension adds two further photographs, bringing the full-image count to 56.

## Interface and verification

Rise uses a filled upper semicircle and set a filled lower semicircle. Both derive
from one shared geometry function for the daily list, live orbit, photo and panorama.
Culmination (+), minimum (−), current and approach symbols remain unchanged.

All 20 interface languages include the 47 new names and necessary new labels/notes.
Search recognizes their localized aliases and scientific designations independently
of the active interface language. Version 1.4.4 includes updated news in every interface language.

Required validation: debug and release compilation; full JVM suite; compilation
of Android instrumentation sources (no connected instrumentation without separate
authorization); release lint; image-manifest/hash/format and translation-parity audits.
Physical-device checks and exact results are recorded separately in the work report.

The subsequent [galaxy/nucleus extension](GALAXY_PAIRS.md) brings this catalogue to 104 entries and the external v1.5 pack to 56 images.

## Independent reference previews

The external pack now also includes 56 `thumb_*.webp` previews, 128 x 128,
WebP quality 90, method 6. Every preview inherits its parent image's credit and
licence; the manifest records its parent digest, render mode and independent
byte/dimension/SHA-256 pins. Full image bytes and published URLs are unchanged.
The app's catalogue/live/captured glyphs request previews only. Full maps are
requested by the observatory and image exports that need them. A verified full
map already cached by an older app remains a local fallback, without downloading
it to build a catalogue thumbnail. Runtime lunar phase masking remains dynamic.
Preview memory is capped at 4 MiB; persistent verified files remain available offline.

Run `python scripts/GenerateCelestialThumbnails.py` to regenerate previews,
manifest/credits/checksums and APK integrity pins; `--check` verifies reproducibility.
Nothing is embedded in the APK. Every preview has its own v1.5 URL and integrity pins. Previous release files remain available.
