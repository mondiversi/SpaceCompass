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

Island-based pages use the shared `SpaceCompassPageSpacing` defaults: 8 dp outer
horizontal margins and 14 dp island horizontal padding, with existing vertical spacing
and accessible touch targets preserved. New pages should use these same defaults.

Secondary page toolbars have no separate background fill. Island content begins
directly below the toolbar with no top content margin; the main toolbar is independent.

## Celestial catalog filters

The catalog page owns a single observation/range snapshot for its opening time and
quantized GPS observer. Its filter panel combines an OR-set of object types with
a geometric-horizon visibility filter. Missing/stale satellite positions remain
available only under All. Filters never mutate object selection. Select-all acts
only on displayed objects and preserves hidden checked objects and a valid active
object. Both the back button and Android Back first collapse the animated panel.
Earth centre is a guide cross, not a selectable object. Legacy selection identifiers
are migrated without resetting other preferences; zenith and pole crosses share the
reference-guide renderer and the capture visibility rules.


Catalog sorting uses localized collation for names and a separate heliocentric-AU
snapshot for numeric distances. Nearby Earth-centred/GPS display ranges never
serve as solar-distance sort keys. Missing distances stay last in both directions;
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
Current release 1.2.3 uses the same display/protocol version with code 24; signed
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
music at 50%; saved disabled choices remain respected. Volume gestures preview
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
