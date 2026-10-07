# App presentation and preferences

## Detail islands clear the scrollbar — current local update

The celestial object facts island and Position details table have an 8 dp
external margin on each horizontal side of their scrolling viewport. The same
shared inset controls descriptive notes and attribution text, keeping the card
edges aligned with the external text inset. The overlay scrollbar stays at the
viewport edge and no longer draws across the island background. Internal card
padding, map/model sizing, top alignment and vertical gaps are unchanged. The
inset lives inside the readout in both portrait and landscape split layouts.

## Independent position and date/time — current local update

Place and Date and time are persistent islands with the shared coordinates and
calendar heading icons. Each contains its own styled checkbox: Simulate position
and Simulate date. Only that island's fields expand when its switch is checked.
Set remains uppercase and scrolls after both islands, before Open-Meteo credits.
With both off it returns to live observation; either or both can be enabled.

A location override changes coordinates/altitude and the displayed IANA zone.
When date simulation is off, the current absolute instant continues advancing;
it is never rebuilt using the phone's wall-clock numbers in a different zone.
New York therefore shows New York's current local clock and current sky. A date
override fixes the chosen absolute instant while leaving real GPS and its accuracy
intact when position simulation is off. The date editor displays its interpretation
zone: the selected place's zone, or the device zone with real GPS. Toggling the
position source converts valid date/time entries between those zones while
preserving the selected instant.

Saved seven-field plans include both independent switches. Existing five-field
plans load with both enabled, preserving their previous behavior and values.
Validation ignores disabled inputs and retains the last valid custom place or
fixed moment for reuse. The main scene, catalogue, paths, details and panorama
share the resolved inputs; weather uses current conditions with a live date and
hourly estimates with a fixed date. Both switches, titles and captions are
translated in all twenty catalogues. No system GPS/clock changes are made.

## Sky notices ordered by measured width — current local update

Simulation, compass and data/connectivity notices now share a single measured
layout. The widest complete island is placed first, with remaining islands in
descending width and 4 dp gaps. The Retry button and existing island padding
are part of each measurement. Equal widths retain stable source order. Changes
to visible state, text, translation, font scale or available width reorder the
islands in the same layout pass, without waiting for a size-state callback.
Leading-edge placement mirrors in RTL. The maximum width, visual styles and
callbacks remain unchanged; an empty stack reports zero size to the scene's
label-avoidance frame.


## Simulation follows the settings design system — current local update

Simulation's checkboxes share the option surface and inset label used by
Appearance/Language/Units, including the selected rounded background, minimum
row height and font size. Plain auxiliary links remain plain. All icon/text
button captions use app-locale uppercase, including repository/update actions.
The selected place/time also supplies hourly weather estimates when available,
with a clear base sky and unavailable weather text otherwise. See
OBSERVATION_SIMULATION.md for provider/time coverage and caching.



## Primary confirmations and secondary text actions — current correction

Set and Use this point are uppercase outlined primary buttons with a leading
icon and text centered in the remaining width. Search, Choose on map, Find
altitude/time zone and Use current GPS position are plain text actions without
icons or outlines. The GPS shortcut remains inside custom fields; the independent
simulation checkboxes, compact status notice and Set commit semantics are unchanged.


## Simulation checkbox and button content — current local update

Simulation uses separate position and date checkboxes inside persistent Place
and Date and time islands. Each expands only its own fields; Set commits enabled
overrides independently with the shared validation/persistence behavior. The current-GPS
shortcut is a plain text action inside custom fields. The orange Simulation active notice is text-only and matches
the compass warning's visual padding/type size without moving the scene.

Observer and repository actions follow UVIR: the icon stays in a leading slot,
then the label is centered in the remaining width. Shared single-line text fitting
preserves the full accessible caption. All shared icon/text captions are uppercase
in the selected app locale. The native map button uses the equivalent
relative drawable and remaining text region. See OBSERVATION_SIMULATION.md.


## Simulation actions and compact status — current local update

The former Place and date menu/page is titled Simulation. Its primary and
secondary actions use the shared design system: primary confirmations have an
outlined theme accent and leading icon, while auxiliary links are plain text.
The map confirmation uses an equivalent native control. Simulation active
appears as a compact orange clickable notice in the sky's leading corner,
alongside compass/connectivity notices. It no longer adds a row below the main
toolbar or moves the sky/data panels down. Tapping it returns to Simulation.
Stored observer parameters, calculations and capture disclosure are unchanged.

## Object facts and photo position — current local update

The object detail information pane has one rounded island for tabular facts only.
Explanatory text, missing/reference-value caveats and credits follow below it on
the page background, sharing the same scroll viewport and scrollbar. All such
object cards/notes/credits and Position details cards/notes/weather attribution
reserve a shared 8 dp inset on both horizontal sides; internal card/table padding
and vertical spacing stay unchanged. Model and
information splits retain their existing portrait/landscape behavior.

The photo preview's position pin offers Complete, Area (region and country only)
and Hidden. Area is the first-use default; existing explicit choices remain
respected. The independent stored choice controls both Save and Share and the
next capture, without changing the live GPS observer, astronomy or other settings.
A slim separate heading keeps the caption clear of an object at +90° without
changing the angular projection.
Changing it uses the frozen capture context and a separate private JPEG. Nothing
is saved automatically. A separate Simulation page now supports a custom
location, a fixed observation time, or both; photo disclosure remains independent.
See [OBSERVATION_SIMULATION.md](OBSERVATION_SIMULATION.md).
See [PANORAMA_CAPTURE.md](PANORAMA_CAPTURE.md) for disclosure and metadata details.

## Landscape island columns — current local layout

Appearance, Units, Simulation and Info/news use the same keyed lazy island grid.
Portrait retains one column; landscape uses two equal columns when the usable
content width accommodates two 280 dp cards plus the 12 dp gap. At larger
accessibility font scales, that minimum card width grows; narrow landscape windows
therefore retain one readable column. The app's adaptive density, 8 dp page sides,
14 dp island padding and 12 dp vertical gaps are unchanged. Language remains one
full-width island in every orientation. Each island retains its own natural
content height in both portrait and landscape, including cards sharing a row.
Simulation's validation/privacy/provider
notes span both columns below the islands, and its draft/effects remain outside
the layout so resizing does not rewrite settings or reset inputs. Card keys and
the same grid composition preserve scroll/focus state through column changes.
The main scene, GPS/map details and object model/facts retain their split layouts;
daily paths and the object catalogue retain their existing lists. Radio choices,
stored preferences and navigation state are unchanged.

## Page-title overflow — local update

Every page title keeps one line and its complete accessible text. Titles wider
than their viewport automatically travel back and forth with soft edge fades;
fitting titles remain stationary. Settings, Info/news, Objects, object details
and daily paths share the UVIR-style scroller. The native Position details title
uses the same timing and 14 dp edges instead of ellipsis. Its toolbar retains
the ordinary 48 dp minimum while accommodating enlarged fonts vertically.

## Orbit names and Capture — version 0.1.13

Capture's camera action appears before the main menu button and opens a complete
360° sky-map preview with selected objects, paths, direction arrows and live positions.
Gallery writes happen only after an explicit Save; Share uses the prepared image. Repeated object names follow the actual orbit
curves near arrows in the live scene and saved image. The snapshot freezes the
current position/time and selections; capture never changes saved preferences.
Sky/ground colors follow the same solar phase/weather model, unavailable data
remain explicit and new strings are complete in all twenty languages.
See [PANORAMA_CAPTURE.md](PANORAMA_CAPTURE.md) for rendering/storage details.

## Estimated place — version 0.1.11

Environment details adds an Estimated location island directly below GPS altitude
and before weather. Available city, region and country names occupy separate
right-aligned lines at the normal data font size. Duplicate administrative names
are removed; absent names are not fabricated. Loading and unavailable states,
the recent-news summary and online-data note are translated in all twenty locales.
The underlying GPS coordinate/accuracy/altitude formatting is unchanged.

The foreground-only device-geocoder lookup reuses a small memory cache across
navigation, changes its key with approximate coordinates/interface language, and
never shares another location's label. See [ESTIMATED_LOCATION.md](ESTIMATED_LOCATION.md).

## Info heading icons — version 0.1.10

The repository heading reuses the existing GitHub glyph, and Credits and licences
has a copyright glyph. Both use the Latest news heading's 20 dp icon slot, title
color and 10 dp separation. The introduction keeps its existing app logo.
Decorative icons do not duplicate spoken headings and inherit the existing
light/dark theme, RTL row order and tablet density scaling. No strings or saved
preferences changed.

## Unit and numeric consistency — version 0.1.9

Gravity, gas-giant reference pressure, numeric counts/zoom and every point clock
now honor the saved units/digit choices. Daily table, island, balloon and live
marker share one time formatter; device digits remain independent of the app
language. Mass, density, pressure and temperature choices remain independent.
The complete presentation inventory, conversion definitions and regression
coverage are documented in [UNIT_FORMAT_AUDIT.md](UNIT_FORMAT_AUDIT.md).

## Dynamic mass display — version 0.1.8

The kg/lb preference now applies to every known mass below 10 solar masses,
including the Sun, nearby stars and compact stars. At the inclusive 10 M☉
threshold, presentation switches to solar masses independently of kg/lb and object
category. Published uncertainties, approximation/model qualifiers and binary
component identification survive conversion. Components use the same preference
and their own mass, rather than inheriting a binary total's display unit.
Unknown masses remain unknown. Twenty-language recent news and the plain mass
qualification note describe the updated behavior; no preference migration/reset
or new user setting is needed. Scientific calculations and density units are
unchanged. See [DEEP_SKY.md](DEEP_SKY.md) for reference/display distinctions.

## Mass/density and pressure — version 0.1.7

The Mass island is renamed Mass and density without changing its saved kg/lb
choice. Pressure follows it in the unit list, with System, bar, pascal and psi.
It has an icon, description and aligned unit examples in all twenty languages.
Pressure has its own presentation preference and regional default, independently
of length/mass/interface-language choices. The viewer shows a pressure row only
for supported atmosphere/exosphere reference facts, with surface/night/upper-limit
and constituent qualifications retained. The recent-news box now summarizes pressure.
Scientific sources, unit definitions and excluded cases are in
[ATMOSPHERIC_PRESSURE.md](ATMOSPHERIC_PRESSURE.md).


## Info and news — version 0.1.6

The app-title destination is now called Info and news in all twenty languages.
A separate latest-news island immediately follows the description/Earth-use note,
before GitHub and credits. It reuses the app island styling and the same two-sparkle
glyph as UVIR, with the current version and three short localized summaries of
recent units/mass changes, clearer viewer controls and signed GitHub updates.
It scales with the existing UI and preserves light/dark styling, scrolling and RTL.


## Mass units — version 0.1.5

The Mass island follows Distances and speeds, with System, Kilograms and Pounds.
System resolves from the device region (US/LR/MM use pounds; other/unknown regions
use kilograms), independently of interface language and the length/speed choice.
The saved preference is observed immediately and retained across app/device restarts.
Planet, moon and spacecraft mass readouts convert at presentation time using
1 lb = 0.45359237 kg exactly ([NIST SP 811](https://tsapps.nist.gov/publication/get_pdf.cfm?pub_id=200349)).
Density combines the chosen mass and length units: kg/m³, lb/m³, kg/ft³ or lb/ft³.
One cubic foot is exactly 0.028316846592 cubic meters. Reference data/calculations,
solar-mass readouts, published uncertainties and unknown-value markers are preserved.
The island includes a weight icon, description and right-aligned kg/lb examples in
all twenty interface languages.


## Units and viewer selection — version 0.1.4

The Units page replaces the separate speed/astronomical/normal-distance controls
with one distances-and-speeds island. System follows the device region; explicit
meters/kilometers or feet/miles controls every corresponding display, including
Mkm/Mmi, km/s or mi/s, GPS altitude/accuracy and physical sizes. Extrasolar distances
retain light-years, and AU references remain where already present. Temperature,
coordinate notation, numeric formatting and date/time remain separate choices.
Legacy preferences migrate once without clearing app data. If older length/speed
choices differ, explicit astronomical distance wins, then nearby length, then speed.

The planet viewer current-view/rotation tabs use light fill/dark glyph when
selected and dark fill/white glyph when inactive. Their shape, touch targets and
accessible selection semantics remain unchanged in both app themes.


The independent package remains `me.mondiversi.spacecompass`.

The original vector identity combines a ringed planet, compass pointer and star.
It supplies the adaptive launcher and launch artwork. Native startup uses day/night
resources, followed by an animated Compose loading transition using the saved app
theme: gradient background, pulsing glow, orbital arcs, stars, circular identity,
version and fade-out. Adaptive launcher foregrounds stay inside the circular safe zone.
Location permission prompting starts only when the compass screen is mounted.

The main toolbar displays Space Compass with a 16 dp start inset and settings.
The celestial selection control is at the sky viewport end below the toolbar,
with a compact 16 dp circular counter. Settings open Appearance, Language and Units panels. Display settings
remain unavailable; existing adaptive phone/tablet behavior is preserved.
The language selector offers the 20 existing catalogs, including Greek and Persian.
App preferences remain in the independent Space Compass preference file.
Changing theme or language preserves the activity context and sensor lifecycle.

Display conversions include km/s or mi/s, km/Mkm/mi/Mmi/AU distance, metres/feet
for GPS altitude and accuracy, Celsius/Fahrenheit reference temperatures, decimal
or DMS coordinates, and existing number/date/time formats. Default distance keeps
the inherited Mkm/AU presentation. Stellar effective temperature remains kelvin.
Astronomical calculations and network inputs retain their original units.

Build, JVM tests, instrumentation compilation and lint are verified separately.
No connected device or emulator execution is implied by these checks.

## Verification — 2026-10-04

- Debug APK: built successfully.
- JVM tests: 335 passed, 0 failures.
- Android instrumentation sources: compiled; not executed.
- Android lint: 0 errors, 56 non-blocking warnings.
- Physical device/emulator UI checks: not performed.


## Reduced compass accuracy

The Samsung magnetometer reported Android LOW accuracy with plausible fresh field
readings (approximately 41 microtesla). Earlier code rejected LOW altogether,
preventing scene projection. LOW now enables approximate pointing after sustained
plausible samples, with a compact reduced-accuracy notice. Reliable alignment still
requires MEDIUM/HIGH. UNRELIABLE, stale/nonfinite samples and anomalous fields remain
blocked. Calculations and the magnetic north reference are unchanged.

Revision verification: 336 JVM tests passed; APK built; Android tests compiled;
lint reported 0 errors and 56 warnings. The signed update was installed on the
authorized Samsung without clearing data. A real landscape screen was inspected;
the celestial markers and compass render with LOW accuracy. This is not a claim of
full instrumentation, physical bearing calibration or all-device UI coverage.

## Info and full-screen settings

The celestial selector now has a neutral circular surface, outline and shadow.
The intro keeps its animated glow/orbits but removes the decorative color bars.
Tapping the main title opens Info with version, description, an Earth-only-use
joke, repository status, compass precision explanation and bundled credits.
The current Information page contains the public GitHub repository, with separate open/check-update buttons and a global update dialog; see [UPDATES.md](UPDATES.md).

Appearance, Language and Units are full-screen pages with neutral light/dark
gray cards and direct radio selections. Unit page order is distances/speeds,
temperature, coordinates, numbers, date and time.
All new interface text is present in the 20 existing resource catalogs.
Navigation saves the compass selection via a saveable-state holder. Sensors stop
while a settings/info page replaces the compass and resume when it returns.
Every page remains inside the existing adaptive density wrapper; cards use all
available width. Appearance, Units, Simulation and Info use readable two-column landscape islands; Language stays a single column.
Phone-to-tablet scale behavior and translation completeness have targeted JVM
tests. No physical tablet or tablet emulator execution is claimed.

Page verification: 338 JVM tests passed, Android instrumentation sources compiled,
APK built, lint 0 errors / 56 warnings. On the authorized Samsung, title-to-Info,
back navigation, language/formats pages, neutral menu colors and live theme changes
were checked. Theme was restored to Default. System-bar foregrounds follow the app
theme. These focused device checks are separate from the unexecuted full Android
instrumentation suite and physical tablet checks.


## Recent Android layout compatibility

The connected Samsung runs Android 12 (API 31); Android 8 is the minimum supported version. The app targets API 37. Safe-drawing insets protect all pages from status/navigation bars, cutouts and window decorations. Adaptive scaling now uses the actual safe window bounds, so split-screen and desktop resizing do not retain full-tablet scaling. Legacy navigation-bar color is applied only below API 35; modern windows use the background drawn behind system bars. No orientation or resizability lock is declared.

No emulator AVD or system image was available locally, so Android 15-17 runtime checks remain unexecuted. Source compatibility review is not a substitute for those device tests.

## Interface refinement

Data availability notices have their own island above the body data. Compass precision remains in the sky header. Daily-path and body angle pairs use a middle dot. Environment details are a full-screen page with separate GPS coordinate, accuracy and altitude cards. The celestial selector is a centered scrollable modal; its host supplies scrolling to avoid nested unbounded scroll containers. Info no longer displays the package identifier. Language choices omit the redundant island heading. Display shows the disabled small/large screen modes. Regional unit defaults use the device region before app language overrides; coordinates default to decimal degrees. Distance choices are Mkm/Mmi, with AU still included in the body readout; legacy selections resolve to valid million-unit choices.

Celestial selector performance: shortened localized title, bounded LazyColumn with keyed rows, catalog snapshot per opening, and thumbnail CPU work limited to two workers. Sensor-driven visual updates and the scene timer pause while the catalog is visible, then resume. A short four-swipe Android 12 debug-build check reported 79/88 deadline-missed frames before pausing background updates versus 5/122 after (not a controlled benchmark). Build/JVM tests/instrumentation compilation passed; popup opening, scrolling and closing checked on device.


## Format and translation audit (2026-10-04)

Normal-distance preferences also control spacecraft dimensions and antenna sizes
(m/ft), plus large physical diameters and their uncertainty (km/mi). Astronomical
ranges, including the object's selector, use Mkm/Mmi with the AU reference retained.
The selector still measures distance from the Sun; the main panel and observer row
measure distance from the observer. These are intentionally distinct quantities.
Mass readouts follow the independent kg/lb preference; density combines that
choice with the selected m³/ft³ volume. Masses from 10 solar masses use M☉; lower masses, including the Sun and compact stars, follow kg/lb.
Surface acceleration follows the saved length family (m/s² or ft/s²) as of version 0.1.9; the comparison in standard Earth g remains invariant.

All reference temperatures follow Celsius/Fahrenheit, including the stellar
effective temperature; its Kelvin reference is retained in parentheses. System
dates follow the device locale independently of app language. Explicit date
conventions and 12/24-hour choices retain their existing behavior and time-zone/DST
handling. UI language continues to translate labels and AM/PM markers.

The audit checked all 214 translatable keys in each of the 20 catalogs, including
format arguments, duplicate keys, nonempty values and UTF-8 decoding. Names and
international unit/mission identifiers may coincide across languages. Plural
object titles, Japanese culmination wording (highest altitude, avoiding a southern
transit assumption), decimal-angle terminology, French punctuation and Japanese/
Chinese copyright labels were corrected. This is an engineering and terminology
review, not certification by native translators in all languages.


Final validation: debug APK assembled, 347 JVM tests passed (zero failures/errors),
and Android instrumentation tests compiled. The full connected instrumentation
suite was not run. On the Android 12 phone, selecting imperial/US presentation
was checked in the main panel, catalog, environment/GPS, celestial viewer and daily
path: Mmi, mi/s, ft including GPS uncertainty, DMS, American decimal punctuation,
Fahrenheit, physical diameters in mi, American dates and 12-hour times. All eight
format settings were returned to their original system-default choices. Each of
the 20 language choices loaded its translated page heading on the phone, including
Korean and RTL Arabic/Hebrew/Persian; the original system-language setting was
restored. The final APK was verified against UVIR's certificate and installed.


## Satellite connectivity and compact environment readouts

On 2026-10-04 the physical phone's app log showed TCP connection timeouts to
celestrak.org:443 for both ISS and STARLINK_V3, before any HTTP response. A desktop
HTTPS request to the same identified Starlink CSV endpoint also timed out. This
establishes an unreachable provider from the tested connection, not missing general
Internet connectivity or a CSV parsing error. It does not establish whether the
provider, routing or a firewall caused the timeout. No alternate spacecraft, stale
fabricated orbit, insecure TLS workaround or proxy was substituted.

Transport connection/time-out/DNS failures retry after five minutes rather than two
hours; valid cached orbital data retain their two-hour refresh window. Any non-200
response stops requests to that satellite provider for the session, while its
independent alternative remains eligible, following CelesTrak's usage policy
(https://celestrak.org/usage-policy.php). Unavailable text names CelesTrak / Satcat
and explains that this can occur with working Internet, in all 20 languages.

The GPS/details action is now a single-line control below altitude within the same
column beside the compass. Its accessible target remains at least 48 dp. Estimated
weather uses the same label/value table as other environment fields, with the value
aligned to the right.

The retained foreground orbital owner now warms every online catalog object,
including unchecked comets, Sedna, Voyager and the two satellites. It restores
validated disk caches before scheduling downloads and prioritizes selected
objects, then unchecked objects in catalog order (including the satellites near
the start). Unchecked Horizons objects fetch heliocentric distance/speed vectors
before apparent pointing. Selection, retry and network events wake a stable queue
without cancelling its current read or restarting the warm-up at the first object.
Downloads remain serialized, with independent two-hour freshness and the existing
failure/provider backoff. Warming does not change selection or show the selected
object's loading indicator. No background service is added.

The open catalog recomputes distance rows and their distance sort values locally
every ten minutes; horizon visibility retains its existing minute cadence.
Actual new orbital models and rounded GPS input changes update both immediately
using the current time. Loading flags alone do not recompute either. The inspected
object's live compass/viewer updates keep their existing cadence. Its keyed scroll
state is retained; new downloads do not reopen the page, scroll to the top or change
saved filters/order/checked objects.
Valid caches remain available across launches and failed refreshes. Missing or
out-of-coverage data remain unavailable until a successful provider response.

The Objects toolbar places Refresh after Sort and Filters. It immediately recalculates
all rows/visibility at the current observation time, releases recoverable failures for
the entire online catalog, and requests one fresh JPL position/motion pair per target.
Valid satellite elements are propagated locally; their provider downloads retain the
two-hour minimum required by CelesTrak (https://www.celestrak.org/usage-policy.php).
Automatic ten-minute catalog recalculation is distinct from downloading unchanged
orbital elements. Missing/expired elements still download as soon as policy allows.
A progress glyph disables duplicate refresh taps while local calculations or the
serial remote batch are active. Filter/sort preferences, selections and scroll are
unchanged; the inspected object's live update cadence is unaffected.

ISS now uses the same validated public-source fallback handling as Starlink: after
a failed primary request it can read the public Space-Track TLE block published by
Satcat at https://www.satcat.com/sats/25544. Identity (NORAD 25544, 1998-067A), both
checksums, SGP4 bounds and the existing 72-hour freshness window are checked before
cache replacement. HTTP stops remain independent per provider, cancellation never
starts a fallback, and the last successful source is preferred for the session.
Both satellite notes/status messages credit/name CelesTrak / Satcat · Space-Track.
HTTPS trust and the JPL-specific CA configuration are unchanged.



## Compact extrasolar distances and catalog scrollbar

Far ranges at or above the existing 10,000 AU presentation threshold use only ly,
independent of Mkm/Mmi preferences. The threshold is a display rule, not a definition
of the physical Solar System boundary. Polaris is therefore compact in the catalog,
main island, point summary and observer details; Solar System and Voyager ranges
retain the selected million units and AU reference. The catalog scrollbar now uses
its LazyListState and is drawn inside the clipped list viewport with reserved end
padding. The popup host does not draw or intercept an external scrollbar when the
content owns its own scroll state. Header and close action stay fixed.


## Main-page lifetime across navigation

Settings/info overlay a retained compass composition. The hidden main layer is measured but not placed, so it retains remember/effects and existing sensor, weather, orbital-refresh and path owners without exposing background touch/accessibility nodes. Main data follows the activity lifecycle and is disposed when the activity ends, not when a settings route opens. Object viewer and daily-path branches come after overlay data producers, preserving their caches through internal page navigation too. Returning still updates the current time, sensor readings and legitimately changed astronomical inputs; it does not reconstruct the whole screen or reset refresh backoff.

Regression coverage includes a disposable Compose navigation fixture checking remembered state/owner lifetime across repeated info round trips. Instrumentation compilation and actual execution are reported separately.


## Settings confirmation and compact Retry notice — current local update

Actual presentation preference changes show the shared short, dark Uvir-style
bottom confirmation after the new app language takes effect. Initial loading,
unchanged radio selections, automatic system appearance/locale updates and
unrelated preference writes do not announce changes. Simulation retains its
existing save confirmation; the generic settings_saved resource reuses the same
already-translated confirmation text in all catalogues.

The sky's Retry notice retains the same 12 dp horizontal / 6 dp vertical outer
padding used by Simulation and compass notices. Its button no longer contributes
48 dp of visible layout height or additional content padding: a compact primary
text action shares the 10 sp / 13 sp line styling, while Foundation's expanded
48 dp touch target and the existing accessible action remain. Width-based notice
ordering, the 4 dp gap and retry callback are unchanged.


## Live camera background (2026-10-06)

The toolbar camera now toggles an initially disabled live rear-camera background,
using camera/slashed-camera glyphs. Capture moves into the menu before Simulation
with a shutter glyph. Reticle, bodies and paths share the preview's optical crop
and rotation; only a gravity horizon is drawn over the real scene. The camera
permission is requested on demand and the preview is released on navigation or
backgrounding. See CAMERA_VIEW.md for geometry, lifetime and accuracy limits.


## Full-scene capture and adaptive preview (2026-10-06)

The Capture shutter circle now sits immediately after the object selector and is
removed from the menu. Camera mode captures a real still image with frozen orbital
overlays across its full lens field; normal mode keeps the 360° panorama. Both
previews follow device orientation, with portrait fill/drag and landscape fit/zoom.
The main view's columns never truncate export pixels; saving remains manual.
