# Units and numeric-format audit — version 0.1.9

All measurement presentation is derived from saved preferences. Device-region
defaults are resolved before the interface-language override; choosing another
language cannot change the measurement system or numeric defaults. The System
radio remains System when automatic defaults are used. No preference reset or
migration is introduced in this release.

## Presentation inventory

| Quantity | Surfaces audited | Display rule |
| --- | --- | --- |
| Distance | Main object island, catalogue and object viewer, parent-distance bounds, horizon/diameter/component rows | One saved metric/imperial family; Moon/Earth-centre/satellite distances in km/mi, more distant Solar System ranges in Mkm/Mmi with AU, extrasolar ranges in ly only |
| Speed | Main object island, including orbital and outward probe speeds | km/s or mi/s from the distance/speed preference; signed outward speeds retain their sign |
| Physical length | GPS altitude/accuracy, spacecraft sizes/antennas, diameters and uncertainties | m/ft; large physical sizes in km/mi; both value and uncertainty convert |
| Gravity | Object viewer | m/s² or ft/s² from the length preference; the dimensionless comparison in standard Earth g is invariant |
| Mass | Object viewer, binary total and component rows | Selected kg/lb below 10 M☉; solar masses at or above the exact threshold; published uncertainties and qualifiers retained |
| Density | Object viewer | Independent mass and length choices combine as kg/m³, lb/m³, kg/ft³ or lb/ft³ |
| Pressure | Atmospheric/exosphere rows, gas-giant temperature-layer label and reference note | Independent bar/Pa/psi preference, including the converted 1-bar reference level; unavailable surface pressures remain absent |
| Temperature | Object viewer, day/night/range and binary component rows | Celsius/Fahrenheit affine conversion of every endpoint; stellar Kelvin reference retained |
| Coordinates | Environment details | Decimal degrees or DMS, selected digits and decimal symbols; DMS rounding carries to minutes/degrees |
| Angles | Main compass/object readouts, daily table, point island and balloon | Selected numeric format, including fractional values; degrees are the common angular unit |
| Fractions and durations | Phase readouts/accessibility, stellar luminosities, spin/orbital/binary periods | Selected numeric format; %, solar luminosity, hours/days and seconds/ms retain their physical meanings |
| Counts and zoom | Object-selection counter and viewer accessibility | Selected digit system; no grouping or decimal fraction |
| Date/time | Daily header/table, selected-point island, balloon and live-marker accessibility | Independent saved date order and resolved 12/24-hour choice; numeric digits come from the number preference/device default, AM/PM text from the interface language |
| Countdown | Selected-point island and balloon | Same number-preference digit policy; whole minutes, T−/T+ sign and elapsed duration independent of daylight-saving transitions |

Settings samples intentionally demonstrate each available standard. Catalogue
names, spectral classes, application versions, copyright/citation years, licence
identifiers, time-zone IDs/UTC offsets, scientific notation and chemical symbols
are identifiers or reference notation rather than convertible measurements.
Network payloads, hashes, orbital catalogues and mathematical models retain their
canonical machine/SI formats; conversion happens at the presentation boundary.
Legacy fixed-distance helpers are unused by current UI callers.

## Corrections

- Gravity now follows the selected metres/feet family while its g comparison
  remains derived from the original SI acceleration.
- The gas-giant 1-bar reference was formerly literal text. Both the temperature
  label and explanatory note now show the chosen pressure unit and number format
  (for example 100,000 Pa or 14.5 psi).
- Object count and zoom accessibility accept already formatted digit strings.
  Point-name resources now use a string placeholder directly in all twenty
  languages, instead of rewriting an integer placeholder at runtime.
- Daily-table, island, balloon and live-marker time presentation share the same
  date/time formatter. The app language no longer overrides numeric digit choices;
  localized AM/PM, time-zone conversion and repeated-hour offsets are preserved.
- Numeric readouts retain a stable direction within RTL pages, so negative
  angles keep their minus sign before the value. Translated labels/textual values
  retain their language direction.
- Speed and GPS accuracy use shared presentation helpers. Nonfinite display
  numbers become unavailable (—), never NaN/Infinity UI text.

## Conversion references and checks

Conversions use 1 ft = 0.3048 m, 1 mi = 1,609.344 m, 1 lb = 0.45359237 kg,
1 ft³ = 0.028316846592 m³, °F = °C × 9/5 + 32, 1 bar = 100,000 Pa, and
1 psi = 6,894.757293168361 Pa (pound-force per square inch with standard gravity
9.80665 m/s²). Unit references remain in [ATMOSPHERIC_PRESSURE.md](ATMOSPHERIC_PRESSURE.md)
and [APP_PRESENTATION.md](APP_PRESENTATION.md). The solar reference and display
threshold are documented in [DEEP_SKY.md](DEEP_SKY.md).

Fourteen new regression tests check independent reference values, signed speeds,
negative altitude/uncertainties, deliberately mixed saved units, every available
catalogue gravity/density/temperature, both endpoints and Kelvin references,
localized pressure placeholders, numeric grouping, all twenty app-language clock
choices, Arabic device digits versus explicit Latin formats, 12-hour AM/PM,
date order/year grouping, daylight-saving repeated hours, counts and unavailable
numbers. Existing tests cover near/far distances, DMS carry, mass thresholds and
uncertainties, all pressure scales and preference persistence. All 439 JVM tests
passed; debug/release builds, Android test-source compilation and release lint
(zero errors) succeeded. The Android instrumentation suite was not executed.

Device UI verification is recorded separately in [VERIFICATION.md](VERIFICATION.md).
