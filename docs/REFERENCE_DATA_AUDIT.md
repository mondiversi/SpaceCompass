# Catalog reference-data audit — 2026-10-04

All 25 catalog objects were reviewed. Unknown and non-applicable quantities remain distinct from measured and model-dependent reference values. No generic mass is assigned to an individual star. Planetary reference data are not live telemetry.

| Object | Review and additions | Remaining limitations |
|---|---|---|
| Sun | Existing diameter, mass, gravity, density, photosphere temperature and equatorial rotation retained | Differential stellar rotation; no single Galactic orbit reference in planetary fields |
| Mercury | Existing physical, thermal and heliocentric orbit fields checked | Temperature varies by location and time |
| Venus | Existing physical, thermal and retrograde rotation data retained | Reference atmospheric surface temperature |
| Moon | Existing physical, thermal, Earth orbit and normal-distance units checked | Reference thermal extremes |
| Mars | Existing physical, thermal range, rotation and orbit retained | No live surface weather |
| Jupiter | Existing physical, 1-bar thermal and orbital fields checked | Atmospheric reference radius/gravity, not a solid surface |
| Saturn | Existing physical, atmospheric and orbital fields retained | Rotation convention and reference atmospheric layer |
| Uranus | Existing physical, atmospheric and retrograde rotation retained | Atmospheric reference layer |
| Neptune | Existing physical, atmospheric and orbit fields retained | Atmospheric reference layer |
| Pluto | Existing physical, thermal and orbital fields retained | Reference values, not live measurements |
| Io | JPL gravity parameter, radius and mean elements retained; diameter uncertainty added | Derived Newtonian gravity and mass use stated constants |
| Europa | JPL gravity parameter, radius and mean elements retained; diameter uncertainty added | Same reference qualifications as Io |
| Sedna | Diameter 995 ±80 km and rotation/orbit references retained | Mass, density and surface gravity not measured; no invented values |
| ISS | Existing NASA dimensions/mass retained; usable orbital data now supply revolution period | Mass and dimensions are references; attitude/temperature are not available |
| Starlink-40083 | Usable orbital data now supply revolution period | No authoritative individual spacecraft mass/dimensions found; no generic constellation mass assigned |
| Voyager 1 | NASA FAQ approximate mass 733 kg added with ≈; antenna diameter retained | FAQ estimate, not current telemetry; no closed revolution period |
| Voyager 2 | NASA FAQ approximate mass 735 kg added with ≈; antenna diameter retained | Same qualification as Voyager 1 |
| Alpha Centauri AB | Separate A/B masses and diameters with errors, luminosities, estimated effective temperatures, binary period and spectral types added | No single binary diameter/temperature; Proxima excluded |
| Polaris Aa | Mass uncertainty ±0.28 M☉, Aa–Ab binary period and spectral type added | Current rotation not assigned from uncertain photometric interpretation |
| Stephenson 2-18 (DFK 1) | M6 supergiant classification added; unknown-mass explanation shown | No reliable individual current mass found in reviewed primary papers. DFK 18 is a different star. No population initial mass or mass-loss rate substituted |
| RX J1856.5-3754 | 7.055 s pulse/rotation period added | Existing 1.4 M☉ explicitly assumed, not measured. Radius/temperature depend on emission/redshift models |
| PSR J0437-4715 | 5.7574519367 ms rotation, 5.741048 d binary period, NICER equatorial diameter ≈22.72 km (+1.90/−1.26 km) added | Radius is model-inferred, not a Newtonian surface-gravity measurement |
| Sagittarius A* | Existing mass retained; Schwarzschild-equivalent horizon diameter added | Nonrotating model scale; no photosphere or planetary density/rotation assigned |
| M31* | Same mass/horizon review | Central black hole only, not galaxy or nuclear cluster |
| TON 618 | Same mass/horizon review | Estimate-dependent mass, model horizon; not a universal largest-mass claim |

## Sources

- NASA planetary fact sheets and conventions: https://nssdc.gsfc.nasa.gov/planetary/planetfact.html and https://nssdc.gsfc.nasa.gov/planetary/factsheet/planetfact_notes.html .
- JPL satellite physical data and mean elements: https://ssd.jpl.nasa.gov/sats/phys_par/ and https://ssd.jpl.nasa.gov/sats/elem/ .
- Sedna thermal diameter: https://arxiv.org/abs/1204.0899 .
- NASA ISS facts: https://www.nasa.gov/international-space-station/space-station-facts-and-figures/ .
- NASA Voyager FAQ: https://science.nasa.gov/mission/voyager/frequently-asked-questions/ . Its approximate current masses are used instead of the conflicting dry/launch-mass tables on other mission pages.
- Alpha Centauri AB components and binary orbit: https://arxiv.org/html/2104.10086 , Table 8. The two radii are converted to diameters using 695700 km per solar radius.
- Polaris orbit and mass: https://arxiv.org/html/2407.09641 , replicated fit, Table 5. Existing reference mass/diameter are retained with their qualifications.
- Stephenson identification, membership and spectral classification: https://arxiv.org/html/1208.3282 and https://arxiv.org/html/1203.4727 . Follow-up https://arxiv.org/html/2008.01108 does not furnish a present-day mass for DFK 1. Internet claims of 12/40 solar masses could not be grounded in these primary studies.
- RX J1856 pulsations: https://arxiv.org/abs/astro-ph/0612501 .
- PSR J0437 timing: https://academic.oup.com/mnras/article/463/3/2612/2646601 . NICER radius: https://arxiv.org/abs/2407.06789 . Existing mass uncertainty remains the timing measurement in https://arxiv.org/abs/2407.07132 .
- Black-hole mass sources remain documented in DEEP_SKY.md. The horizon scale is derived as 4GM/c² for zero spin, without inventing a spin measurement.

## Persistence review

Settings already use private persistent preferences. Checked celestial objects and active object now use the same store with stable enum names. Missing saved selection means first-launch defaults; an explicitly empty selection remains empty. Unknown catalog IDs are ignored and an invalid active ID falls back to a selected object. Saving selection does not overwrite theme, language, units, date, time or number preferences.

Alpha Centauri component effective temperatures are derived using Stefan-Boltzmann scaling T=5772 K × (L/L☉ / (R/R☉)²)¼ and are shown with ≈, not as direct thermometry. IAU nominal conversion reference: https://arxiv.org/abs/1510.07674 .
