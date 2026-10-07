# Panorama centering and photographic angular grid

## Panorama center

The preview's top-right controls begin with a compass button, above Labels.
It opens the app's customized alert dialog with exactly four radio choices:
North, East, South and West. There is no System/Default option. The first capture
uses South for nonnegative/unknown latitude and North for southern latitude;
the capture receives the resolved device or Scenario latitude. An explicit choice
is saved immediately under `panorama_center` and takes precedence on later captures.

The snapshot freezes this choice. Changing it regenerates the full equirectangular
scene, moving object images, orbit samples/arrows/names, time/elevation pills,
cardinal names/symbols, degree axes, reference circles and pole crosses together.
Segment clipping follows the selected seam; no long chord crosses the picture.
The caption remains in its separate heading. All four center values are part of
the export variant identity, alongside disclosure, labels and export profile.
Gallery/document/share actions use the prepared current file, so they preserve
the chosen center and cannot reuse an image with another center or disclosure.

The compass button is absent from a real-camera photograph. Camera geometry is
always determined by the captured lens, shutter attitude and full JPEG dimensions.

## Photo grid

Only the rendered camera capture receives the new angular grid. The live camera
view keeps its previous horizon/reticle/overlays. The full camera JPEG retains all
its pixels and its previous display rotation; the grid is an overlay, not a crop.
A slim framed area leaves room for the existing caption and edge values.

Azimuth meridians and elevation parallels are sampled every 10 degrees and
projected through the exact same perspective and shutter-time orientation as
the objects/orbits. Projection clips behind-camera rays before division and clips
segments to the photographic grid frame. Therefore no 360-degree scale, unseen
cardinal or out-of-frame angle is copied from the panorama.

Azimuth ticks appear where a projected meridian intersects the frame's top or
bottom. Elevation ticks appear along its left edge. Tick positions and values
remain valid with tilt, roll, portrait/landscape rotation and an off-center optical
axis. A numerical inverse-ray audit verifies the labels against the photographed
rays. Zero-elevation cardinal/intercardinal points are drawn only when visible,
with their names above and symbols below the actual gravity horizon. If the
horizon is outside the frame, those marks are absent.

Phone verification exposed a pre-existing 180-degree inversion of the capture
screen axes in landscape. Shutter attitude now matches the live screen's Android
`SensorManager.remapCoordinateSystem` axes for both 90/270-degree rotations.
The SDK's SensorManager.java implementation was checked directly. A physical
upright-landscape regression requires positive elevation above the horizon and
eastward bearing to the right. The top-axis margin also includes the full glyph
ascent, padding and shadow so the caption cannot silently suppress its numbers.

The frame and text are subtle white with shadow/translucent contrast, and tick
labels avoid each other. Their bounds are reserved against orbit time/name pills.
The frozen export number format and translated cardinal names are used for all
labels, including selected-language exports. Existing annotation visibility still
controls object/orbit/reference captions; angular axes remain available as in the
panorama. Unreliable/missing shutter heading suppresses the new angular grid just
as it suppresses astronomical camera overlays; the existing horizon and warning
policy are retained instead of fabricating compass bearings.

## Validation

Fourteen new JVM regressions cover first-use hemisphere/manual persistence,
centered directions, rotated cardinal coordinates, seam splitting, vector/position
consistency, all 48 cache variants, optical-center inverse projection, in-frame
curves/ticks, north wrap, tick values under tilt/roll, true slanted horizon, absent
cardinals above the horizon, exact rectangular clipping, and physically upright
landscape exposures in both rotations. Execution and device
results are recorded in VERIFICATION.md and the local validation artifact.
