# Changelog

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
