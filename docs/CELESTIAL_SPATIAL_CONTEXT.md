# Celestial detail sections and spatial context

The object viewer presents identity/classification, physical properties, stellar light
and components where applicable, temperature/atmosphere, orbit/observation, and spatial
context as separate tables. The existing uncertainty, atmospheric reference levels,
component conventions, scientific notes and image credits remain applicable.

Spatial context follows the physical/orbital tables rather than dominating identity.
No location permission is needed for the astronomical coordinate rows. Observer range
and current-face orientation still require the observer position as before.

## Membership

Nearby bodies belong to the Solar System in the Milky Way; interstellar Voyager
trajectories retain their origin/central gravitational system rather than implying
the spacecraft are inside the heliosphere. Alpha Centauri AB and Proxima share the
Alpha Centauri system. Polaris and Sirius are multiple systems; Rigel is listed under
its Beta Orionis system and the existing physical values describe the relevant primary.
TRAPPIST-1e belongs to TRAPPIST-1, and PSR J0437−4715 identifies its binary system.
No system is invented for an isolated source or for the uncertain Stephenson 2
association. M31* belongs to Andromeda; TON 618 identifies its distant host galaxy,
not a Milky Way object or a fabricated galaxy catalogue designation.

LV-426 retains the fictional reference direction toward ζ² Reticuli with an explicit
fiction/host-direction qualifier. Its membership must not imply a discovered real moon.

## Coordinate convention

- Distant sources use the existing ICRS catalogue direction at J2000.0, not today's
  apparent horizontal position. Proper-motion, parallax and aberration continue to be
  applied by the pointing implementation; this change does not modify that path.
- Astronomy Engine supplies geocentric J2000 equatorial vectors (EQJ) for its bodies
  and Io/Europa, evaluated for the indicated instant with its existing aberration mode.
- Valid Horizons samples supply ICRF geocentric light-time/stellar-aberration corrected
  vectors, interpolated only within their validated time span. There is no extrapolation
  or fallback to heliocentric motion samples.
- ISS/Starlink angular coordinates use their propagated geocentric TEME vector. The
  frame is explicitly labelled TEME and never silently relabelled J2000 or ICRS.
- Unresolved TRAPPIST-1e coordinates indicate its host star; LV-426 uses the fictional
  host direction. Missing/expired remote ephemerides show unavailable coordinates.
- Right ascension is an angle expressed in hours, not a local wall clock. The coordinate
  preference selects decimal hours/degrees or sexagesimal HMS/DMS; the numeric preference
  controls decimal symbols/digits. Computation instants use the app date/clock formats
  with an explicit UTC suffix, independent of GPS and scenario time-zone settings.

Reference diameter can additionally produce an approximate spherical volume (`πd³/6`).
This is explicitly qualified, not an independently measured volume. Spacecraft,
irregular comet nuclei, extended sources, fictional bodies and black-hole horizons are
excluded. Imperial conversion is cubic (`km³ / 1.609344³` → `mi³`).

## Primary references

- [Astronomy Engine Kotlin reference: geoVector and EQJ](https://github.com/cosinekitty/astronomy/blob/master/source/kotlin/README.md)
- [JPL Horizons manual: coordinate frames, vector corrections and observing centres](https://ssd.jpl.nasa.gov/horizons/manual.html)
- [CDS SIMBAD: astronomical source identification and catalogue coordinates](https://simbad.cds.unistra.fr/simbad/)
- [NASA: Alpha Centauri triple system](https://www.nasa.gov/image-article/alpha-centauri-triple-star-system-about-4-light-years-from-earth/)
- [NASA: Rigel multiple system](https://science.nasa.gov/image-article/apod-2023-april-7-rigel-wide/)
- [NASA: Polaris multiple system](https://science.nasa.gov/missions/hubble/theres-more-to-the-north-star-than-meets-the-eye/)
- [NASA Exoplanet Archive: TRAPPIST-1e](https://exoplanetarchive.ipac.caltech.edu/overview/TRAPPIST-1e)
- [NASA: quasars and their host galaxies](https://science.nasa.gov/mission/hubble/science/science-behind-the-discoveries/hubble-quasars/)

Existing object-specific measurements and credits remain documented in
`DEEP_SKY.md`, `CELESTIAL_VIEWER.md` and `CATALOG_ADDITIONS.md`.
