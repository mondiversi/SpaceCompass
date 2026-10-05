# App presentation and preferences

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
available width and larger logical viewports can arrange format cards in columns.
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
satellite HTTP response halts automatic requests in the current policy instance,
following https://celestrak.org/usage-policy.php. Satellite unavailable text names
CelesTrak and explains that this can occur with working Internet, in all 20 languages.

The GPS/details action is now a single-line control below altitude within the same
column beside the compass. Its accessible target remains at least 48 dp. Estimated
weather uses the same label/value table as other environment fields, with the value
aligned to the right.

Orbital downloads now include selected objects only; opening the catalog continues
to use existing caches without starting downloads for unchecked objects.


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
