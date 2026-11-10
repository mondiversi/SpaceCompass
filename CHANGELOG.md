# Changelog

## 1.9.1 — 2026-11-10

- Rename the virtual view to Sky and add a remembered Frame/Panorama switch for Sky and Globe captures. Camera keeps its single photographed frame.
- Freeze the visible framing, pose and observation data; export either or both image projections alongside the same captured CSV/readable tables.
- Add a persistent capture Grid control and consistent fine angular/geographic grids. Main Sky/Camera grids follow overlay visibility and adapt locally to background brightness, with a two-tone fallback on Android 8/9.
- Improve live Globe label details, selected-point priority and T± values; draw terrestrial parallels and pole captions in captured frames and flat maps, with balanced text sizes and collision handling.
- Refine compact capture controls: portrait zoom at upper left, caption options in an upper-right row, and a filled Panorama switch at lower right. Preserve landscape columns and accessible controls.
- Update the tutorial, in-app News in all twenty languages and the README. Keep the 9,999-object catalogue and individual-download graphics collection unchanged.

## 1.9.0 — 2026-11-09

- Add interactive Globe mode alongside Panorama and Camera, with Earth-centred satellite tracks and apparent directions for distant objects.
- Add axis-locked/free rotation, saved zoom, current-object images, point selection, Antipodes and theme-aware space backgrounds.
- Let live Globe labels follow the active island object. Offer four main-view scopes and all five capture scopes, with the main choice inherited at capture.
- Generate Globe images with frozen geometry, star backgrounds, rear-side labels, privacy-aware location pins and the existing image/table exports.
- Add a saved live-camera angular grid; refine controls, optical alignment and gesture exclusion margins.
- Extend positive and negative simulated time to fifteen days per second; refine Earth screensaver lighting and ocean visibility.
- Update the tutorial and twenty-language News; expand the English portrait/landscape README gallery without personal location data.

## 1.8.9 — 2026-11-08

- Clarify the zoom tutorial: two-finger zoom works in both the simulated sky and the real camera view. Remove the unnecessary explanation about separate saved choices.
- Keep the disabled 0°/90° control highlighted and explainable in the camera tutorial without activating the real camera or changing the saved pointing axis.
- Keep the tutorial explanation clear of the axis control in landscape, including the collapsed-island layout.
- Place 0°/90° immediately beside the chevron when landscape islands are visible; keep it below Menu when the islands are collapsed. Apply the same layout to the tutorial.
- Update tutorial text and in-app News in all twenty languages; keep astronomical calculations, saved selections and the graphics collection unchanged.

## 1.8.8 — 2026-11-07

- Add remembered virtual-sky zoom from 0.5× to 1× in six steps, defaulting to 1×. Use one perspective for objects, paths, stars, terrain, targets and guide labels.
- Keep camera zoom separate, starting at 0.5× or the widest available lens and respecting device limits and saved choices. Pair zoom with weather in the virtual view and object transparency in camera mode.
- Refine control placement: axis beside the chevron in portrait and compact landscape, below Menu in expanded landscape; selected points above Capture in portrait, beside it in compact landscape and at the sky column's lower-right edge in expanded landscape.
- Default new installations to the phone-length pointing axis (0°); retain existing axis preferences and the real camera's optical axis.
- Let the screensaver Earth be dragged, holding the chosen view until ten idle seconds pass and returning smoothly to the current rotation phase and seasonal orientation. Keep geographic sunlight and the real-position marker aligned in WebGL and the offline fallback.
- Require a double tap anywhere to leave the screensaver. Retain tracking suspension, music and low-rate idle rendering; update its return hint and zoom tutorial in all twenty languages.

## 1.8.7 — 2026-11-07

- Tap empty sky to show or hide the reticle and floating controls together; double-tap to pause or resume the effective observation clock.
- Resume live time at the actual current instant. Active custom dates, times or non-1× speeds continue from the held instant without overwriting Location settings.
- Keep an orange date, time and UTC offset visible while paused, even with controls hidden. Draw the pause symbol as a theme-coloured vector with the same dark-grey outline as the text. Running Location dates on either side of today retain the date readout.
- Use a single tap to toggle photo/panorama preview controls; title-bar actions remain visible and pinch/drag navigation is preserved.
- Place clock and T together with a middle dot in main-sky, photo and panorama point bubbles. Separate dated comparison headings from their date, wrap long titles and avoid repeated Personal Moment names.
- Update the concise tutorial and in-app news in all twenty languages. Keep the graphics collection unchanged.

## 1.8.6 — 2026-11-07

- Extend the symmetric time scale to ±86,400× (one day per second), with pause at the centre. Migrate supported legacy rates conservatively without changing direction.
- Defer Location changes until returning to the main sky and hold simulated time during editing, including across process restoration.
- Show a running future-day Location date, time and UTC offset at the top of the sky, with theme orange and a thin grey outline.
- Restore the last chosen time rate when re-enabling it, including zero and reverse rates.
- Clear selected live-path points when the effective local date changes, preserving fixed dated comparisons; restore imported scene selections against the incoming date.
- Compact main-sky azimuth/elevation bubbles with shared icons and middle-dot separation. In photos and panoramas, place clock and T at opposite edges without a separating dot.
- Update in-app news in all twenty languages; keep the external graphics collection unchanged.

## 1.8.5 — 2026-11-06

- Keep background music playing when headphones are disconnected. Android handles the media route; actual audio-focus interruptions still pause playback.
- Remove the object-island removal button and give the two navigation arrows equal visible edge margins and equal spacing around the name.
- Add dedicated Scenario export and Location export headings; other export pages retain Data export.
- Use consistent Load, loaded and loadable terminology in commands, feedback, accessibility labels and the Personal Moments file-format choice, translated into all twenty languages.
- Preserve the compact-portrait reticle at 40% of the usable height from the top; expanded portrait and landscape remain centred. The original compass island and its GPS details link are retained.

## 1.8.4 — 2026-11-05

- Save and share Scenes from the main menu, including selected objects, the active object, dated comparisons, active Personal Moments, orbit points and Antipodes.
- Restore scene display choices, with optional Location import; preserve the recipient's observer settings when Location is omitted.
- Separate Location files from celestial selections. Choose whether automatic parameters are completed with a stable snapshot of their current values.
- Keep local Personal Moment definitions while restoring incoming scene selections, and support previous mixed Location/object files.
- Compact and align the two Scene export checkboxes; automatic completion remains visible but disabled when Location is excluded.

## 1.8.3 — 2026-11-05

- Smoother scrolling in Key Events and Key Heights, including long lunar lists. Only visible rows and nearby content are composed.
- Pause hidden Observatory model, geometry and rotation updates while Key Events, Key Heights or Personal Moments is open, and resume on return.
- Reuse formatted row values and export resource profiles instead of repeating that work during scrolling.
- Retain per-object list positions during the current session, including across temporary loading placeholders.

## 1.8.2 — 2026-11-04

- Tap empty sky to toggle the reticle and its point information. Double-tap to show or hide the main sky controls. The gestures work independently.
- Double-tap a photo or panorama preview to toggle its overlay controls. Back and Export remain accessible in the title bar.
- Export Location and Personal Moments through dedicated screens. Personal Moments supports importable files and CSV, with a choice of all or one object and a selected-moments filter.
- Suggested names for both Personal Moments file types use the chosen export language, date and time formats. The importable data stays portable across locales.

## 1.8.1 — 2026-11-04

- Dated paths stay visible when their live object is unchecked. Live and dated selections are saved independently.
- Unchecking a dated path in Objects leaves its row in place for the current visit, so you can check it again.
- The Personal Moments trash icon uses the theme’s destructive red, with a confirmation before removal.

## 1.8.0 — 2026-11-04

- **Compare the sky across dates.** Add paths from Key Heights and Key Events alongside the live sky without changing your Location. The Sun also has a culmination entry for the 21st of each month.
- **Create Personal Moments.** Save a named date with an optional time for an object, then turn its path on or off. Share or import a dedicated Personal Moments file for that object.
- **Keep comparisons organized.** Dated paths follow their parent object in chronological order and reappear when it is enabled again. Key Heights and Key Events now show azimuth and elevation.
- **Find object tools faster.** Tap an object's name in the sky controls to open its Observatory. Its menu groups Daily Path, Key Heights, Key Events, Personal Moments and Export.
- **Share what you see.** Photos and panoramas label paths and include active comparisons in their exports. Location files can include the active comparisons, selected points and antipode visibility with the selected objects.

## 1.7.9 — 2026-11-03

- Same-version refresh: keep the antipode symbol fixed, with a grey icon/counter when hidden and the day/night accent when visible. Short visibility confirmations are translated in twenty languages; long-press clearing and tutorial behavior are retained.
- Replace off-state slashes with muted day/night greys while retaining each glyph's shape, size and optical alignment. Location uses grey when no parameters are customized; its active orange and counter remain unchanged.
- Add side columns specifically to compact landscape: Objects, Location, celestial guides and 0–24 on the left; axis/weather or camera transparency/zoom on the right. Camera moves above the fixed shutter, and selected-point/antipode controls move below Menu.
- Centre both side groups between their corner anchors and retain measured hit exclusions, so hidden orbital points cannot intercept button taps. Short windows keep bounded scrollable control groups.
- Pulse the shutter border between theme blue/cyan and the same neutral outline grey used by other controls, with a 3.2-second cycle. Preserve a visible static outline when system animations are disabled.
- Enable visual weather by default when no choice has been saved. Preserve explicit saved settings and keep weather retries hidden when visual weather is off or the camera is active.
- Keep preview Labels and Position grey and actionable when set to None. Place key/selected-point labels below selected-object/selected-point labels, preserving the default and stored mode keys.
- Consolidate control containers and shared alignment calculations, clean up the edited UI code, refresh documentation and translate release news in all twenty languages.

## 1.7.8 — 2026-11-02

- Include selected orbit points with celestial objects in Location exports and restore them on import. Preserve the active object, event information and point selection; keep older files compatible and reset derived antipodes on import.
- Use distinct, equally sized selected-point and antipode symbols with a neutral day/night counter. Short antipode toggles no longer vibrate; holding the button still clears selected points with haptic feedback.
- Remove the dark fill behind projected antipodes so the sky or camera image remains visible, matching photo and panorama exports.
- Keep tutorial taps read-only for antipodes and other controls. Only the camera control changes its tutorial presentation; it never starts the real camera. Explanations and accessibility labels follow the actual point/antipode state.
- Clarify the Android notifications for selected points alone or selected points with antipodes, with translations in all twenty languages.

## 1.7.7 — 2026-11-02

- Add an interactive main-view tutorial, accessible from the menu, and a welcome shown once. The camera demonstration does not enable the actual camera or alter saved preferences.
- Choose calendar-day trajectories (0–24) or the rolling next 24 hours (+24h), consistently across Daily path, labels, satellite passes, reminders, snapshots and capture-table exports.
- Replace the reticle button with a tap on empty sky to show or hide the reticle and hover information. Capture-label settings remain independent; the GPS details link is the island’s explicit navigation target.
- Show antipodal directions of selected points. Tap the Antipodes button to toggle them; hold it to clear selected points with haptic feedback. Visibility resets when all points are removed and starts off on a fresh session.
- Preserve visible antipodes in photo/panorama snapshots and their CSV/readable-table exports, without adding them to Daily path or its exports.
- Use a single orbit-coloured current-object outline, including above/below-horizon colours and offscreen locators. Selected current positions use the day/night accent, even with translucent object images.
- Refine tutorial highlights, accessible control actions and consistent twenty-language explanations.

## 1.7.6 — 2026-11-01

- Make the default catalogue order genuinely increase by Sun-centred distance, with consistent value columns, stable ties and retained selected-first grouping.
- Prefer validated structured satellite responses and share host-outage backoff across objects. Retain individual caches, provider protections and explicit orbital-validity limits.
- Distinguish loading, outdated data, dates outside coverage and unavailable quantities in all catalogue orders and twenty languages. Keep approximate solar references marked with ≈.
- Center Current on the first Daily path visit while preserving manually consulted session positions on return.
- Stabilize orbit and celestial-guide labels near crossings, viewport re-entry and camera-mode changes without unnecessary clearance from their own lines.
- Refine the 55/45 landscape split, GPS-altitude summary, floating capture size/pulse, optical chevron centering and normal-mode Weather/0°/90° order. Camera control positions are retained.
- Refresh the English portrait/landscape screenshot gallery and its main README previews.

## 1.7.5 — 2026-11-01

- Add **Key events** below Key heights in Observatory, with independent year selection and object/session scroll. Use the inspected body and effective Location, observer altitude, civil-year bounds and time-zone rules. Tapping a date preserves spatial overrides, applies the exact instant and pauses time atomically at 0×.
- Calculate solar polar daylight/darkness boundaries and full durations, longest/shortest ordinary daylight and local-clock sunrise/sunset extremes. Include lunar phases and geocentric apsides, supported planetary elongations/oppositions/apsides and annual Earth-distance extrema. Keep unavailable full-year remote/satellite data explicit; no fictitious physical events or eclipse predictions are introduced.
- Export frozen annual events as CSV or readable TXT, sharing the selected/international profile and position-disclosure choices. Reuse cancellable background calculations and bounded, observer-aware session caching; clock ticks and ordinary GPS drift do not reload completed calendars.
- Stabilize orbit/reference captions when nearby curves compete for space. Share measured text clearances, allow placement above/below tracks and retain accepted placement through small changes. Keep scientific positions and capture geometry unchanged.
- Reorganize main controls with Info/logo left, Menu right and centred upper actions. Place Camera beside compact capture and Weather/pointing-axis controls at the lower centre; camera mode reuses the centre pair for Transparency/Zoom. Use 60% sky and 40% information in readable landscape windows, with GPS altitude uncertainty on a second line below its value. Default unsaved camera zoom to 0.5× or the widest supported view; retain later saved choices.
- Improve 300 ms information-panel transitions by composing and ordering the displayed celestial subset rather than visiting the entire catalogue every frame. Retain canonical marker order, selection, directional locators, touch shields and accessibility fallbacks.
- Refresh six in-app news summaries in all twenty languages. Release 1.7.5/code 61 with the existing certificate, signed update index, reference-information archive, checksums and validation metadata. Reuse immutable graphics collection 1.8 and individual downloads, with no complete texture ZIP.

## 1.7.4 — 2026-10-31

- Open **Key heights** from Observatory on a dedicated daily-path-style page. Show moment, elevation and date/time in separate 40% / 15% / 45% columns, with compact event icons and numbering only for repeated events of the same kind.
- Choose an independent consultation year from 1900–2100, initially following the Location year. Keep significant annual extrema and midpoint passages, the effective observer and seasonal offsets. Cache completed calculations through GPS drift, navigation and rotation; do not invent annual data outside orbital coverage.
- Export the frozen annual result as a CSV spreadsheet or readable TXT table, with the shared selected/international profile and area-by-default position disclosure. Date actions still configure the Location at the exact event instant and pause time.
- Reorganize the main scene into upper-left Info/logo and Menu controls and two upper-right rows. The second row is Camera, Weather, 0°/90°; camera mode reuses the last two slots for Transparency and Zoom. Keep collapse/clear at the lower left and preserve capture size and its fixed landscape anchor.
- Share compact text/selection field geometry across settings, Location forms and export names while retaining native editing and existing catalogue search/filter dimensions. Tapping the Location-management title toggles prepared fields and time speed together, retaining the mandatory resumption policy.
- Retain independent per-object session scroll for Observatory, Daily path and Key heights. Refresh six news summaries and the shared page/action/export title in twenty languages.
- Release 1.7.4/code 60 with the existing distribution certificate, signed update index, reference-information archive and validation metadata. Reuse immutable graphics collection 1.8 and its individual downloads; no complete texture ZIP is distributed.

## 1.7.3 — 2026-10-31

- Add annual observation moments to Observatory: compare upper culminations throughout the Location calendar year and retain every significant local height maximum and minimum, using effective coordinates, observer altitude and local time-zone rules. The number adapts to each object and location, including both equatorial solar peaks and troughs.
- Display each event’s date, local time, UTC offset and elevation in degrees, using the selected numeric format. Show each passage near the mean of the absolute annual maximum and minimum heights. Choose the nearer actual upper culmination instead of interpolating a non-culminating time between days. Preserve multiple lunar/other passages; identify increasing and decreasing heights.
- Tap a calculated date/time to configure the Location at that exact instant and pause time at 0× atomically. Preserve spatial overrides and resumption policy, discard stale temporal checkpoints, and retain precise instants across daylight-saving overlaps and app reopening.
- Calculate annual tracks in cancellable background work, cached by object, year, Location and source. Clearly mark incomplete annual ephemerides and short-lived satellite data unavailable instead of extrapolating them into an annual prediction.
- Retain independent Observatory and Daily path scroll positions for each object within the activity session, including rotation, normal backgrounding and all main/catalogue entry points. Keep the desired offset through temporary loading/remeasurement, remember the daily header state, and prefer returning to the consulted position over incidental selected-point centering.
- Refresh all six in-app news summaries in twenty languages. Release version 1.7.3/code 59 with the existing certificate, signed update index and optional reference-information archive. External graphics collection 1.8 remains unchanged, with no new complete texture archive.

The refreshed 1.7.3 retains version code 59 and the existing release/tag. An already installed 1.7.3 does not receive a same-code update offer; manually reinstall the refreshed APK to obtain these refinements.

## 1.7.2 — 2026-10-30

- Refine the main scene with compact, optically balanced floating glyphs, equal-size centered Info/logo and Menu controls, and a standard anchored menu. Retain the lower-left collapse/pointing-axis row and restore dark capture symbols at night.
- Improve portrait/landscape reticle placement, information-panel spacing and object-island action glyph weight without changing scientific projections or saved choices.
- Embed a full-width rectangular map in the Location position editor; keep paired coordinate fields, estimated weather and consistent action buttons. Remove edge stretching only from the native-map editor.
- Keep the last complete catalogue observation and facet snapshots during automatic updates, preventing row/count/selection flashes. Let the first snapshot finish under accelerated simulated clocks.
- Add the app logo to photo and panorama metadata bands, preserving original floating-icon stroke weights while fitting their visible geometry. Sort language choices by their codes and refresh all six news summaries in twenty languages.
- Release version 1.7.2/code 58 with the existing signing identity, signed update index and optional information archive. Reuse immutable external graphics collection 1.8 without new texture archives.

## 1.7.1 — 2026-10-29

- Add a persistent main-view reticle/point-callout control, enabled by default and accented for the day/night theme. Hide live captions and their reminder action together without changing selected points or photo/panorama label policies.
- Move localized indexing, search, filtering, sorting and displayed-selection counts away from composition. Cache reusable sort/index results, publish only completed current queries, and calculate complete facet counts only while the filter panel is open. Count facets in one pass while retaining checked-object exceptions, favorite scopes and missing-data semantics.
- Defer optional thumbnail downloads/decodes and newly visible-object prefetch until scrolling settles. Keep cached pixels and selected/observed object updates available; do not cancel an already-started image download merely because a gesture begins.
- Replace the initial linear wait animation with a circular theme-colored indicator centered inside the empty list viewport, excluding the toolbar, search header and filter panel. Do not add artificial loading delays.
- Improve generated celestial-symbol contrast with translucent black backing in thumbnails and capture markers. Existing external photographic resources, catalogue identities and scientific calculations remain unchanged.
- Refresh six in-app news summaries in all twenty languages. Release version 1.7.1/code 57 with the existing certificate, signed update index, optional reference-information archive and unchanged external graphics collection 1.8.

## 1.7.0 — 2026-10-29

- Explore **9,999 catalogue objects**, adding 8,990 references across Solar System bodies, spacecraft, satellites, stars, exoplanets and deep-sky objects. Existing selections and favorites remain compatible.
- Expand real tracking with individually verified JPL targets and NORAD satellite references. Historical-only missions and unavailable measurements remain explicitly qualified; unresolved host directions do not pretend to be measured three-dimensional planetary positions.
- Optimize catalogue calculation, search and automatic data refresh. Remote resources are requested for selected, observed or visible objects rather than downloading thousands of unused ephemerides.
- Prepare Location values in always-visible, fully editable subpages. Enable or disable their use from Location management; prepared inactive values remain saved. Compact cards and a permanently checked, disabled Resumption checkbox clarify the controls.
- Add **graphics collection 1.8**: five new photographs and five independent thumbnails, with per-resource integrity checks, attribution and offline cache support. All celestial photography stays outside the APK.
- Refresh in-app release notes in all twenty languages. Retain Android 8.0+ support, the existing update signature, settings, filters, favorites and scene selections.

- Publish version code 56 with the existing distribution certificate, signed update metadata, reference-information archive, validation report and individual graphics resources.

## 1.6.0 — 2026-10-29

- Introduce persistent favorites in Observatory and a heart filter in Objects. Keep favorite membership independent of scene selection and active targets; use the shared day/night pink accent on favorite checkboxes without adding row washes or thumbnail rings.
- Make newly checked objects active immediately. Keep selected-first priority fixed for a catalogue visit, including Observatory round trips; regroup only after returning to the main scene and reopening Objects. Preserve saved filters, sorting, query behavior and checked-object exceptions.
- Keep open filters visible while only the search/value header follows quick-return scrolling. Count every filter option against favorites, search and the other active facets, using the same retained selected-object rule as the displayed list.
- Refresh object distances silently on their ten-minute cadence and as observer/model data change, retaining the independent horizon cadence, provider validity limits and verified offline caches. Remove exposed refresh gestures and progress notices.
- Rename scenario UI to Location in twenty languages without changing stable preference keys or version-1 file identity. Compact menu cards show effective values, cached locality and bold customized data. Only the leading icon/checkbox/title changes activation; other space opens the child view. Match child controls and pack natural-height cards in landscape. Resumption remains a direct, always-editable three-choice policy with the original toolbar glyph.
- Export optional checked celestial objects and the active object with a Location. Apply them atomically on import without transferring favorites or catalogue filters; preserve existing scene checks when omitted and clear them only for an explicitly empty included selection.
- Export fully automatic locations as current position, altitude and time zone only. Keep date/time live on the recipient and leave its resumption choice intact. Preserve full temporal settings for configured locations, canonical values under both export profiles, backward file compatibility and validation before modifying preferences.
- Refresh all six in-app news summaries across twenty languages. Release 1.6.0/code 55 with the existing distribution certificate, signed update index, optional reference-information archive, verification metadata and checksums. Reuse unchanged graphics collection 1.7 and its immutable individual downloads; no complete graphics ZIP or new celestial image is added.

## 1.5.4 — 2026-10-28

- Distinguish catalogue filters with a funnel glyph and sorting with paired up/down arrows. Retain the title-bar icon dimensions, theme-driven states, accessible actions and saved choices.
- Remove category background colors from object rows and Type-filter choices. Use neutral light/dark surfaces while preserving checkboxes, bold selected names, thumbnail rims, selected-object pinning, category membership and result counts. Remove the unused category palette and its three obsolete tint-only tests.
- Reduce the search outline from 48 to 40 dp within the existing 48 dp header touch space. Share one text style between input and placeholder, center both vertically, remove mismatched font padding and keep the clear action aligned. Preserve field width, native editing, query matching and keyboard actions.
- Refresh all six in-app news summaries in twenty languages. Advance version/display to 1.5.4 and Android code to 54. Keep catalogue data, celestial calculations, scenario files, preferences, the distribution certificate and external graphics pack 1.7 unchanged.

- Publish the signed APK and update index, matching optional reference-information archive, verification metadata and checksums. Verify the committed source, retained signing identity, physical phone, hosted checks and public downloads separately; preserve older releases and immutable graphics URLs.

## 1.5.3 — 2026-10-28

- Standardize every celestial guide label with a compact parenthesized qualifier in all twenty languages, live views, camera photos, panoramas and both export profiles. Retain the published 1.5.3 version/code while replacing its source tag and signed assets in place.
- Open Scenario management directly from the main view, replacing the intermediate summary dialog. Seven independent menu cards lead to Position, Altitude, Time zone, Date, Time, Time speed and Resumption. Keep the current effective values and cached locality in the menu, use trailing toolbar activation checkboxes, and retain full disabled forms in grey with consistent descriptions and 300 ms transitions.
- Offer optional automatic-value completion in scenario exports. The export dialog starts unchecked, in the order Name → automatic-values checkbox → divider → Data format. Save/Share require configured values or a complete effective snapshot. Complete copies use the applied position, altitude, zone and instant, retain clock/resumption choices, do not mutate the observer, reject missing or unsupported data, and preserve canonical version-1 file compatibility.
- Add persisted Origin (all/real/fiction) and Pointing (all/available/unavailable) filters alongside Type and Visibility. Pointing requires a valid direction, not a distance or an above-horizon position. Preserve selected objects as pinned visible exceptions, keep counts aligned with the displayed results, and do not reuse observations from a different scenario.
- Share category colors between catalogue rows and Type dropdown choices, including selected rows and day/night variants. Fictional objects retain their physical family colors; cross-category filter choices remain neutral. Move compact filter captions inside their outlined fields without opaque caption patches.
- Refresh the six in-app news summaries and align all 1,711 translatable keys across twenty languages. Preserve scientific data, trajectory calculations, object selections, units, permissions, caches, the distribution certificate and the immutable external graphics collection 1.7.
- Release 1.5.3/code 53 with the signed APK and update index, optional reference-information archive, verification metadata and checksums. Record local JVM tests, Android-test compilation, release lint, physical-device checks and hosted checks separately.

## 1.5.2 — 2026-10-28

- Move the rear-camera zoom selector beside the capture button. Add a lightweight half-dotted transparency control in its former position, slashed when off and theme-accented when on. Remember the camera-only 50% opacity choice; composite the complete object image, rim and backing together without fading trajectories or captions. Freeze that opacity in every captured photo and its later export variants.
- Share one remembered selected-format or international-format choice between image, table and scenario exports. Generate consistent filenames using the chosen language, units, date/time format and frozen capture time: hyphens within fields and underscores between fields. Preserve manual scenario names and canonical version-1 import data beneath localized readable comments.
- Add the scenario-resumption policy to Current scenario and refine the scenario-export description and divider. Shorten the Italian title to Ripresa scenario without changing saved policy values or scenario compatibility.
- Make the Earth screensaver marker follow only the last valid real GPS position, with a translucent grey far-side marker. Calculate seasonal tilt and geographic daylight from the real UTC Sun, independently of scenarios, device zone and interface theme; transform surface and sunlight consistently during decorative rotation. Keep bundled Earth maps unchanged, update the isolated renderer pin, and retain idle power-saving behavior and minute-rate solar updates.
- Refresh all six in-app news summaries in twenty languages. Retain catalogue data, astronomical calculations, camera calibration, permissions, selections and saved preferences. Reuse immutable graphics pack 1.7 and its individually verified offline downloads; no complete graphics ZIP or new celestial image is bundled or uploaded.
- Release 1.5.2/code 52 with the existing distribution certificate, signed update index, optional reference-information archive, verification metadata and checksums. Record local builds, JVM tests, Android-test compilation, physical-device checks and hosted checks separately.

## 1.5.1 — 2026-10-27

- Expand the catalogue to 1009 unique selectable entries with ten additional, explicitly illustrative science-fiction references. Science fiction overlaps the matching planet, moon and spacecraft filters without duplicate results. Approximate ranges identify their real reference source; unavailable physical facts remain absent.
- Publish incremental graphics pack 1.7: ten quality-90/method-6 WebP images and ten independent previews, plus the complete manifest, credits, naming rules and checksums. Preserve all immutable pack-1.6 URLs, aliases, bytes and verified offline caches. No complete graphics ZIP or new celestial image is bundled in the APK.
- Add an Observatory travel-time comparison from ordinary speeds through light speed and fictional Warp 9.999, with saved speed selection, current unit preferences and compact duration symbols.
- Save, share and import named scenario files with all independent overrides, time speed and resumption policy. Validate imports before applying them; restore configured starting values without adding the time a file spent archived. Suggest compact filenames from enabled settings and cached place names.
- Clarify the Current scenario popup with neutral labels/icons, highlighted effective values, automatic altitude/time-zone/weather indicators and cached place names. Keep the editor titled Scenario, the summary at 40/60 columns without dividers, and explicit altitude/time-zone overrides in their own islands. Use fast-forward for time flow.
- Show settings-save confirmation once on leaving a modified settings page. Refine theme accents, counters, object controls, catalogue interaction and callout layout; preserve real GPS details independently of the scenario.
- Refresh news in all twenty languages. Release 1.5.1/code 51 retains the distribution certificate and includes the signed update index, checksums and optional reference-information archive. Public release summaries describe general changes without singling out an individual catalogue entry.

## 1.5.0 — 2026-10-26

- Expand the selectable catalogue to 999 entries, including all 88 IAU constellations. Keep descriptions and twenty-language resources in the APK. Reuse observer/date-dependent pointing, daily paths, scenarios, search, filters, reminders and exports; unavailable facts stay explicit.
- Add identity-checked JPL Horizons, catalogue astrometry and NORAD/SatNOGS adapters with offline caches, validity limits, qualified distances and source provenance. Constellation patterns follow their actual linked stars. Selected objects alone participate in angular close approaches; alignments are not eclipse predictions.
- Publish graphics pack 1.6 with 174 full WebP resources and 174 independently pinned 128×128 previews, quality 90/method 6. Use coherent canonical filenames, exact individual URLs and integrity checks. Preserve legacy aliases, verified caches and every older release URL. No complete texture ZIP or new celestial photograph is bundled in the APK.
- Refine the panorama glyph with a central crest, visible points and dashed horizon, and join shutter blades into a closed aperture. Increase the object-island action gaps slightly and invert Observatory/Daily path circles at night for contrast; daytime styling and touch targets stay intact. Clarify Earth-only orientation references in all twenty Info translations.
- Refresh in-app news in twenty languages. Release version 1.5.0/code 50 with the existing signing certificate, a signed update index, optional reference-information archive, validation metadata and checksums. Install over earlier versions to preserve settings and selections.

## 1.4.11 — 2026-10-26

- Split the main object island's color dot horizontally: the upper half uses the full above-horizon orbit identity color and the lower half uses the existing below-horizon treatment. Keep the complete outline in the object's name text color, independent of the object's current elevation.
- Replace line swatches in photo and panorama legends with the same split-color circles. Size them to visible letter height and draw their outlines in the legend text color. Retain centered, wrapping legend rows and trailing truncation for long object names.
- Share the circle renderer between the live island, capture previews and image exports, preventing visual differences between photographic and panoramic output. Preserve astronomy, trajectories, horizon colors, point selections, privacy, saved preferences and graphics pack 1.5.
- Refresh three in-app news summaries in all twenty languages and verify translation key/placeholder parity. Publish 1.4.11/code 49 with the existing signing certificate, signed update index, matching optional reference-information archive, metadata and checksums. No complete texture ZIP or new bundled celestial image is added.

## 1.4.10 — 2026-10-25

- Make the main island's object name use the normal theme text color. A leading dot preserves the above-horizon trajectory identity color and adds a thin theme-text outline for contrast. Keep the dot visible when a long name is shortened on its trailing edge. Balance the visible caption-to-chevron gaps with a 12 dp minimum while preserving RTL and font scaling.
- Use shared solid-color button counters for objects, selected points and scenario sections. Increase the original counter baseline by 10%, remove the perimeter border, and use day-blue/night-cyan fills with white/dark bold digits. Keep the scenario icon's separate orange attention accent.
- Give Daily path data more room with 32 dp-wide reminder slots and a matching header allowance. Keep their existing height, action semantics and touch-target support. Use the same thin base bell stroke for every row; preserve bold/highlighted key-event rows.
- Refresh three in-app news summaries in all twenty languages. Verify translation key and format-placeholder parity, builds, JVM tests, release lint, signing, source identity and downloads. Record physical-device checks separately from compiled Android test sources; no connected instrumentation is executed.
- Publish 1.4.10/code 48 with the existing distribution key, signed update index and matching optional reference-information archive. Retain settings, selections, caches, astronomy and export behavior. Reuse immutable graphics pack 1.5; no new image package or complete texture ZIP is added.

## 1.4.9 — 2026-10-25

- Put checked objects first in the catalogue while retaining the chosen sort within checked and unchecked groups. Checked objects remain visible despite type/horizon filters or name searches; unchecking an excluded object hides it again. Preserve filter, sorting and selection preferences, hidden-object availability and Select all behavior.
- Open the object catalogue by tapping the main island's object name. Remove the island's X/removal action and its confirmation flow, giving more room to the name. Keep the existing Observatory and Daily path island buttons.
- Remove reciprocal destination actions from the Observatory and Daily path title bars; retain Export and direct Back navigation to the entry screen. Catalogue inspection does not add an unchecked body to the scene.
- Preserve the shared current-view/rotation capsule, model controls, celestial calculations, point selections, exports, privacy choices and graphics pack 1.5.
- Update the three in-app news items in all twenty languages and replace the files of the existing v1.4.9 release. Keep the visible version 1.4.9; increase the internal Android code from 46 to 47 so installations of the original 1.4.9 can detect this refresh. Rebuild the signed APK/index and optional information archive without creating another release.

## 1.4.8 — 2026-10-24

- Present export choices as Image (JPG), Readable table (TXT), then Spreadsheet (CSV). Daily paths show the two table choices in the same order; the Observatory keeps both image choices first.
- Rename the CSV option to Spreadsheet in all twenty interface languages. Keep the CSV format, MIME type, schemas and scientific values unchanged.
- Refresh the in-app news with three concise, localized export notes. Preserve formatting profiles, location-disclosure choices, initial unchecked file selections and return-to-preview behavior.
- Increase the Android/update identity to 1.4.8/code 45 using the existing distribution certificate, a signed update index and matching optional reference-information archive. Reuse graphics pack 1.5 without new images or a texture ZIP.

## 1.4.7 — 2026-10-17

- Refine outlined settings, repository and export actions with soft, same-family press feedback in both themes; keep label/icon colors and disabled styling stable.
- Strengthen thin action glyphs without changing their size, toolbar icons or solid repository-logo cutouts. Republish this maintenance revision as 1.4.7/code 44 at the owner's request, without a new version.
- Refine the object island with smaller navigation/removal glyphs, compact spacing, optically centred observatory artwork and neutral dark circular observatory/daily-path controls without perimeter borders.
- Unify floating icon stroke weights in the main sky and capture preview, improve the daily-path glyph, and align catalogue toolbar actions with the shared title-bar spacing.
- Refine the daytime blue accent while preserving the night palette, celestial calculations and saved preferences.
- Keep all twenty in-app news summaries and credits unchanged. Reuse immutable graphics pack 1.5, its individual downloads and offline cache; no new texture package or texture ZIP is added.
- Increase Android/update identity to version 1.4.7, code 44, with a signed update index and matching optional reference-information archive.

## 1.4.6 — 2026-10-17

- Lighten the morning and afternoon ground greens in the shared palette used by the main sky and panoramic captures. Keep solar-height-driven transitions, horizon haze, dawn, sunset, evening and night colours unchanged.
- Preserve all twenty in-app news summaries and credits, existing preferences, scientific calculations and the distribution certificate. Reuse immutable graphics pack 1.5 without a new image package or texture ZIP.
- Increase Android/update identity to version 1.4.6, code 43, with a newly signed update index and matching reference-information archive.

## 1.4.5 — 2026-10-17

- Rewind or advance custom scenario time using the symmetric −3600× to +3600× stepped slider, with 0× pause at its centre. Preserve the reached instant when changing direction and reset to +1× when re-enabling Time.
- Show a compact direction, signed rate and simulated-duration-per-real-second readout, with a centre-origin track and consistent physical slider direction in RTL languages.
- Cross midnight, month/year boundaries and daylight-saving transitions in either direction. Keep existing restart, active-only and always resume policies, provider validity limits and frozen capture moments.
- Refresh accelerated data consumers when the scenario clock changes direction. Add reverse-time persistence and integration regression coverage; maintain translation parity across twenty languages.
- Preserve the twenty in-app news summaries byte for byte from 1.4.4, all credits, saved preferences and distribution identity. Continue individual downloads from immutable graphics pack 1.5; no new texture package or complete texture ZIP is needed.

## 1.4.4 — 2026-10-17

- Expand the ordinary catalogue from 53 to 104 objects: additional moons, dwarf planets, asteroids, comets, stars, nebulae, clusters, galaxies, probes, compact objects and exoplanets. Hidden LV-426 stays separate from the ordinary count.
- Use identity-pinned JPL Horizons ephemerides for the added moving targets and catalogue directions for distant objects. Preserve explicit origin/validity checks, observer-dependent pointing, qualified reference facts and unavailable values. Correct parent-relative moon velocities with independent JPL fixtures.
- Add separate galaxy/nucleus entries for Andromeda, M87, the Milky Way centre and M51. Shared galaxy/nuclear directions do not create artificial close-approach events; approach calculations are limited to checked objects.
- Publish graphics pack 1.5 with 56 full external WebP images and 56 independent 128 x 128 previews, all quality 90/method 6. Catalogue and sky glyphs fetch previews without downloading full images; Observatory views request full images when needed. Preserve dynamic lunar phases, per-file integrity checks, offline caches and earlier release URLs. No texture ZIP or celestial photographs are bundled in the APK.
- Replace the retired Earth-centre guide with exact Nadir at −90°, opposite Zenith. Keep live reference names anchored continuously to their curves, avoiding jumps caused by moving markers and point callouts; give crosses consistent captions.
- Add 0× time pause and reset acceleration to 1× when re-enabled. Move scenario resumption to the Custom scenario title-bar action, preserve independent time controls and clarify midnight rollover.
- Order object types by total category count and sorting options alphabetically. Refine rise/set semicircles, daily trend chevrons, matching object controls and the 50/50 landscape split. Keep missing properties last.
- Show an explicit loading state while weather is being fetched. Refresh all twenty localized news summaries, scientific/image credits, reference-information archive and signed update metadata.

## 1.4.3 — 2026-10-16

- Expand the catalogue to 53 regular entries with Ceres, Enceladus, Vega, Deneb, Altair and the Southern Cross. Retain the existing Sun/Moon defaults and saved selections.
- Add Earth-centred distance sorting and a compact per-object Observatory shortcut. Inspect unchecked objects without changing the current target or checked selection; returning preserves the catalogue scroll and search.
- Add optional scenario time acceleration from 1x to 3600x, automatic day rollover and independent restart/active-only/always resumption choices under General > Events. Restart from the configured instant is the new-install default; existing choices are retained.
- Detect close approaches within 1.5 degrees only when both objects are close at the same absolute instant. Show the shared key event in live paths, daily paths, captures and exports. A crossing at different times is not an approach, and an approach is not an eclipse prediction.
- Refine daily-path symbols and spacing: ordinary hourly points indicate rising/falling elevation, key events retain their own symbols and Current retains its target marker and highlighted background.
- Add the 15-minute screensaver delay, shorten its explanation and use the compact delay dropdown. Balance the main object-action spacing and remove excess catalogue info-button margin.
- Make General's natural-height cards pack independently into two landscape columns; correct the system-theme preview and standardize the scenario-speed caption typography.
- Publish graphics pack 1.4 with 29 individual quality-90/method-6 WebP files, including Ceres and Enceladus. The APK pins the exact release, byte lengths, hashes and dimensions. Keep individual downloads, offline cache reuse, placeholders and previous release URLs; do not distribute a duplicate texture ZIP.
- Update in-app news in all twenty languages, documentation, information archive and the signed update index.

## 1.4.2 — 2026-10-16

- Add eleven trackable catalogue objects: New Horizons, James Webb Space Telescope, Parker Solar Probe, 99942 Apophis, 3I/ATLAS, Europa Clipper, JUICE, Hubble Space Telescope, Tiangong, M87* and Proxima Centauri b.
- Use verified NASA/JPL Horizons identities for seven moving targets, with apparent geocentric pointing, independent heliocentric motion and velocity-aware interpolation. Preserve position-only legacy caches.
- Track Hubble and the Tianhe core of Tiangong with validated current NORAD elements, provider fallback and SGP4/TEME propagation. Do not extrapolate beyond the existing element-validity window.
- Add an asteroid filter, scientific designations and explicit catalogue-direction notes for M87* and unresolved Proxima b. Unmeasured physical quantities remain unavailable.
- Pin external graphics pack 1.3: 27 individual quality-90 WebP files, including seven new verified archival images. Keep all celestial imagery outside the APK, preserve matching offline caches and retain earlier pack URLs.
- Refresh news and explanatory strings in twenty languages. Stop attaching duplicate complete texture ZIPs; safely reuse release drafts instead of creating duplicates.

## 1.4.1 — 2026-10-16

- Show bold object headings and optional event names in focused point callouts, with wrapping titles, balanced clock/countdown and angle rows, and matching light translucency on the caption and reminder button.
- Improve borderless count badges with bold white numbers, exact day/night scenario accents and a subtly lighter daytime blue.
- Place a plain remove-object X between navigation and Observatory, with balanced visual gaps, a narrower horizontal hit region and the existing confirmation.
- Bound weather requests and show a localized Retry action when relevant data are unavailable; preserve location/orbital status priority.
- Move LV-426 out of the APK. All 20 celestial images use external quality-90 WebP files from texture pack 1.2.1, including the Roadster photograph. Verify individual downloads and reuse matching legacy caches.
- Refresh news in all twenty languages and publish consistent graphics/reference archives. Preserve scientific calculations, saved settings, selections and distribution identity.

## 1.4.0 — 2026-10-16

- Navigate a steadier sky with adjustable Low, Balanced and High fluidity. Balanced is now the default; saved choices are retained. Gyroscope-assisted stabilization reduces stationary and handheld jitter without freezing real motion or changing geographic north.
- Send future orbital points to Android Clock or Calendar from the focused sky callout and daily path. The Events setting can hide the main-screen alert button; Android owns reminder confirmation and scheduling.
- Add Tesla Roadster (2018), with guarded JPL heliocentric ephemerides, reference facts, aliases and a downloadable 2K WebP archival photograph. It is a heliocentric payload, not an Earth satellite.
- Highlight the current daily-path row consistently, retain lunar-phase icons and key-event emphasis, and keep unavailable properties explicit.
- Remove an object directly from the main card after confirmation. Improve object-name space, optical arrow alignment and matching full palette colors in names and photo/panorama legends.
- Keep focused point callouts above celestial icons without the repeated above/below jump; center the alert button vertically beside the callout.
- Refine shared icons, confirmation buttons, export file headings and the selected-object label option. Hide the unavailable Display section while retaining its implementation.
- Refresh all twenty in-app news summaries. Preserve settings, selected objects/points, scenarios, scientific calculations and the existing distribution certificate.

## 1.3.8 — 2026-10-15

- Add compact symbols before rise, culmination, set, minimum and current-position names in daily paths, including satellite-pass events and coincident events. Preserve hourly rows and lunar-phase icons.
- Emphasize the complete key/current row: name, clock/T±, azimuth and elevation use bold text. Ordinary hourly rows retain their normal weight, and selections retain their existing colours.
- Use a plus sign for culmination and a minus bar for minimum consistently in the daily list, live orbit markers, photos, panoramas and selected/exported image variants.
- Make the shared photo/panorama title bar fully opaque using the standard page palette. Match the daily-path title bar to the Android status background in both themes, retaining the white light-mode table.
- Preserve all twenty in-app news files byte for byte from 1.3.7, together with translations, credits, preferences, selections, signing identity and immutable image pack 1.1.

## 1.3.7 — 2026-10-15

- Separate the custom date and time into independently enabled sections, alongside position, altitude and time zone. Add current-date/current-time shortcuts and an optional advancing clock whose offset survives app/device restarts.
- Add a Current scenario summary with localized values and an approximate place beneath coordinates, plus an orange active-section counter on the main scenario icon.
- Apply matching altitude and time zone from the selected position together, with validated metadata, section-specific activation/change/deactivation notices, automatic saving and consistent expanding-card feedback.
- Keep enabled Earth-reference names visible across reticle crossings and in both camera/panorama captures, regardless of object-label policy.
- Add a last-known-position marker to the screensaver globe; correct map handedness and eastward rotation, and vary the Sun-facing seasonal perspective continuously with the effective date, including advancing scenarios.
- Keep screensaver coordinates in session memory, pause lookup/sensor/GPS work, share globe/marker transforms across WebGL and the software fallback, and preserve the established illustrative lighting and accelerated spin.
- Refresh in-app news in all twenty languages. Preserve credits, saved preferences and selections, signing identity, immutable image pack 1.1 and previous release assets.

## 1.3.6 — 2026-10-14

- Add Key and selected points only below All in the capture label picker. Show rise, culmination, minimum, set and current labels across all captured objects, plus explicitly selected points; omit unselected hourly labels.
- Apply the same event/current policy to panoramas, camera photographs and exported image variants, including coincident hourly/key events.
- Make key-point labels the default for fresh settings while preserving existing saved modes and legacy switch choices.
- Translate the new option in all twenty languages and refine its Italian wording.
- Preserve the existing in-app news text, image pack 1.1, credits, saved preferences and distribution identity.

## 1.3.5 — 2026-10-14

- Display relative T+/T− offsets as compact hours and minutes (for example, T−3h27m) throughout the main view, daily paths, frozen photo/panorama labels and CSV/TXT exports. Clock times and time-zone offsets are unchanged.
- Keep orbital points and live markers behind the main title bar visible but prevent selection through it, including covered targets reached from below by their touch radius.
- Absorb blank-surface taps on the translucent object and compass/position islands while retaining child navigation, details actions and scrolling.
- Add numerical regression coverage for covered target/tap exclusion and compile Android gesture tests for island shields, child actions and scroll priority.
- Refresh all twenty in-app news summaries. Preserve preferences, selected points, scientific calculations, signing identity, image pack 1.1 and previous release assets.

## 1.3.4 — 2026-10-14

- Retain the visual scene during pause and sensor recovery so ground, orbits and object markers do not disappear when returning from background.
- Keep the last star frame while paused and avoid resubmitting unchanged GPU drawings.
- Separate display-only pose memory from current sensor trust, hit testing, live measurements and camera capture attitudes; remap held poses across all display rotations.
- Show compass notices only when the main view is active in the foreground, avoiding orientation-loading notices during exit.
- Use compact 28 dp, correctly positioned press feedback for object-navigation arrows while retaining 48 dp touch targets; apply a mirrored 1 dp optical correction to disclosure chevrons.
- Add a centred, cutout-safe screensaver return hint in all twenty languages.
- Refresh the in-app news in twenty languages. Preserve preferences, selected points, scientific calculations, signing identity and texture pack 1.1.

## 1.3.3 — 2026-10-14

- Keep the reticle white; highlight the existing object outline near the aim point without adding another circle.
- Use a single accented object outline for persistent selections, including exported photos and panoramas.
- Keep landscape capture fixed at the global bottom right throughout panel transitions, below the compass when panels are expanded.
- Center a floating translucent title/menu between the corner controls when landscape panels are collapsed.
- Match photo/panorama control top and side margins at 14 dp, retain 8 dp floating-control gaps and reduce the visible Observatory/daily-path gap to 13 dp for more title space.
- Use a slightly lighter daytime blue (#1B7E9F), retaining the night cyan (#55C8DB); share the accent across controls, links, selections and volume.
- Reuse selected-point projections in the shared scene interaction layer and refresh the in-app news in all twenty languages.
- Preserve saved settings, selected points, scientific calculations, privacy choices, signing identity and texture pack 1.1.

## 1.3.2 — 2026-10-14

- Replace permanent selected-point callouts with the reticle-focused callout, with clock and T± in separate columns and a heading only for named events. Point pins and exported selections remain intact.
- Add a shared translucent title bar to photo and panorama previews, with Back and Export actions and top-aligned side controls.
- Show active label/location controls in blue or cyan, with panorama direction selected independently below the location control.
- Include an object-colour legend in both generated panoramas and camera photos.
- Add larger wrapping photo metadata on an opaque black band above the complete lens frame; preserve field of view and projection.
- Draw paths, points and object images behind translucent main islands while retaining the reticle and interaction geometry in the free sky area.
- Rename the first Observatory section Name and type in all twenty languages and refresh the in-app news.
- Preserve selected objects/points, saved settings, location privacy, distribution signing and texture pack 1.1.

## 1.3.1 — 2026-10-13

- Preserve the active target when orbit points are selected or deselected.
- Include the active target's labels in the object-and-selected-points capture mode.
- Add point selection/deselection/clear haptics and a selected-point counter.
- Place named point titles above clock/T±, with azimuth/elevation on a separate row in both capture types.
- Preserve in-app news summaries, credits, saved preferences and texture pack 1.1.

## 1.3.0 — 2026-10-12

Explore and export the sky with multiple pinned orbit points, richer reference data and consistent location privacy.

- Select multiple points across orbits, keep their time/T± and azimuth/elevation labels open, manage the same selections in the daily path and clear them together.
- Export captures, daily paths and Observatory data as images, CSV or readable TXT. Share or save files using either your language/units or the international scientific profile.
- Interleave current, selected, rise/set and maximum/minimum points chronologically in exported tables. Capture exports retain the actual frozen observation and selections.
- Apply complete, area-only or hidden location disclosure to images and tables. Daily-path exports begin with area only on every opening; capture exports inherit the preview. Hidden disclosure displays UTC without observer coordinates, altitude or accuracy.
- Rename the object viewer Observatory and group identity, classification, physical properties, qualified core temperatures, orbital data and spatial context into readable sections.
- Add four-sided angular scales to camera photos and panoramas, improve narrow-angle/rolled-camera ticks, compact capture headings and refine labels and controls.
- Restore opaque main information islands in both themes and refine scrolling, spacing and compact position choices.

## 1.2.6 — 2026-10-11

- Make the Earth screensaver immersive, with local time, weekday/date and battery status; suspend live page sensors, GPS, camera and remote refresh while foreground music continues.
- Set the initial background-music volume to 20%; retain explicitly saved activation, volume and screensaver delay.
- Monitor stale orientation readings with bounded sensor recovery and distinguish calibration from possible magnetic interference.
- Refine inline status actions and progress messages; location search no longer offers Retry while it is in progress.
- Open the daily path directly from a trajectory point, center the selected row within the real scroll bounds and clear the temporary highlight on return.
- Keep the current position even when outside the visible sky; preserve lunar phase symbols and number hourly points 0–23.
- Combine the object name and date/time zone in a compact left-hand table heading, with azimuth/elevation aligned to their value columns.
- Add frozen T+/T− minute offsets to panorama and photo labels; refine main controls, capture controls and object identification.
- Refresh the in-app news in all twenty languages and describe Space Compass independently in current presentation text.


## 1.2.5 — 2026-10-10

- Add an offline animated Earth screensaver, enabled after five minutes by default, with saved delay/activation and touch-to-dismiss behavior.
- Keep the globe centered at the same physical size across rotation; its illumination follows observer/scenario solar height independently of the interface theme.
- Blend dawn, sunrise/sunset and daylight continuously from solar altitude across the sky, ground, atmospheric effects and frozen panoramic captures.
- Arrange main controls in mirrored corner groups: camera/pointing axis/references on the left, scenario/objects/weather on the right.
- Center status notices immediately below the reticle; keep data/retry priority and inline theme-aware actions.
- Place a larger, semibold camera zoom value beside capture, with a scrollable preset menu that can open above it.
- Let trajectories continue beneath the translucent main header, use a 55/45 landscape split and give object names more room between symmetric navigation arrows.
- Start background music at 50% on fresh settings, use a muted blue/cyan inactive volume track and refine loading ellipses in all twenty languages.
- Refresh all twenty in-app news summaries. Retain saved preferences, the existing distribution certificate and celestial texture pack 1.1.


## 1.2.4 — 2026-10-09

- Generate the original website space score continuously on-device, replacing the bundled recording.
- Retain saved activation/volume, seamless overlapping voices, foreground/audio-focus behavior and offline playback.
- Place status actions immediately after their message, using theme-aware blue/cyan links.
- Use normal text for routine statuses and orange only for magnetic interference, calibration and outdated orbital data.
- Add localized sentence endings to notices, keep action labels unpunctuated and simplify the satellite warning.
- Preserve the existing in-app news summaries and image pack 1.1.


## 1.2.3 — 2026-10-09

- Distinguish compass calibration, magnetic interference, reduced precision and recovering readings.
- Consistent theme-aware orange notices with no trailing full stops/ellipses across twenty languages.
- Clearer localized 0°/90° pointing-reference glyphs with improved optical alignment.
- Panorama/shutter capture glyph follows camera mode; export chooser says Export image.
- Retain the existing in-app news summaries for this maintenance release.


## 1.2.2 — 2026-10-09

- Original meditative space music available offline, enabled initially at 20% volume.
- Saved music activation and volume, with ten-percent slider steps and foreground/audio-focus handling.
- Appearance renamed General in twenty languages, with a distinct cog icon.
- Music and Custom scenario cards expand/collapse over 300 ms; disabled music hides its controls.
- Clearer pointing-reference icons with straight camera-axis and phone-length arrows.
- Saved audio settings are applied before the initial foreground resume.


## 1.2.1 — 2026-10-09

Maintenance release. The in-app news summaries remain unchanged.

- Compact two-line daily-path captions and theme-aware backgrounds for key events.
- Frozen current/event labels in both capture modes, collision avoidance and clearer colored borders.
- A saved choice between camera-axis and physical phone-length pointing, with camera mode retaining its optical alignment.


## 1.2.0 — 2026-10-08

Space Compass 1.2.0 expands the catalog to 35 objects and improves the live sky, daily paths and capture controls.

- Sirius, Betelgeuse, Titan, the Orion Nebula (M42) and the Pleiades (M45), plus a new Andromeda galaxy image alongside its existing core entry.
- Image pack 1.1: eighteen 2048 x 1024 WebP images, downloaded individually from the exact pinned release and verified before offline reuse. Matching older cached images are reused without another download.
- Collapsible main information panels, reorganized floating controls and a clearer orange Custom scenario indicator.
- Current position included in daily paths, improved selected-point spacing and one priority status notice, with connection retry taking precedence.
- Weather lighting follows dawn, daytime, sunset and night consistently in the live sky and panoramas.
- Refined panorama controls and export margins, capture vibration and compact object-viewer zoom.
- Updated news and complete key/format-argument parity across all 20 language catalogs.

## 1.1.3 — 2026-10-08

- Add an observer/time-aligned bundled star atlas, with sharper panoramic sampling.
- Download verified planet maps on demand and reuse them offline; upgrade Io and Europa to 2K WebP.
- Match weather coverage between the main sky and panoramas; save the weather toggle and camera zoom.
- Refine floating controls, toolbar icons, borderless islands, settings and scenario fields.
- Add compact horizontal object-viewer zoom controls with the initial size as the lower limit.
- Rename the information page to Info & credits and confirm completed object refreshes with a short notice.
- Update release news in all 20 languages and distribute optional information/graphics archives.


## 1.1.0 — camera zoom, catalog search and lighter captures

- Add consistent camera zoom steps, ultrawide lens selection where available,
  pinch gestures and direct level selection alongside +/−.
- Add live object search by localized name or catalog identifier, combined with
  existing type/visibility filters and numeric sorting.
- Include exposure-time direction, inclination and horizontal/vertical field of
  view in camera-photo captions. Keep user-formatted previews separate from the
  persisted international/current-settings export choice.
- Compact floating controls and object-view mode switch, centre both capture
  action columns, and keep capture/zoom above a selected point panel in portrait.
- Match calibration warnings to theme-aware Scenario orange; keep reduced
  precision neutral.
- Separate camera UI and lifecycle controller; memoize camera choices and
  release unfinished bitmap decodes on cancellation or errors.
- Save 4,450,604 bundled asset bytes: cap Pluto at the existing 2048-pixel GPU
  texture limit and encode the fictional texture as pixel-identical lossless WebP.
- Update six concise in-app news items in all twenty languages; retain complete
  localization, credits, offline calculations and saved settings.

Android/display version: 1.1.0; version code: 17. Same distribution certificate.


## 1.0.1 — landscape island layout fix

- Pack natural-height settings and information islands into the shorter landscape
  column to remove unused row space. Portrait layout is unchanged.
- Retain the existing in-app news text and saved preferences.

Android/display version: 1.0.1; version code: 16.

## 1.0 — first stable release

Space Compass 1.0 is the first stable release, bringing together the full sky compass,
camera view, panoramas, scenarios and configurable catalog.

- Track 30 celestial objects, explore daily paths and inspect textured object views
  with qualified physical facts and sources.
- Use type/visibility filters and sorting by distance, mass, diameter, pressure,
  gravity and temperature; unavailable values sort last.
- Overlay orbits on the rear camera or capture a complete 360° panorama. Optional
  reference curves have names following the curves; pole/zenith/geocentre crosses
  remain point references.
- Export manually to the gallery, a file or sharing with current-language/unit or
  international formatting. Labels and location disclosure are configurable; the
  default shows only the general place, without precise coordinates.
- Explore day/night maps and scenarios with independent position, date/time,
  altitude and time zone. Catalog data refresh every ten minutes and on request.
- Keep selections, filters, sorting and settings across restarts. Twenty complete
  interface languages, automatic light/dark themes and adaptive phone/tablet layouts.
- Enable release code/resource optimization and fix API 26–32 simulated-altitude
  compatibility, localized feedback and image timestamp metadata.

Install `space-compass-1.0.0.apk` over the existing app; do not uninstall or clear data.
Requires Android 8.0/API 26 or newer. The visible version is 1.0; Android and the
signed update protocol use 1.0.0, version code 15, for compatibility with prior releases.
The APK uses the same authorized distribution certificate as earlier versions.

Validation: debug and optimized release builds; 669 JVM tests; Android-test source
compilation; release lint with zero errors; twenty-language parity/placeholder/encoding
audit; real Samsung phone smoke checks. Connected instrumentation was not executed.
APK, signed update index and `SHA256SUMS.txt` are included. Source and scientific/image
license notices are available under the matching tag.
