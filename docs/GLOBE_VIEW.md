# Interactive Globe view

The main view-mode modal offers Sky (Frame), Sky (Panorama), Camera, Globe (Frame) and Globe (Panorama). Globe reuses the
bundled Blue Marble and Black Marble maps and the offline screensaver Earth shader,
without its decorative Solar System, automatic spin, footer or exit gestures.
One-finger drag rotates around the seasonal Earth axis by default. The former
pointing-axis control switches between axial and free rotation; both modes preserve
the same seasonal tilt and the shader/trajectory coordinate transform. Pinch and the
zoom selector share a saved 0.5–3x zoom. At 1x, the complete trajectory envelope
fits inside the free frame with a 24 CSS-pixel marker margin. A rotation-invariant
3D extent avoids resizing while the user rotates Earth. Captures use the same
fitting rule with an output-scaled margin; higher zoom intentionally permits cropping. Single tap on empty space pauses/resumes the observer clock; double tap toggles
floating controls. Globe and Sky panoramas have no central reticle; framed Sky and Camera retain theirs. Earth sunlight follows the effective Location
instant, including frozen and accelerated time. The screensaver remains independent.

## Coordinate and distance contract

WGS84 horizontal observations are rotated from ENU to ECEF and their observer
position is added back when a finite topocentric range exists. Renderer coordinates
are +X at 90E, +Y north, +Z Greenwich, matching the texture and lighting transforms.
The globe is an orthographic spherical visualization, not a terrain or navigation map.

Earth satellites retain their physical geocentric positions in Earth radii.
Other objects use a direction shell at radius 1.55: each sample keeps its apparent
Earth-fixed direction at that sample's own instant. This restores the daily arcs
around the globe, including Earth's diurnal rotation. The shell is a directional
visualization, not a distance scale or a claim that the Sun, Moon, stars and planets
share gravitational satellite orbits around Earth. No common inertial reference or
radial distance-compression function is applied. Actual data tables retain km.
Missing satellite or Horizons data never produce a fabricated track. The localized
note distinguishes terrestrial satellite orbits from the other apparent directions.

Paths, 0–24/+24h intervals, fixed-date comparisons, active object, point selections
and antipodes share the existing observer and selection owners. Back-facing segments
are faint/dashed, and points occulted by Earth cannot intercept touches. The contextual control offers a label-scope popup. New installations start
with the selected-points-only scope; explicit saved choices remain unchanged. The 0°/90°
control becomes the axial/free rotation configuration. Label visibility follows the
four live policies; captures retain all five photographic/panoramic policies.
The label tag reuses the capture preview geometry and optical correction. Labels omit
the object name and follow the capture horizon/selection outline colours and translucent
near-black rounded background. Labels keep a larger clearance from their point
and visible trajectories, with colour-matched leader lines. Interactive placement
retains a clear side with hysteresis; captures reuse panorama placement with curve
and marker obstacles. All connectors paint before all label boxes. Current positions
use the same cached object miniatures as the other sky views; rise/set, culmination,
minimum and approach markers share the existing event geometry. The real compass
information island is unchanged. Geographic parallels follow the
reference-line switch. The viewpoint preserves manual longitude and, in free mode, pitch offset while
seasonal tilt follows the effective instant. The globe centre is the exact midpoint
of the measured free vertical interval between upper and lower controls. Temporary
point/antipode controls are excluded from this calculation, so pinning and clearing
points cannot shift the globe. Its horizontal centre is always the viewport midpoint;
trajectory asymmetry never changes the Earth centre. The universe
background spans the complete main view, using the screensaver day/night palette
and gradient stops selected by the app theme; the embedded scene remains transparent.

## Rendering, privacy and persistence

Astronomy and curve preparation run on Default dispatchers. Per-path geometry is
cached; gestures send only the small view pose, not the complete track document.
The WebView accepts only bundled HTML/data images, blocks all networking/navigation,
has no JavaScript-to-Android bridge and runs no idle animation loop. Gesture paints
coalesce at the same Low/Balanced/High minimum frame intervals as General settings
(60 ms / 30 ms / 16.667 ms). Backgrounding cancels pending paint work. Its fallback
supports the same pose, picking and selection geometry. Release destroys the WebView.
Overlay controls own their existing input shields; captions avoid their measured bounds.

Captured images use a frozen software Earth/track renderer with the shared caption,
legend, label modes, formatting profiles and orbit-table export. Later gestures, GPS,
time or selection changes cannot change captured geometry. A precise observer pin is
included only in Complete position exports, never Area or Hidden. File names identify
Globe using the selected export language/profile. The optional `globe` scene field
round-trips the non-camera mode, label visibility and rotation configuration; old scene files decode as Panorama, and old readers
ignore the new optional field. Camera activation remains permission-gated and is
never restored automatically on cold start.

Sources: [ESA ENU/ECEF transformation](https://gssc.esa.int/navipedia/index.php/Transformations_between_ECEF_and_ENU_coordinates),
[Astronomy Engine coordinate transforms](https://github.com/cosinekitty/astronomy/tree/master/source/kotlin#coordinate-transforms).
Earth texture credits and original asset integrity remain in SCREENSAVER.md and
SCREENSAVER_ASSETS.json. The new adapter is pinned separately in GLOBE_ASSETS.json.

## Globe capture options

Live Globe offers All, Key and selected points, Selected points, and None.
These live scopes apply only to the active island object (or its active fixed-date
variant). Switching the island target updates the labels immediately, without
changing other tracks, point selections or the chosen label scope.
Captured Globe images retain all five panorama/photo scopes, including Selected
object and selected points. The main-view default is Selected points;
the removed live scope migrates to Key and selected points, including older scene imports.
Other saved choices and legacy off switches are retained.
Every Globe capture freezes the main-view scope;
preview edits affect that image, while the next capture starts from the main view.
Labels, compass, position and the view selector use the shared modal
radio picker. The view selector separates Sky/Camera/Globe from Frame/Panorama radio choices. Projection choices are disabled for Camera. Apply commits the draft; Close, Back and the close icon discard it. Close uses the primary day/night colour and precedes Apply. Live and captured labels share one glyph
component and optical offset. Back-side labels follow the selected scope, use the
below-horizon border tint and a dashed connector, and do not make occluded markers
pickable. This visual distinction does not change their astronomical coordinates.
A complete-position capture uses the same light-azure geographic pin as the live
Earth. Area/Hidden exports omit the precise pin entirely. Scene files preserve the
canonical main-view label mode with a legacy Boolean for older readers. Tutorial
previews select all five views and their three control sets through the same modal as the real view,
without changing the real scene or starting the camera. Explanations follow the
selected mode and the control actually touched.
The first-install main mode remains Panorama; an explicit saved Globe choice is retained.


## Camera grid control

In Camera mode the pointing-axis slot becomes a saved angular-grid toggle (enabled on
first install). It draws subtle azimuth/elevation lines with the same optical-frame
projection and ten-degree sampling as the photographic grid. Immutable sky rays
are cached; live paths are prepared on Default without photographic border ticks.
The decorative layer handles no input and stops when the camera/main view is not
active. Panorama retains its pointing-axis control; Globe retains its rotation
control. The tutorial explains the camera grid without changing scene preferences.
The existing photographic grid and data-export policy are unchanged.


## Overlay gesture guard

Single/double empty-space gestures ignore a 12 dp margin around measured controls,
notices and covered regions. This does not move or enlarge the controls themselves.
The main sky, Globe and capture previews share the guard; hidden controls no longer
reserve their former area. A second tap landing in protected chrome cancels the
empty-space gesture instead of falling back to a pause.

## Captured space background

Globe captures draw the same decorative star field and radial day/night palette
as the main view, using the theme frozen at capture time. Star positions are
normalized to the image area and sizes scale with export resolution. The field
is drawn before Earth and tracks; the caption and legend retain their black bands.
These decorative stars do not represent measured celestial coordinates.

The experimental live panoramas reuse the selected trajectories and point identities.
Sky uses an azimuth/elevation shell, while Globe uses a geographic plate-carree map;
longitude seam segments split at the edges instead of crossing the full map. Drag
and pinch update the retained map pose; content serialization and model building
remain off the UI thread. The main mode and map pose are saved, the tutorial stays
isolated, and captures start with the selected live projection. Camera remains a
framed, opt-in mode on each cold start. No new image resources are bundled.

The main-view Grid and references dialog applies two independent saved switches.
The lightweight angular/geographic grid defaults to visible, retaining the legacy
`camera_grid_visible` preference. Named equator, tropic, polar-circle and pole
guides retain `sky_references_visible` and default to hidden. Checkbox drafts only
commit on Apply; Close, Back and dismissal leave the scene unchanged. The shared
grid setting covers Sky, Camera and both live map projections, and initializes
both switches in each new capture. Preview changes apply only to that capture and
its exported image variants; they never write main-view settings. Both switches
share the main-view dialog and Apply/Close semantics. Scene documents include
the grid choice; legacy documents without that field retain the visible default.

Live Globe panoramas retain their own longitude, vertical pan and zoom (0.5–8x),
independent of spherical tilt and zoom. At 1x, the map fits the viewport width
while preserving its 2:1 proportions. Landscape panoramas use the complete scene
width behind both expanded and compact panels; portrait panels and the compact
footer still bound the viewport. Tall maps can be panned vertically to the poles,
and short maps stay vertically centred. Resizing and rotation recompute the pan
limit in rendered map pixels. The spherical view and frozen capture framing retain their existing behaviour.

Sky panoramas now share the Globe panorama width fit, viewport boundaries, 0.5–8x
zoom and bounded vertical pan, while retaining their own saved pose and angular
azimuth/elevation axes. Grid numbers are a separate saved display choice, disabled
by default. Geographic views use latitude/longitude; Sky and Camera use azimuth/
elevation. The live choice seeds each capture, whose preview/export overrides
remain isolated. Numbering is included in scene archives and image cache keys.
The live perspective grid collects number anchors from the existing projected
segments rather than running a second sensor-frame projection.

## Sky label and weather controls

Sky (Panorama) shares the live Globe label choices and selected-points-only initial
scope, with an independent saved preference. Sky (Frame) hides the label button, centres Zoom on its own, and uses the
existing reticle-driven point details. Camera keeps its
object-transparency button. Sky panorama captures inherit the live label choice;
preview changes remain local to that image. Scene archives optionally carry
`skyLabelMode`; older archives fall back to the first-install live scope.

Weather is a checkbox below the projection choices in Display. It remains visible
and is disabled for Camera and Globe. It shares the
mode/projection draft: Apply saves it, while Close and Back discard changes.
The weather preference remains enabled initially and is retained across modes.
