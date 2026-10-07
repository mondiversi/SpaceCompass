# Orbit names and panorama capture

## Vertically centered caption — current local update

The fitted single-line caption is centered vertically between the upper image
edge and the top angular grid line, using the shaped text layout's actual height.
The renderer and caption share a 144 px heading at the 4096 px export width,
scaled proportionally for other resolutions. This leaves room for the unchanged
44 px reserved extent of a current object at +90 degrees. Horizontal centering,
text fitting, the full 360 by 180 degree scene and its 2:1 projection are retained.
The same bitmap layout serves preview, Save, Share and all disclosure variants.

Capture uses a neutral shutter circle at the bottom trailing corner of the main
sky viewport, including its left column in landscape. It has a localized
accessible label and is disabled while generating or awaiting camera readiness.
The separate upper sky controls are Camera, Celestial references and Objects.
Camera and celestial references are initially off; the initial checked objects
are Sun and Moon only. Captures freeze the current explicit guide-toggle state.
The menu ends with Info, opening Info/news, after Appearance, Language, Units and Scenario.
A tap on Capture freezes the current
observer position, selected catalog set, time, cached remote inputs, available
trajectories and the effective observation timezone. Generation runs off the main thread and
writes a private, 95-quality JPEG preview. Capture itself does not publish a photo
or request storage permission. Later GPS/time changes cannot alter this snapshot.

The preview follows device orientation with pinch/pan, double-tap reset/zoom and
accessible zoom buttons. The image fills the page without reserving a toolbar.
Back overlays the upper leading corner, with the chevron's optical correction
mirrored in RTL. Zoom in and Zoom out form a vertical column directly below Back,
with the same 8 dp inter-control spacing and leading alignment. A trailing vertical
column contains Point labels, Position and Export in that order. All six controls
share the celestial selector's 48 dp circular surface, theme colors, outline and
elevation. Point labels is a checkable on/off control with a label-tag glyph:
both states retain the same neutral ink, outline and surface; only a diagonal
slash distinguishes OFF. Disabled controls retain their accessible states. Export
uses Uvir's upward arrow/tray geometry and optical correction. Controls stay fixed
while panning/zooming and never appear in the JPEG. Portrait begins filled and
pannable; landscape begins fitted. No forced orientation or system rotation change.

Export opens the shared neutral light/dark, bounded-scroll dialog. Its Format data
radio rows select Current language/formats or Scientific international. Below a
divider are Gallery, Save on phone and Share, with the app's full-width primary
outline, leading icon, centered uppercase label and pressed fill. Gallery saves
directly; Save on phone and Share open their respective Android interfaces without
an additional app destination dialog. System interfaces retain their own appearance.

## Custom observation context — current local update

Simulation can replace real GPS/current time with an app-local observer and
fixed instant. The panorama geometry, caption and point clocks use that selected
instant and zone and explicitly display Simulation. Available hourly weather estimates
for that place/instant are frozen into the same sky and caption; absent estimates
use a clear base sky and Weather unavailable,
GPS accuracy is not fabricated, and the altitude label is generic. Actual capture
time identifies the file, EXIF and gallery/storage timestamp; the simulated
reference time is visible in the image. Returning to Here and now restores normal
live inputs and the current device timezone. Disclosure remains independent and
defaults to Area, with no automatic gallery save or GPS EXIF. See
[OBSERVATION_SIMULATION.md](OBSERVATION_SIMULATION.md).

## Image contents

The image unfolds all 360 degrees of azimuth and all elevations from +90 to −90.
It includes eight cardinal/intercardinal directions with contrasting dots at their
exact positions on the horizon. Full direction names sit above the line; the
international symbols N, NE, E, SE, S, SW, W and NW sit directly below their same
markers, with space reserved against overlapping time pills. Azimuth ticks every 30 degrees,
and zenith/nadir plus intermediate elevation grid lines. The scene uses the app's
solar-phase/weather palette, clouds and illustrative ground, with rain, snow or
fog when those estimated conditions are available.

All selected objects retain their normal orbit colors, daily samples, chronological
direction arrows and dashed below-horizon paths. Seam and horizon crossings are
split without false long chords. When Point labels is ON (the initial default), every marked path point has a pill containing its local 24-hour time and
its own elevation in degrees, for example `18:30 · +24.5°`. Elevation uses the
international decimal point and one decimal; positive values are above the horizon,
negative values below it, and rounded zero is `0.0°`. The elevation comes directly
from each object's marker rather than from the time key shared across paths.
Point labels OFF removes only these time/elevation pills and their leader lines;
orbital dots, paths, arrows, curved object names, current object images and the
capture caption remain. This preference is stored independently for later captures
and retained after process/device restart.
Collision-aware placement measures the complete label and leader lines keep it
associated with its point. All point pills are drawn after all curves/orbit names,
so another object's line cannot cross their text; current images/names keep their
reserved space and remain the last layer. Current positions use the same drawing primitives and textures
as the live app, including the frozen lunar phase and spacecraft/ISS models.
Known objects are named on the scene without a redundant object legend.
Objects without a usable position or trajectory cannot be drawn; their live
status remains available in the app.

Object names follow readable curves near the direction arrows. Whole-string
Android shaping preserves complex scripts. A measured parallel baseline and
curvature guard prevent squeezed characters at tight bends; short, clipped or
colliding names are omitted rather than overlapping.

A single centered caption in its own slim heading contains the app name, date,
time and effective observation timezone offset, estimated place, latitude/longitude, horizontal GPS
accuracy, GPS altitude and estimated weather with cloud percentage. Dot separators
separate the values; the line scales to fit without wrapping. Scientific international
is the first-use profile: English labels, decimal-degree coordinates, metric/SI
lengths, decimal points, thin-space grouping, ISO dates (`yyyy-MM-dd`) and 24-hour
times. Current language/formats uses the frozen app language, resolved units,
coordinate mode, numeric format, date and clock format. Device defaults remain
independent of the app language. Both profiles translate object, direction, weather,
celestial-reference and pole labels, including orbital point times and elevations.
The profile choice persists for subsequent captures; it never edits live settings. Raw GPS values are formatted directly, including
negative altitudes and missing-data dashes; localized UI strings are never parsed
or reconverted. The live app's unit, number, language and time preferences stay
unchanged. The effective observation zone and frozen reference instant determine the
explicit UTC offset, for example `20:32 (UTC+02:00)`. The heading
and orbital time pills use that same frozen zone, including fractional offsets
and daylight-saving changes. Saving or sharing later cannot change their time.
Place lookup uses the frozen coordinates and the same bounded, cached geocoder
as Position details, even when that page has not been opened. The disclosure mode
controls which location fields appear, as described below. Missing data is not
invented. No lower information footer or side panel occupies the scene.

Output is 4096 × 2192 pixels (3072 × 1644 on lower-memory devices), keeping the
full 2:1 angular scene plus a slim separate heading. The fitted caption is centered
in that heading, with space for an entire current-object marker at +90° below it. Texture/science/weather attributions remain in the app and in
the JPEG Artist/Copyright EXIF fields. Temporary previews older than 24 hours are
cleaned only inside the app cache.

## Position in the capture

The preview's pin control opens the shared theme-aware, bounded-scroll dialog:

- Complete: estimated place, profile-formatted GPS coordinates, accuracy and altitude.
- Area: administrative region and country only; never city, district or county.
- Hidden: no explicit place, coordinates, accuracy or altitude in the caption.

The first-use choice is Area, omitting precise GPS details. Existing explicit
choices remain unchanged. The choice is
stored independently and remains after process or device restart. An invalid
stored value fails closed to Hidden. Every mode retains original capture time,
UTC offset, weather/cloud percentage, orbital geometry and scientific credits.
Missing structured region/country values are omitted; no full-address parsing or
city fallback can disclose finer detail. Time and sky geometry may still suggest
an approximate area, as explained in the chooser.

Changing Profile, Point labels or Position prepares a separate app-private cached
JPEG keyed by all three options, from the immutable snapshot and caption context.
Label changes never alter the selected disclosure mode, capture/reference time or
weather. A previous GPS-complete or label-enabled JPEG cannot be reused for a
different combination. It does not query a new GPS position or time, and
does not publish to the gallery. Pending/failed changes hide the previous image
and disable export actions until the requested variant is ready. Each file has its
own saved-gallery URI; the document picker freezes its selected source on launch,
and Share exposes only the currently displayed file. Fresh JPEG encoding prevents
GPS EXIF from another image being inherited. Only capture date/time, UTC offset,
software and attribution metadata are added. Hidden does not remove time or sky
geometry and is not a promise of complete location anonymity.

## Save and share

The Export dialog offers the image gallery, Android's destination picker or Share
for the selected profile. Capture and preview do not save automatically. Gallery output goes
to `DCIM/SpaceCompass`; the picker writes the same cached JPEG to the chosen URI.
Share opens Android's chooser with a narrow FileProvider URI, ClipData and temporary
read permission. Both actions use the prepared variant without rerendering or changing selected objects.

Android 10+ uses a pending MediaStore row and needs no camera or media-read
permission. Android 8/9 requests legacy write permission only for Gallery save,
atomically reserves a new filename (including a suffix for another profile of the
same capture) and scans it. Incomplete gallery writes are removed on failure.
EXIF records capture time, milliseconds, UTC offset and app version, without GPS
EXIF tags. Only position information permitted by the selected mode appears in the
top caption. Cancelling the picker or share sheet leaves the preview available.
A successful Gallery save displays a short confirmation. Repeating Save/Gallery
reuses the existing readable photo; a deleted photo can be saved again.

Storage follows [shared-media guidance](https://developer.android.com/training/data-storage/shared/media),
[secure sharing](https://developer.android.com/develop/ui/compose/sharing/send)
and [ExifInterface](https://developer.android.com/reference/androidx/exifinterface/media/ExifInterface).

## Validation

The current position-disclosure update passed 501 JVM tests and debug/release
builds; Android test sources compiled without connected execution. Nine new JVM
checks cover disclosure, structured-area fallbacks, persisted modes and frozen
context. Two compiled Android tests cover separate cache variants and fresh EXIF
encoding. A physical Android 12 Samsung check verified the external object notes,
all three profiles on one frozen capture, retained choice after process restart,
explicit hidden Gallery save with no GPS EXIF, credits, repeat-save reuse and
share-chooser cancellation. All twenty catalogs have 365 matching translatable
keys. Current runtime limits and APK identity are in [VERIFICATION.md](VERIFICATION.md).

Earlier international export validation:

All 492 JVM regressions passed, including seven international export checks for
metric/raw GPS values, signed below-sea-level altitude, unavailable measurements,
cloud percentage, ISO/capture timestamps, seasonal and fractional timezone
offsets, and independence from later system locale/zone changes. Debug/release
builds and Android test-source compilation passed. Android instrumentation was
not executed for this revision.

A physical Samsung phone check used Italian UI, European numeric presentation
and a temporary feet/miles app preference. The saved 4096 × 2048 JPEG retained
English labels, decimal-degree coordinates with decimal points, metre GPS values,
ISO date and 24-hour capture time with `(UTC+02:00)`, all on one line. Save
occurred more than a minute after Capture; the caption, filename and capture EXIF
retained the original time. Preview did not publish automatically, repeated
explicit saves did not duplicate the photo, and credits remained embedded. Zoom,
share chooser cancellation and Back worked. The original default distance
preference was restored and verified. Tablet/RTL/legacy-permission execution was
not repeated. Earlier fixture results and current execution limits are recorded
in [VERIFICATION.md](VERIFICATION.md). These changes remain local and unpublished.


## Event symbols and translucent point labels — October 6 local follow-up

Orbital event markers use the same vocabulary as the live orbit: upward triangle
for rise, downward triangle for set, diamond for culmination/maximum and horizontal
bar for minimum. Hourly points remain plain dots. Special markers are slightly
larger with a dark halo so the inset glyph remains readable at export resolution.
For a coincident event, the merged point's representative event is drawn just as
in the live orbit. Dots/symbols are drawn after every orbit stroke and wrapped at
the 0/360-degree seam; time pills reserve their space. They remain visible when
Point labels is OFF. Current-object imagery remains the final layer.

Time/elevation pills are always near-black with white text, in both app themes.
The background uses ARGB `0xD9101418`: approximately 85% opacity / 15% transparency,
letting the sky/ground subtly show through while keeping the text fully opaque.
Object-colored outlines and leader lines remain. This supersedes the earlier
light/dark pill palette; no theme field is needed in the snapshot. Save, Share and
label/disclosure variants render the same translucent style. The JPEG flattens
the background blend into the scene. Solar sky phase, event symbols, geometry,
capture time, export units, credits and selected preferences remain unchanged.


## Capture control and orientation update (2026-10-06)

Capture now lives after the object selector, in a matching 48 dp shutter circle;
it is removed from the menu. Normal mode renders the complete 360° panorama.
Camera mode requests an actual still JPEG and reprojects the selected objects and
paths into the entire lens field, including areas covered by the landscape main
page's data column. See CAMERA_VIEW.md for exposure/optical metadata and lifetime.

The common preview no longer requests landscape orientation or rotates its layout.
It follows the device. Portrait starts filled and can be dragged horizontally to
explore the captured image; landscape starts fitted and supports pinch/zoom/pan.
Preview framing never crops the saved/shared file. Save remains manual. Camera and
synthetic captures support the same frozen location disclosure and point-label
variants, recreated from their respective immutable sources.


### Celestial reference guides — 2026-10-06

New panoramas and rear-camera photographs include dashed celestial-equator and
terrestrial-parallel sky projections: the Tropics of Cancer/Capricorn and
Arctic/Antarctic Circles. The mean obliquity of the captured/simulated date sets
the tropical latitudes; polar latitudes are their complements. These are
declination circles projected from Earth's axis, not the ecliptic, local
altitude circles, or the locations of geographic landmarks.

Crosses identify the exact geometric north/south celestial rotation poles;
Polaris is not used as the pole. Frozen observer latitude controls all guides.
The panorama splits the 0/360-degree seam and handles undefined azimuth at
zenith/nadir. Camera guides share the actual exposure lens and attitude, clip
to the full photograph, and require usable pointing and a known observer
position. They remain in save/share variants when hourly labels are hidden.
Guide labels follow the existing international English export convention;
there are no new app-interface strings or translation fallbacks.

Reference definitions: https://aa.usno.navy.mil/faq/asa_glossary
Coordinate conversion: https://aa.usno.navy.mil/faq/alt_az


### Main-view celestial references — 2026-10-06

The main pointing viewport also displays the dashed celestial equator,
terrestrial tropic/polar-circle sky projections and exact rotation-pole crosses,
in both virtual-sky and rear-camera modes. All references use the same observer
latitude, true-north attitude and local perspective as the bodies and orbits;
camera guides wait for actual lens geometry. Missing position/orientation or
unusable compass data suppresses guides rather than inventing an alignment.

The live layer is clipped to the pointing pane, decorative and non-interactive.
Its seven captions are localized in all 20 languages. Caption placement avoids
notices, selection controls, current/off-screen bodies, event dots, selected-point
panel and reticle; if no clear caption slot exists, the guide remains drawn.
Dense 0.5-degree export sampling is retained. Live 2-degree directions are cached
across orientation updates and clock ticks; the date key updates mean obliquity
at UTC day changes. Existing international English annotations in saved captures
remain independent of the live UI language. Arabic/Hebrew/Persian guide captions
are shaped with the Android text layout engine.


### Earth centre and zenith references — 2026-10-07

The reference-guide layer also contains geometric Earth-centre and exact zenith
crosses. Captured guide/annotation preferences and language profiles govern these
in both panorama and camera photos. Celestial circle/pole projection remains
unchanged. See EARTH_REFERENCE_POINTS.md for geocentre/vertical handling.


## Curved circle captions — 2026-10-07

Equator, tropic and polar-circle names/degrees now follow the projected reference
curves in live views and both capture modes. Point/cross captions remain horizontal.
See CURVED_REFERENCE_LABELS.md for shared orbit-text shaping, reserved-space rules,
annotation/export preferences and validation.
