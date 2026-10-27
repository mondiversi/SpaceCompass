# Observatory travel-time comparison

The interactive island follows Spatial location in the Observatory. Its state is separate
from scenario clocks, ephemerides and exports. The last speed is stored by a stable key,
shared across objects. A missing or obsolete key defaults to walking at 5 km/h.

The 17 equal slider steps are nonlinear in speed: walking (5 km/h), bicycle (20 km/h),
car (100 km/h), aircraft (900 km/h), rocket (28,000 km/h), fast probe (200,000 km/h),
0.01 c, 0.1 c, c, and warp 2, 3, 5, 7, 9, 9.9, 9.99 and 9.999. Ordinary vehicle speeds are
illustrative comparison choices, not claimed vehicle performance or launch trajectories.
The slider changes the estimate live and does not advance the scenario clock.

## Calculation and limits

- Duration is the current observer-to-object distance divided by the chosen constant
  speed, using kilometres and seconds internally. Observer distance is reused from the
  existing calculation, including the active scenario; no independent network query is made.
- The calculation refers to the target's represented centre/reference position, not a
  landing site, surface clearance or travel along an actual navigable route. It freezes
  the distance for each estimate and does not solve interception of a moving object.
- Accelerating, braking, gravity, cosmic expansion, propulsion and route restrictions are
  outside this comparison. Time is in the observer's reference frame, not proper time
  aboard a relativistic vehicle. At/above light speed it is a mathematical comparison,
  not a physically achievable journey for a massive traveller.
- Missing, nonfinite, negative and unmeasured direction-only ranges remain unavailable.
  Fictional targets may use an explicitly qualified estimate from their named real reference,
  marked with ≈ in every distance presentation. This does not create measured fictional orbits.
  The reference name and provenance also appear in the Observatory and its table exports.
- Duration scales from nanoseconds through years. A year is 365.25 days (31,557,600 s).
  Large year counts use compact scientific notation; no integer timestamp arithmetic is
  used, so remote targets cannot overflow a calendar or a Long-valued duration.

## Units and language

Speed follows the user's distance/speed family: km/h or mph at ordinary speeds, km/s or
mi/s at relativistic/warp speeds. All calculations retain their original km/s values.
Distance uses the existing display policy. Number separators and digits follow the numeric
preference/device locale; duration unit names and plural forms follow the app language.
Android ICU MeasureFormat (API 24+, below this app's API 26 minimum) supplies the translated
duration units and floating-point plural rules. No additional library or graphical asset
is downloaded or bundled for this feature.

## Sources

- Exact light speed: 299,792,458 m/s, [NIST CODATA](https://physics.nist.gov/cuu/Constants/Value/c.html).
- Fictional TNG-era comparison factors: warp 2 = about 10 c; 3 = 39 c; 5 = 214 c;
  7 = 656 c; 9 = 1,516 c; 9.9 = 3,053 c; 9.99 = 7,912 c. These are rounded reference
  values, not an asserted physical formula or an interpolation of a real warp drive.
  Source: *Star Trek Writers' Technical Manual, Fourth Season Edition*, p. 8,
  [reference scan](https://engineering.thetafleet.net/Journals/TNG/Franchise%20-%20Star%20Trek%20TNG%20Writer%27s%20Technical%20Manual%20%28fourth%20season%29.pdf).
- The additional warp 9.999 comparison uses approximately 200,000 c. *The Star Trek Book*
  (DK, 2016), p. 266, lists about 214 trillion km/h for this factor. This is deliberately a
  rounded comparison: publications assign different near-10 factors, so no universal
  canonical conversion or physical warp formula is claimed. Book metadata:
  https://books.google.com/books?id=RUoxDAAAQBAJ
- [Android MeasureFormat](https://developer.android.com/reference/android/icu/text/MeasureFormat).

Only ordinary app strings are added, in all twenty supported languages. Graphics pack
versions, existing downloads, scientific catalogue entries and published releases are unchanged.
