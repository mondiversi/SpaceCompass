# Live rear-camera view

The camera toggle is the first circular action above the sky, followed by celestial
references and the celestial selector. It is outside the title toolbar. A slash
indicates off; the ordinary camera indicates on. It starts off on each cold app
start. Celestial guides also start off until explicitly enabled; the initial
checked object set contains only Sun and Moon. Existing stored object selections
remain independent of these first-use defaults. Capture is a separate neutral shutter circle at the bottom trailing corner
of the sky, including the left sky column in landscape. The selected-point panel
and point captions reserve space for that action.
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
