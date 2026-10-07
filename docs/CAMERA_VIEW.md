# Live rear-camera view

The camera toggle is the first circular action above the sky, followed by celestial
references and the celestial selector. It is outside the title toolbar. A slash
indicates off; the ordinary camera indicates on. It starts off on each cold app
start. Celestial guides also start off until explicitly enabled; the initial
checked object set contains only Sun and Moon. Existing stored object selections
remain independent of these first-use defaults. Capture is a separate neutral shutter circle at the bottom trailing corner
of the sky, including the left sky column in landscape. In portrait, selecting
a point puts its panel at the bottom of the sky and moves the complete zoom/shutter
row immediately above it. Closing the panel returns the row to the bottom. Landscape
keeps the selection above the data in the right column and the capture/zoom row at
the bottom of the left sky column. The optical viewport does not resize, and
captions/reference labels/edge locators reserve the measured controls and panel
bounds in both layouts, including RTL.
When camera mode is off it generates the synthetic 360-degree panorama. When on,
it requests a still rear-camera JPEG and draws frozen celestial overlays on it.
The common preview has one Export control with the same two frozen presentation
profiles and three explicit destinations as virtual panoramas. Reformatting redraws
annotations over the frozen original JPEG; it never captures another camera frame.

The real preview replaces the decorative sky, ground and weather. The centre
reticle, selected bodies, trajectories, interactions and notices remain. A thin,
contrasting gravity horizon follows pitch and roll; it can leave the screen when
looking up or down. No artificial centre horizon, ground grid or clouds are drawn
over the camera.

## Optical alignment

Camera2 chooses an ordinary wide rear camera, with no front-camera fallback. A
bounded preview stream uses continuous autofocus where available; a second,
bounded JPEG surface is targeted only by explicit still-capture requests. Video
and optical stabilization are disabled to prevent an unmodelled stabilization crop.
Android 12+ rotate-and-crop is explicitly disabled; display changes, including a
180-degree rotation, are observed. No new camera dependency is required.

The geometry accounts for sensor rotation, display rotation, stream aspect crop,
the returned capture crop and current focal length. Hardware focal/principal-point
calibration is used where supplied with valid zero-skew intrinsics; physical sensor
dimensions and focal length provide the fallback. Available fast lens distortion
correction is enabled. TextureView's built-in sensor rotation/stretch is compensated
without mirroring. A single uniform scale fills the app scene and maps the lens axis
to the pointing viewport's reticle, including the split landscape layout.

The same density-independent horizontal and vertical focal ratios are passed to
body projection, paths, direction arrows, event markers, hit/focus targets, floating
point captions and horizon clipping. Camera overlays wait for capture metadata
instead of using the virtual sky's fixed 60-degree field of view.

This is sensor-based sky pointing, not visual SLAM or an astrometrically calibrated
camera. Absolute direction still depends on the handset's compass/declination and
attitude accuracy. Devices without intrinsic calibration use nominal optical
metadata; residual lens distortion and hardware axis tolerances can limit precision.
The existing compass warning and Simulation notice remain visible.

## Permission and lifetime

CAMERA is optional hardware and requested only after tapping the sky camera toggle.
Denial leaves it off. Camera errors return to the synthetic scene with a localized
short message. Preview resources are released on off, pause/stop, child pages,
catalogue, panorama and disposal, even though hidden main-page data owners stay
composed. Returning may resume an enabled preview only on the visible foreground
main page. A pending asynchronous open is closed when its callback arrives.

The JPEG reader is used only on an explicit shutter tap and each acquired image is
closed promptly. No recorder, camera background service, frame upload or automatic
gallery write is added. Exposure metadata is paired with its matching JPEG timestamp.
A bounded history of device attitude is sampled at exposure time and remapped into
the capture's display orientation. Camera realtime timestamps are used when supported;
otherwise the shutter callback's elapsed time is the timing fallback. Stale or
unusable bearings suppress celestial overlays instead of inventing their direction.

The photograph uses the whole JPEG lens field, not the main view's software crop,
reticle offset, toolbars or data columns. Lens crop, focal axes and real principal
point are retained when rendering bodies, paths, arrows, event glyphs and labels.
The JPEG is manually rotated from sensor into capture display orientation, with
JPEG_ORIENTATION=0 and no OS rotate-and-crop. The original camera JPEG stays in
private cache while the preview can regenerate label/location variants, then is
deleted on preview close. Exported variants are freshly encoded with explicit time
and credit tags, retaining existing disclosure choices without copying source GPS
EXIF. The preview follows device orientation and supports drag/pinch/zoom; portrait
starts filled, landscape fitted. All export pixels remain independent of this crop.

## Verification

Seven additional JVM tests exercise numerical focal/crop projection, all sensor and
display rotation combinations, non-mirrored offset reticle centring, returned zoom
crop, optical-centre shifts, body/path/hit/arrow agreement, anisotropic gravity
horizon clipping and invalid metadata rejection. Existing virtual perspective
tests remain active. Android test sources are compiled; connected instrumentation
is not run without separate authorization. Device checks and hashes are recorded
in VERIFICATION.md and the workspace's camera validation record.


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


## Celestial reference visibility

A neutral 48 dp round toggle precedes the object selector. It shows a parallel-lined
globe, with a diagonal slash when disabled. The sky_references_visible preference
defaults to false and is written immediately; rotation and cold starts retain it.
It controls all celestial parallel curves, labels and pole crosses in both virtual
and camera views. The capture snapshot freezes the same visibility at the tap;
both panorama and camera JPEG renderers respect it. Save/share disclosure and
point-label variants copy that snapshot rather than re-reading the current setting.
Orbit paths, markers, reticle and horizon are independent of this toggle.
The external actions reserve 174 dp horizontally and 60 dp vertically for label
collision avoidance; notice columns reserve the same action width.

The celestial equator extends Earth's equatorial plane, and celestial poles mark
the rotation-axis directions. Tropic and polar-circle guides are custom declination
parallels corresponding to terrestrial latitudes, not geographic surface outlines.


## Complete capture annotation toggle

The existing Labels control and panorama_point_labels preference now control all
scene annotations in both virtual panoramas and camera photographs: time/elevation
pills, object names on orbits and beside current images, celestial equator/tropic/
polar-circle names and pole names. The short control caption is localized in all
twenty catalogues. Hidden reference labels are not generated or reserved during
collision layout; curves and pole crosses remain. Object images, trajectories,
arrows, event glyphs, horizon, axis graduations and the capture header are independent.
The main celestial-reference switch still controls whether reference geometry
appears at all. Save/share variants use the frozen label visibility in their copied
snapshot; the preference key is preserved for existing installations. Main-view
live reference captions are unchanged by the export-only Labels switch.


## Celestial parallel degree annotations

The shared reference renderer now appends the signed declination to each parallel
name and ±90° to the celestial-pole captions. Equator is 0°; tropics use mean
obliquity of the snapshot/scenario date and polar circles its complement, rounded
to one decimal (approximately ±23.4° / ±66.6°). These describe projected latitude
parallels, not local elevation. The live view uses the selected numeric format;
panorama and camera exports keep the international decimal convention.

Short numeric-only captions repeat at widely separated visible curve samples.
Spacing is at least 640 renderer scale units, with at most eight repeats per
parallel in a panorama or three in a perspective viewport. Distance wraps across
the panorama's north seam, includes the existing name anchor, and repeated values
never force an overlap with reserved content. Names and pole captions get layout
priority. Small clipped or compact circles may have only one caption. All numeric
annotations follow the existing Labels switch in captures; hidden annotations
reserve no space. The main reference switch continues to control the geometry.


## Photographic angular reference

Camera captures now overlay a perspective-correct 10-degree grid. Only visible
azimuth/elevation curves and their edge intersections are labelled; cardinal and
intercardinal marks follow the actual horizon. The preview's compass-centering
button is reserved for the 360-degree panorama, never a camera photograph.
See PANORAMA_CENTER_AND_PHOTO_GRID.md for frozen data, optical-center/roll handling,
export behavior, unreliable-heading policy and validation coverage.


## Rear-camera zoom controls

The main pointing viewport shows minus, the current ratio and plus immediately
before the shutter only while rear-camera mode is enabled. The circular actions
reuse panorama icons, existing translated zoom labels and the current numeric
format. Buttons, the ratio list and pinch requests share one adaptive zoom ladder
with an exact ordinary 1x stop, and disable at the hardware endpoints or while
capture/the lens handoff is not ready. Camera mode
still starts disabled; turning it off resets zoom to 1x. Navigation keeps the
requested ratio without writing camera settings to persistent preferences.

Public rear devices must expose valid optical metadata, preview and JPEG streams.
The ordinary lens defines 1x. Separately exposed wide/tele lenses use ratios of
their normalized focal lengths; a bounded camera-busy retry permits asynchronous
close/open during a handoff. No vendor-only camera IDs or private APIs are used.
Android 11+ uses the advertised zoom-ratio range; Android 8–10 uses the supported
centered crop range. Logical zoom coordinates are post-zoom: focal lengths and
the principal-point offset are transformed into that virtual array instead of
double-counting the active physical lens's focal length. Both preview and JPEG
use their own capture-result crop/ratio metadata. Still requests inherit the
zoom request, and exported grids/orbits use the frozen photographed lens field.

Debug/release builds and release lint pass. All 675 JVM tests pass, including six
new regressions for lens selection, limits/1x transitions, offset sensor crops,
native-versus-legacy projection equivalence at all rotations and wide/zoom photo
fields. Instrumentation sources compile; connected instrumentation is not run.
No in-app text catalog, version or GitHub release is changed by this local feature.


Physical Samsung Android 12 checks completed for public cameras 0 (ordinary) and
2 (ultrawide). The reported minimum is 0.51x. The advertised maximum is 8x, while
capture-result metadata reports about 7.96x; the overlay uses that realized ratio.
Both endpoint controls disable, crossing 1x returns to the ordinary lens, JPEG
previews open at both extremes, and returning preserves the selected ratio.
Reviewed portrait/landscape screenshots confirm the controls fit before the
shutter in the left pointing column. Camera mode was turned off afterwards,
controls disappeared, and system rotation was restored. No gallery save, file
export or share occurred. The photographed frames were dark and carried the
existing reduced-heading-accuracy warning, so no measured room-target angular
accuracy is claimed; projection equivalence is covered by the JVM regressions.


## Compact floating controls

Floating main/capture controls use 80% of their original size, including circles,
glyphs, selector badges and zoom-ratio badges. This applies to the main menu,
camera/reference/objects/shutter controls, camera zoom, and the capture preview's
back/zoom/compass/labels/location/export controls. The user explicitly requested
smaller hit regions too: the common modifier scales measured layout bounds and
places the full control in a transformed layer, including pointer and semantic
coordinates. The original 48 dp controls now occupy approximately 38.4 dp.

Scale is applied outside the original fixed size and Material control minimums.
A scoped ViewConfiguration override disables automatic minimum-touch expansion
only inside these actions. Dropdowns, search fields and other controls retain
their existing touch defaults.
RTL order and child alignment remain intact. Preview zoom controls start below
the scaled Back button with the existing 8 dp gap, and main-view annotation
exclusions follow the reduced control dimensions. Controls in islands, catalog
search, dropdown rows, dialogs and celestial scene markers remain unmodified.

## Direct zoom selection

Tapping the live zoom ratio opens the adaptive, themed radio dropdown. Buttons and
pinches use this same fixed ladder; the menu never adds arbitrary gesture values.
Levels use tenths below 1x, quarters from 1x to 2x, halves to 4x, then whole values
to 10x and progressively wider regular steps for larger advertised hardware ranges.
The exact physical minimum/maximum and ordinary 1x remain reachable. Near-round
optical endpoints receive nominal names (for example 0.51x as 0.5x); aliases cannot
create duplicate menu labels or remove the separate ordinary lens stop.

The badge maps reported camera zoom to the same nominal ladder, so driver values
such as 7.96x show the matching 8x level. Unrounded capture metadata still drives
all preview/JPEG geometry and orbital projection. Labels retain the current numeric
format without redundant decimal zeroes. Direct choices use the same lens handoff
and optical calibration as buttons and gestures; dismissing the list changes
nothing. The control retains its translated accessibility label and shared 80%
artwork/touch bounds.


## Live camera pinch zoom

Two-finger spreading/pinching inside the main sky viewport uses the same bounded
zoom request as the ratio selector and +/- buttons. The gesture remains attached
to the stable Compose sky parent when a separately exposed wide/ordinary lens
is replaced. An already accepted pinch continues through the temporary capture
readiness gap; a new gesture requires a ready camera and capture must not be busy.
Each event clamps to the public hardware range, including the ultrawide endpoint,
so reversing at a limit responds immediately instead of consuming overshoot.

Only a genuine change in finger spacing beyond touch slop changes zoom. Pan and
rotation do not move the camera, single-finger orbit/control taps remain intact,
and a two-finger camera gesture cancels scene taps before children process it.
The gesture is inactive with the camera off or while taking a photograph, and is
restricted to the sky column in landscape. Actual camera-result optical metadata
continues to drive both live overlays and the frozen JPEG/export. No translation,
preference, version or GitHub publication is added by this local feature.


Local validation: debug and release builds succeed, all 683 JVM tests pass, and
instrumentation sources (including the synthetic pinch/tap/handoff cases) compile;
connected instrumentation is not run. App-scoped two-finger input checks on the
authorized Samsung Android 12 phone confirm zoom in/out, a continuous pinch from
the ultrawide lens through 1x to the ordinary lens, maximum clamping, immediate
reverse zoom at the maximum, and the existing minus and direct-ratio controls.
The original camera state is restored. The signed APK is installed and its
checksum verified; no gallery export, share or GitHub publication takes place.


## Photograph pointing in the capture header

Photographic headers append the rear-camera bearing and signed inclination relative
to the local horizon. They use the exposure-matched camera attitude already frozen
with the JPEG; display rotation changes screen axes but never its photographed
forward axis. The bearing follows the existing compass trust and near-vertical
policy. Missing/stale attitudes show dashes, and unavailable heading does not discard
a valid inclination. Positive tilt points above the horizon and negative tilt below.

Both international and selected export presentations freeze their own formatted
values with the existing Direction/Tilt translations in all twenty catalogues.
The one-line header survives location disclosure, label toggles, Save and Share
without querying subsequent sensors, GPS or process locale. Virtual panoramas have
no exposure attitude and keep their existing header. No new translated strings,
preferences, version or GitHub publication are introduced.


Local validation: debug/release builds succeed; all 688 JVM tests pass, including
five new exposure/rotation, missing or untrusted heading, frozen-profile, later
sensor-change and disclosure regressions. Instrumentation sources compile;
connected instrumentation is not run. The existing Direction/Tilt templates are
present in all twenty catalogues. A cache-only real-phone photograph confirms the
appended header fields; with the phone almost vertically down the inclination is
near -89 degrees and the undefined horizontal bearing correctly shows a dash.
The preview opens/closes normally and the original camera state is restored.
The signed local APK is installed and checksum-verified. No gallery save, export,
share or GitHub publication is performed.


## User formatting in capture previews

Both virtual panoramas and photographs initially render the selected user language,
number/date/time formats and units. Header pointing, weather/place text, orbital
labels and angular graduations all come from the same frozen selected presentation.
Location, label and panorama-center changes preserve that preview profile.

The remembered scientific/selected export choice belongs only to the export dialog.
Opening it or changing its profile prepares an independently keyed export file and
never replaces the displayed preview bitmap. Gallery, document and share actions
wait for that exact profile/disclosure/label/center file and use it, rather than
assuming the currently displayed file is the export. The original file explicitly
records its profile, so a selected preview cannot satisfy the scientific fast path.
Both frozen presentations remain available without consulting later sensors, GPS,
weather, clock or settings. Legacy fixtures with an international base file retain
their existing default and cache behavior. No new strings, version or publication
are introduced.


Local validation: debug/release builds succeed and all 688 JVM tests pass.
Instrumentation sources compile, including a selected-base versus scientific-file
cache regression; connected instrumentation is not run. The authorized Samsung
phone shows Italian date/weather/place/cardinal text and comma-decimal orbital
labels in both virtual and camera previews while Scientific International remains
the stored export choice. Both export profiles prepare successfully in the dialog.
After selecting the scientific profile and dismissing the dialog, hashes of the
displayed preview regions remain identical for both capture types. Export choice
and camera state are restored. The signed local APK is installed and checksum
verified; no gallery save, document save, share or GitHub publication occurs.


## Unified pinch detents

The finger-distance target accumulates continuously even when successive events
stay inside the same detent. Only ladder changes are emitted as camera requests,
avoiding stalled slow pinches and arbitrary ratios. A small hysteresis margin stops
jitter from repeatedly crossing the ordinary-lens boundary. Continuous targets
clamp at physical endpoints on every event, so reversing direction does not consume
overshoot. Range discovery also normalizes any retained request onto this ladder.
No permission, translation, version, persistent camera setting or publication is
added; camera/JPEG optical calibration continues to use real capture-result data.


Local validation of the unified ladder: debug/release builds succeed, all 693 JVM
tests pass, and instrumentation sources compile; connected instrumentation is not
run. App-scoped input on the authorized Samsung Android 12 phone confirms the
1.25/1.5/1.75/2x button sequence, matching direct and pinch selections, the nominal
0.5x wide endpoint with its checked radio row, maximum 8x clamping, and the 4/5/4x
button sequence after reversing a pinch. Original camera state is restored. The
signed local APK is installed and checksum-verified with app data preserved. No
gallery save, document export, share or GitHub publication occurs. Real endpoint
metadata remains separate from nominal labels for optical calibration.


Local portrait panel-placement validation: debug/release builds succeed and all
693 JVM tests pass. Updated instrumentation layout assertions compile; connected
instrumentation is not run. The authorized Samsung Android 12 phone confirms
capture and all zoom controls above the selected-point island in portrait, with
and without camera view, after point navigation and after rotation. Landscape
retains the selected point above the right data column and capture/zoom at the
bottom of the left sky column. Closing the island returns capture to the bottom.
Camera state and Android rotation policy are restored. The signed local APK is
installed and checksum-verified with app data preserved. No photograph, gallery
save, document export, share or GitHub publication occurs.


## Full photograph field of view

Photographic headers also append horizontal × vertical angular coverage in degrees.
The field comes from the exposure's lens/crop and the actual JPEG dimensions after
sensor/display rotation. Edge angles use the real principal point, rather than
assuming centred optics or dividing an advertised field by the nominal zoom badge.
The JPEG header is read off the UI thread without allocating another bitmap.
Both selected and scientific presentations freeze and translate the field at
capture time. Preview panning/zooming and later sensor, lens or settings changes do
not alter it. Location disclosure and orbital-label toggles preserve the value;
virtual 360-degree panoramas have no photographic field appended.


Local field-of-view validation: debug/release builds succeed and all 698 JVM tests
pass, including rotation, JPEG aspect crop, optical-centre asymmetry, zoom and
frozen-profile/disclosure regressions. Instrumentation sources compile; connected
instrumentation is not run. All twenty string catalogues retain identical keys
and the new field template has matching indexed placeholders. Cache-only photo
previews on the authorized Samsung Android 12 phone show the localized field at
0.5x, 1x and 2x, narrowing with increasing zoom. Original camera state is restored.
The signed local APK is installed and checksum-verified with app data preserved.
No gallery save, document export, share or GitHub publication occurs.


## Rounded object-view mode switch

The object detail viewer groups Current View and Rotation in one rounded capsule.
A single selected surface slides between the eye and rotation glyphs; selecting
the already active mode leaves it active. The capsule is 60 x 30 dp including its
outer border, matching the previous circular controls' 30 dp visible height. The
glyph centres are 30 dp apart, with the 26 dp selected circle aligned to each
18 dp glyph. Press feedback remains inside that circle. Both halves retain adjacent,
independent 48 dp touch targets; the glyphs move towards their shared boundary
while remaining inside their own target, without overlapping touch regions. They retain
translated accessible labels, selected Tab semantics and tooltips. The existing
high-contrast viewer palette remains readable over any object image. Relative
placement mirrors the thumb and choices in RTL. The control remains inside the
model at its trailing top corner in portrait, tablet and landscape layouts.
Model gestures, saved viewport/rotation state and animation lifecycle are unchanged.
No new string, preference, version or GitHub publication is introduced.


Local mode-switch validation: debug/release builds succeed and all 698 JVM tests
pass. Updated viewer instrumentation assertions compile; connected instrumentation
is not run. The authorized Samsung Android 12 phone confirms adjacent options
with exactly one selected in portrait and landscape, movement of the light active
surface between both icons, and an unchanged active mode when selected again.
Landscape keeps the control inside the first model column. Back returns to the
main view and the original Android rotation policy is restored. The signed local
APK is installed and checksum-verified with app data preserved. No preference,
gallery save, export, share or GitHub publication is changed.


## Centred capture-preview control groups

Both photographic and virtual panorama previews centre their left and right
control groups vertically in the available viewport, in either device orientation.
Back and both image zoom actions form one left column; the available centre,
labels, disclosure and export actions form the right column. Their order, 8 dp
inter-control spacing, 8 dp edge inset and shared 80% visual/touch bounds remain.
The right column centres its actual contents, including the shorter photograph
group without panorama direction. Back remains available while an image loads or
a variant is being prepared. Image fitting, gestures, header data and export
profiles are unchanged. No new string, preference, version or publication is added.


Local centred-controls validation: debug/release builds succeed and all 698 JVM
tests pass. Instrumentation sources compile; connected instrumentation is not run.
On the authorized Samsung Android 12 phone, the left and right action bounds centre
within two pixels of the image viewport's vertical midpoint for virtual panoramas
and camera photos in both portrait and landscape. The three-button photographic
right group and four-button virtual group each centre their actual content. Shared
80% bounds, zoom in/out and Back are verified. Camera state and Android rotation
policy are restored. The signed local APK is installed and checksum-verified with
app data preserved. No preference, gallery save, document export, share or GitHub
publication is changed.
