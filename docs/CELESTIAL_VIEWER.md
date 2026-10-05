# Celestial object viewer

The standalone celestial compass offers a telescope **View** control and the
existing curved daily-path icon together beside the inspected object's island title. The same compact
control geometry is used by Current face/Rotation in the viewer. The viewer
is a separate, read-only screen; Back returns to the compass without clearing its
path selection. It does not connect to UVIR hardware or share measurement
archives, associations or preferences with UVIR.

## Rendering and interaction

* The initial compass selection checks Sun and Moon, with Sun inspected in the
  data island. This is an initializer, not a refresh rule: user changes and
  restored screen state are retained during recomposition and rotation.
* Portrait: model and facts occupy two vertically stacked halves. Landscape:
  equal left/right halves. Facts have bounded scrolling and themed text.
  The viewer's image starts directly below the toolbar in either orientation;
  landscape facts start at the same height without an added top gap. Its 10 dp
  side insets, 6 dp bottom inset, portrait image/facts gap and internal fact padding
  are unchanged. The compass's full-height landscape data column still uses
  10 dp on both its toolbar/sky side and its outer side; its minimum-width
  fallback accounts for both insets. Internal island paddings are unchanged.
  Eye/current-face and circular-arrow/rotation icons sit at the image's top-right,
  with selected fills, accessible labels and long-press tooltips. Both the sky and viewer
  controls share a neutral dark-grey fill, an always-present 1 dp white
  outline, white symbols and the same subtle fill lightening while pressed. The
  telescope and trajectory actions keep exactly the same fill even with the
  curve visible. Only the mutually exclusive viewer tabs lighten when selected.
  Selection never adds a different outline or changes the size.
  Press feedback uses the same interaction source as the real action and stays inside
  the 30 dp visible circle, independently of the 48 dp touch target.
  The smaller explanatory note overlays the image. Reference
  rows start directly without an Information heading. Compact controls retain
  48 dp touch targets separately from their 30 dp visible circles. Icons are 18 dp,
  except the telescope's 22 dp canvas compensates for its inset tube/tripod geometry;
  its stroke is still 1.5 dp. Spacing and the 4 dp top/right inset match both screens.
  Direction and inclination use their normal localized labels in the environment
  table, before altitude; no pointing readout overlays the sky.
  Time captions and the offscreen locator no longer reserve room for sky actions;
  both actions now live in the body-data island.
  Observer distance (Mkm · AU on one line), reference-frame speed and a shared
  azimuth–elevation label with its paired values on the right forms one translucent tabular island. Its centered body
  name sits inside a normal header above a divider, between previous/next arrows
  and beside the telescope/path controls, including in RTL locales. Header padding is
  4 dp vertically (3 dp compact), while all action touch targets remain 48 dp. Distance and
  speed use label/value rows with aligned right edges; the two distance units remain
  on the same line beside the label at normal font sizes. Accessibility fonts retain
  a wrapping/scrolling fallback instead of shrinking indefinitely.
  Arrows cycle checked objects with wraparound, but no arrows appear in single-body
  mode. Weather, compass and GPS form
  another with the same surface style and an 8 dp gap. This grouping is identical
  in portrait and landscape; the selected-point panel stays above the islands in
  landscape. Scrolling is retained only as an accessibility/small-window fallback.
  The entire weather/orientation/altitude text block is vertically centered in the
  environment island. An underlined localized Details action is centered below the
  larger compass (80 dp, 68 dp compact), freeing the former right-hand action column
  while retaining a 48 dp touch target. Coordinates are no longer shown. Altitude
  includes its available GPS vertical uncertainty in parentheses, e.g. 112 m (±8 m).
  Missing uncertainty is omitted rather than substituted with horizontal accuracy. The contextual popup is
  titled Orientation and environment and retains the available horizontal and
  vertical GPS uncertainties, plus pointing/GPS/weather explanations in all locales.
  Unreliable magnetic-heading warnings overlay the reticle
  centrally instead of growing the data panel; the contextual popup no longer
  includes the solar viewing warning. Trajectory
  captions include the pointed moment's time, azimuth and elevation without
  repeating the object name; redundant below-horizon messages are omitted.
* Current face: topocentric telescope-style upright view, calculated in UTC using
  Astronomy Engine 2.1.19. Its EQJ vectors and IAU pole/prime-meridian rotation
  model provide phase, visible hemisphere and lunar libration. The pole is
  evaluated at light-emission time. Local zenith defines screen-up. No magnetic
  compass readings or time-zone offsets are used by this view.
* Rotation: accelerated illustration, not real-time planetary rotation. Drag
  stops it and sets an unrestricted quaternion orientation. Screen-space drags
  follow the finger in both axes, including full vertical turns across the poles.
  A single tap while manually paused, or a double tap, returns that manual
  orientation to its original axis along the shortest quaternion arc over 450 ms,
  then resumes spinning, preserving spin phase and zoom. Smoothstep easing avoids
  a sudden start/end; dragging interrupts the return immediately. System-disabled
  animations skip the transition. No Euler pitch clamp or gimbal lock.
  Retrograde planets turn in the opposite direction. State survives tab/window
  changes. Animation updates are capped at 30 Hz and stop on pause/background.
* Both tabs support a two-finger pinch from 0.75× to 3×, with bounded panning and
  a finger-centred zoom. One finger still rotates the model in Rotation; in
  Current face it pans an enlarged image. A double tap resets zoom in Current
  face, or restores the axis/resumes without changing zoom in Rotation. Zoom is shared
  between tabs and survives resizing. Accessible zoom/reset actions are supplied.
* Planets are GPU-shaded textured spheres. Saturn has a geometric ring model,
  including front/back occlusion and the Cassini gap; ring textures/shadows and
  changing cloud formations are not observational reconstructions.
* Surface maps are reference composites, not telescope photographs or live
  weather/cloud imagery. Do not claim resolved markings are present-day images.
* Sedna's surface and pole are unknown, so its reddish sphere is illustrative in
  either tab. The ISS/Voyager meshes are recognisable structural illustrations,
  not live attitude solutions. Both limitations are stated in the UI.
* Without usable GPS the current face of a physical body is unavailable;
  rotation and reference facts still work. Unknown/non-applicable fields use —.
* A sphere is one GLES2 quad and a locally cached map up to 2048×1024, loaded on the GL
  thread. No map downloads or CPU bitmap allocation per frame. Surface context
  follows the foreground lifecycle. GPU mipmaps smooth reduced views; zoom only
  updates projection uniforms, never rebuilds/downloads textures. Texture size
  also respects device limits. Spacecraft use small depth-sorted vector meshes.
* The screen title is Space Compass in every language, independent of the active object.
  In landscape, equal-width sky/data columns place the toolbar above the sky only;
  the data islands use the full right-column height. Portrait retains its full-width toolbar.
  A neutral ringed-planet action at the toolbar's end opens a bounded-width,
  scrollable catalogue. Its width derives from the window, not the narrow icon anchor.
  Checkboxes select any set of visible trajectories; the tri-state master selects all
  from an empty/partial set and clears all from a full set. The menu stays open
  while choosing multiple objects. No selection is valid: the body-data island
  and its actions disappear, while orientation and environment details remain available.
  Weather text is shown only in the environment-details dialog; the backdrop still
  uses the existing weather data. The Details link sits below altitude at the right
  edge of the environment island, not under the compass.
  Scalar and paired-angle body rows share one table and identical vertical insets.
  The compass column reserves a little more space on its trailing side; the values
  and Details column is vertically centred on it, with the same outer insets as
  body data. Details keeps a 48 dp hit target inside the data column without a
  visually empty extra row. Its dialog presents local GPS coordinates (DMS, N/S/E/W),
  horizontal coordinate accuracy, GPS altitude/vertical uncertainty, then weather
  before the explanatory notes. No precise coordinates are added to requests/logs.
  All checked bodies render their available apparent paths and known live positions.
  Cached catalogue observations are filtered immediately, before constructing the
  viewport's projections, thumbnails and interactive targets. Unchecked objects
  never remain on screen while background observations/paths are refreshing.
  Only the inspected body receives an edge-direction miniature when outside the viewport;
  switching the inspected body replaces that locator without changing checked paths.
  An empty selection has no edge locator, and unavailable ephemerides/location do not produce fabricated directions.
  Explicit object-specific colours and matching miniature outlines identify paths
  consistently for single and multiple selections; the Sun is always yellow.
  Below-horizon segments are dashed and subdued without losing the object's hue;
  these are apparent sky crossings, not physical orbit intersection claims.
  Telescope and path list concern the inspected body; its arrows never change the checked
  set. All visible curves share one nearest-point focus/tap detector, retaining each point's
  body, timestamp and regular ordinal or named event. Both the reticle caption and tapped
  panel use two-column azimuth/elevation rows with values aligned at the right edge.
  Each floating caption starts with the focused point's translated body name;
  tapped panels prefix that name to the point/event/current label and time in normal
  weight. This also identifies another checked body's point when it is not the
  inspected object. The daily-path list itself remains free of repeated body names.
  Tapping another checked live miniature inspects it. The path list has no visibility
  checkbox: selected trajectories remain visible, and its rows only select a time/point.
  Paths are cached per object/date/GPS cell and computed off the UI thread;
  switching the inspected object does not invalidate other paths. ISS retains its
  minute path cache and 500 ms live clock even when its path is unchecked. Other catalogue
  positions update every two seconds locally. Catalogue positions request orbital updates,
  but velocity requests are restricted to checked bodies and checked bodies refresh first;
  one serial queue and the existing two-hour caches/backoff prevent JPL
  request overlap. No new sensor/database/archive format is introduced.
  Each row has a cached miniature reused by the live and offscreen locators, name, coarse heliocentric
  distance and checkbox. Below 10,000 AU the catalogue uses AU; at or above
  that threshold it converts to light-years (ly), with one decimal and the configured
  numeric locale. Polaris therefore displays about 446.5 ly. Raw distances and the
  compass/viewer distance fields are unchanged. The heading remains unit-neutral.
  Distance text uses a secondary neutral shade derived from
  the themed text colour and an 8 dp gap before the radio button. Thumbnail sphere maps decode at most 512 px wide and
  are projected once into 64 px cached images, never per orientation frame.
  Spacecraft reuse the viewer mesh with a circular thumbnail crop, keeping the
  full-size model viewer unchanged. Order is explicitly stable: Sun, Mercury,
  Venus, ISS, Starlink V3 (40083), Moon, Mars, Jupiter, Io, Europa, Saturn, Uranus, Neptune, Pluto,
  Sedna, Voyager 1, Voyager 2, Polaris. ISS/Moon share Earth's approximate solar
  distance, not their much shorter range from the observer. Missing/expired
  remote spacecraft or Sedna ephemerides show —, not a fabricated distance.
* The live sky locator uses that same 32 dp reference miniature instead of the
  uniform dot. The Moon's live and edge miniatures mask the cached texture with the
  same continuous phase silhouette as the daily-path phase icons (waxing/waning
  side and illuminated fraction). Both share one UTC phase refreshed off the UI
  thread once per minute, independently of compass frames and the inspected body.
  It is the conventional upright phase diagram, not the parallactic orientation of
  a telescope view. No extra textures are decoded/cached per phase. Catalogue
  previews stay static, and hourly/event list icons retain their own moment's phase.
  Below the geometric horizon it is desaturated and subdued, but
  remains visible through the virtual ground. The 32 dp circular touch radius
  selects the exact current time/azimuth/elevation, not the nearest hourly time.
  Overlapping targets still prefer the nearest point; the live instant wins an
  exact tie. A selection freezes the tapped instant while the live preview keeps
  moving. Previous/next step to the surrounding trajectory markers. The same
  selection works when a selected object's curve is unavailable and for Voyager targets,
  without inventing a spacecraft orbit. TalkBack offers a labelled current-point
  action, and absolute placement preserves the projection in RTL languages.
* Outside the viewport, the same upright 32 dp miniature replaces the old arrow.
  A coloured tip rotates toward the original projected direction, independently
  of the miniature. The entire indicator stays inside the sky and moves clear of
  the top-corner controls and visible miniatures, including in RTL. Only the inspected
  object is passed to edge placement, so unrelated catalogue positions do not crowd
  the perimeter or shrink its indicator. Below-horizon miniatures remain muted.
  This indicator is not a selectable trajectory point and does not modify heading.

## Reference values

Surface gravity is displayed both in m/s² and in standard Earth gravities (g),
using exactly 1 g = 9.80665 m/s². The comparison is derived from the same reference
acceleration, not an additional measurement. Up to three decimals in g preserve
small values such as Polaris (~0.067 g); unavailable quantities still use —.
Source: https://goldbook.iupac.org/terms/view/S05905

The viewer's observer-distance row uses two lines for every object: millions of
kilometres (Mkm), with at most two decimal places, followed by AU with at most four.
Both follow the configured numeric locale. Positive ranges smaller than 0.01 Mkm
or 0.0001 AU show a less-than bound rather than a misleading zero. Missing or
invalid ranges show only —. The compass uses one line, Mkm · AU. Its small/live
distances retain six Mkm decimals for ISS/Voyager and four for the Moon, so live
range updates are not lost to coarse rounding; other bodies use two. AU remains
limited to four decimals. Missing speed/range values stay a bare —, not zero.

### Polaris

The selector includes Polaris after the spacecraft. Its apparent daily path is
caused by Earth's sidereal rotation, not an orbit around Earth. The immutable
ICRS/J2000 vector uses SIMBAD RA 02h31m49.09456s, Dec +89°15′50.7923″ and tangent
proper motion (pmRA*cosDec 44.48, pmDec -11.85 mas/year). Earth/site parallax and
first-order annual aberration precede Astronomy Engine's EQJ-to-horizontal
rotation (precession/nutation/sidereal time). No shared mutable engine star slot
is used by production code. Angular precision is adequate for phone pointing,
not an astrometric or occultation solution. No atmospheric refraction is applied,
matching the virtual geometric horizon.

Reference: https://simbad.cds.unistra.fr/simbad/sim-id?Ident=Polaris
Evans et al. (2024): https://arxiv.org/abs/2407.09641
Adopted distance 136.90±0.34 pc from Gaia DR3 Polaris B, displayed in light-years
(ly) and AU. Polaris Aa mean radius 46.27±0.42 solar radii and mass 5.13 solar
masses provide reference diameter/mass, not combined-system quantities. Assuming
a spherical star, mean Newtonian surface gravity is GM/R² (~0.657 m/s²) and bulk
density is M/(4πR³/3) (~0.073 kg/m³), with G=6.67430×10⁻¹¹ SI. They are explicitly
qualified as derived approximations, not time-varying measurements of this pulsating star.
Unknown rotation axis/period and solar orbital speed are not fabricated. The
viewer explicitly labels its warm stellar disk as illustrative, not a resolved
photograph, in both modes. Scalar distance is representative per civil day.

### Solar-system objects

#### Reference temperatures

The viewer adds themed, localized temperature rows after density. These are
static reference facts, not live temperatures, a thermal map, or a result derived
from the Moon's current phase or the observer's location. All non-stellar-effective
values use rounded degrees Celsius with an approximation sign; stellar effective
temperature uses kelvin (K, never °K). Day/night rows explicitly mean local sunlit
maximum and night minimum, not uniform temperatures of two hemispheres. Other
objects use a documented mean or range instead of fabricated day/night values.
The common note explains reference/extreme values and spatial/time variation.
The existing bounded facts scroll handles longer translations and larger fonts.

* Mercury: sunlit maximum 430 °C, night minimum −180 °C.
  https://science.nasa.gov/mercury/facts/
* Moon: full-sun maximum 127 °C, night minimum −173 °C. These reference
  values do not represent the colder permanently shadowed polar craters.
  https://science.nasa.gov/moon/facts/
* Venus: mean surface 464 °C; Mars: mean −65 °C; Pluto: mean −225 °C.
  Jupiter −110 °C, Saturn −140 °C, Uranus −195 °C, Neptune −200 °C are
  atmospheric reference temperatures **at 1 bar**, not solid-surface values.
  https://science.nasa.gov/resource/solar-system-temperatures/
* Mars additionally lists the overall reference range −153 to 20 °C, not
  a pair of global daytime/nighttime temperatures.
  https://science.nasa.gov/mars/facts/
* Europa: surface reference range −223 to −133 °C; not a day/night split.
  https://science.nasa.gov/blogs/europa-clipper/2024/10/14/nasas-europa-clipper-mission-investigating-an-ocean-world-2/
* Io: mean surface −155 °C from the NASA small-worlds table, not the much
  hotter local volcanic/lava temperatures.
  https://nssdc.gsfc.nasa.gov/planetary/factsheet/galileanfact_table.html
* Sun: approximate photosphere temperature 5,500 °C, not the core or corona.
  https://science.nasa.gov/sun/facts/
* Polaris Aa: mean effective temperature 6,017 K in the cited 2015 spectra;
  a historical reference for the Cepheid component, not live temperature or
  the unresolved triple system as a whole (Usenko et al., 2016).
  https://arxiv.org/abs/1610.03813
* Sedna and ISS/Starlink/Voyager spacecraft: Temperature —. We do not invent
  a measured Sedna temperature or a single hull temperature without telemetry.

#### Other physical reference facts

NASA Planetary Fact Sheet (updated 18 March 2025):
https://nssdc.gsfc.nasa.gov/planetary/factsheet/
https://nssdc.gsfc.nasa.gov/planetary/factsheet/planetfact_notes.html

Equatorial diameters/gravity and sidereal rotation periods follow the sheet.
Negative rotation denotes retrograde. Giant-planet gravity is at the 1-bar level,
not a solid surface. Planet distances refer to the Sun; lunar apsides refer to
Earth; Io/Europa apsides refer to Jupiter. Values are indicative rather than a fresh apsis prediction.

Io and Europa: local Astronomy Engine jovicentric state vectors are added to
Jupiter's apparent EQJ vector after backdating by its light travel time, following
the official Jupiter-moons example. Observer/site parallax then gives horizontal
pointing and the whole apparent civil-day path. No GPS is sent to an orbital
service. Actual visible surface geometry uses local zenith and NASA/NAIF IAU pole,
prime-meridian and J3..J7 nutation terms at emission time. Speeds explicitly refer
to Jupiter, not the Sun. Moon speed comes from the jovicentric velocity vector.
JPL mean radii/GM/density provide reference mass and spherical gravity. Mean
orbital sizes/eccentricities provide indicative apsides; synchronously locked
sidereal periods are derived from IAU prime-meridian rates, not the distinct
anomalistic periods in the orbital-element table.

https://github.com/cosinekitty/astronomy/blob/master/demo/kotlin/src/main/kotlin/JupiterMoons.kt
https://naif.jpl.nasa.gov/pub/naif/generic_kernels/pck/pck00011.tpc
https://ssd.jpl.nasa.gov/sats/phys_par/
https://ssd.jpl.nasa.gov/sats/elem/

Sun: https://nssdc.gsfc.nasa.gov/planetary/factsheet/sunfact.html
609.12 h is the adopted reference period; actual rotation varies with latitude.
No galactic orbital period is invented in the parent-body rows.

Sedna: https://arxiv.org/abs/1204.0899 (Pál et al., 2012: 995±80 km).
https://ssd-api.jpl.nasa.gov/sbdb.api?sstr=90377&phys-par=1
SBDB reference snapshot 2026-10-04: q=76.2 au, Q=1010 au, period=4.63e6 d,
rotation=10.273 h. The rotation is an uncertain synodic photometric estimate,
not an IAU attitude solution. Mass, gravity and density remain unknown.

ISS: https://www.nasa.gov/international-space-station/space-station-facts-and-figures/
109 m maximum span and 419725 kg reference mass; visiting vehicles/configuration
change both. Axial period, surface gravity and fixed orbital apsides remain —.

Voyager: https://science.nasa.gov/mission/voyager/frequently-asked-questions/
3.7 m is the antenna diameter, labelled as such rather than overall spacecraft
size. Launch mass is not misrepresented as current mass. No orbital/rotation
periods are invented for outbound probes.

Geometry reference: https://github.com/cosinekitty/astronomy/blob/master/source/kotlin/doc/-axis-info/index.md

## Maps and attribution

Sun, Mercury, Venus **atmosphere**, Mars, Jupiter, Saturn, Uranus, Neptune:
Solar System Scope / INOVE, https://www.solarsystemscope.com/textures/,
**Creative Commons Attribution 4.0 International**,
https://creativecommons.org/licenses/by/4.0/.
Original direct downloads were unavailable; byte-for-byte public copies were
obtained from https://github.com/TanvirAhmedArnab/SolarSystem/tree/main/SourceAssets/ThirdParty/Textures/SolarSystemScope
with the original asset credits/license retained. Do not use the radar-derived
Venus surface map as its optical telescope appearance.

Moon: NASA/GSFC/Arizona State University, CGI Moon Kit, centred on 0° longitude:
https://svs.gsfc.nasa.gov/4720/
https://svs.gsfc.nasa.gov/vis/a000000/a004700/a004720/lroc_color_2k.jpg

Io/Europa: NASA/JPL/USGS Voyager/Galileo SSI global reference mosaics, native
512×256 browse images (grayscale; no invented colour). The maps' geographic
longitude labels are PositiveWest, but ISIS Simple Cylindrical raster X increases
eastward. Io uses a -180..180 domain/zero-centred map; Europa uses 0..360 and
its prime meridian lies at the seam. Both viewer and miniatures share these
offsets so Europa's visible hemisphere is not rotated by 180 degrees.
https://astrogeology.usgs.gov/search/map/io_voyager_galileo_ssi_global_mosaic_1km
https://astrogeology.usgs.gov/search/map/europa_voyager_galileo_ssi_global_mosaic_500m
https://github.com/DOI-USGS/ISIS3/blob/dev/isis/src/base/objs/SimpleCylindrical/SimpleCylindrical.cpp

Pluto: NASA/JHUAPL/SwRI, New Horizons Ralph/MVIC global colour mosaic:
https://science.nasa.gov/resource/pluto-global-color-map/
https://assets.science.nasa.gov/content/dam/science/psd/solar/2023/09/p/l/pluto_color_mapmosaic.jpg

NASA source credits are retained; these maps do not imply endorsement. All maps
are capped at 2048×1024 for display and illumination is applied by UVIR.
Unmapped black areas in the Pluto/Europa mosaics are rendered as neutral unknown terrain,
not interpreted as a physical black region on the dwarf planet.
Texture attribution is also visible in the viewer and in the asset license file.

## Verification boundary

The catalogue toolbar uses the same outlined count bubble as saved acquisitions,
with the number of checked objects (including those awaiting ephemerides). It
updates while the menu stays open, hides for an empty selection, and does not
change when the inspected object is switched. The selector exposes the count to
accessibility without creating a second focusable action.

The body-data island retains 12 dp horizontal insets and 8/10 dp vertical insets
around its data in compact/regular layouts. Only its title/action row uses the
smaller 1 dp vertical padding; navigation and action touch targets are unchanged.

Trajectory labels use the localized rise, culmination, set and minimum event names for
all bodies, including the Sun. Only regular samples are numbered chronologically
(Point 1, Point 2, etc.); named events do not consume an ordinal. The list, reticle
caption and selected-point panel share the same names. A tapped live snapshot is
labelled Current position, even when it coincides with a numbered sample.
Reticle ties at exactly coincident positions prefer the named event over a
regular marker; distinct points still select the closest dot. Every named event
uses the same floating time/azimuth/elevation caption as regular markers.
Civil days shortened/extended by DST keep their actual 23/25 hourly points; ISS keeps
its 24 samples per orbit. This naming does not alter any celestial calculations.

Small chevrons overlay the curve after every fourth regular marker, halfway toward
the next marker (or the existing final sample). Named events do not consume this
cadence. Their direction follows increasing sample time, including below the
horizon, where they use the same subdued path colour. The tangent comes from the
existing dense projected segment; anchors are cached with the path and require
no new ephemeris requests. Arrows are omitted behind the phone, at clipped edges,
on stationary projections or near selectable dots/live miniatures. They do not
add touch targets, change point numbering, or join the path endpoints artificially.

The minimum is the lowest geometric elevation in the displayed civil day, or in
the displayed complete ISS revolution, not necessarily midnight or a horizon
crossing. The existing ephemeris is evaluated around every sampled local extremum
and at both interval boundaries; both maximum and minimum use this refinement.
Polar days may have an above-horizon minimum, and polar nights may lack rise/set:
no horizon event is invented. Ordinary geometric-altitude conventions are unchanged
(https://aa.usno.navy.mil/faq/alt_az).
Named events are combined only when within 500 ms and 0.002 degrees of each other
in actual sky direction. A combined minimum uses the minus symbol and all its
localized event names; regular numbered points are retained. Matching projected
pixels alone never merge events at different times.

JVM tests cover right-handed frames, lunar near-side longitude/phase, observer
location, UTC/time-zone independence, rotation drag/resume, retrograde motion,
reference nulls and meshes, plus zoom bounds, invalid gestures, pinch anchoring,
panning limits and texture/decode sizing. Zoom accessibility strings are supplied
in all 20 locales. Android UI tests are compiled separately. Never run
the connected test suite on a user's phone: installing a test build can remove
the personal application. Only matching-signed `adb install -r` is permitted.

## Starlink V3 locator

The catalogue includes **Starlink V3 · 40083**, immediately after ISS. This is
STARLINK-40083, NORAD **100855**, COSPAR **2026-225A**, a member of the first
operational V3 launch (Starship Flight 14, 28 September 2026). Its catalogue
designation does not establish the physical order in which spacecraft deployed.

Sources:

- SpaceX mission: https://www.spacex.com/launches/starship-flight-14
- CelesTrak OMM-compatible GP CSV:
  https://celestrak.org/NORAD/elements/gp.php?CATNR=100855&FORMAT=CSV
- Format and caching requirements:
  https://celestrak.org/NORAD/documentation/gp-data-formats.php

New six-digit NORAD numbers cannot use the legacy five-digit TLE format.
The app parses the named CelesTrak CSV fields directly into the existing Vallado
SGP4 implementation, with UTC epoch, Earth centre and TEME frame. It validates
name, numeric catalogue ID and international designation before replacing the
atomic disk cache. No fictitious orbit or ISS-derived fallback is supplied.
Default CelesTrak metadata (EARTH/TEME/UTC/SGP4) is accepted when omitted; explicit
conflicting metadata, invalid rows, non-finite elements and wrong identities fail.
Requests contain no observer location or personal device/sensor information.
HTTPS certificate and hostname validation are unchanged.

Live pointing/range/velocity update every half-second. The orbit uses its actual
mean-motion period, 24 regular markers and a smooth ten-second sampled curve,
including below the horizon. The current/next geometric pass is separate from
the full revolution; absence of a pass does not remove its below-horizon path.
All normal selection, edge locators, touch captions and multi-object controls
apply. The distinct cyan path does not change ISS's green path.

Disk data refresh no more often than every two hours; transient CelesTrak failures
also wait two hours. HTTP 403/404 requests are not repeated in the same view
session. Valid cached data survives failed updates. Elements older than 24 hours
show an accuracy warning, and older than 72 hours (or over six hours in the
future) do not drive pointing or trajectories. Predictions are not guarantees
of optical visibility and cannot account for unreported manoeuvres.

The spacecraft preview is explicitly illustrative, not a measured engineering
model or live attitude. Unknown satellite mass and dimensions remain em dashes.
The named spacecraft is consistent across locales; its warning and identification
note are supplied in all 20 languages. The checked-in public CSV is a deterministic
test fixture only, never a bundled live-data fallback.


## Starlink public-provider fallback

The identified STARLINK-40083 / NORAD 100855 / 2026-225A retains CelesTrak OMM as its initial source. On failure, the app reads the public copyable TLE block from https://www.satcat.com/sats/100855 (Space-Track source, as attributed there). No account, authenticated API, executable scripts or private endpoints are used. The complete Alpha-5 ID A0855 maps to 100855; the name, launch identifier, both checksums and element ranges are verified. UTC epoch and exact TLE mean elements are normalized into the existing CSV parser/cache and independently tested against SGP4 TLE propagation. Freshness and cache intervals remain unchanged. Rejected HTTP providers stop independently for the session; cancellation never initiates a fallback. The last successful source is preferred on the next refresh.

The public HTML is bounded at 512 KiB and requires one unambiguous matching pre/code TLE block. Missing/changed markup, duplicate records, corrupt checksums, other spacecraft and stale epochs fail closed. This is a website fallback rather than a guaranteed versioned API; a site markup change can make it unavailable. Provider and Space-Track credits are shown with the object information.

Provider documentation: https://docs.satcat.com/ (public metadata/TLE publication), https://www.satcat.com/terms-of-use. Space-Track grants blanket basic-SSA redistribution with appropriate citation: https://www.space-track.org/documentation. No publication or paid service has been configured.


## Atmospheric pressure

The viewer conditionally displays supported atmospheric/exospheric reference pressures
with the independent bar/Pa/psi preference. See [values, sources and exclusions](ATMOSPHERIC_PRESSURE.md).

## Dynamic mass presentation — version 0.1.8

Every mass/component row uses the shared dynamic kg/lb/M☉ formatter: kg/lb below
10 solar masses, M☉ at or above the threshold. Converted uncertainties share a
scientific exponent for compact display, with estimate/model notes retained in
all twenty languages. Reference data, physical calculations and binary A+B
identification are unchanged; unknown current stellar mass remains unavailable.
