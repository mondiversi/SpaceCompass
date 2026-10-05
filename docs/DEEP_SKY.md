# Deep-sky references

Alpha Centauri AB is the unresolved binary reference, not Proxima Centauri. SIMBAD J2000 position 14:39:36.50, -60:50:02.3; tangent proper motions -3608, +686 mas/year; parallax 750.81 mas (Akeson et al. 2021). https://simbad.cds.unistra.fr/simbad/sim-basic?Ident=Alpha+Cen

Sagittarius A* identifies the Galactic central black hole, not the broader Sagittarius A complex. SIMBAD radio J2000 position 17:45:40.03599, -29:00:28.1699. Approximate reference distance 26,400 ly, ESO Messenger 177 (2019). Proper motion is omitted at phone-compass resolution. https://simbad.cds.unistra.fr/simbad/sim-basic?Ident=Sagittarius+A https://www.eso.org/sci/publications/messenger/archive/no.177-sep19/messenger-no177.pdf

M31* identifies Andromeda's central black hole using the M31 nuclear catalogue direction, not a resolved black-hole image or its event-horizon astrometry. SIMBAD infrared J2000 position 00:42:44.330, +41:16:07.50. Approximate galaxy distance 2.5 million ly (NASA). https://simbad.cds.unistra.fr/simbad/sim-basic?Ident=M31 https://www.nasa.gov/missions/chandra/andromeda-galaxy-vibaj/ https://science.nasa.gov/asset/hubble/our-neighboring-galaxys-unusual-core/

Pointing uses the existing Polaris vector model: tangent proper motion where available, observer/Earth subtraction, first-order annual aberration and J2000-to-horizontal rotation including precession/nutation. These catalogue distances are approximate and remain light-years in every distance presentation. No solar orbital velocity, invented hull/surface temperature, surface gravity or planet texture is assigned. The AB-pair and nucleus illustrations are explicitly symbolic in every supported language. Daily paths represent apparent diurnal motion, not an orbit about Earth.

No universe-centre target is defined: the Big Bang was not an explosion from a point in existing space. No scientifically established RA/declination exists for such a centre. https://science.nasa.gov/mission/webb/big-bang-q-and-a/

## Satellite connection diagnosis (2026-10-04)

HTTPS requests to the CelesTrak GP endpoint for CATNR=100855 and to its home page time out from the development network. The official domain aliases also timed out. Other public HTTPS services respond. SatNOGS returned HTTP 200 with an empty list for that exact ID, so it was not added as an ineffective fallback and no different Starlink was substituted. This establishes reachability failure, not a globally down server or the precise network/firewall cause.

Foreground default-route changes retry transport failures immediately. Successful cache freshness remains two hours; HTTP-rejected CelesTrak requests remain stopped. Timeout status is exposed separately from generic missing data. HTTPS trust is unchanged; no proxy, credentials or background service are added.

## TON 618

TON 618 denotes the quasar hosting an ultramassive black hole, rather than a resolved image of its event horizon. No absolute mass ranking or planetary surface quantities are claimed. ICRS/J2000 pointing uses SIMBAD: https://simbad.u-strasbg.fr/simbad/sim-basic?Ident=TON+618 . Its approximate 17.8 billion ly distance is comoving, not light-travel or luminosity distance, calculated for z=2.2 in flat Lambda-CDM (H0=70 km/s/Mpc, Omega_m=0.3, Omega_Lambda=0.7, radiation neglected), rounded to 0.1 billion ly. See https://www.astro.ucla.edu/~wright/CosmoCalc.html . This distance is metadata only; it must not be used to infer a local Euclidean orbit or velocity. NASA context: https://www.nasa.gov/universe/nasa-animation-sizes-up-the-universes-biggest-black-holes/ .

## Stellar extremes and nearby compact stars

Stephenson 2-18 is also Cl* Stephenson 2 DFK 1. It is included as a candidate among the largest stars, never as a confirmed absolute record. Its association membership, hence size and distance, is uncertain. The approximate 5.5 kpc distance is an association-based assumption, not a parallax measurement. Direction and proper motion: https://simbad.cds.unistra.fr/simbad/sim-id?Ident=Cl%2A+Stephenson+2+DFK+1 . Membership and distance limitations: https://arxiv.org/html/1208.3282 . No precise radius, mass or effective temperature is claimed in the app.

RX J1856.5-3754 is a nearby isolated, radio-quiet neutron star with weak X-ray pulsations (so it is not a non-pulsating counterpart to radio pulsars). J2000 direction: https://simbad.cds.unistra.fr/simbad/sim-id?Ident=1ES+1853-37.9 . Approximate distance 123 pc and proper motion from Walter et al. 2010: https://arxiv.org/abs/1008.1709 , with updated motion context https://academic.oup.com/mnras/article/417/1/617/980459 . The older 180–200 ly NASA articles predate the revised parallax and are not used for distance.

PSR J0437-4715 is the nearest known millisecond radio pulsar, not asserted to be nearer than every X-ray pulsar or neutron star. Distance 156.96 +/- 0.11 pc: https://arxiv.org/abs/2407.07132 . Direction and proper motion use the SIMBAD J2000 optical binary reference https://simbad.u-strasbg.fr/simbad/sim-id?Ident=PSR+J0437%E2%80%934715 ; binary separation is negligible for phone compass accuracy. Identification: https://astronomy.swin.edu.au/cosmos/P/PSR%2BJ0437-4715 . No live pulse timing, binary orbit, planetary gravity or surface simulation is implied. All three have distinct static symbolic illustrations and offline apparent daily tracks.

## Solar-mass references

Mass presentation switches to M☉ at 10 solar masses (inclusive); lower masses use the selected kg/lb unit, including the Sun, low-mass stars, pulsars and neutron stars. This is an app display policy, not a new physical classification. Existing kg physics values are unchanged (conversion reference 1.9884e30 kg). Missing estimates remain unknown rather than applying a generic star mass. The UI defines ≈ as a published approximate estimate and † as an assumed model input, not an object measurement.

- Sagittarius A*: ≈4.297e6 M☉, GRAVITY orbit-based best fit, statistical and systematic errors not collapsed into a misleading single uncertainty: https://www.aanda.org/articles/aa/full_html/2022/01/aa42465-21/aa42465-21.html .
- M31*: ≈1.4e8 M☉ for the central black hole, not the whole galaxy/nuclear cluster: https://science.nasa.gov/asset/hubble/our-neighboring-galaxys-unusual-core/ .
- TON 618: ≈6.6e10 M☉, NASA reference estimate, not a guaranteed current mass or universal ranking; virial estimators can give different results: https://science.nasa.gov/universe/black-holes/ .
- PSR J0437-4715: 1.418 ± 0.044 M☉, timing measurement of the pulsar alone, not its binary companion: https://arxiv.org/abs/2407.07132 .
- RX J1856.5-3754: †1.4 M☉, explicitly a canonical mass assumed in a cited surface-field simulation: https://academic.oup.com/mnras/article/464/4/4390/2417409 . It must never be described as a measured mass.

These mass references do not populate Newtonian surface gravity or density rows for black holes and neutron stars, whose physical interpretation would require additional relativistic models and measured radii.

Alpha Centauri mass is the A+B total, 1.988 solar masses (1.0788 + 0.9092), excluding Proxima. Component masses: https://doi.org/10.3847/1538-3881/abfaff . No individual total uncertainty is invented from potentially correlated component errors. Stephenson 2-18 retains an unknown present-day mass with no inferred display threshold; published initial-mass population estimates and mass-loss rates must not be represented as a measured current mass.
Nearby distances (Moon, ISS, Starlink) use normal km/mi units, independently of astronomical-distance preferences. The catalog uses Earth-centred range labelled Earth for those objects; other entries keep their existing heliocentric catalog reference. Observer-distance fields retain their actual topocentric range.


## Proxima Centauri and Rigel

Both appended enum IDs preserve stored existing selections; presentation places Proxima before Alpha Centauri AB and Rigel after PSR J0437 (before Stephenson). Both have symbolic stellar icons, offline apparent daily paths, light-year distances and no fictitious Solar-System orbital speed or resolved surface.

Proxima's J2000 direction and proper motion are SIMBAD/Gaia EDR3: https://simbad.cds.unistra.fr/simbad/sim-id?Ident=Proxima+Centauri . Distance uses 768.0665 mas parallax (about 4.2465 ly). Physical references from Faria et al. 2022 Table 1: mass 0.1221 ±0.0022 M☉, radius 0.141 ±0.021 R☉, effective temperature 2900 K, rotation 90 ±4 d, luminosity 0.0016 L☉: https://www.eso.org/public/archives/releases/sciencepapers/eso2202/eso2202a.pdf . Gravity and mean density are spherical Newtonian estimates, explicitly qualified. This is the nearest star to the Sun; it is not the closest star when counting the Sun itself.

Rigel's J2000 position, motion and 3.78 mas Hipparcos parallax: https://simbad.cds.unistra.fr/simbad/sim-id?Ident=RIGEL . Approximate distance 862.85 ly, with significant distance uncertainty. Main-component reference radius 78.9 ±7.4 R☉ and temperature 12100 K: https://arxiv.org/html/1201.0843 . Model-dependent mass 21 ±3 M☉ and spectroscopic log g=1.75 cgs: https://arxiv.org/html/1202.1836 . Luminosity uses 10^5.08 L☉. These values refer to Rigel A, not the summed multiple system. Rotation remains unknown rather than treating v sin i or pulsation periods as rotation. Radius is converted to diameter using 695700 km per solar radius; spectroscopic gravity to SI divides 10^logg by 100.

## Earth-centre reference

The geocentre is appended as a stable catalog ID and displayed immediately after Venus.
Its topocentric vector is the negative Astronomy Engine observer vector, transformed
into the local geometric horizon. Range therefore uses the oblate Earth model and observer
altitude, rather than a fixed mean radius. There is no daily sky orbit or invented core
temperature; speed is unavailable. This is a geometric point, not the core or
a physical body. Its viewer shows only observer range; no diameter, mass or
other planetary physical facts are assigned. The Earth symbol is a locator illustration.


## TRAPPIST-1 e

An Earth-sized habitable-zone candidate, not a confirmed inhabited or habitable
world. NASA Exoplanet Archive, Agol et al. (2021): mass 0.692 Earth masses,
radius 0.920 Earth radii (+0.013/-0.012), mean density 4.90 g/cm³,
and orbital period 6.101013 days. Kilometre conversion uses the 6371 km mean
Earth radius and mass conversion uses 5.9722e24 kg; gravity is derived from those
reference estimates. Surface temperature and axial rotation remain unknown.
The host is displayed as TRAPPIST-1, never as the Sun.

Pointing uses SIMBAD's J2000 host-star coordinates, proper motions and Gaia
parallax 80.2123 mas (about 40.66 ly). The planet is unresolved: no fabricated
orbital offset is added to the star's sky direction. Its daily path is Earth's
rotation relative to that direction. A neutral rocky-world symbol does not
assert an observed surface, ocean or atmosphere.

- https://exoplanetarchive.ipac.caltech.edu/overview/TRAPPIST-1e
- https://simbad.cds.unistra.fr/simbad/sim-id?Ident=TRAPPIST-1
- https://science.nasa.gov/missions/webb/nasa-webb-looks-at-earth-sized-habitable-zone-exoplanet-trappist-1-e/

Proxima Centauri already represents the nearest red dwarf; its note now makes
that classification explicit in every supported language.

## Dynamic mass display — version 0.1.8

The threshold is evaluated from the canonical reference before rounding and before
kg/lb conversion. Explicit solar references take precedence over kg references;
invalid or unavailable mass never invents a value or a unit threshold. Below 10 M☉,
both mass and published uncertainty convert using the existing 1.9884e30 kg solar
reference and exact pound conversion. A shared scientific exponent keeps the
value/uncertainty compact. Approximation (≈), model assumption (†), Alpha Centauri
A+B identification and all source values are retained. The resolved A/B component
rows use the same preference and each component's own threshold. Large objects
such as Rigel and the three central black holes retain M☉ regardless of kg/lb.
The threshold does not alter density, luminosity, gravity, horizons, temperatures,
positions, paths or distance formatting. Mass presentation resides in
SpaceCompassMassUnits.kt; scientific reference facts remain separately stored.
