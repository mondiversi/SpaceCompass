# Thermal and physical reference audit — 2026-10-06

## Root cause and behavior

Catalog refresh retrieves orbital models and distances, not live temperatures or
pressures for distant bodies. The previous day/night sort required an exact
DAY_MAXIMUM/NIGHT_MINIMUM entry, excluding already packaged means, ranges,
1-bar atmospheric references, stellar effective temperatures and photosphere data.

The shared resolver now prefers the exact day/night extreme. If absent, it uses
the first applicable physical reference and prints its translated quantity below
the value. It never turns a mean into a measured night/day extreme. A range sorts
by its midpoint, in both day/night views; Alpha Centauri uses explicitly labelled
component A as its stable catalog representative, while details retain A and B.
The canonical Celsius sort key and displayed reference are resolved together.
Unavailable values still sort last in either direction. Reference values work
offline and are independent of whether the object has been clicked or selected.

All 23 regular catalog objects for which a defensible temperature reference is
available now display it in either temperature ordering; the seven unsupported
cases are four spacecraft and three black holes. Earth centre became a guide
point on 2026-10-07 and is no longer a selectable catalog entry. The hidden fictional
object retains its explicitly fictional terraforming-era mean separately.

## New references and qualifications

| Object | Reference | Qualification |
| --- | --- | --- |
| Sedna | About −240 °C | Brown's discovery-era estimated surface temperature at about 90 AU, labelled estimated and 2004 |
| TRAPPIST-1e | 251.3 ±4.9 K | Gillon 2017 equilibrium model with zero Bond albedo; not an observed surface temperature or evidence for an atmosphere |
| Halley | 300–400 K | Vega 1986 nucleus observations near 0.8 AU; year visible, not present-day temperature |
| 67P | 205–230 K | Rosetta/VIRTIS July–August 2014 surface observations; year visible, not current temperature |
| Alpha Centauri A/B | Effective temperature per component | Existing published luminosity/radius references with Stefan–Boltzmann and nominal solar effective temperature 5772 K |
| Stephenson 2-18 / DFK 1 | Adopted effective temperature 3200 K | Siebert et al. September 2026 SED model; cluster-distance/membership assumption remains explicit |
| RX J1856 | Apparent thermal components 38.9–62.4 eV, approximately 451000–724000 K | Two-blackbody spectral-model components reported by Yoneyama 2017, redshifted to infinity; not a uniform local surface range |
| PSR J0437 | 125000–350000 K | Durant 2012 UV bulk-surface thermal-model range; excludes hotter polar caps and companion temperature |

Primary sources:

- Sedna discoverer: https://web.gps.caltech.edu/~mbrown/sedna/
- Sedna radiometry/diameter: https://arxiv.org/abs/1204.0899 . Its volatile-retention
  equivalent 20 ±2 K is not misused as a current surface measurement.
- TRAPPIST-1 discovery paper, Table 1 and zero-albedo footnote:
  https://www.eso.org/public/archives/releases/sciencepapers/eso1706/eso1706a.pdf
- 67P: https://blogs.esa.int/rosetta/2014/10/03/measuring-comet-67pc-g/
- Halley mission review:
  https://iki.cosmos.ru/docs/gringaus-et-al-papers/1997/1997Encyclopedia%20of%20planetary%20science_Verigin_VEGA%20Mission.pdf
- RX J1856: https://academic.oup.com/pasj/article/69/3/50/3749249
- PSR J0437: https://arxiv.org/abs/1111.2346
- Stephenson 2-18: https://arxiv.org/html/2609.31362 , Table 3, DFK 1 (not DFK 18).
- Existing Alpha Centauri component sources: REFERENCE_DATA_AUDIT.md / CELESTIAL_VIEWER.md.

For Stephenson 2-18, the model luminosity is 321000 ±115000 Lsun. The newly included
diameter is derived from L = 4πR²σT⁴, at the assumed 3200 K and cluster distance.
Its asymmetric errors propagate the quoted luminosity interval alone; they do not
claim to encompass membership/temperature systematics. Current mass, gravity and
density remain unknown; initial mass and wind mass-loss rate are not substitutions.

## Complete temperature/pressure coverage

| Object/group | Temperature quantity available | Atmospheric surface pressure |
| --- | --- | --- |
| Sun | Photosphere | Not a planetary atmosphere |
| Mercury | Sunlit maximum / night minimum | Qualified exosphere upper bound |
| Venus | Surface mean | Reference surface pressure |
| ISS / Starlink | No single whole-hull temperature | Cabin pressure is not surface atmosphere |
| Moon | Sunlit maximum / night minimum | Night exosphere reference |
| Mars | Mean and overall surface range | Variable reference surface pressure |
| Jupiter / Saturn / Uranus / Neptune | Atmosphere at the chosen 1-bar layer | No defined solid surface; 1 bar is a reference level, not a new measurement |
| Io | Surface mean | Approximate near-surface SO₂ pressure |
| Europa | Surface range | O₂ exosphere reference |
| Pluto | Surface mean | Epoch-dependent reference surface pressure |
| Sedna | Discovery-era surface estimate | No confirmed measurable atmospheric pressure |
| Halley / 67P | Encounter-era surface ranges | Expanding coma does not define one stable surface pressure |
| Voyager 1 / 2 | No single whole-hull temperature | No exterior planetary atmosphere |
| Proxima Centauri | Stellar effective | No planetary surface pressure |
| Alpha Centauri | Effective A and B separately | No planetary surface pressure |
| TRAPPIST-1e | Equilibrium model | Atmospheric existence/pressure unconfirmed |
| RX J1856 / PSR J0437 | Spectral thermal model | No catalog planetary atmosphere |
| Polaris | Effective temperature of Aa | No planetary surface pressure |
| Rigel | Effective temperature | No planetary surface pressure |
| Stephenson 2-18 | Adopted effective model | No planetary surface pressure |
| Sagittarius A* / M31 core / TON 618 | No exterior surface temperature | No planetary surface atmosphere |

Pressure retains the seven published surface/exosphere references and their
upper-limit, night and constituent qualifications. Unknown pressure is omitted
from object details and shown as a dash in pressure sorting; no generic zero,
Earth-like atmosphere or arbitrary 1-bar surface pressure is invented. Sources
and conversion tests remain in ATMOSPHERIC_PRESSURE.md.

## Units and validation

Celsius/Fahrenheit conversion applies to each range endpoint. Uncertainty widths
multiply by 1.8 and never receive the +32 offset. Numeric formatting applies to
all values and visible epoch years. The atmospheric-layer label converts 1 bar
to the selected bar/Pa/psi unit. All six new detail labels/notes and nine short catalog qualifiers exist in all 20 languages.

Validation results for the installed local APK are recorded separately in
VERIFICATION.md. No GitHub publication is part of this change.
