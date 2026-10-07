# Custom observer and observation time

## Four independent Scenario overrides — 2026-10-06

Position, Altitude, Time zone and Date and time each have the existing compact
icon/checkbox/title heading. They are always present and expand independently.
Checked Altitude/Time zone enable manual fields. Unchecked fields use matching
terrain/time-zone metadata for an overridden position; with real coordinates,
height follows the real GPS fix and the clock follows the device zone. Height or
time zone may be overridden without replacing GPS coordinates or fixing the time.
Manual height retains real coordinate accuracy but removes GPS vertical accuracy.
Altitude labels and export rows distinguish an overridden height from GPS height.

Valid edits autosave using the existing notice and debounce. All four off returns
to live mode while retaining previous values for reuse. The eleven-field plan
persists four switches, manual values and separate automatic height/zone caches.
Five/seven-field plans retain their exact current values with height/zone switches
enabled whenever their position override was enabled. Their automatic caches are
unknown: an explicit switch to automatic triggers matching metadata lookup rather
than relabeling old manual values as measurements. Coordinate edits invalidate the
automatic cache. Failed required metadata lookup cannot replace the last valid
observer; manual controls remain available with a translated explanation.

Changing the effective time zone converts the displayed date/time while retaining
the chosen UTC instant, including the second occurrence of an overlapping DST
hour. A transient preserved-instant hint applies only while the edited fields match
that instant; explicit date/time input clears it and uses the ordinary first-valid
occurrence rule. Neither device GPS nor the Android system clock is changed.

## Scenario naming — 2026-10-06

The public feature name is now Scenario (localized in all twenty catalogues),
with Scenario active in the main scene and Space Compass · Scenario in the
English scientific export caption. The menu icon combines a location pin and a
clock. Observer routes, resource identifiers and stored preferences are unchanged;
existing saved scenarios need no migration. The independent position/date-time
checkboxes keep their action descriptions and behavior.

## Previous position/date-only editor — superseded by the four overrides above

Position and Date and time are persistent islands. Each heading contains its
coordinates/calendar icon, a compact checkbox and the section name in that order,
following UVIR's LED/buzzer section layout. The accessible checkbox descriptions
remain Simulate position and Simulate date and time. Only that island's fields
expand when its heading switch is checked.
The page has no Set action. Valid changes save automatically while the page stays
open; both off returns to live observation. Either or both can be enabled.

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

## Shared settings rows, button captions and simulated weather

The simulation checkboxes now share their islands' heading row rather than
reserving a separate option row. The order is decorative 20 dp icon, 8 dp gap,
24 dp checkbox slot, 6 dp gap and the existing semibold section title. The complete
heading retains a 48 dp minimum interactive area and Checkbox semantics; its
decorative checkbox has no second click action. Checkbox colors use the existing
app selection helper, with no additional selected-header background. Collapsed
sections draw only the heading; the divider/fields appear when enabled. Shared
card colors, outer/internal padding and non-simulation island headings are unchanged.
Valid switch changes save immediately; typed complete values save after a 600 ms
pause. Validation and stored observer semantics are independent of other settings.

All shared icon-and-text button captions, including repository/update actions,
are uppercased in the selected app locale before fitting their remaining width.
Native Use this point remains equivalent. Search/map/metadata/GPS text links
retain their normal translated case and have no leading icon or outline.

For a fixed simulated date, a rounded approximately 2 km location cell and the selected
UTC day are sent to Open-Meteo. The user explicitly authorized this provider.
The request asks for hourly WMO weather code and total cloud cover only. Recent
dates use the Forecast API; dates older than five days use the Historical Weather
API. Requests are skipped before 1940 and beyond the sixteen-day forecast window
including today. Provider coverage or valid estimates can be missing within
those bounds too. The date is derived from the plan's absolute instant, handling
observer zones, daylight-saving changes and fractional offsets correctly.
With date simulation off, the current-weather request/cache is used at the
effective location, including a custom location. Date-only simulation requests
the fixed hour at real GPS coordinates when a valid fix is available.

Only the matching UTC hour is accepted; adjacent hours, null/malformed values
and unknown WMO codes are rejected. Historical negative/zero Unix timestamps
are supported. Live freshness checks remain unchanged; simulated snapshots
are validated against their selected hour. Location/time changes clear the old
estimate and cancel its worker. Foreground-only memory caching, the one-minute
global request throttle, eight-second connection/read limits, 32 KiB response
limit, disabled redirects, HTTPS verification and coordinate-free error logs
remain. Forecast estimates refresh every fifteen minutes, historical estimates
every twenty-four hours; failures retry after five minutes.

The same immutable snapshot drives the main sky, Position details and panorama
capture, including cloud cover, rain, snow, fog or storm motifs. Solar phase still
comes from local Sun elevation. If weather is unavailable, the visual uses a
clear base with no clouds/precipitation/fog, and nighttime stars; the text stays
Weather unavailable rather than asserting observed clear conditions. A panorama
freezes both the selected weather and its percentage at capture, preserving the
normal privacy choice and explicit Save behavior. Open-Meteo credit is retained
in details and JPEG credits whenever estimates are used.

Sources: [Forecast API](https://open-meteo.com/en/docs) and
[Historical Weather API](https://open-meteo.com/en/docs/historical-weather-api).


## Map navigation alignment

Choose on map and Position details use the secondary-page toolbar's shared
40 dp back slot, side padding and chevron size/stroke/optical offset. The native
48 dp touch target expands symmetrically around that slot rather than increasing
its layout width. Insets are calculated after rounding to pixels, preserving
both the back-button center and adjacent title start at fractional screen
densities. System-bar/cutout insets and RTL mirroring remain in place.



## Primary confirmations and secondary text actions — current correction

Calendar/clock confirmations and Use this point are uppercase outlined primary
buttons with a leading icon and text centered in the remaining width. Search, Choose on map, Find
altitude/time zone and Use current GPS position are plain text actions without
icons or outlines. The GPS shortcut remains inside custom fields; the independent
simulation checkboxes and compact status notice retain their styling. Automatic
saving replaces the page-wide Set confirmation.

The main menu's Simulation page follows Units and precedes Info/news. Its two
independent switches control Position and Date and time. Automatic saving validates
enabled inputs and stores the chosen modes together; both unchecked returns to
real GPS and current time. Back flushes the latest complete edit. Incomplete values
retain the last valid plan and show a longer notice on leaving. The current-GPS shortcut remains a plain
text action inside custom place fields. Unchecking position cancels lookup and
closes the map picker; unchecking date closes its calendar/clock. Toggling either
draft back on retains the entered values. No Android mock-location provider or
system clock change is used.

Simulation active is a compact, clickable orange text-only notice inside the
sky, with the compass warning's 10 sp/13 sp typography and 12 dp horizontal/6 dp
vertical padding. No visible 48 dp row or pin is reserved. Android/Compose expands
its minimum touch bounds independently of its visible content; the main toolbar,
scene and data panels retain their normal positions. Tapping it opens Simulation.

Primary observer confirmations and repository actions use UVIR's fixed leading
20 dp icon slot, 8 dp gap and text centered in the remaining content width. Labels fit a single line
between 14 and 10 sp with the existing fitting function; settings labels use
semibold. Only primary confirmations are full-width button rows. Search, map selection,
metadata lookup and the current-GPS shortcut remain compact text actions without
borders or leading icons. Search and map links can share a wrapping row. Calendar/
clock confirmation and Use this point captions use the selected app locale; the
underlying translated strings retain their normal case. Native Use this point uses the
equivalent relative compound drawable, 24 dp side insets and centered remaining
text area. Outlined theme colors, ripple and disabled states are unchanged.
Ordinary action controls retain 48 dp minimum targets.

## Choosing and storing a plan

Place search uses Android Geocoder with a bounded ten-second request. The map
picker uses the same local Leaflet/OpenStreetMap map as Position details; tapping
the map selects a red pin and enables Use this point. Selected coordinates are
sent to Open-Meteo's fixed HTTPS forecast endpoint, with `forecast_days=0` and
`timezone=auto`, to obtain terrain elevation and an IANA timezone. The request
is bounded to twelve seconds and 8 KiB, retains normal certificate/hostname
validation and does not log coordinate-bearing provider errors. Geocoder and
terrain metadata require their providers to be available. Elevation is a terrain
estimate rather than measured GPS accuracy or a building/floor height.

Latitude, longitude, altitude, date, time and timezone remain editable. Altitude
input honors the app's metre/foot preference and is stored in metres. Changing
coordinates invalidates the previous automatically resolved altitude and zone;
a completed in-range coordinate edit requests matching metadata after an 800 ms
pause, or the user can use the lookup link/enter them manually. A failed lookup
cannot silently reuse the previous place's elevation or timezone.

The calendar and clock dialogs adapt their content to the available width and
retain the shared theme-aware dialog, close action and bounded scrolling body.
Text inputs use unambiguous ISO date and 24-hour time. Values must be finite;
coordinates are bounded to ±90°/±180°, altitude to −500…20,000 m and dates to
1900…2100. The timezone must be recognized. Nonexistent local times during a
daylight-saving jump are rejected; repeated local times use the first occurrence.
These input bounds do not guarantee ephemeris availability or equal precision
across the entire date range.

Automatic saving commits the validated plan and mode together to SharedPreferences.
Opening the editor alone never rewrites stored values. Switch/picker confirmations
save immediately; typing is batched for 600 ms and complete edits are flushed on
leaving. Position-only, date-only and combined plans survive process/device restarts. Seven-field records append strict boolean
switches; legacy five-field records enable both. Malformed records and custom
records with both switches off fall back safely to live inputs. Setting both
unchecked returns to real observation and retains the last plan for reuse.
Inactive fields cannot block a valid enabled override; the last custom location
and fixed instant remain reusable. Observer preferences are independent of
objects, appearance, units, language, filters, order and photo disclosure.

## Calculations and limits

The effective observer and instant feed the main scene, catalogue, object views,
daily paths, selected-point readouts, Position details and panorama capture. The
custom location has no fabricated GPS accuracy; its details use generic
Coordinates and Altitude labels. Date-only simulation retains the real GPS fix,
accuracy and GPS-specific labels. Compass orientation still comes from the real
device sensors. Local horizon coordinates and daylight follow the chosen place
and moment. A custom location uses its stored IANA zone; real GPS, including
date-only simulation, resolves the current device zone. Daylight-saving and
fractional offsets are handled at the effective instant. With date simulation
off, the main clock keeps advancing every foreground update.

A simulated date uses hourly Open-Meteo estimates for the effective place and
absolute instant. Present-day weather is never substituted for another date.
Satellite elements and remote ephemerides retain their existing coverage/validity
limits; unusable dates yield unavailable positions/paths. A chosen simulated
date/time remains fixed; automatic passage of that simulated instant and dedicated
satellite-ephemeris history are not implemented. Position-only mode follows real time.

Panoramas freeze the effective observer/moment and zone, visibly mark Simulation
and use that moment for geometry and point clocks. File names, storage timestamps
and capture EXIF instead identify the real instant the image was generated. The
Complete/Area/Hidden disclosure choice is unchanged, defaults to Area and never
adds GPS EXIF. Capture still requires an explicit Save to write to the gallery.

## Map appearance, data and licences

Leaflet 1.9.4 JavaScript/CSS and its BSD-2-Clause licence are bundled. Their bytes
were checked against the official release SRI hashes. Runtime map requests use
only HTTPS raster tiles from `tile.openstreetmap.org`, with normal WebView HTTP
caching and an app-identifying user agent. No runtime JavaScript CDN is required.
Visible OpenStreetMap contributor attribution and Leaflet credit are retained.

The map follows the app's light/dark flag. Night mode recolors only the raster
tile pane with inversion, hue rotation and contrast/brightness adjustments; this
is a dark rendering of the standard OSM tiles, not a separate vector style or
provider. The red marker remains unfiltered and controls/attribution use a neutral
theme palette. Android's additional algorithmic darkening is disabled for this
explicitly styled WebView so that tiles are not inverted twice.

The native WebView retains disabled file/content access, blocked mixed content,
no geolocation permission and no JavaScript application interface. Picker and
tile-failure messages use a validated per-instance nonce and bounded coordinates.
Map failures preserve the app's Retry behavior; external attribution links require
a user gesture. Custom coordinates/plan are deliberately stored locally; map
tiles reveal the viewed area to OSM, and selected coordinates go to Open-Meteo for
metadata. The page includes the corresponding translated provider/privacy note.

Open-Meteo metadata keeps its CC BY 4.0 attribution. Existing GPL, scientific
image/astronomy and SGP4 notices remain. See [VERIFICATION.md](VERIFICATION.md) for
the actual test and device-check scope.


## Save feedback and investigated page-button insets — October 6 local update

Successful changes show Settings saved using the existing custom bottom Toast:
translucent dark `0xEE2A2A2E`, white 14 sp text, 20 dp horizontal/13 dp vertical
padding and 14 dp corners, with existing automatic screen scaling and RTL handling.
This nonblocking message retains its always-dark UVIR appearance in both app themes.
Modal calendar/clock dialogs already use light/dark settings-card colors and retain
their bounded, themed layout and confirmation action. Opening/changing focus or
resaving an identical plan produces no save notice. A newer message cancels the
previous Toast so consecutive settings edits do not build a notification queue.
The native Toast `LENGTH_SHORT`/`LENGTH_LONG` flags govern durations (short success,
long incomplete-on-exit notice); no app-specific dismissal timer is substituted.
See [Android Toast reference](https://developer.android.com/reference/android/widget/Toast).
Enabled incomplete/invalid fields cannot replace the last complete plan; disabled
fields cannot block a valid override or live mode. A pending altitude/zone lookup
must complete for the matching place, with no silent stale metadata reuse.

The removed page-wide Set button's extra horizontal inset was investigated before
removal, without changing shared page/card padding: the scrolling page already has
8 dp on each side. Settings islands fill that width; Set additionally applied
`padding(horizontal = 8.dp, vertical = 4.dp)` around its outlined button, giving
16 dp total side inset instead of the islands' 8 dp. The islands' separate 14 dp
internal content padding affects their text, not their outer border. This was a
page-specific layout difference rather than an Android version/orientation issue.
Other action controls and shared page/card margins were not realigned for this
investigation. Only the now-unneeded page Set action was removed.


The Simulation position island is now titled Position (Italian: Posizione), with
matching geographic-position labels in all twenty locales. The resource key and
location/date controls, autosave behavior and layout are unchanged.

The date/time checkbox's accessible description reads Simulate date and time (Italian:
Simula data e ora) in every locale. Its saved key, switch behavior and input fields
are unchanged.


The October 6 header-checkbox adjustment removes the duplicate simulation-label
rows. Both translated section titles remain visible next to their checkboxes;
complete translated action descriptions remain available to screen readers.
Relative Row layout preserves icon/checkbox/title flow in RTL. The existing
simulation callbacks, preference keys, independent switches and picker behavior
are unchanged. No UVIR files were edited; its source was consulted for layout only.


The compact-header follow-up removes the explicit 48 dp *layout* height from the
two Simulation headings. Visible rows now use the title/24 dp checkbox's natural
height, matching ordinary island headings, inside the unchanged 14 dp card padding.
Compose's minimum touch-target expansion extends the toggleable heading into the
existing padding without adding layout height; the neighboring fields start after
the normal divider/gaps and do not share that target. Accessible labels, checkable
state, RTL layout, autosave and independent switches are unchanged. See the
[Compose API defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults#minimum-touch-target-sizes).


The landscape-column follow-up places Position and Date and time side by side when
the shared island-grid width/font policy allows it; portrait and narrow windows
remain stacked. Stable grid keys and page-owned draft/autosave/lookup effects keep
the same values while columns change. Validation, provider attribution and privacy
notes remain outside the islands and span the whole grid width below them. Shared
padding, compact checkbox headings and independent switch callbacks are unchanged.


### Separate metadata islands and consistent confirmations — 2026-10-06

When simulated Position is enabled, Altitude and Time zone use their own keyed
settings islands, with the same icons, internal spacing, independent card heights,
and landscape grid as other settings. The existing draft, metadata lookup,
autosave, validation and independent date/time switches are unchanged.

Repository actions and date/time confirmations share an outlined action component:
uppercase semibold label, leading icon, centered remaining text area, 48 dp target,
primary-color fill while pressed, contrasting theme text, and Material ripple.
The native map's Use this point button mirrors those states, theme colors and
type weight without mixing Compose drawing into the legacy WebView window.
It remains disabled until a point is chosen and clears press feedback on release.
Auxiliary search/map/metadata/GPS links remain text actions.


### Coordinate source for metadata detection — 2026-10-06

Find altitude and time zone reads the coordinates currently entered in the
simulation. Find altitude and time zone at current location replaces the old
Use current GPS position wording in all 20 languages: it reads the most recent
valid physical fix from devicePlace, updates the position fields and retrieves
terrain elevation and time zone for those same coordinates. It is disabled
without a valid fix, and neither action mixes metadata from one location with
coordinates from another. devicePlace is populated before simulated location
overrides; a synthetic map point is never treated as the physical GPS fix.
Both actions retain existing asynchronous lookup, validation and autosave.


## Field-specific altitude and time-zone lookup

Altitude now uses a mountain heading glyph. Its manual input is followed by two
auxiliary text actions: detect from the set position (available when simulated
position is enabled and valid), or detect from the current physical GPS fix.
Time zone has the same two source actions under its selector. The old combined
actions were removed from Position. Each explicit lookup updates only the requested
manual field. Neither source changes coordinates or overwrites the matching
automatic metadata cache for a different position. Position search/map selection
continues to resolve its automatic terrain altitude and time zone together.
Lookup progress and failures appear in the corresponding island. Cancellation
and request revisions prevent a superseded request from clearing the busy state
of its replacement. Incomplete/failed values retain the previous valid plan.

The time-zone field is a themed searchable list, with localized exemplar city
names, geographic zone IDs and trailing UTC offsets for the scenario instant
(current instant when date/time simulation is disabled). Stored geographic IDs
retain daylight-saving rules; offsets are not permanently substituted for them.
Half/quarter hours and historical sub-minute offsets are represented exactly.
The Android ICU canonical mapping removes true aliases from the visible list,
while the exact selected legacy/fixed ID remains selectable and unchanged.
Changing or detecting a time zone preserves the chosen absolute instant, including
the second occurrence of a repeated DST hour. Selection closes the list and saves
through the existing observer autosave/notification flow.

## Remote ephemerides follow the viewed date — local 2026-10-06

The observer's effective time override now reaches the shared foreground remote
queue. All five JPL targets (Sedna, Halley, 67P, Voyager 1/2) request and validate
their three-day UTC vector window around that viewed date, rather than the download
clock. Public requests remain Earth/Sun-centered and contain no GPS coordinates.
Actual wall time still governs file age, retry deadlines and satellite freshness.
Switching UTC observation days clears only prior JPL query backoff; fresh covering
caches, serial requests, satellite provider minimum intervals and HTTP stops remain
intact. Manual catalog refresh retains validated models and selection.

ISS/Starlink providers publish current orbital elements. The existing six-hour
future-epoch allowance and 72-hour maximum element age are retained; they cannot
provide defensible positions months ahead or historic spacecraft states. When a
restored model exists but does not cover the viewed moment, the catalog shows a
localized Date not covered status and the main notice explains the unavailable
date instead of claiming a network failure. With All visibility such objects remain
listed; Above/Below requires a known elevation and never guesses one.

JPL's API supports explicit START_TIME/STOP_TIME, within each target's supported
span: https://ssd-api.jpl.nasa.gov/doc/horizons.html and
https://ssd.jpl.nasa.gov/horizons/time_spans.html.
