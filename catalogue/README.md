# Expanded catalogue provenance

The canonical source is `expanded-catalog.json`. All science and all 20 language resources
are compiled into the APK; none are fetched from the graphics package.

Derived catalogue data and constellation patterns: CC BY-SA 4.0. Application code:
the repository's existing GPL-3.0 licence. Retain these notices and the original dataset
licences when redistributing this derived catalogue. Independent source terms remain
applicable to NASA/JPL, SIMBAD and NORAD records; no ownership of their observations is claimed.

- HYG v4.1 (Hipparcos/Yale/Gliese), https://github.com/astronexus/HYG-Database.
  `licenses/hyg-license.txt`. Coordinates, proper motion, distance, spectral classification
  and apparent V magnitude only. The HYG unknown-distance sentinel is not a measurement.
  V-band luminosity is not relabelled as a bolometric luminosity or used to invent a stellar mass.
- OpenNGC v20260501, https://github.com/mattiaverga/OpenNGC/tree/v20260501.
  `licenses/opengc-license.txt`. Positions, identifiers and measured catalogue magnitudes.
- Stellarium western sky culture: https://github.com/Stellarium/stellarium-skycultures/tree/master/western.
  Star links and translated constellation names; no constellation illustrations were copied.
  Pattern stars use HYG astrometry. Stellarium's text/data are CC BY-SA; illustration licences
  do not apply because no illustrations are included. Original notice: `licenses/western-description.md`.
- NASA Exoplanet Archive confirmed `pscomppars`: https://exoplanetarchive.ipac.caltech.edu/.
  Planet masses/radii retain their model qualifications; equilibrium temperature is explicitly
  a model, not measured surface/atmospheric day or night weather. Pointing follows the unresolved host.
- SIMBAD, CDS Strasbourg: https://simbad.cds.unistra.fr/. Compact binaries include candidates,
  identified as such rather than asserted to be confirmed black holes. No unknown masses are invented.
- NASA/JPL Small-Body Database: https://ssd-api.jpl.nasa.gov/doc/sbdb.html.
  Physical-parameter references and unit conversions are retained per record. Absolute H magnitude
  is not displayed as apparent visual magnitude. A hyperbolic orbit has no invented aphelion/period.
- NASA/JPL satellite physical table: https://ssd.jpl.nasa.gov/sats/phys_par/.
  GM is converted to mass; radii to diameters; g/cm³ to kg/m³. Unadopted fields remain absent.
- NASA/JPL Horizons: https://ssd.jpl.nasa.gov/horizons/.
  All 206 new moving targets were resolved and verified against 49 hourly apparent Earth-centred
  ICRF vectors. Fixtures are test-only. Natural-moon velocities use their own primary's geometric
  states; spacecraft/other motion keeps its stated origin. Validity windows are never extrapolated.
- NORAD/CelesTrak, public Satcat copies and SatNOGS: https://celestrak.org/ and https://db.satnogs.org/.
  Satellite identities are NORAD + COSPAR + orbital envelope, with TLE checksums, SGP4 status and
  freshness validation. GEO has a specific envelope; old LEO identity checks are retained.
  SatNOGS record identity metadata is credited to Libre Space Foundation and contributors (CC BY-SA 4.0);
  redistributed NORAD element sets retain their source terms. Current elements are downloaded at runtime.

Per-object URLs, target headers, validation dates, raw-query SHA-256 values and available
physical-parameter reference identifiers are retained in the canonical source. The two earlier
regular catalogue datasets and their source notes are preserved. LV-426 remains explicitly fictional.
