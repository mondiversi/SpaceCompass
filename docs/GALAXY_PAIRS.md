# Galaxies and their central black holes (Space Compass 1.4.4)

Four independent entries extend the ordinary catalogue from 100 to 104. They are
appended to the enum to retain saved IDs and every existing ordinal-derived colour.
Default Sun/Moon selection and previous objects' relative order remain unchanged.
Galaxies and nuclei remain separately selectable, filterable, searchable and exportable.

| New entry | Category | Pointing target | Adopted distance |
| --- | --- | --- | --- |
| Andromeda galaxy, M31 / NGC 224 | Galaxy | Existing M31 nuclear direction | ~2.5 million ly |
| M87 galaxy, NGC 4486 | Galaxy | Existing M87* nuclear direction | ~55 million ly |
| Milky Way (centre) | Galaxy | Galactic Centre / Sgr A* direction | ~26,400 ly to centre |
| M51 central black hole | Black hole | NGC 5194 active nucleus | ~31 million ly |

The observer is **inside** the Milky Way: its entry points at the centre, not at a
unique 3D position of the entire galaxy or at every part of its extended band.
Distances are approximate reference scales. M87 and M87* deliberately share the
existing rounded 55-million-ly scale (NASA's general galaxy page rounds to 54 million).
Nearby catalogue centres are not independent distance measurements of an internal offset.

## Calculation and identity

The existing ICRS/J2000 deep-sky engine computes observer-dependent azimuth/elevation
with precession, nutation, sidereal rotation and annual aberration/parallax. It feeds
daily paths, scenarios, photographs, panoramas and frozen exports without live network
requests. A galaxy marker follows its nucleus; it does not pretend to trace the
extent of the galaxy. A galaxy and its own nucleus do not generate a false
conjunction with each other. Actual encounters with other targets remain eligible.

M51 nuclear J2000 coordinates are 13h29m52.71s, +47d11m42.79s (PAWS Table 2).
The mass is rounded to ~4 million solar masses from log10(M/Msun)=6.60 +/-0.49
(Izumi et al. 2016 Table 1, indirect M-sigma estimate). This has large logarithmic
uncertainty and is not a direct horizon measurement or precise dynamical mass.
The Schwarzschild-equivalent diameter is a model scale, using the same qualified
formula as other supermassive black holes. No surface gravity, atmospheric pressure,
temperature or resolved horizon photograph is invented. Galaxy masses do not copy
the mass of their central black holes. Apparent magnitudes adopt NASA: M31 3.1, M87 9.6.

## Photographs and compatibility

`andromeda_galaxy.webp` is reused, not duplicated. The former full-galaxy supplement
is removed from the M31* viewer/export notes; its new galaxy entry owns that photo.
M87 retains the existing EHT nuclear image, distinct from `m87_galaxy.webp`.
`milky_way.webp` is an archival ESO full-sky panorama, not an external photo of our
galaxy or a live view. M51's black hole uses the existing symbolic fallback.

Both new files are proportionally resized/letterboxed WebP, 2048x1024, lossy Q90,
method 6. The v1.5 pack has 56 full images and 56 independent previews; all previous 54
files are byte-identical. Manifest, credits, SHA256SUMS and app integrity pins agree.
No photos are bundled into the APK; no complete archive is generated. Every image has an individual versioned download URL. The device can also
import each via the normal hash-verified offline pack folder. Legacy release URLs
and individually downloaded/cached assets remain supported. Version 1.4.4 publishes
the complete individual-file graphics release alongside the application.

## Primary references

- M31 galaxy and photograph: https://science.nasa.gov/mission/hubble/science/explore-the-night-sky/hubble-messier-catalog/messier-31/
- M31 nucleus: https://science.nasa.gov/asset/hubble/hubble-zooms-in-on-double-nucleus-in-andromeda-galaxy/
- M87 galaxy and photograph: https://science.nasa.gov/mission/hubble/science/explore-the-night-sky/hubble-messier-catalog/messier-87/
- Milky Way centre / M87 nuclei: https://www.eso.org/public/news/eso2208-eht-mw/
- Milky Way panorama (ESO/S. Brunier): https://www.eso.org/public/images/eso0932a/
- ESO attribution licence: https://www.eso.org/public/outreach/copyright/
- M51 active nucleus: https://chandra.harvard.edu/photo/2002/0158/
- M51 nuclear astrometry (PAWS): https://doi.org/10.1088/0004-637X/784/1/4
- M51 indirect mass (Izumi et al. 2016): https://doi.org/10.3847/0004-637X/827/1/81

TON 618's poorly characterized host and unconfirmed central black holes in the
Magellanic Clouds remain excluded. Stellar-mass Cygnus X-1 and V404 Cygni belong
to the Milky Way; they do not imply additional host galaxies.
