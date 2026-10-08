# Catalogue additions: reviewed references and external images

The selectable catalogue contains 35 public entries. Sirius, Betelgeuse, Titan,
M42 and M45 are additions; existing selected/active objects and defaults are unchanged.
Andromeda remains the existing M31* nuclear entry, with a whole-galaxy photograph
and explanatory section in its detail. Its tabulated mass is the black hole's mass.

## Scientific identification

The four distant additions use ICRS/J2000 catalogue centres, proper motion when
available, annual aberration, precession/nutation and topocentric projection. Their
daily paths describe Earth's changing viewpoint, not fabricated closed orbits.
Their directions and approximate distances work offline. Nebulae and clusters have
no invented planetary surface pressure, gravity, rotation or single day/night temperature.
Missing physical values follow the existing sort policy and remain at the end.

| Object | Adopted reference | Qualification |
|---|---|---|
| Sirius | J2000 06:45:08.91728, -16:42:58.0171; parallax 379.21 mas; proper motion -546.01/-1223.07 mas/year | Physical values describe Sirius A. The archive photograph also contains Sirius B. |
| Betelgeuse | J2000 05:55:10.30536, +07:24:25.4304; proper motion 27.54/11.30 mas/year; distance 168 pc | Joyce et al. model: radius 764 (+116/-62) solar radii, present-day mass 16.5–19 solar masses. The mass range is not a symmetric statistical error. |
| M42 | J2000 catalogue centre 05:35:16.8, -05:23:15; distance 414 pc; apparent magnitude 4.0 | Centre of an extended star-forming nebula; distance follows the Orion Nebula Cluster measurement. |
| M45 | J2000 centre 03:46:24.2, +24:06:50; parallax 7.364 mas; proper motion 19.997/-45.548 mas/year; magnitude 1.6 | Centre of the Pleiades open cluster, not a single star or a resolved individual orbit. |

Primary references:

- [SIMBAD Sirius](https://simbad.cds.unistra.fr/simbad/sim-basic?Ident=Sirius),
  [Bond et al. 2017](https://arxiv.org/abs/1703.10625) and
  [Kervella et al. 2003](https://arxiv.org/abs/astro-ph/0306604).
- [SIMBAD Betelgeuse](https://simbad.cds.unistra.fr/simbad/sim-basic?Ident=Betelgeuse) and
  [Joyce et al. 2020](https://arxiv.org/abs/2006.09837).
- [SIMBAD M42](https://simbad.cds.unistra.fr/simbad/sim-basic?Ident=M42),
  [Menten et al. 2007](https://arxiv.org/abs/0709.0485) and
  [NASA M42 catalogue](https://science.nasa.gov/mission/hubble/science/explore-the-night-sky/hubble-messier-catalog/messier-42/).
- [SIMBAD M45](https://simbad.cds.unistra.fr/simbad/sim-basic?Ident=M45) and
  [NASA M45 catalogue](https://science.nasa.gov/mission/hubble/science/explore-the-night-sky/hubble-messier-catalog/messier-45/).

## Titan

Titan uses NASA/JPL Horizons target 606, apparent Earth-centred LT+S vectors for
pointing and separate geometric Sun-centred states for motion. Hourly samples span
the same three-day cache window as the existing remote objects. Requests transmit
the target and dates, not the observer's GPS coordinates. Missing/out-of-range data
remain unavailable; Saturn's direction is never substituted for Titan's.

Orbital speed subtracts Saturn's heliocentric velocity before computing magnitude,
so the displayed reference is Saturn rather than the Sun. The face orientation uses
NAIF pck00011.tpc: pole RA 39.4827°, declination 83.4279° and prime meridian
186.5855° + 22.5769768° per TT day from J2000, evaluated at light emission time.
The surface mosaic has longitude zero at the left seam, increasing eastward.

JPL SAT441 reference means are radius 2574.76 ±0.02 km, GM 8978.13710 km³/s²,
density 1881.4 kg/m³, semi-major axis 1221900 km, eccentricity 0.029 and orbital
period 15.945448 days. Mass and surface gravity are derived from GM and radius.
The 93.7 K temperature and 146700 Pa pressure describe the Huygens landing site
in 2005, not current global telemetry.

- [JPL physical parameters](https://ssd.jpl.nasa.gov/sats/phys_par/)
- [JPL mean satellite elements](https://ssd.jpl.nasa.gov/sats/elem/)
- [NAIF planetary constants](https://naif.jpl.nasa.gov/pub/naif/generic_kernels/pck/pck00011.tpc)
- [NASA Titan atmosphere chapter](https://ntrs.nasa.gov/api/citations/20240013879/downloads/NixonChapter6STI.pdf)
- Captured real JPL responses: `app/src/test/resources/catalog-additions/titan-pointing.txt`
  and `titan-motion.txt`, dated 2026-10-07 through 2026-10-10.

## External image distribution

All six new images are lossy WebP, quality 90, method 6, exactly 2048 × 1024.
Titan's map is resampled with geographic orientation preserved. The telescope
photographs retain their original proportions through black framing, never a
stretch or a spherical surface projection. Sirius's available 733 × 800 source
is upsampled to fit the standard frame; this does not invent additional detail.
Betelgeuse is an ALMA submillimetre observation; the Pleiades photograph is WISE
infrared. None represents a live optical camera view.

| File | Verified bytes |
|---|---:|
| titan.webp | 159238 |
| sirius.webp | 69180 |
| betelgeuse.webp | 16418 |
| orion_nebula.webp | 87134 |
| pleiades.webp | 356448 |
| andromeda_galaxy.webp | 172094 |
| **Total new images** | **860512** |
| **All 18 pinned external images** | **3969738** |

`textures/celestial-textures-v1.1/manifest.json` records source,
attribution, dimensions, processing, byte lengths and SHA-256. Corresponding full
credits are included in `CREDITS-catalog-additions.txt` and the app's licence text.
The additive ZIP is a distribution convenience; the app downloads and verifies
individual requested files, retaining the existing offline cache. Images are
excluded from APK assets. Existing release assets and legacy URLs are preserved.
Andromeda's photograph is requested only in that entry's detail.

New strings have complete key/placeholder parity across the existing 20 locales.
New object/type identifiers append to their enums, retaining existing identities.
No default selection, scenario setting, image cache or saved format is cleared.
