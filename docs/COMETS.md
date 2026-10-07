# Comets and catalogue ordering

## Catalogue additions

The catalogue contains 31 objects, including **1P/Halley** and
**67P/Churyumov–Gerasimenko**, after Sedna and before the Voyagers in default order.
They have a distinct Comets type in the existing multiselect filter. Existing
object IDs, checked selection, active object, filter and ordering preferences
retain their stored names. The filter and additional ordering choices use the
same persistent preference store; opening a menu does not save a new selection.

Pointing, observer/Sun distance, heliocentric speed and the daily sky path use
[NASA/JPL Horizons](https://ssd-api.jpl.nasa.gov/doc/horizons.html). The targets are
`DES=1P;NOFRAG;CAP;` and `DES=67P;NOFRAG;CAP;`. The
[Horizons manual](https://ssd.jpl.nasa.gov/horizons/manual.html) documents CAP as
the closest previous apparition; numeric solution record IDs are not hardcoded.
Requests retain the existing strict Earth-/Sun-centred ICRF vector formats,
apparent light-time/aberration correction for pointing, geometric vectors for
motion, serial fetch policy, HTTPS validation and independent cache files.
Unknown remote states remain unavailable rather than being guessed.

The nucleus/tail graphic is an illustrative symbol, not an observed shape,
present-day activity, spin-axis reconstruction or live telescope image. This is
stated in all twenty languages. It is shared by the catalogue thumbnail and
viewer; no external image or model is downloaded.

## Physical references

* Halley: maximum nucleus extent about 15 km, from
  [NASA's dimensions](https://science.nasa.gov/solar-system/comets/1p-halley/).
  It is a dimension, not a spherical diameter. Reference revolution period
  76.1 years follows the
  [NASA comet fact sheet](https://nssdc.gsfc.nasa.gov/planetary/factsheet/cometfact.html).
  No unsupported individual mass, density or temperature is assigned.
* 67P: approximate mass 10 billion tonnes (10^13 kg), density 470 kg/m³,
  12.4-hour spin and 6.5-year revolution period, from
  [ESA's Rosetta reference measurements](https://www.esa.int/Science_Exploration/Space_Science/Rosetta/Getting_to_know_Rosetta_s_comet).
  These describe the early Rosetta observations, not current telemetry. The
  published 4.1 km dimension belongs to its larger lobe and is not used as a
  whole-object diameter. Activity and spin can change.
* Neither comet receives an invented atmospheric surface pressure, surface
  gravity, daytime/nighttime temperature or stellar mass. Existing formatting
  and selected units apply to the available physical quantities.

The checked-in fixtures contain real Earth-apparent and Sun-geometric Horizons
responses for both targets. Tests verify exact identities, correction/reference
frames, interpolation, finite topocentric azimuth/elevation, distance and speed,
and a full day's changing sky position. Wrong-comet and mixed-frame responses
are rejected.

## Compact ordering menu

Default is the first option. Each subsequent row pairs its criterion with
adjacent upward/ascending and downward/descending radio controls, preserving
48 dp touch targets, selected semantics, bounded scrolling and tablet scaling.
Criteria are alphabetical name, Sun distance, mass, diameter, atmospheric
pressure, surface gravity, daytime temperature and nighttime temperature.

Sorting compares physical numeric values before display formatting: ordinary
and solar masses share kilograms, lengths use kilometres, pressure uses pascals,
gravity uses m/s² and temperatures use signed Celsius values. Name order uses
the selected locale's Collator. Missing values stay last in both directions;
equal values retain default catalogue order. Unknown dimensions and black-hole
model horizon scales do not silently become physical diameters. The generic
atmosphere-temperature sort is removed; old saved names fall back to Default
without changing type/visibility filters. The atmosphere references remain in
object details. Daytime/nighttime sorts use their corresponding published
maxima/minima, with the physical layer identified in the details. Surface means,
temperature ranges and stellar effective/photosphere temperatures are not
substituted for these different quantities.

See [VERIFICATION.md](VERIFICATION.md) for local build and device evidence.
