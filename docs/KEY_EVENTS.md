# Key events

The Observatory opens **Key events** immediately below **Key elevations**. Each page
uses the inspected object and the effective Location, including its geographic
position, observer altitude, local time zone and selected civil year (1900–2100).
Browsing another year does not change the Location. Selecting an event date uses
the same atomic `observeAt` operation as Key elevations: spatial overrides remain
unchanged, date/time are applied together, the clock rate becomes 0x, and obsolete
accelerated-clock checkpoints are removed.

## Supported events

- Sun: start/end of continuous polar daylight and darkness, their full durations,
  longest/shortest ordinary daylight periods, and earliest/latest local-clock
  sunrise/sunset. Polar periods crossing New Year retain both external endpoints
  for duration calculations. The page lists only event instants in the chosen year.
  A polar season replaces arbitrary choices among equally long/short days.
- Moon: four principal phases and geocentric perigees/apogees.
- Engine-supported planets: morning/evening maximum elongations for Mercury and
  Venus, oppositions for outer planets, perihelia/aphelia when they occur in the
  year, and minimum/maximum geocentric Earth distance over the selected year.
- Other moving bodies: annual Earth-distance extrema require continuous, suitably
  sampled full-year apparent geocentric ephemerides. The current short Horizons
  caches usually do not cover this interval; such results are explicitly unavailable.
- Annual satellite predictions are unavailable with short-lived TLEs. Fixed-sky
  and fictional references do not receive invented orbital events. Eclipses and
  visibility predictions for those references are outside this implementation.

Orbital phenomenon instants are universal; their displayed local date/time and
topocentric elevation use the Location. Distance extrema are centre-to-centre,
avoiding observer-rotation artefacts. Year-limited distance extrema can occur at
the year boundary and are not necessarily physical close-approach encounters.

## Horizon and search conventions

Solar events reuse the existing geometric Meeus/NOAA solar model and the daily
path's -0.833-degree centre-altitude ideal-horizon convention. Observer height
contributes parallax; the app does not invent a terrain horizon from GPS altitude.
Refraction and obstructions can change actual observed rise/set times. Clock
extrema use local civil time, including daylight-saving transitions; daylight
durations use elapsed time rather than clock subtraction.

The annual solar scan includes refined extrema before bracketing threshold roots,
so a near-tangent sunrise/set pair shorter than the coarse interval is retained.
Root brackets are narrowed to 250 ms; this numerical resolution does not imply
subsecond physical accuracy. Long/short daylight rows open the midpoint of their
daylight interval, while polar rows open the actual horizon crossing.

The existing Astronomy Engine 2.1.19 supplies lunar and planetary event searches.
Calculations run on `Dispatchers.Default`, support cancellation and use bounded
session caches. Cache keys include object, year, time zone, accepted observer and
relevant orbital data. Exact manual edits invalidate the cache; live GPS uses the
same reviewed 100 m / 50 m anchoring policy as Key elevations. A previous result
may remain visible during replacement, but its dates and export are disabled.

## Export

CSV and readable TXT freeze the accepted calculation and all rows before export.
They share the existing format choice, unit conversions, file naming and position
disclosure policy. Hidden position removes location metadata and uses UTC. Area
disclosure omits precise coordinates and altitude. Export returns to Key events.

## References

- [USNO rise, set and twilight definitions](https://aa.usno.navy.mil/faq/RST_defs)
- [USNO sunrise/sunset extrema and solstices](https://aa.usno.navy.mil/faq/rs_solstices)
- [NOAA solar calculations](https://gml.noaa.gov/grad/solcalc/calcdetails.html)
- [Astronomy Engine Kotlin API](https://github.com/cosinekitty/astronomy/blob/master/source/kotlin/README.md)
