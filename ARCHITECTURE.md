# Architecture

## Application boundary

`MainActivity` owns the standalone launcher, system-bar insets, adaptive density,
theme and foreground screen-awake request. It opens `SpaceCompassSunFinderScreen`
directly. The inherited internal SunFinder naming denotes the evolved celestial
compass, not a second application or hidden UVIR entry point.

The application ID, Kotlin namespace, app-private files and preferences are distinct
from UVIR. No parent project, symlink, external source directory or UVIR module
is required at build time. Shared UI helpers were copied and renamed, not linked.

## Code groups

- `SpaceCompassSunFinder*`: lifecycle-scoped location/orientation, sky/ground
  rendering, adaptive layout and environment panels.
- `SpaceCompassCelestial*`: catalog, selection, per-body ephemerides, refresh
  policy, path interaction, thumbnails, data facts and OpenGL model viewer.
- `SpaceCompassMoon*`: continuous lunar phase geometry and shared live marker mask.
- `SpaceCompassIss*`, `SpaceCompassSatellite*`, `SpaceCompassStarlink*`, `sgp4`:
  validated satellite elements and SGP4 propagation.
- `SpaceCompassHorizonsEphemeris`: validated JPL ephemerides and motion parsing.
- Copied presentation helpers: accessible controls, dropdowns, bounded dialogs,
  scrollbars, numeric/date formatting, title overflow and screen scaling.
- `SpaceCompassErrorLog`: independent bounded app-private error/crash log.

Astronomy calculations and downloads remain off the UI thread according to the
existing refresh policy. Orientation updates project cached paths; they must
not regenerate orbital calculations, decode textures or dismiss open dialogs.
Moon live/edge markers share one current phase per UTC minute. The textured
viewer and all asset credits are described in `docs/CELESTIAL_VIEWER.md`.

## Testing boundary

Unit tests cover celestial calculations, geometry, selection, formatting,
localization and source-level UI/security contracts. Ported instrumentation tests
use synthetic location and orientation rather than recording a user's position.
They need a device or emulator; compilation alone is not execution.

Keep settings, release signing, persisted object selection and onboarding
within this application's boundary. Do not reintroduce UVIR sensor
credentials, database migrations or hardware services into this project.

## Presentation preferences and page boundaries

`SpaceCompassAppPages` owns navigation and retains the main compass composition.
`SpaceCompassSettingSpecs` defines the choices, `SpaceCompassSettingsChoices`
renders their shared controls, and `SpaceCompassInfoPage` owns information and credits.
`SpaceCompassRegionalUnits` resolves unit defaults without an Android/UI dependency.

`SpaceCompassPresentationSettings` observes only presentation keys, once per provider.
The provider and choice rows share the same immutable snapshot; object selections and
remote-cache metadata do not invalidate it. Configuration resources are memoized by
system configuration, theme and language, rather than every preference write.
Missing preferences remain system defaults, and existing storage keys are preserved.
Device-region formats remain independent of the explicitly selected interface language.

The complete 20-language resource sets are checked for exact key parity, placeholders,
empty text, duplicate keys, broken UTF-8 and untranslated long English sentences.
Catalog identifiers, scientific unit symbols and proper names can legitimately match.

Island-based pages use the shared `SpaceCompassPageSpacing` defaults: 10 dp outer
horizontal margins and 12 dp island horizontal padding, with existing vertical spacing
and accessible touch targets preserved. New pages should use these same defaults.

Secondary page toolbars have no separate background fill. Island content begins
directly below the toolbar with no top content margin; the main toolbar is independent.

## Celestial catalog filters

The catalog page owns complete observation/range snapshots for the current minute and
quantized GPS observer. Its previous completed snapshot remains displayed during a refresh. Its filter panel combines an OR-set of object types with
a geometric-horizon visibility filter. Missing/stale satellite positions remain
available only under All. Filters never mutate object selection. Select-all acts
only on displayed objects and preserves hidden checked objects and a valid active
object. Both the back button and Android Back first collapse the animated panel.
Earth centre is a guide cross, not a selectable object. Legacy selection identifiers
are migrated without resetting other preferences; zenith and pole crosses share the
reference-guide renderer and the capture visibility rules.

Each catalog row separates its actions: the checkbox changes scene selection, while
the thumbnail/name opens the Observatory, including for unchecked objects, without
changing scene selection or the active compass target. The sort value stays passive. The retained, unplaced catalog keeps its search, filters and scroll
position while the viewer is open; it exposes no hidden touch or accessibility nodes.
Both toolbar Back and Android Back return to that catalog before leaving it.


Catalog sorting uses localized collation for names, a heliocentric-AU snapshot for
Sun-distance order and a separate geocentric-km snapshot for Earth-distance order.
Earth-distance rows show the exact range used to sort, in the selected units; the
reference is Earth's centre, not the GPS observer or satellite altitude. Validated
SGP4/Horizons windows and the local planetary/stellar models supply the ranges.
Neither distance mode substitutes the other's values when a model is unavailable.
Missing distances stay last in both directions;
ties retain catalog order. `SpaceCompassCatalogPreferences` restores and immediately
persists `catalog_sort`, `catalog_types` and `catalog_visibility` together in the
app-private SharedPreferences file. Both app/process restarts and device restarts
restore the chosen values. Missing keys keep existing defaults and unknown enum
names are discarded safely. Filter reset saves the empty type set and All visibility
without changing the sort, selected objects or the active compass target. The panel's
open/closed state remains transient. Select-all is
unlabelled above the checkbox column and still affects only filtered rows.


Point captions share a compact time slot: the localized wall-clock moment and
UTC elapsed/remaining duration alternate every two seconds. T− counts down to
future points; T+ counts elapsed time after them. Both use whole hours and minutes, without seconds.
Hours can exceed 24 and durations do not inherit timezone/DST offsets. The live
screen clock feeds both badge and selected panel; each reserves the measured
width of both alternatives to retain right-edge angle alignment. Point names
remain fixed, only the time slot slides. Invisible text is excluded from
accessibility, and reduced-motion settings suppress the sliding transition.

## Stable 1.0 release boundary

The release variant enables AGP 9.3 R8 code/resource optimization with default Android
and dependency consumer rules. App entry points are manifest-declared and JSON/maps
use explicit parsing rather than reflective object serialization. No broad keep rules
are introduced. Distribution keeps all 20 locale catalogs and original credited images.
Texture decoding is sampled and capped to device limits, while decoded GPU images are
released after upload. Native map split-layout parameters are preallocated and reused.

API 26-compatible `LocationCompat` clears vertical accuracy when only altitude is
simulated; real horizontal accuracy remains intact. AndroidX ExifInterface 1.4.2 writes
capture timestamps with explicit UTC offsets on all supported versions. Caption time
and location-disclosure policy remain frozen independently of these EXIF timestamps.
Composition-observed configuration/resources update preference examples and callbacks
when locale/theme changes. The initial 1.0 release used display version 1.0 and protocol version 1.0.0.
Current release 1.4.7 uses the same display/protocol version with code 44; signed
metadata, APK name and tag share that identity.


## Camera and capture responsibilities

`SpaceCompassCameraPreview` only binds Compose state and the native preview view.
`SpaceCompassCameraPreviewController` owns the camera worker, session, surfaces,
exposure-matched JPEG metadata and bounded close/open handoffs. Device choices are
memoized independently of orientation updates. Zoom policy and discovery remain
separate from camera IO and projection/field-of-view calculations remain pure.
Preview decoding owns its bitmap across the IO cancellation boundary and transfers
it to the UI only after a successful return; unfinished images are recycled.

Capture previews always use the captured user presentation. Export prepares a
separate variant in the persisted selected/international profile. Location
privacy, label visibility, centre choice and exposure timestamps stay frozen in
capture snapshots. A change to export format never re-renders the visible preview.

Pluto is distributed at the existing 2048 x 1024 runtime texture limit, avoiding
an oversized decode and resize. LV-426 remains a bundled lossy WebP map. The
eighteen external images live in the versioned public `celestial-textures-v1.1` pack;
reference copies are outside the APK asset tree. A shared two-request queue
verifies HTTPS downloads against embedded sizes, SHA-256 digests and dimensions
before atomic publication to private no-backup files. UI, GL and export readers
use the same persistent files. Visible models use a provisional colored sphere
until a map arrives, then update without changing their observation or rotation.
Downloads retry while their miniature/viewer is visible and failures are bounded
by a 30-second cooldown. Astronomy and trajectories work without image downloads.
Credits and longitude conventions remain unchanged.

Release 1.2.0 keeps Titan ephemerides/axis calculations in `SpaceCompassTitan`,
archival image rendering in `SpaceCompassCatalogPhotograph`, and physical
reference tables separate from UI. The image-policy lookup is indexed by filename.
Pack 1.1 is pinned to an exact GitHub release; legacy caches are imported only
when their byte count and SHA-256 match the current APK. Import uses the same
atomic publication path as downloads and leaves previous files intact. Remote
manifests never change the APK's trust policy or image pins.


## Pointing reference and capture label layout

`SpaceCompassPointingAxis` derives a virtual view along the phone's physical top
edge from the display-remapped sensor basis. Camera mode always retains the
optical axis. The reference choice persists in the existing private preferences;
changing display rotation never substitutes the phone's short edge.
`SpaceCompassPointingAxisButton` renders the two physical poses with neutral
native vectors, and is disabled while the camera is active.

`SpaceCompassDailyPathPointCaption` keeps the point name above its clock and
relative time. Key events share a theme-aware gray row background. Both camera
and panoramic captures use `SpaceCompassPanoramaPointLabel` for localized frozen
current/event text and `SpaceCompassPanoramaLabelLayout` for measured collision
avoidance. Current and key-event labels are placed before hourly labels; labels
are omitted when no clear position exists. Leaders are drawn before backgrounds
and text. Two scaled pixels of the original path color outline each label.


## General settings and ambient audio

The General section retains the existing appearance navigation/resource identity;
all twenty visible section labels are updated together. Its compact cog glyph is
distinct from the main toolbar menu. Theme, display and music use the existing
settings geometry and colors. The pointing-reference glyph uses localized bold 90° beside a vertical line or
0° above a horizontal line. Paint and digit strings are remembered; the visible
zero is optically centered over the bar and both move together inside the circle.
The actual orientation calculations are unchanged.

`SpaceCompassAmbientMusic` observes only two saved audio preferences, avoiding
invalidation of celestial data and the main scene. The activity-owned player applies
saved settings before its first foreground callback and retains the pure focus/
lifecycle policy. `SpaceCompassAmbientMusicOutput` owns one serialized PCM worker
and atomically cancelled native AudioTrack sessions. Pure, reusable oscillator,
filter, compressor and FFT/convolver modules generate the original website space
score continuously off the UI thread. The recorded Ogg is no longer bundled.
No extra library, service, permission or network is required. Fresh settings enable
music at 20%; saved disabled choices remain respected. Volume gestures preview
immediately and persist only when completed, in ten-percent steps.

`SpaceCompassSettingsIslandReveal` shares UVIR's 300 ms top-anchored expansion,
FastOutSlowIn easing and fade for music and the four scenario toggles. All body
spacing/dividers belong to the animated area; heading padding stays stable. Closing
controls are immediately inert and excluded from accessibility; uncommitted music
volume previews are cleared when hidden. Scenario handlers and saved drafts remain
owned by the page independently of body visibility.


## Compass status and maintenance release 1.2.4

`SpaceCompassSunCompassReliability` provides the pure issue classification shared
with the lifecycle-scoped sensor listener. Fresh field anomalies, an uncalibrated
sensor, low accuracy and stale/recovering readings have distinct states. The
existing field tolerance, sample freshness, north reference and recovery period
remain unchanged. Six JVM regressions cover the boundaries and precedence.

`SpaceCompassSunStatusNotices` retains exclusive data/retry priority. A single
annotated paragraph puts the native clickable link immediately after its sentence,
without a separate right-hand action column. Links use the theme primary blue/cyan;
routine text uses the regular foreground. Only magnetic interference, calibration
and outdated orbital data use theme-aware orange. All twenty catalogs retain
localized sentence endings and three-dot ellipses for orientation, orbital-data and
location loading messages, while state-action labels have no final punctuation.
The outdated-data notice is generic; it no longer needs an object-name argument.
Celestial ephemerides and camera transforms remain independent of presentation.
The shared capture button chooses a shutter or panorama glyph from camera mode;
export keeps its separate translated image-dialog title and saved profile.

Version 1.2.4 retains the twenty existing in-app news summaries byte for byte. The
information archive records current resources/docs while image pack 1.1 retains
its immutable individual URLs, byte sizes, dimensions and SHA-256 pins.

## Idle screensaver

See [the screensaver architecture](docs/SCREENSAVER.md). Its retained activity
policy and isolated settings do not invalidate astronomy or presentation state.
An activity-scoped solar-height sink reuses the retained sky's GPS/scenario calculation;
the visible cover samples it every 30 seconds without registering extra sensors.
Earth lighting is independent of the interface theme and uses the shared solar bands.

## Continuous solar lighting

Sky, ground and atmospheric effects share a smooth solar-height lighting sample.
Panorama capture freezes it; live weather reuses its geometry as lighting changes.
See [the lighting model](docs/SOLAR_LIGHTING.md).


## Main controls and release 1.2.5

The pointing viewport keeps the optical frame stable beneath its translucent
header. Measured corner controls and the selected-point panel still exclude
captions and edge locators. Camera/axis/references form the start-side L;
scenario/objects/weather form the mirrored end-side L. Layout direction mirrors
the arrangement in RTL. A shared bottom row places camera zoom before capture.
Its preset menu sizes itself for space above or below the anchor, retaining the
adaptive scale and internal scroll indicator. The 14 sp semibold value uses the
existing control size. Status notices sit eight dp below the reticle's outer arms;
their measured exclusion follows that same centered placement. Existing data/retry
precedence, colors and independent inline action semantics remain.

The expanded main layout assigns 55% to the sky and 45% to details when the latter
can retain its minimum readable width. Object navigation keeps symmetric visible
chevrons and existing 48 dp targets, reserving more of the header for its name.
Version 1.2.5 updates all twenty news summaries and preserves external texture pack
1.1, cached images and scientific image/ephemeris trust policies.


## Release 1.2.6: retained pages and direct point navigation

SpaceCompassScreensaverContentLifecycle isolates page work from the activity-owned
saver/audio lifecycle. SpaceCompassScreensaverWindow owns immersive bars and restores
them on dismissal; Footer/Info handle time/date/battery without notification access.
SpaceCompassSunSensorWatchdog performs monotonic sample-freshness checks and bounded
registration recovery; reliability classification does not assert magnetic causation.
SpaceCompassControlNotices limits feedback to meaningful state transitions.
SpaceCompassSceneOptics keeps the reticle within the title/content viewport.

Sky markers pass the complete current-point map, not only projected visible targets,
into daily navigation. SpaceCompassDailyPathScroll clamps row centering to the real
scroll extent. Fixed object/date/angle headings are layered above the clipped list;
temporary selected-row state clears on return. Shared capture label formatting uses
the frozen capture instant and the requested preview/export number profile.


## Release 1.3.0: shared selections, observations and exports

`SpaceCompassSunDailyPathUiState` owns session point selections independently of
the currently displayed object. Orbit markers and daily rows toggle the same
stable keys; retained captions alternate clock/T± without regenerating ephemerides.
The former automatically opened daily page and selected-point island are replaced
by multiple pinned captions. A frozen capture copies selection keys and observation
time, so later live changes cannot alter its exported rows or rings.

`SpaceCompassDataExportSource` describes the immutable source and permitted file
types. `SpaceCompassDataExportController` manages explicit preparation and delivery;
`SpaceCompassPanoramaExportPage` renders the shared capture/daily/Observatory choices.
`SpaceCompassOrbitExportTable` and `SpaceCompassOrbitExportText` keep hourly, current,
selected and key-event entries chronological by their absolute instant.
`SpaceCompassObservatoryExport` reuses typed detail sections and separates current
and rotated images. Fresh openings have no checked file types; only the formatting
profile is persisted. Completing delivery returns to the retained source page.

`SpaceCompassExportLocation` and `SpaceCompassExportPositionChoices` share the
complete/area/hidden disclosure policy. Captures initialize it from their preview;
daily exports initialize it to area on every opening. Hidden presentation uses UTC
without changing the observation zone, astronomy geometry or relative durations.
`SpaceCompassPageExportCache` keys and the panorama variant key separate disclosure
and formatting. JPEG re-encoding excludes GPS EXIF; coarse geocoding does not
fallback to the precise locality. Sky angles are not a guarantee of anonymity.

`SpaceCompassCelestialDetailData` and `SpaceCompassCelestialDetailSections` group
identity, physical/stellar, atmosphere/temperature, orbit and spatial context.
Pure identity, core-temperature and spatial reference modules retain source and
uncertainty qualifiers, separately from table rendering and observer-dependent data.
The largest application source module remains below 700 lines. Release R8/resource
shrinking stays enabled; no new service, permission or native dependency is added.

`SpaceCompassCaptureCaption` shares compact translucent heading metrics across
photos and panoramas. Four border scales use the frozen projection and exposure
geometry. Main information cards share the full opaque settings-island surface;
floating control fills are also opaque. The title overlay remains translucent.


## Release 1.3.4: display-only resume memory

SpaceCompassSceneResumePose retains an accepted pose for decorative scene continuity
across pause/resume and remaps the two screen axes when rotation changes. It never
restores measurement validity or camera capture attitudes. Current sensor usability
continues to gate live point hit targets and readouts. Background registration and
watchdog behavior are unchanged. StarField retains its last basis while inactive;
StarTextureView skips unchanged drawings. ScreensaverView overlays a native return
hint with display-cutout margins without resizing the globe or changing its timing.


## Release 1.3.5: shared time labels and overlay input

PointCountdown remains the single formatter for relative time in views, frozen
captures and tabular exports. SceneTargets applies chrome exclusion to both tap
and target coordinates; SceneInteraction reads the latest exclusions without
restarting its pointer coroutine. Main title and information-island input shields
absorb passive-surface taps while preserving child actions and drag scrolling.
The full-scene decorative backdrop and free viewport projection stay separate.


## Release 1.3.6: key-event capture labels

PanoramaLabelMode owns shared path-event/current eligibility, including coincident
events. KEY_SELECTED shows key events for all captured bodies and retains selected
captions through the existing selection pipeline. Camera and panorama renderers
use the same methods; variant keys keep each policy's generated image distinct.
DEFAULT is independent of display order. Nullable legacy input distinguishes an
absent preference from an explicitly saved on/off switch during upgrade.


## Release 1.3.7: scenario clock and seasonal screensaver

ObserverClock resolves independent calendar/clock components and persisted real-time
offsets; ClockEditing preserves editor values through timezone/DST changes. Draft
validation, Summary and ChangeNotices separate stored plans from incomplete typing.
The summary and orange badge count the same five independently enabled sections.
ObserverIslandReveal scrolls newly expanded cards into the available viewport.

ScreensaverState owns session-only last-valid coordinates and the effective observer
plan. ScreensaverSeason derives apparent roll/pitch from solar longitude and mean
obliquity. Host refreshes scene data once per minute while visible; View sends only
finite numeric updates to the bundled, network-disabled WebView. Shader, software
fallback and marker share east-positive coordinates and seasonal transforms. The
fallback caches source maps/normals and caps sphere rasterization at one frame per
second; reduced-motion startup still renders a complete frame. Existing pause,
music, native clock/battery, touch-to-dismiss and lifecycle contracts remain.


## Release 1.3.8: daily-path emphasis and capture chrome

DailyPathPointCaption combines optional decorative event vectors with stable
two-line text. The row's key/current condition controls both caption lines and
both angle columns, including satellite-pass and coincident-event rows. Event
symbols do not change point identity, selection or time data. Live Compose and
native capture renderers both use plus/minus for culmination/minimum; exported
camera/panorama variants reuse the native marker helper.

The shared capture toolbar and daily-path toolbar use the standard page palette
at full opacity. Android status backgrounds already use that same palette. The
daily light-mode table remains white, and the main scene's translucent overlays
retain their separate policy.


## Release 1.4.0: fluidity, stabilized motion and Android handoffs

SkyFluidity owns stable saved level identifiers and sensor/coalescing periods,
with Balanced as the missing/invalid-preference fallback. Settings and listeners
share this policy. SunOrientationSmoothing uses monotonic elapsed time; the bounded
SunMotionStabilizer predicts short physical rotations from gyro increments and
corrects toward accepted magnetic/gravity axes. Re-registration, invalid references,
display changes and lifecycle stops reset prediction. A stale/missing gyro falls
back to the existing absolute filter. Screensaver/background stop sensor listeners.

PointReminder is the pure eligibility/Clock-versus-Calendar policy; the separate
PointReminderIntent is the Android handoff boundary. EventSettings controls only
the focused main-screen bell. A shared foreground ticker expires reminders without
per-row timers. Calendar/Clock own scheduling and confirmation; no local alarms,
background service, calendar database or observer-coordinate handoff is introduced.

Tesla Roadster reuses the guarded Horizons/cache pipeline, catalogue models and
frozen exports. Its separately pinned archival photograph follows the existing
download/cache layer. Missing data remain explicit; enum identifiers and saved
selections retain their meaning. Additive texture assets preserve legacy URLs.

Main object names and shared photo/panorama legend swatches use full identity colors,
independent of elevation. Below-horizon scene paths keep their separate dimmed style.
Only application chrome excludes focused callouts: celestial markers are drawn
beneath them and cannot cause placement feedback. The bell aligns to the callout center.


## Release 1.4.1

The foreground weather owner distinguishes loading, usable cached data and failure. Bounded requests retain TLS checks and expose a manual retry without losing status priority. Point callouts and their reminder share a translucency constant; count badges and scenario accents share their component palette. Object removal keeps its confirmation and a narrower horizontal hit region without changing navigation targets. Texture policy pins twenty external files in pack 1.2.1 and checks legacy caches before downloading. Neither runtime ZIP extraction nor a remote executable manifest is introduced.


## Catalogue 1.4.2

The expanded target registry separates stable remote IDs from names and catalogue order. A checked generic SGP4 adapter adds Hubble/Tianhe without changing ISS/Starlink identities. The remote owner restores and merges new satellite caches by body; independent Horizons pointing/motion requests retain their refresh and cancellation policy. Apparent states support velocity-aware Hermite interpolation with a position-only legacy fallback. See [catalogue sources and limits](docs/CATALOG_1_4_2.md). Pack 1.3 pins 27 individual external WebP files, importing matching prior caches and showing symbolic placeholders without blocking astronomy. No new celestial raster enters the APK and no new texture ZIP is distributed.


## Settings and distribution 1.4.3

Device theme metadata is captured before the application appearance override, keeping the System example independent of a forced light/dark selection. General uses keyed, independent staggered-grid items with the existing shared spacing and natural heights. Scenario speed uses an explicit compact value style. The 53-object catalogue, scenario clock and time-coincident close approaches are documented in their dedicated modules. All 29 celestial images pin graphics pack 1.4; legacy cache import and public URLs remain compatible. No complete texture ZIP is added.


## Additional catalogue registry

`SpaceCompassAdditionalCatalog` registers the 47 additional stable enum identifiers,
categories, scientific names and identity-pinned Horizons targets. Its reference,
facts, image and search-alias modules keep data separate from Compose screens.
Existing observation, refresh/cache, daily-path, selection and export pipelines are
reused; no parallel catalogue/calculation engine is introduced. See
`docs/CATALOG_100.md` for sources and versioned graphics-pack policy.

## Distribution 1.4.4 and independent previews

The catalogue has 104 ordinary objects, with hidden LV-426 outside that count.
All 56 full images and 56 independent previews pin graphics pack 1.5. Previews
are 128 x 128 WebP Q90/method 6; each has its own length, dimensions, SHA-256 and
parent-image credit. Catalogue/live/captured glyphs request only previews. Full
images are requested by the Observatory and image exports as needed. Previously
cached full maps remain a local fallback without triggering a catalogue download.
The in-memory preview LRU is capped at 4 MiB; verified persistent resources retain
offline availability. Publication keeps older releases and individual URLs intact.
Nadir uses exact local -Up. Live reference placement retains continuous curve
anchors and stable cross captions; frozen captures use independent collision layout.


## Catalogue and graphics 1.5.0

The append-only identity catalogue has 999 selectable entries and a chunked O(1) registry.
Scientific/reference text and twenty-language resources are bundled; graphics alone are external.
All 88 constellation patterns use linked astrometric stars. Direction-only targets do not invent parallax
or distance. Horizons and TLE adapters retain identity, coverage and freshness checks; unknown fields stay absent.
Only selected objects enter angular close-approach work, with conservative spatial screening.
The canonical source and deterministic generators are documented in `catalogue/README.md`.

Graphics pack 1.6 provides 174 full WebP resources and 174 independently pinned previews.
The app requests canonical individual URLs, validates bytes/dimensions/SHA-256 and imports matching legacy caches.
No remote catalogue text or executable manifest is loaded. No complete graphics ZIP is generated.
Main object actions preserve 48 dp touch targets and their independent name insets; only their two trailing
gaps expand slightly. Night island action circles invert to a light neutral fill with dark glyphs;
daytime actions, viewer tabs and floating controls keep their existing styling.


## Location management and filters 1.5.3

One retained observer editor owns the Location management menu and its seven child routes.
ScenarioMenu and ScenarioEditorChrome separate navigation and shared controls from form logic.
ScenarioCurrentValues resolves the applied observer without modifying clock anchors; child
forms retain editable drafts without enabling overrides. Dormant values persist in live mode;
the management menu owns activation and displays effective values. Position summaries read the existing place
cache. ExportSnapshot completes one portable copy using the same effective-value resolver,
without introducing a new observer state, schema or device-location injection.

Catalogue origin/pointing preferences extend the existing stable filter keys. Calculation-keyed
observations prevent availability results from leaking across scenario changes. Object rows and
Type dropdown choices use neutral theme surfaces; the removed category palette has no runtime
consumer. Selection remains explicit in checkboxes, bold names and thumbnail rims. The search
input and placeholder share typography and centered alignment inside a 40 dp outline, preserving
the existing 48 dp header touch space. Scientific ephemerides, image pins and catalogue identities
are unchanged.


## Catalogue presentation and live reticle 1.7.1

SpaceCompassCatalogPresenter owns a per-visit immutable localized search index and bounded derived caches.
Its Default-dispatched, mutex-protected work prepares filtering, sorting, selected-row totals and optional
facet counts without catalogue-sized work in composition. Effect cancellation prevents obsolete queries
from replacing the current presentation. Checked objects retain the visit's priority snapshot; favorites,
search and every filter keep their existing intersection and checked-object exception rules. Complete
facet counts are computed only for an open panel, in one pass; pending values use a dash rather than zero.

The row-scoped load gate waits 160 ms after dragging/flinging stops before allowing new optional image
work and visible-object prefetch. Existing verified pixels remain drawable. An image request already
started may finish; lifecycle cancellation, download integrity, retry policy and selected/observed target
updates retain their existing behavior. The main sky and Observatory do not inherit this browsing gate.
The initial circular progress item fills only the list viewport and takes the active theme primary color.

sky_reticle_visible defaults to true and persists independently of panorama_label_mode. Disabling it
suppresses the live reticle, focus captions and their reminder action, including the backdrop's automatic
focus highlight. Orbit-point selections and capture-label settings are not changed. Generated symbols
receive circular 82%-black backing in live thumbnails and rendered capture markers; photographic pixels
and surface-map previews keep their original content.


## Floating controls, native-map layout and snapshot publication 1.7.2

The top-center scene group contains independent Info/logo and Menu controls at the same compact size. The logo opens the existing Info page directly; the menu uses its own normal start-aligned popup anchor. Bottom-start collapse and pointing-axis controls retain their row and persistent choices. Shared ink fitting uses 53% of the circle's largest dimension while compensating stroke width separately; optical translations apply after fitting and do not change the hit region. Weather and label glyphs balance their normal and slashed states independently. Capture symbols use white/dark foregrounds for day/night; the white border and pressed-surface animation remain independent.

The Location position editor keeps ordinary vertical scrolling with a disabled edge overscroll effect. Its native WebView is a sibling layer positioned and clipped from the Compose map slot. Stretching only the Compose renderer would deform text/buttons independently of that layer. The map spans the island width with square corners; descriptive/form content retains the shared inset. Visible native hit regions continue to clip at the page viewport, protecting the toolbar while scrolling.

Catalogue observation and presentation snapshots publish atomically. A background observation refresh does not invalidate displayed user-scope controls. Facet counts keep their last complete presentation through panel transitions, while closed panels still avoid recomputing facets. Initial observation work uses a latched first key until completion so a rapidly advancing simulated clock cannot continuously cancel the first catalogue load. User-scope changes still invalidate bulk actions until the corresponding presentation is ready.
