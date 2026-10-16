# Southern-sky catalogue additions (1.4.3)

The ordinary catalogue now contains 53 entries. The saved Sun/Moon default, existing
identifiers, active object, user selections and the Earth-distance sort
are retained. This catalogue is distributed in application 1.4.3.

## Tracking and reference data

- **Ceres**: JPL Horizons `1;`, validated header `1 Ceres (A801 AA)`. This is a dwarf
  planet, not a generic asteroid or a spacecraft. Current apparent geocentric states
  drive observer azimuth, elevation and range; separate heliocentric states drive
  orbital speed. Physical radius, GM, spin and osculating parent-distance limits
  use the JPL record. The mean surface temperature is the NASA small-worlds reference
  of approximately −105 °C, not current weather or invented day/night extremes.
- **Enceladus**: Horizons `602`, validated header `Enceladus (602)`. Topocentric
  pointing uses the same validated states. Orbital speed subtracts Saturn's velocity,
  rather than reporting heliocentric speed as moon-orbital speed. GM/radius/density
  follow the JPL satellite table; spin/orbital period is 1.370218 days. The approximate
  surface-temperature reference of −201 °C comes from NASA's Enceladus page.
  Pole/prime-meridian orientation uses BODY602 in NAIF `pck00011.tpc`.
- **Vega and Altair**: SIMBAD ICRS/J2000 coordinates, proper motion and Hipparcos
  reference parallaxes (130.23 and 194.95 mas). The existing local stellar model
  applies proper motion, Earth/observer parallax, annual aberration, precession,
  nutation and sidereal rotation. Vega's reference mass is 2.135 ± 0.075 solar masses
  (Yoon et al. 2010); Altair's is 1.86 ± 0.03 (Bouchaud et al. 2020). These are rapid,
  oblate rotators; no unqualified spherical diameter, density or uniform surface
  temperature is invented. Their on-screen symbols are illustrative.
- **Deneb**: SIMBAD ICRS/J2000 direction and proper motion, with the adopted 802-pc
  distance from Schiller & Przybilla rather than directly inverting the problematic
  bright-star Hipparcos parallax. Radius 203 ± 17 solar radii, spectroscopic mass
  19 ± 3 solar masses, luminosity 196,000 solar luminosities and effective temperature
  8525 ± 75 K are reference estimates, not live measurements. Distance is uncertain.
- **Southern Cross / Crux**: a constellation filter entry, not a bound object. Four
  independently projected SIMBAD references (Acrux, Mimosa, Gacrux and Imai) form the
  cross; the centre of their apparent unit directions supplies pointing and its
  daily track. There is no single distance, diameter, mass or orbital speed. Physical
  tables are omitted and distance sorting keeps this unknown value last. The
  geometric figure and current centre are distinct from the south celestial pole.
  Extending Gacrux–Acrux beyond Acrux is an approximate navigation aid; its accuracy
  and visibility depend on the observer and date. The real four-star figure is drawn
  in the main sky, camera overlay, panorama and photo export with their existing
  perspective/azimuth projections. A large centre thumbnail never covers the figure.

New Horizons-backed objects retain strict finite-window validation, independent
pointing/motion caches, ten-minute catalogue refresh and explicit refresh. Missing
or expired data produce unavailable values, never fabricated coordinates.

## External graphics pack 1.4

`textures/celestial-textures-v1.4` is published for application 1.4.3: 29 individual WebP assets,
quality 90/method 6, 6,771,736 image bytes. The 27 published images are byte-identical
to pack 1.3. The two new images are not embedded in the APK:

- `ceres.webp`: NASA Dawn PIA19562, **Dawn RC3 Image 19**, aspect-preserving archival
  photograph. It is displayed flat and never reprojected as a surface map.
- `enceladus.webp`: Cassini PIA18435, **Color Maps of Enceladus – 2014**, resized to
  2048 × 1024 as a cylindrical surface map. It is a composite, not a live photograph.

All 29 image pins now download individually from **published pack 1.4**, with
size, SHA-256 and dimension checks before atomic cache replacement. Verified offline resources may also be imported from
the app's external-files `celestial-textures/celestial-textures-v1.4` directory; only
filenames, byte counts and SHA-256 hashes pinned by this APK are accepted. Previous
internal caches remain intact and can be imported through the same atomic verifier.
Without a verified cached image or a successful download, the object remains usable
with its placeholder.

Application 1.4.3 pins pack 1.4. All earlier releases and URLs remain available
for older APKs. No complete texture ZIP is attached.

## Primary sources

- [JPL Horizons API](https://ssd-api.jpl.nasa.gov/doc/horizons.html)
- [JPL planet and dwarf-planet physical parameters](https://ssd.jpl.nasa.gov/planets/phys_par.html)
- [JPL satellite physical parameters](https://ssd.jpl.nasa.gov/sats/phys_par/sep.html)
- [NAIF IAU orientation coefficients](https://naif.jpl.nasa.gov/pub/naif/generic_kernels/pck/pck00011.tpc)
- [NASA small-worlds reference temperatures](https://nssdc.gsfc.nasa.gov/planetary/factsheet/galileanfact_table.html)
- [NASA Enceladus](https://science.nasa.gov/saturn/moons/enceladus/)
- [SIMBAD Vega](https://simbad.cds.unistra.fr/simbad/sim-id?Ident=Vega),
  [Deneb](https://simbad.cds.unistra.fr/simbad/sim-id?Ident=Deneb),
  [Altair](https://simbad.cds.unistra.fr/simbad/sim-id?Ident=Altair),
  [Acrux](https://simbad.cds.unistra.fr/simbad/sim-id?Ident=Acrux),
  [Mimosa](https://simbad.cds.unistra.fr/simbad/sim-id?Ident=Mimosa),
  [Gacrux](https://simbad.cds.unistra.fr/simbad/sim-id?Ident=Gacrux),
  [Imai](https://simbad.cds.unistra.fr/simbad/sim-id?Ident=Imai)
- [Vega mass, Yoon et al.](https://doi.org/10.1088/0004-637X/708/1/71)
- [Altair model, Bouchaud et al.](https://doi.org/10.1051/0004-6361/201936830)
- [Deneb spectroscopy, Schiller & Przybilla](https://arxiv.org/abs/0712.0040)
- [Astronomical Society of Southern Africa: celestial direction finding](https://assa.saao.ac.za/wp-content/uploads/sites/23/2024/01/Celestial-direction-finding.pdf)

The frozen synthetic-site JPL fixtures in `app/src/test/resources/catalog-south` are
test data only and are excluded from the APK. Full image credits, processing and
checksums accompany the local graphics manifest.
