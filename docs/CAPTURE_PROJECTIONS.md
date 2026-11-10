# Captured view representations

The virtual observing mode is Sky; Panorama is an output format. Camera and Globe
remain separate observing modes. Archive enum keys are retained for compatibility.

Sky and Globe previews offer a top-start Panorama toggle above Zoom out. Sky initially uses the
360-degree representation; Globe initially uses the captured spherical frame.
The two preferences are independent and persistent. Camera has no such toggle.

The shutter freezes visible/optical frame geometry, orientation, Globe pose and
normalized radius/centre along with the existing observer, paths, selections,
Antipodes, formatting and disclosure data. Changing projection never reads live
sensor, location or orbital updates. The same photo overlay renderer is reused
for the framed Sky. A Globe panorama is a geographic plate-carree map using the
same Earth-fixed track vectors; dateline crossings are split at both boundaries.
Satellite height is not encoded as a map distance. Earth illumination uses the
captured UTC instant and the existing maps and ocean shading.

Exports offer Framed view and Panorama independently, or both; photographs retain
their single image option. Image names include the representation in the chosen
formatting language, with different cache identities for each projection. CSV/TXT
remain complete frozen orbit tables independent of the chosen image projection.
All new strings and the compact mode-selector tutorial are localized in twenty languages.

The flat Globe panorama includes a 30-degree geographic graticule, numbered on all four edges. Greenwich and the Equator are slightly stronger; reserved edge bands keep point labels away from coordinate numbers. Framed Sky/photo grids and full-sky grids share faint strokes normalized to output width; the live camera grid uses the same low-contrast palette.

Every capture preview offers a persistent Grid switch below its right-side controls. The chosen visibility travels into both image projections and export variants, including the cache identity; it never changes frozen orbit-table data. The main Camera axis slot is empty and is not a tutorial target.

All preview representations fill the available height below the toolbar at their original aspect ratio. Wider images remain horizontally pannable, and changing a rendered variant resets its preview transform to this height-filling baseline. The main Sky shares the faint live angular grid; Globe keeps its existing Guides switch. Mode changes report the chosen mode, except tutorial demonstrations and unchanged selections.

Capture grids use shared white 35% lines (1 px at 4096 px scene height, capped at their previous width-based thickness) and a thin black halo, with slightly stronger primary circles. Live Sky/Camera grids remain faint and follow the double-tap overlay visibility without changing the saved capture-grid choice. Capture overlay buttons use the original compact size. Selected live Globe labels include T±; spherical captures centre the Earth across the complete image, compensating caption and legend heights. The compact tutorial documents the current controls in all twenty languages.

Portrait previews place Zoom in before Zoom out side by side in the upper-left corner. Caption options use the upper-right row in portrait and the upper-right column in landscape, scrollable when the height is limited. Landscape retains zoom in the upper-left column. The Panorama/frame switch uses a filled blue/cyan primary surface while Panorama is active and the shared muted gray while it is off, with day/night contrast ink. It remains at the lower right in both portrait and landscape. The photo never receives a Panorama switch. Every visible live Globe label includes T±; selected points are independent of the active track and are laid out before ordinary labels, with curve-overlap fallback to avoid dropping selected pills.

Live Globe pills offer full identity/event, clock/T and azimuth/elevation, compact event/clock, and minimal clock/T tiers. Detail responds to projected Earth size, label count and actual placement space, and remains stable between gesture/layout changes. Selected points are placed first and can overlap curves rather than vanish. Geographic guide names are independent of point-label scope. Equator, tropics and polar circles use terrestrial names and latitude, without the celestial qualifier. Their shaped text follows the visible front-face arc, with a dark outline; screen clipping retains visible segments even when both sampled endpoints lie outside the frame. Whole-string raster shaping preserves RTL joining. Names reserve space before point pills and remain attached to their own parallel when zooming or rotating.

The live Globe also marks the visible ends of the Earth rotation axis with outlined crosshairs and localized North Pole/South Pole captions (±90° latitude). Pole markers follow the Guides switch, remain attached to the geographic vertices, are not selectable orbit points, and reserve caption space independently of the point-label scope. Hidden-hemisphere and offscreen poles do not create detached labels.

Frozen Globe images include geographic guide names and pole captions whenever Guides was enabled at capture time, independently of point-label scope. Globe reference names use the selected/international export language without the celestial suffix. Visible geographic arcs are clipped to the actual image and split at the map dateline; shared shaped curve lettering reserves space before orbit pills. Both spherical frames and flat panoramas use the same frozen geometry, formatting and disclosure policy. Grid stroke thickness follows the scene height so filling a portrait preview does not make full panoramas heavier than perspective captures.

Globe captures freeze the live geographic guide text size relative to the expected capture viewport height below the system bars and toolbar. Both framed views and full maps preserve that ratio, instead of shrinking names with output width. Curve names, pole captions, their clearance and pole markers use the same frozen text scale; the grid and trajectory strokes are unchanged.

Visible guide arcs that cross the closed 0/360 sampling origin are joined before measuring long names. Pole captions reserve their space first, and captions displaced from a flat-map pole receive a subtle connector back to the actual pole edge.

Geographic guide typography is capped at 110% of the point-pill base font. This prevents the viewport-height scale from overwhelming point labels in tall framed captures, while preserving readable names in full maps.

Main Sky and Camera angular grids use a thin two-tone treatment: a 0.30 dp white stroke at 40% opacity over a 0.60 dp black guard at 35% opacity. Unlike attenuated inversion, this retains distinct light and dark edges on the green ground and middle-grey backgrounds. The same drawing works on Android 8 onward, without camera readbacks, copied frames or additional image streams. Capture/export grids and the Globe keep their independent styles.
