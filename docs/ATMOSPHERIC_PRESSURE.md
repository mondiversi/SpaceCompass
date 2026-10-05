# Atmospheric pressure reference values

Only atmosphere/exosphere pressure near a body's exterior is represented. These
are approximate reference values, not current telemetry, internal/core pressure,
photospheric pressure or a spacecraft cabin reading. A missing/invalid value omits
the row and its explanatory note entirely.

| Body | Original published reference | Stored Pa | Qualification |
| --- | --- | --- | --- |
| Venus | 92 bar | 9,200,000 | Approximate surface value |
| Mars | 6.36 mbar | 636 | Mean-radius reference; seasonally 4.0–8.7 mbar |
| Pluto | about 13 microbar | 1.3 | Fact-sheet reference; variable, not a present-day forecast |
| Mercury | less than about 5e-15 bar | 5e-10 | Approximate upper limit; exosphere |
| Moon | 3e-15 bar at night | 3e-10 | Night exosphere reference |
| Io | about 2 nbar SO₂ | 2e-4 | Approximate near-surface SO₂ pressure; patchy/variable |
| Europa | about 200 pbar O₂ | 2e-5 | Approximate O₂ exosphere pressure |

Venus/Mars/Pluto/Mercury/Moon values and definitions:
[NASA Venus](https://nssdc.gsfc.nasa.gov/planetary/factsheet/venusfact.html),
[NASA Mars](https://nssdc.gsfc.nasa.gov/planetary/factsheet/marsfact.html),
[NASA Pluto](https://nssdc.gsfc.nasa.gov/planetary/factsheet/plutofact.html),
[NASA Mercury](https://nssdc.gsfc.nasa.gov/planetary/factsheet/mercuryfact.html),
[NASA Moon](https://nssdc.gsfc.nasa.gov/planetary/factsheet/moonfact.html).
Io/Europa use Table 1 of [Bagenal & Dols 2020, The Space Environment of Io and Europa](https://agupubs.onlinelibrary.wiley.com/doi/10.1029/2019JA027485).
Constituent labels and qualifiers remain visible through unit conversion.

There is no assigned atmospheric surface pressure for Jupiter, Saturn, Uranus or
Neptune: their surface positions/pressures are not defined. Their existing 1-bar
temperature reference is a chosen atmospheric level, not a measured surface
pressure ([NASA definitions](https://nssdc.gsfc.nasa.gov/planetary/factsheet/planetfact_notes.html)).
Stars, black holes, neutron stars, spacecraft and Earth center have no applicable
external atmospheric surface-pressure entry. No atmosphere is assumed for the
unconfirmed TRAPPIST-1e case or for Sedna.

## Display units

The independent saved pressure choice is System, bar, pascal or psi. System uses
psi for US/LR/MM device regions and bar elsewhere, regardless of interface language
and length/mass overrides. Symbols remain bar, Pa and psi in every language.
1 bar is exactly 100,000 Pa. A psi is pound-force per square inch, calculated from
the exact international pound (0.45359237 kg), standard gravity (9.80665 m/s²) and
inch (0.0254 m), yielding approximately 6894.757293168361 Pa.
[NIST conversions](https://www.nist.gov/pml/owm/metric-si/unit-conversion/pressure-and-gas-flow-unit-conversions),
[NIST SP 811](https://www.nist.gov/pml/special-publication-811/nist-guide-si-appendix-b-conversion-factors/nist-guide-si-appendix-b9).

Scientific notation with signed superscript exponents keeps very small pressure
values meaningful in every unit. Numeric conventions follow the existing numeric
preference. The density preference still independently combines kg/lb and m³/ft³;
pressure conversion never uses the density or speed preferences.
