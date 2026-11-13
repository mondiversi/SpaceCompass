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

The flat Globe panorama includes a 15-degree geographic graticule, numbered on all four edges. Greenwich and the Equator are slightly stronger; reserved edge bands keep point labels away from coordinate numbers. Framed Sky/photo grids and full-sky grids share faint strokes normalized to output width; the live camera grid uses the same low-contrast palette.

Every capture preview offers a persistent Grid switch below its right-side controls. The chosen visibility travels into both image projections and export variants, including the cache identity; it never changes frozen orbit-table data. The main Camera axis slot is empty and is not a tutorial target.

All preview representations fill the available height below the toolbar at their original aspect ratio. Wider images remain horizontally pannable, and changing a rendered variant resets its preview transform to this height-filling baseline. The main Sky shares the faint live angular grid; Globe keeps its existing Guides switch. Mode changes report the chosen mode, except tutorial demonstrations and unchanged selections.

Capture grids use shared white 35% lines (1 px at 4096 px scene height, capped at their previous width-based thickness) and a thin black halo, with slightly stronger primary circles. Live Sky/Camera grids remain faint and stay visible when the main interface is hidden, without changing the saved capture-grid choice. Capture overlay buttons use the original compact size. Selected live Globe labels include T±; spherical captures centre the Earth across the complete image, compensating caption and legend heights. The compact tutorial documents the current controls in all twenty languages.

Portrait previews place Zoom in before Zoom out side by side in the upper-left corner. Caption options use the upper-right row in portrait and the upper-right column in landscape, scrollable when the height is limited. Landscape retains zoom in the upper-left column. The Frame/Panorama capsule highlights the selected choice in blue/cyan with day/night contrast ink. It remains at the lower right, side by side in both portrait and landscape. The photo never receives a Panorama switch. Every visible live Globe label includes T±; selected points are independent of the active track and are laid out before ordinary labels, with curve-overlap fallback to avoid dropping selected pills.

Live Globe pills offer full identity/event, clock/T and azimuth/elevation, compact event/clock, and minimal clock/T tiers. Detail responds to projected Earth size, label count and actual placement space, and remains stable between gesture/layout changes. Selected points are placed first and can overlap curves rather than vanish. Geographic guide names are independent of point-label scope. Equator, tropics and polar circles use terrestrial names and latitude, without the celestial qualifier. Their shaped text follows the visible front-face arc, with a dark outline; screen clipping retains visible segments even when both sampled endpoints lie outside the frame. Whole-string raster shaping preserves RTL joining. Names reserve space before point pills and remain attached to their own parallel when zooming or rotating.

The live Globe also marks the visible ends of the Earth rotation axis with outlined crosshairs and localized North Pole/South Pole captions (±90° latitude). Pole markers follow the Guides switch, remain attached to the geographic vertices, are not selectable orbit points, and reserve caption space independently of the point-label scope. Hidden-hemisphere and offscreen poles do not create detached labels.

Frozen Globe images include geographic guide names and pole captions whenever Guides was enabled at capture time, independently of point-label scope. Globe reference names use the selected/international export language without the celestial suffix. Visible geographic arcs are clipped to the actual image and split at the map dateline; shared shaped curve lettering reserves space before orbit pills. Both spherical frames and flat panoramas use the same frozen geometry, formatting and disclosure policy. Grid stroke thickness follows the scene height so filling a portrait preview does not make full panoramas heavier than perspective captures.

Globe captures freeze the live geographic guide text size relative to the expected capture viewport height below the system bars and toolbar. Both framed views and full maps preserve that ratio, instead of shrinking names with output width. Curve names, pole captions, their clearance and pole markers use the same frozen text scale; the grid and trajectory strokes are unchanged.

Visible guide arcs that cross the closed 0/360 sampling origin are joined before measuring long names. Pole captions reserve their space first, and captions displaced from a flat-map pole receive a subtle connector back to the actual pole edge.

Geographic guide typography is capped at 110% of the point-pill base font. This prevents the viewport-height scale from overwhelming point labels in tall framed captures, while preserving readable names in full maps.

Main Sky and Camera angular grids use a thin two-tone treatment: a 0.30 dp white stroke at 40% opacity over a 0.60 dp black guard at 35% opacity. Unlike attenuated inversion, this retains distinct light and dark edges on the green ground and middle-grey backgrounds. The same drawing works on Android 8 onward, without camera readbacks, copied frames or additional image streams. Capture/export grids and the Globe keep their independent styles.

The live portrait compass island has a 5 dp bottom inset, matching the 5 dp gap between the object and compass islands in both orientations. Sky/Globe capture previews use an explicit, mutually exclusive Frame/Panorama capsule at the lower right: side by side in both portrait and landscape. Frame uses four viewfinder corners. Choices retain separate saved projection preferences, localized labels, selected-state accessibility and full gesture exclusions; Camera has no projection selector. Both spherical and flat-map Globe captures draw parallels and meridians every 15 degrees; flat-map edge numbers follow the same interval. The compass island link is localized as Real position details and its tutorial title reuses that label.

Live Sky/Camera grids share the capture grid's cached 2-degree samples. A dedicated single-worker projector transforms each vertex once, rejects invisible segments with frustum outcodes, reuses primitive scratch buffers and emits one immutable path per frame. The draw phase reads the latest published path and draws its two tones in two calls; there is no grid frame-rate cap or pose rounding. Capture/export rendering remains separate.

Compact main-view panels retain the object selector header. Portrait reserves its measured height; compact landscape places the selector in a vertical rail beyond the right-hand controls, using a colour dot followed by upright name graphemes stacked from top to bottom along the rail. Long names truncate at the trailing end; accessibility retains the complete name. The sky/globe uses the remaining width at full height, with Daily path and Previous at the bottom, the dot/name group in the middle, and Next at the top. The daily-path glyph and split colour dot remain upright relative to the landscape screen. The painted gap between the daily glyph and its adjacent chevron retains the established half-gap geometry in all header layouts, independently of the outer inset. The daily glyph and outer chevron share the same painted-edge inset, including in the normal horizontal header. Live Globe fit uses separate horizontal/vertical trajectory bounds, stable across longitude rotation, so wide layouts use their available height without reserving an unnecessarily large sphere. Capture framing freezes the same fitted radius.

The native host supplies the Globe canvas size in actual WebView CSS pixels, independently of adaptive Compose density. Responsive canvas styles prevent a previous wide layout from inflating the mobile document viewport after rotation or opening the panels.

Live Globe transport separates immutable orbit/label/image content from viewport, exclusions and pose. Content serialization runs on a background worker; native updates coalesce to the latest state per presentation frame and do not parse content during rotation. The offline renderer retains track/image references across frame updates and skips unchanged DOM styles and canvas allocations. Pending updates are painted before point hit-testing.

Live Globe fit reserves symmetric geometric top/bottom insets in every layout, independently of floating-control visibility or measured button sizes. Floating controls never alter its fit. Double-tap now hides the information panels too, so the scene recentres and fits the full safe area; restoring the interface restores the saved panel layout. Compact landscape also follows the shared landscape optical axis across the complete available height. The corner controls, information caption and paused clock keep local label/touch exclusions without shifting the Earth centre or changing its fit when controls are hidden.

In compact landscape, Labels and Zoom form a vertical column and the Globe caption retains its bottom scene placement. In expanded landscape, the caption sits in the measured horizontal gap between the bottom Labels/Zoom group and the Mode/Shutter group. Its bounds contribute only a local label/touch exclusion; they do not change Earth centering or fit. The side rail retains the shared glyph sizes, painted-edge insets, action gaps and accessible actions, with its own upright stacked-name presentation. The live Globe drawing surface spans the complete main scene in all four portrait/landscape and compact/expanded layouts. Insets apply to its optical/control viewport, not to the native canvas, eliminating side strips in portrait and the lower clipping strip in landscape. Frame-only transport translates viewport and exclusions into scene coordinates; the frozen capture converts back to pointing coordinates, preserving zoom and centering. Panels and outside-pointing regions remain non-pickable label/touch exclusions while trajectories can paint beneath transparent islands. Control and caption positions keep their original insets.


The main double-tap clean view hides controls, reticle, object/compass islands, captions and status chrome. It preserves grid/reference choices, time, selections and the saved compact/expanded panel mode. Returning restores that mode. Main Sky/Camera grids remain drawn; photo/panorama preview toolbars retain their separate behavior. The object header has 16 dp between its painted outer chevron/Daily-path glyphs and the card edges; data/divider side insets remain 8 dp and body vertical padding is 3/4 dp. Header background empty space is halved around unchanged text/glyphs while retaining full action children. The compact side rail is 35 dp wide with 16 dp end insets.


The live Globe has an independent, batched 15-degree coordinate grid without numbers. It remains visible when planet reference lines are off or app chrome is hidden. Globe frame captures share this geometry, with discreet hemisphere-suffixed values such as 30° N and 60° E. Flat-map captures use the same suffix convention. Direction abbreviations follow the chosen image/export language (for example O in Italian and W in international English); zero is conventionally 0° N or 0° E so the axis remains explicit. The capture grid toggle controls both lines and numbers. Named planet/celestial references remain separate and retain label priority.

In Globe frame captures, latitude numbers follow both sides of the visible globe perimeter; longitude numbers attach only to their visible equator crossings. Fully visible polar parallels use truthful curve anchors and leaders to the perimeter. Coordinate text is drawn above trajectories, with collision avoidance and a discreet outline. The flat-map panorama keeps its edge longitude scales unchanged.

Globe Frame coordinate numbers use every visible 15-degree parallel and meridian.
Latitude labels retain their true parallel anchor without vertical collision shifts;
small leaders and anchor ticks disambiguate necessary horizontal offsets. Longitude
numbers stay beside their visible equator crossings. Each visible geographic pole
is marked at its projected position and labelled 90 degrees N/S. Main-view numbering
remains disabled, and flat-map top/bottom longitude scales retain their existing layout.
