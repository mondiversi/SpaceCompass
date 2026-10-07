# Verification

The stable **1.0** release summary, final checks and optimization/compatibility
review are documented in [RELEASE_1_0.md](RELEASE_1_0.md).

## Panorama caption vertical centering — 2026-10-06, unpublished

The caption uses half the difference between the shared heading height and
the fitted/shaped single-line layout height, so it stays centered above the grid
when text shrinks. The heading is 144 px at 4096 width, preserving clearance for
the unchanged 44 px zenith-marker reservation. The angular scene remains 2:1;
resulting exports are 4096 by 2192 or 3072 by 1644. Caption contents, frozen
reference time/zone, scientific formatting, disclosure and explicit Save behavior
are unchanged. All bitmap variants use the same renderer/annotation helper.

Debug and release builds succeeded, and all 532 JVM tests passed with zero
failures/errors/skips. Android instrumentation sources compiled but connected
instrumentation was not executed. The existing compact-caption fixture now
checks the vertical midpoint and +90-degree clearance for short, long, doubly
long and Arabic captions. Its exact solar-marker comparison follows the shared
header offset instead of an obsolete fixed offset. No resource strings changed.
Whitespace checks passed.

The new APK was installed as a same-key replacement on Samsung SM-G970U1 /
Android 12. A freshly generated live Sun/Moon panorama was visually reviewed
in landscape: its one-line caption is centered in the band above the grid,
with Save/Share ready and normal Back navigation. No Save/Share was pressed;
the gallery file set and system rotation preferences were unchanged. No new
tablet, fixed-zenith-marker or alternate-disclosure runtime check was performed.
Those caption/marker edge cases are in the compiled Android fixture only.

Exact installed APK SHA-256: `b417769ca8e06da1e3b7250900ac845f851744feac568a080d06f96c9ec217d9`.
Workspace evidence: `panorama-caption-center-phone-check.json` and
`privacy-caption-centered-*` XML/PNG captures. No GitHub publication occurred.

## Detail-card scrollbar clearance — 2026-10-06, unpublished

The celestial facts card and native Position details card now reserve the same
shared 8 dp horizontal inset as their external notes/credits. The inset precedes
the Compose clipping/background and is a relative margin on the native card,
so the unchanged viewport-edge overlay scrollbar remains outside the island.
Both readout implementations use this inset in portrait and landscape; internal
card padding, map/model frames, toolbar alignment and vertical gaps are unchanged.

Debug/release compilation succeeded and all 532 JVM tests passed with zero
failures/errors/skips. Android instrumentation sources compiled, including the
existing landscape/RTL/viewer layout fixtures; connected instrumentation was
not executed. No new tests or resource strings were added for this spacing edit.
Whitespace checks passed.

Manual Samsung SM-G970U1/Android 12 portrait checks captured Sun detail and GPS
detail cards before and after scrolling. Screenshot pixel measurements found
27 pixels of external space on each side of both cards, matching 8 dp at the
app's effective density. Visual review confirmed that the scrollbar is outside
each card, and GPS notes remain clear after scrolling. Back navigation returned
to the main view without changing saved preferences or system settings. No new
landscape/tablet/RTL/large-font runtime check, gallery save or share was performed.
The shared readout placement and compiled landscape fixtures were inspected.

Same-key replacement installation retained app data and verified exact APK bytes:
`4b9208d8e4f67c11771adaf97b2ce3d0ec6abcf6ae898bdee22c4f2343268069`. Workspace evidence: `detail-island-insets-phone-check.json`
and `privacy-detail-insets-*` captures. No GitHub publication occurred.

## Independent location and date/time overrides — 2026-10-06, unpublished

Simulation has separate Place/Simulate position and Date and time/Simulate date
islands with the shared heading glyphs. Both islands persist when collapsed.
The scrolling uppercase Set action is unchanged. Independent flags are encoded
in saved plans; legacy five-field records retain both overrides. Only enabled
inputs are validated. With position only, the live absolute clock, current
weather and selected place's zone are used. Date only retains live GPS/accuracy
and device-zone display but fixes the selected instant; both retain the original
combined behavior. Valid draft times rezone without shifting their instant when
the location source toggles. Both off restores live inputs and keeps the last plan.

Debug and release compilation succeeded. All 532 JVM tests passed without
failures/errors/skips, including ten new tests for legacy migration, all enabled
mode round trips, malformed flags, inactive input validation, clock continuity,
device/selected zones and actual New York night versus Italy daylight at the
same instant. Android instrumentation sources compiled, but connected
instrumentation was not executed. The read-only resource audit found all 392
translatable keys in each of twenty catalogues and no parity, placeholder or
encoding errors. Whitespace checks passed.

Manual Samsung SM-G970U1/Android 12 portrait checks visually confirmed heading
icons and separate expandable checkboxes. The original saved combined plan
loaded with both checked. Position-only mode displayed the current day and
Atlantic/Canary zone in Daily path; date-only mode preserved GPS-specific labels
in Position details. Its unchecked position/checked date state and fixed instant
survived an actual force-stop/relaunch. Switching back converted 22:00 Rome to
21:00 Canary without changing the instant. The original coordinates, altitude,
selected moment and combined mode were restored, with the original Sun azimuth
283.3 degrees and elevation -10.2 degrees. Both-unchecked draft islands were
checked; live mode was not committed in this phone run. New York was checked
in the JVM fixture, not by changing the phone's saved place. No system settings,
gallery save/share or other app preferences were deliberately changed. No fresh
landscape/tablet/RTL/large-font runtime check was performed.

Same-key replacement install retained app data and verified exact APK bytes:
`21e596fdb1242fe98d2ca32b3e40b387c4c0339d83f7b0e14511f7a182cb5132`. Workspace evidence: `independent-simulation-phone-check.json`
and `privacy-independent-simulation-*` captures. No GitHub publication.

## Simulation island and scrolling Set action — 2026-10-06, unpublished

The checkbox is inside the persistent Place island; its custom fields and
the Moment island appear only while checked. Set replaces Apply in all twenty
language resources, including the date/time confirmations. The main action
scrolls after Moment, before Open-Meteo attribution and privacy notes, and
remains available when unchecked. Validation/commit/cancel behavior is unchanged.

Debug and release compilation succeeded. All 522 JVM tests passed with zero
failures/errors/skips. Android instrumentation sources compiled; connected
instrumentation was not executed. The read-only string audit found all 391
translatable keys in each of twenty catalogues, with no parity, placeholder or
encoding errors. Whitespace checks passed.

Manual Samsung SM-G970U1/Android 12 portrait checks visually confirmed the
checkbox inside Place, collapse/re-expansion without losing entered values,
the unchecked Set action, and Set immediately before the credit in the same
vertical scroll. Set was absent at the top before and after scrolling back.
Back returned to the active saved simulation and unchanged Sun azimuth/elevation.
The confirmation was not pressed during this draft-only layout check. No system
settings, saved preferences, gallery files or sharing were changed. No fresh
landscape/tablet/RTL/large-font or confirmation-dialog runtime check was performed.

Same-key replacement installation retained app data and verified exact APK bytes:
`85c5348b037f841cb9f33f05227f38645ff3502579b8c781c820e8acdd01198a`. Workspace evidence: `simulation-scroll-layout-phone-check.json`
and `privacy-observer-simulation-layout-*` XML/PNG captures. No GitHub upload.

## Status-notice width order — 2026-10-06, unpublished

The sky notice layout measures the complete Simulation/compass/connectivity
islands and places them in descending width in the same pass, including Retry
and padding. Equal widths retain source order. It retains styles, 4 dp spacing,
the 280 dp cap, callbacks, minimum action targets and relative RTL placement.
Empty stacks report zero size instead of retaining an obsolete scene exclusion.

Debug/release compilation succeeded and all 522 JVM tests passed with zero
failures/errors/skips. Android UI test sources compiled; connected instrumentation
was not executed. The existing twenty-language phone/tablet/font/RTL fixture now
includes Simulation and checks visual width order/spacing. An additional compiled
fixture changes warning width in both directions, removes Simulation and checks
that Retry and Simulation callbacks still work. No resource strings changed.
Whitespace checks passed.

Manual Samsung SM-G970U1/Android 12 checks in the existing portrait orientation
observed a long low-compass warning above Simulation and, after a natural sensor
state change, the shorter Reduced accuracy warning below it. Both captured
states were in descending width. Tapping Simulation opened its page and Back
returned to the correct main view. No preferences, sensor/network state or
system settings were deliberately changed. Retry/connectivity failure was not
forced or runtime-tested; those checks are in compiled Android fixtures only.
No fresh landscape/tablet/RTL/large-font runtime check was performed.

Same-key replacement install retained app data and verified exact APK bytes:
`b5e6a0f80afacdc6f04da2de46e77f911c2224630f03f8729c307298bd1a97f4`. Workspace evidence: `notice-order-phone-check.json`,
`privacy-observer-notice-order-after-main.*` and `privacy-notice-order-*` captures.
No GitHub publication occurred.


## Shared simulation settings, uppercase buttons and hourly weather — 2026-10-06, unpublished

Simulation reuses the appearance/units option-row surface and label, maintaining
Checkbox semantics and draft/Apply persistence. Shared icon/text button captions
are uppercase before fitting, including repository/update actions; plain text
links retain their existing case. Custom plans now request the selected UTC hour
from Open-Meteo forecast/history, with valid WMO/cloud fields only. Missing data
keeps a clear visual base and Weather unavailable text; main/detail/capture use
the same snapshot. Historical Unix dates are supported and live freshness is
unchanged. Provider notes are updated in all twenty language catalogues.

Debug/release compilation succeeded. All 522 JVM tests passed with zero failures,
errors or skips, including ten new date/zone/coverage/value/clear-fallback tests.
Android instrumentation sources compiled, including three hourly JSON parser
tests; connected instrumentation was not executed. The read-only resource audit
found 391 translatable keys in every catalogue and zero parity/placeholder/
encoding errors. Whitespace checks passed.

On Samsung SM-G970U1/Android 12 in the existing dark landscape layout, checkbox
label start, selected-row RGB and height matched Appearance exactly. Unchecking
hid fields and rechecking restored them without Apply. APRI REPOSITORY and CERCA
AGGIORNAMENTI were uppercase; neither external action was pressed. With the user's
explicit Open-Meteo authorization, two provider checks returned valid 24-hour
forecast/history data. The app was checked using a nearby forecast date, a 2020
historical date and the restored unsupported future plan. Details showed the
matching Mainly clear/Cloudy/Weather unavailable conditions, and all three
panorama previews were generated and visually checked. The original saved date,
place/time and main azimuth/elevation were restored. No gallery save, external
share, system GPS/clock/rotation change or GitHub publication occurred.

No fresh light-theme, portrait, tablet, RTL or large-font runtime check was
performed. Runtime JSON checks exercised provider success and unsupported-date
fallback; malformed/error cases are covered by pure-value JVM tests and compiled
Android JSON tests, not a forced network failure on the phone.

Same-key replacement install retained app data and verified exact APK bytes:
`e9b33372f940c852f0f16861a25c201941481ea76d2293eda1e1651041f675ae`. Workspace evidence: `simulation-settings-style-phone-check.json`,
`observer-weather-provider-check.json`, `observer-weather-phone-check.json` and
the `privacy-simulation-style-*` / `privacy-observer-weather-*` captures.


## Map back-button alignment — 2026-10-06, unpublished

The native map toolbar formerly centered a 48 dp back button after a 5 dp
inset, while Compose pages used a 40 dp visual slot with independently expanded
touch bounds. This moved the map arrow 4 dp to the right. Both map modes now use
the shared visual slot and optical chevron geometry while retaining the full
48 dp native target. Pixel rounding preserves the title start as well.

Debug and release compilation succeeded; all 512 JVM tests passed with zero
failures/errors/skips. Android instrumentation sources compiled, but connected
instrumentation was not executed. No resource strings or unit calculations changed.

Manual Samsung SM-G970U1/Android 12 checks in the phone's existing landscape
orientation compared Simulation, Choose on map and Position details. All three
back targets had identical horizontal bounds and chevron ink bounds agreed within
one pixel. The picker returned to the unchanged draft, and both secondary pages
returned to the main view. The existing saved simulation was retained; no Apply,
map-point confirmation, system rotation/GPS/clock change, gallery save or share
occurred. No fresh portrait, tablet, RTL or large-font runtime check was performed.

Same-key replacement install retained app data and verified exact APK bytes:
`cd16e5b4420dce803b342cc9cc67ae1e77133e2e0b1d45a07ad32a84162f35b8`. Workspace evidence: `map-back-alignment-phone-check.json` and
`privacy-map-back-final-*.xml/png`. No GitHub publication occurred.



## Observer text actions and uppercase confirmations — 2026-10-06, unpublished

Apply (including calendar/time confirmations) and Use this point now display
uppercase captions using the selected app locale and retain the existing outlined
primary style, leading icon and remaining-width label alignment. Search, Choose
on map, Find altitude/time zone and Use current GPS position are plain text
actions without outlines or icons. The GPS shortcut is restored inside custom
fields; its existing bounded metadata workflow is reused. Checkbox, compact
simulation notice, validation and saved observer behavior are unchanged.

Debug/release compilation succeeded. All 512 JVM tests passed, with no failures,
errors or skips. Android instrumentation sources compiled; connected tests were
not executed. The read-only resource audit found all 391 translatable keys in
twenty catalogues without structural/placeholder/encoding errors, including the
restored existing GPS shortcut translations.

Manual Samsung SM-G970U1/Android 12 portrait checks viewed uppercase APPLICA and
USA QUESTO PUNTO, plain search/map/metadata/GPS text actions and the enabled map
confirmation after a point tap. Picker and draft were cancelled, leaving the
saved simulation active. No preferences, gallery save, external share or system
GPS/clock/rotation changes occurred. No fresh tablet, RTL, large-font, other-language
casing or Android 8 runtime checks were performed; network handlers were not
re-executed for this presentation correction.

Installed final APK SHA-256: `e5627d286c2ad066e78f76fb811e02a787f2c87cd4425aabe6615c8558d36a1c`. Same authorized key, byte-for-byte verified
replacement install and app data retained. Workspace evidence:
`observer-text-actions-phone-check.json`. No GitHub publication occurred.



## Simulation checkbox, compact notice and fixed icon slots — 2026-10-06, unpublished

The Simulation editor has one translated checkbox. Unchecked hides custom fields;
checked shows them. Apply retains the previous real/custom commit and validation
semantics. Mode radios, the GPS explanation and extra current-GPS action are
removed. Switching off cancels a pending metadata request. Simulation active is
orange text without an icon or forced visible 48 dp height; its typography and
padding match the compass notice, with Android's expanded touch semantics.

UVIR's helper was read as the layout reference. The shared app helper reserves
a leading icon/gap and centers a fitted, semibold settings label in the remaining
space. It is used by observer and repository actions, with full-width observer
rows. The native map action uses a relative leading drawable and centers the text
within the remaining area, retaining outlined/ripple/disabled states. UVIR source
was not edited.

Debug/release builds succeeded; all 512 JVM tests passed without failures/errors/
skips. Android instrumentation sources compiled but connected tests were not
executed. Resource parity covers 390 translatable keys in all twenty catalogues;
the obsolete four labels were removed and the checkbox label added throughout.
No missing/extra/blank/placeholder/encoding errors were found.

Manual Samsung SM-G970U1/Android 12 portrait checks viewed the compact notice,
Apply/search/map/repository button slots and the single checkbox. Unchecking hid
the fields; checking again restored the draft coordinate text. The native action
enabled after a map tap. Picker and draft were cancelled; the existing custom
observer and Sun values were retained. Main sky bounds did not change. No saved
observer, other preferences, system GPS/clock/rotation, gallery image or external
share was changed. No new tablet, RTL, large-font, light-theme or Android 8 runtime
execution was performed. A changed plan/live transition was not applied during
this UI check; the existing commit branch remains unchanged.

Final installed APK SHA-256: `0bf2e9b710b3391e0cd9b4f4b7baf15e39639ef1c8f84aadab6c099853c2ee7f`. Same authorized identity and byte-for-byte
verified replacement installation, with app data retained. Workspace evidence:
`simulation-checkbox-and-slots-phone-check.json`. No GitHub publication occurred.



## Panorama point elevations — 2026-10-06, local and unpublished

Every path-point pill now shows its time followed by that object's elevation,
using the international export profile, one decimal and degrees. Above/below-
horizon signs are retained; rounded negative zero is normalized. The renderer
uses marker positions, so objects sharing an hour do not share an elevation.
Existing measured collision placement expands to the complete label. Point pills
are drawn after every curve and orbit name to prevent another path from crossing
the text, while reserved current-marker/name space remains intact. The same
renderer serves previews and Save/Share disclosure variants; no preferences,
translation resources, clock policy or angle calculations changed.

Debug/release compilation succeeded and all 512 JVM tests passed with no failures,
errors or skips. Android test sources compiled; connected instrumentation was
not executed. Manual Samsung SM-G970U1/Android 12 checks generated and zoomed a
Sun/Moon panorama under the existing custom observer. Positive/negative values,
different elevations for matching hours and readable final labels above the two
paths were viewed. The preview was closed without gallery save or external share,
returning to the unchanged custom Sun values. No new tablet, RTL, many-object or
live-observer runtime execution was performed for this small rendering change.

Installed signed APK SHA-256: `dc54c8647f822707bd8787c65c4f7fa489b63249aaf3c17f687fc11c05a983ff`; authorized identity, exact installed
bytes verified and existing app data retained. Workspace scope/evidence:
`panorama-elevation-labels-phone-check.json`. No GitHub publication occurred.



## Simulation controls and status layout — 2026-10-06, local and unpublished

The menu/page now reads Simulation in all twenty catalogues. Apply, place search,
map/metadata/GPS actions and date/time confirmation use the same outlined accent
and leading-icon treatment as Check for updates. The native map picker has a
rounded outlined Use this point action, equivalent disabled palette and ripple,
with check and text centered as one group. A compact orange Simulation active
notice replaces the full-width date/time row beneath the toolbar. It opens the
Simulation page without moving the scene or data islands. Notices retain their
shared padding, 48 dp clickable targets and exclusion geometry.

Debug/release builds passed; all 512 JVM tests passed with zero failures/errors/
skips. Android instrumentation sources compiled, but connected tests were not
executed. The resource audit found all 393 translated keys in each of twenty
catalogues without missing/extra/blank/placeholder/encoding errors.

Manual portrait checks on Samsung SM-G970U1/Android 12 viewed the action icons,
orange notice, navigation from it and the native map action before/after a map
tap. The final check and label were visually centered together. XML bounds for
the sky and detail controls matched the earlier live layout exactly. Picker and
draft were cancelled, leaving the existing active custom observer and its Sun
azimuth/elevation unchanged. This turn did not change saved observer parameters,
theme, units, language, selected objects, system rotation/GPS/clock, or save/share
any gallery image. No new tablet, RTL, large-font, light-theme or Android 8 runtime
checks were performed. Prior light/dark map checks remain recorded below.

Installed final APK SHA-256: `722b04531fa0a488e1cbffaadc26d3f6a0f0bf5ecb4c0385df6ed5fadfab353a`. Same authorized signing identity,
replacement install, existing app data retained and installed bytes verified.
The workspace's `simulation-controls-phone-check.json` records this scope.
No GitHub publication occurred.



## Dark map and custom observation — 2026-10-06, local and unpublished

This current section supersedes earlier statements that observation simulation
was only a proposal. The APK keeps version 0.1.13/code 14 for local development.

Compilation: debug and release builds succeeded. All **512 JVM tests** passed
with zero failures, errors or skips. The eleven added observer tests cover
validation, persisted round trips/corrupt fallback, date boundaries, fractional
and summer/winter timezone offsets, DST gaps/overlaps and actual observer-dependent
Sun pointing. Android instrumentation sources compiled; **connected instrumentation
was not executed**. The read-only resource audit found all 392 translatable keys
in each of twenty language catalogues, including the 27 added observer strings,
with no missing/extra/blank/placeholder/encoding errors.

Manual checks used the connected Samsung SM-G970U1 on Android 12. Search found
Fuerteventura; map picking produced a valid point and metadata returned 505 m
terrain elevation with Atlantic/Canary. A plan for 2027-04-06 at 21:00 used the
expected UTC+01:00 offset and changed Sun azimuth/elevation. Its clickable
Simulation banner, Position details, regional place name and panorama caption
matched the selected context. The custom plan survived app restart and same-key
package replacement. The preview explicitly marked Simulation and unavailable
weather, without a gallery save or external share. Both light and dark OSM maps,
red marker and zoom were viewed. The dark rendering recolors raster tiles; it
does not introduce a different vector map provider/style. Calendar sizing was
corrected after portrait QA; all seven day columns and the full 24-hour clock
were then visually checked on the installed APK.

The phone was left in **Here and now**, with the original **Default theme**, Area
photo disclosure and selected Sun retained. The custom plan remains stored for
reuse. System GPS, time and rotation settings were not changed. Existing app data
were retained. Real compass accuracy was reduced during the indoor check; this
work does not claim to eliminate hardware/magnetic interference. No new tablet,
RTL, large-font or Android 8 runtime execution was performed in this update.

Custom weather is unavailable for every custom plan; no current forecast is
substituted for past/future weather. Existing remote/satellite validity limitations
remain. Saved plan date bounds do not guarantee that each remote object has data.
Bundled Leaflet assets were verified against official release SRI hashes and
retain their BSD-2-Clause licence and visible attribution. Open-Meteo metadata
keeps CC BY 4.0 credit; existing GPL/scientific/SGP4 notices remain.

Installed APK SHA-256: `5a52aa51569df97949033ee5fc3bab46c099fb6b13f91b496c6fb24dab3d35f7`. It was signed with the previously authorized
identity and verified byte-for-byte after replacement installation, with no data
reset. No GitHub publication occurred. Workspace evidence is recorded in
`observer-and-map-phone-check.json`, the current installation record and the
local validation record. See [OBSERVATION_SIMULATION.md](OBSERVATION_SIMULATION.md)
for behavior, persistence, map styling, provider transfers and limits.


## External explanatory text insets — 2026-10-06, unpublished

Object notes/credits and native Position details notes/weather attribution now
reserve 8 dp on both horizontal sides, keeping their text clear of the 3 dp
scrollbar and 2 dp track inset. Island/table padding and existing vertical insets
remain unchanged. No new strings or new implementation-mirroring tests were added.

Debug/release builds and all 501 JVM tests passed. Android test sources compiled;
no connected instrumentation was executed. The replacement APK SHA-256 is
`c3130fd117d7d2d5a4c6fdfd5025cba155d14b0d8c51271352199ed2fed3e079`.
The authorized Samsung SM-G970U1 / Android 12 installation preserved app data and
verified the package and signing identity. Manual dark/landscape scrolling
confirmed object explanatory notes/scientific credits and Position details
notes/weather-credit text sit inside the viewport clear of its scrollbar. Main
navigation and the original 1/0 system rotation configuration were restored;
Samsung's delayed rotation-setting notification required a delayed final restore.
No app preference, gallery photo or external share changed. Tablet, RTL and light
runtime checks were not repeated. No GitHub publication.

## Area default and separate panorama heading — 2026-10-06, unpublished

New or unset photo-position preferences default to Area. Existing explicit choices
remain respected; corrupt stored values still fail closed to Hidden. The current
authorized phone was explicitly switched to Area, as requested.

The caption baseline is raised by 20 pixels at full resolution, and a 112-pixel
top heading is added outside the unchanged 4096 × 2048 angular scene. An entire
38-pixel current-object marker at +90° fits below the caption; its drawing area
cannot reach the text. All orbit/grid/image positions retain the original
projection and move together. Output is 4096 × 2160, or 3072 × 1620 on lower-memory
devices; zoom/save/share continue to use the prepared image.

Debug/release builds and all 501 JVM tests passed. Android test sources compiled,
including the adjusted live-marker reference and zenith/caption separation check;
no connected instrumentation was executed. No new strings were added or removed.
The signed replacement APK SHA-256 is
`882276b14af1eaed929285d42c7c7415972961fa4cb48f36563e42060e1c0828`.
Installation on the Samsung SM-G970U1 / Android 12 retained app data and verified
the exact package and authorized signing certificate. Manual dark/landscape
checks showed the caption above the zenith grid, region/country only, the selected
Area radio and a subsequent capture retaining Area. Back returned to the main
view; system rotation stayed unchanged. No gallery write or external share
occurred. No actual object-at-zenith fixture was executed on the phone; tablet,
RTL, light theme and legacy storage behavior were not repeated. No GitHub upload.

## Object facts notes and panorama position disclosure — 2026-10-06, unpublished

The celestial facts island contains only the label/value rows. Explanatory notes,
reference-data caveats and scientific/image credits follow it outside the rounded
surface, within the same scrolling information pane. Portrait/landscape sizing,
insets and the shared persistent scrollbar remain unchanged.

The panorama preview adds a position pin before Save/Share. Complete keeps the
original location caption; Area uses only structured region/country values;
Hidden removes the place, GPS coordinates, altitude and accuracy. The selected
mode is persisted independently of live-unit settings. All variants use the
original immutable snapshot, capture instant, phone zone, science credits and
international export formats. Separate private JPEGs prevent a previous full
gallery/share image being reused for a private variant. Save/Share are disabled
while the requested variant is pending or failed; there is no complete-image
fallback. Fresh variant encoding retains only explicit capture/credit EXIF tags.

Debug/release compilation and all **501 JVM tests** passed (no failures, errors
or skips), including nine new disclosure/cache regressions. Android test sources
compiled, including two cache-only variant/EXIF tests; they were **not executed**.
All twenty language catalogs have 365 matching translatable keys, with no missing,
extra, duplicate, blank, encoding or placeholder errors in the read-only audit.

The signed APK SHA-256 is
`8e9af8d098d2fda21686c2d01bd83cc3853b5fea5ff4190a4557f36defa82157`.
It was installed as a replacement on the authorized Samsung SM-G970U1 / Android
12, with app data retained and the installed APK/signing identity verified.
Manual dark-theme landscape checks confirmed external object notes; Complete →
Area → Hidden → Complete → Hidden on one frozen capture; original time retention;
no gallery write until explicit Save; one saved hidden JPEG at 4096 × 2048 with
no GPS EXIF and retained credits; duplicate-save reuse; and a cancelled share
chooser without an external send. A new hidden capture after force-stop/restart
confirmed the stored mode. The final observed preview was Complete; system
rotation values remained restored to their original values. No additional photo
was created by the final read-only check. No shared-byte comparison is claimed.
Light, RTL, tablet, legacy storage permission and destination-picker behavior
were not repeated for this revision. No GitHub upload occurred.

Choosing another observer location/date is an information-only proposal and has
not been implemented. Hiding the printed location does not anonymize the sky:
the preserved time and orbit geometry can still suggest an approximate region.

## Single-column settings, direction symbols and GPS scrollbar — 2026-10-06, unpublished

Appearance and Units now use a single LazyColumn at every window width. Language,
Info/news, daily paths and the object catalogue already use vertical layouts.
Main-scene, GPS/map and celestial model/facts splits are preserved. Panorama
direction names remain above the horizon, with N/NE/E/SE/S/SW/W/NW directly below
their exact markers; the label reservation includes this additional lower row.

The native GPS table now draws a nonfading scrollbar using the celestial facts
pane's exact caller color at 46% alpha, width/inset/minimum geometry and shared
thumb calculation. It compensates content scrolling, hides when content fits
and supports the same 16 dp right-edge track/drag gesture. Native ScrollView
retains ordinary touch and accessibility behavior.

Debug/release builds and all **492 JVM tests** passed. Android instrumentation
sources compiled; no connected instrumentation suite was executed. The signed
replacement APK was installed on the Samsung SM-G970U1 / Android 12 with data
retained. A dark-theme manual landscape check confirmed Appearance, Language and
Units each retain full-width single-column options, including the scrolled
Display island. GPS screenshots before/after three seconds idle and ordinary
scrolling showed its persistent thumb; the right-edge drag changed scroll
position. The celestial facts pane was captured for a visual style comparison.
The actual panorama preview and zoom showed all eight symbols below the horizon
without a visible collision in this capture. Back returned to the main view.
Original rotation values were restored; selected Sun and settings were unchanged.
No image was saved to the gallery and no external share occurred. Tablet, RTL and
light-mode runtime checks were not repeated. No strings were added or removed.

At the time of this earlier check, location disclosure was only a proposal.
The subsequent Object facts notes and panorama position disclosure section above
records its implementation and current validation. No GitHub upload occurred.

## Uvir-style messages, dialogs and localization — 2026-10-05, unpublished

Transient notices now share Uvir's rounded, always-dark ARGB(238, 42, 42, 46)
surface and white text, with 20/13 dp insets, 14 dp corners, bounded longer
translations and automatic tablet scaling. Native notices retain the 88 dp bottom
offset. Gallery feedback keeps its existing preview-owned Snackbar lifecycle
while using the same message colors and insets.

App-owned dialogs share the neutral settings-card palette: #E6E9EB in light mode
and #282D33 in dark mode, with contrasting text and no Material elevation tint.
The panorama Save chooser now uses the common close button, bounded body scroll
and full-width dialog arrangement. Its card and logical configuration can rotate
together for the existing portrait tablet panorama viewport. Android-owned
permission, file-picker, installation and share surfaces remain system-controlled.

Debug/release builds and all **492 JVM tests** passed. Android instrumentation
sources compiled; no instrumentation suite was executed for this change.
The signed replacement APK was installed on the Samsung SM-G970U1 / Android 12
with its data retained. Manual checks confirmed the Save dialog's actual light/
dark colors and dismissal by X, outside tap and Android Back. The native manual
update-result notice appeared in both themes; its settled light-mode capture
confirmed dark background and white text. The dark-mode capture shows its entry
animation. No gallery photo was created and no external share was sent. The
original default theme and selected Moon were restored. Gallery status, RTL,
Android 8/9 permission handling and the rotated portrait-tablet dialog were not
runtime-tested again; their source changes compiled.

The read-only resource audit found **357 translatable keys in each of 20 locales**,
with no missing/extra/duplicate/empty/corrupt strings or formatting-argument
mismatches. Identical-to-English candidates were checked as proper names,
scientific designations, shared terminology and format templates. Popup,
position, weather and update wording was reviewed across the supported languages;
17 wording/register corrections cover German, Greek, Persian, French, Hindi,
Indonesian and Russian. No Uvir files were edited and no GitHub upload occurred.

## International panorama export profile — 2026-10-05, unpublished

Capture now uses Uvir's international export convention: English labels, metric/
SI lengths, decimal-degree coordinates, decimal points with thin-space grouping,
ISO date and 24-hour time. GPS numbers come from frozen raw measurements instead
of parsing the live UI's localized/unit-converted strings. Missing altitude or
accuracy stays unknown; a missing altitude is not exported as sea level. Object/
direction labels and orbital time pills use the same export profile. Live app
preferences are not changed by export.

The one-line caption includes the explicit UTC offset of the phone timezone
at the frozen capture instant, including daylight-saving and fractional
offsets. Save and Share retain the same prepared JPEG, with capture timestamp and
credits. Capture does not save automatically. No extra footer or side panel was
introduced.

Debug/release builds and all **492 JVM tests** passed. Seven new regressions cover
raw metric/decimal coordinates, signed negative altitude, unavailable/invalid
GPS data, cloud coverage, seasonal zones, fractional offsets/local-date rollover,
and stability after system locale/timezone changes. Android instrumentation
sources compiled; instrumentation was not executed for this change.

The signed replacement APK was installed on the Samsung SM-G970U1 / Android 12
without clearing app data. A manual check temporarily selected feet/miles in the
Italian app; the saved image still showed English export labels, decimal-point
coordinates and metres. The complete single-line caption, ISO date, 24-hour
capture time and UTC+02:00 zone were visually checked in the actual 4096 ×
2048 JPEG. Saving more than a minute later retained the capture time in the
caption, filename and EXIF. Preview performed no automatic gallery write;
explicit Save created one image and repeat Save created no duplicate. Zoom,
Share chooser cancellation and Back worked. The original default distance
preference was restored and verified. No external share or GitHub publication
occurred. Tablet, RTL and Android 8/9 permission runtime were not repeated.


## Consistent scrolling page titles — 2026-10-05, unpublished

Settings, Info/news, Objects, celestial details and daily-path page titles already
used the same single-line soft-edge scroller as UVIR. A source comparison confirms
the Compose behavior is unchanged after extracting shared motion values: 14 dp
edges, a 900 ms initial/end pause, a 1200 ms start pause and linear back-and-forth
travel between 2600 and 8000 ms according to overflow. Fitting titles stay still
and retain complete accessible text.

The native Position details toolbar was the exception: its title used end
ellipsis. It now has a constrained horizontal title viewport, fading edges and
the same shared automatic cadence. Native motion respects RTL, resets when its
width changes, and stops on detach/hidden windows. The normal toolbar retains its
48 dp minimum; its height can grow to show an enlarged accessibility font in full.
No strings, saved preferences, map styling or ordinary title spacing changed.

Debug/release builds and all **485 JVM tests** passed. Android instrumentation
sources compiled; instrumentation was not executed for this change.

The final signed replacement APK was installed on the Samsung SM-G970U1 /
Android 12 without clearing app data. At font scale 1.0, consecutive screenshots
confirmed a fitting Position details title stayed still. At scale 2.0, the native
Position details and Compose daily-path titles scrolled; both edge fades were
visually checked. The native title's complete vertical glyphs remained visible.
Both accessible labels retained their full text. Back returned to the main page.
Original font-scale and rotation settings were restored and verified. These
checks used the phone's dark theme. Tablet, RTL and light-theme execution were
not repeated. No UVIR files were edited and no GitHub publication occurred.

## Position details spacing aligned with object details — 2026-10-05, unpublished

The native GPS/map split uses the celestial viewer's 10 dp portrait gap and
12 dp landscape gap. The shared parent owns 10 dp side and 6 dp bottom insets;
the table no longer repeats them. Both panels start directly below the toolbar
with zero top inset. The existing card padding remains intact. The landscape
choice also uses the celestial viewer's available-width/height and 540 dp minimum
width condition. Rotation updates all child margins before measurement.

Debug and release builds passed; all **485 JVM tests** passed. Android
instrumentation sources compiled; instrumentation was not executed for this
spacing change. No new implementation-mirroring test was added.

The signed replacement APK was installed on the Samsung SM-G970U1 / Android 12
without clearing app data. Measured gaps were 34 px (10 dp at the app's scale) in
portrait and 41 px (12 dp) in landscape, then 34 px after rotating back. The map
started at the toolbar's bottom in every layout, and the landscape table card
shared the same top. Screenshots were visually checked in both orientations.
Back returned to the main page and original system rotation settings were
restored. Tablet, RTL and light-theme execution were not repeated. Map imagery
and its theme were not changed. No GitHub publication occurred.

## Single-line panorama caption and explicit saving — 2026-10-05, unpublished

The capture preview does not publish a gallery photo. Only Save/Gallery writes
one and then confirms success; repeated explicit saves reuse the readable URI.
Save/Choose location and Share retain the same frozen cache JPEG. The former
lower information footer is removed, restoring a full-width 2:1 angular scene.

A single centered line, with a slightly increased top inset, contains Space
Compass, the selected date/time, phone timezone abbreviation, estimated place,
latitude/longitude and horizontal GPS accuracy, GPS altitude and estimated weather
with cloud percentage. Dot separators and existing unit/number/coordinate
formatters are retained. Unknown values are not fabricated. The same bounded,
cached geocoder resolves the frozen location without opening Position details.
The phone's configured timezone and capture instant determine daylight-saving
names; no CEST value is hardcoded or inferred from GPS coordinates. The line
fits long and mixed-script content without wrapping. Image/science/weather
attributions remain in the app and are embedded in JPEG Artist/Copyright EXIF.

Debug/release builds and all **485 JVM tests** passed. Five caption regressions
cover exact content/order, absent data, imperial/S/W formatting, seasonal Rome/
New York/Los Angeles zones and mixed-script place names. Android instrumentation
sources, including the compact-caption and explicit-save fixtures, compiled;
instrumentation was not executed for this change.

The signed replacement APK was installed on the Samsung SM-G970U1 / Android 12
without clearing app data. Opening the preview and waiting did not add gallery
images or display a saved confirmation. Explicit Save/Gallery produced exactly
one 4096 × 2048 JPEG; another Save/Gallery created no duplicate, including suffixed
filenames. Its complete single-line caption, Zinasco/Lombardia/Italia location,
top margin and absence of a footer were visually checked. Capture time and
Artist/Copyright EXIF were verified. Zoom worked, the share chooser opened and
was cancelled, and Back returned to the main page. The checks used the phone's
dark theme. Tablet, RTL and Android 8/9 storage permission execution were not
repeated. No GitHub publication occurred.

## Automatically saved panorama with position footer — 2026-10-05, unpublished

Capture generates and publishes the frozen JPEG to DCIM/SpaceCompass before
opening its preview. An in-preview snackbar confirms success only after MediaStore
publication; a failed gallery write leaves the preview available with an error.
The preparing toast is cancelled before the success message appears. Android
26–28 request their existing storage permission in the preview if necessary.
The preview retains the published URI, and Save/Gallery reuses a readable capture
rather than creating another copy; a deleted capture can be published again.
Save/Choose location and Share still operate on the same frozen cache file.

The complete 360 × 180 degree scene now uses the full image width. Its lower
footer emphasizes the estimated place, then presents GPS coordinates/accuracy/
altitude, estimated weather, orientation and cloud/model time in three-column
cards, followed by complete image/weather credits and any data warnings. Wrapped
text determines footer height, and RTL titles mirror the card order. The capture
uses the GPS-details geocoder and administrative-name formatter on the frozen
coordinates, with bounded waiting and cached success/failure, even if GPS details
have never been opened. Unknown locations remain explicitly unavailable.

Debug/release builds and all **480 JVM tests** passed. Android instrumentation
sources, including footer and gallery-idempotence regressions, compiled;
instrumentation was not executed. All twenty panorama resource sets retain the
same keys and include the localized short saved confirmation.

On the Samsung SM-G970U1 / Android 12, the signed replacement APK preserved app
data. A single capture automatically published a 4096 × 2782 JPEG with capture
EXIF time; its preview displayed the confirmation. The actual photo showed
“Zinasco, Lombardia, Italia”, the lower cards and complete credits. Zoom worked;
Save/Gallery created no duplicate, including suffixed filenames. The document
picker and share chooser opened and were cancelled without an export or external
share. Back returned to the main page. These real-phone checks used the device's
dark theme; legacy storage permission, tablet and RTL execution were not repeated.
No GitHub publication occurred.

## Camera icon for panorama capture — 2026-10-05, unpublished

The main toolbar uses a camera outline in place of the shutter/aperture glyph,
immediately before the menu. The 24 dp themed vector preserves the existing
capture action, accessible label and touch target; the unused shutter drawable
was removed.

Debug/release builds and all **480 JVM tests** passed. Android instrumentation
sources compiled; instrumentation was not executed. The signed replacement APK
was installed on the authorized Samsung without clearing app data. A phone
screenshot confirmed the camera shape and placement; tapping it opened the
panorama preview and Back returned to the main page. No image was saved or shared
during this check. No GitHub publication occurred.

## Final result counts in filter fields — 2026-10-05, unpublished

The Type and Visibility fields append the same final visible-object count in
parentheses. The catalog passes its already filtered list size, so intersecting
type/horizon filters changes both counts together; the full catalog title remains
independent. Counts use the selected number format and reserve their width beside
the selected label, retaining visibility when long/multiple names are ellipsized.
Accessible field descriptions include the count; dropdown choices and persisted
criteria are unchanged.

Debug/release builds and all **480 JVM tests** passed; Android instrumentation
sources compiled. Instrumentation was not executed. On the Samsung phone, both
fields changed from two comets to one comet above the horizon, while the page title
remained “31 Objects”. The original filters were restored and no checked objects
were changed. The new APK was installed with the previously authorized identity
without clearing data. Tablet, RTL and dark-mode checks were not repeated.
No GitHub publication occurred.

## Full catalog total in the Objects title — 2026-10-05, unpublished

The catalog page title is now “31 Objects” (Italian: “31 Oggetti”). Its count comes
from the complete catalog order, independently of visible rows and checked
objects, and uses the selected number format. All twenty locales provide the
short Objects label with the same count placeholder. Additional catalog entries
will update the title automatically.

Debug/release builds and all **480 JVM tests** passed; Android instrumentation
sources compiled. No instrumentation was executed for this label change. On the
physical Samsung phone, the title fit on one line and stayed at 31 while the
Comets filter displayed only its two entries. Original filters were restored,
object selections were left intact and Back returned to the main view. Tablet,
landscape, RTL and dark-mode checks were not repeated. The APK was signed with
the already authorized identity and installed without clearing app data.
No GitHub publication occurred.

## Borderless panorama and side-panel metadata — 2026-10-05, unpublished

The panorama image fills its landscape page without a reserved title bar. Back,
Save/Share and Zoom overlay the appropriate corners with the celestial selector's
48 dp circular surface, theme colors, border and elevation. Save uses Uvir's
data-export folder/down-arrow geometry; Share retains its three-node glyph.
Disabled states, labels, image clipping and temporary landscape policy remain.

The export now integrates a right-side table rather than a footer: localized
heading, aligned labels/right-aligned values, wrapping cells and subtle dividers.
The angular scene occupies 72% of the output width, retaining the full 360 × 180
degree projection; the remaining panel contains position, accuracy, altitude,
estimated place/weather, orientation, cloud/model time, full credits and warnings.
Contrasting dots mark cardinal/intercardinal positions on the horizon. Orbit
labels/time pills avoid those reference labels. Live images, lunar phase, frozen
inputs, selected units and save/share file handling remain.

Debug and release builds passed; all **480 JVM tests** passed. Android instrumentation
sources compiled; the existing native panorama fixtures were adjusted for the
scene's new bounds. Instrumentation was not executed for this revision. Tablet,
RTL and dark-mode checks were not repeated.

The authorized Samsung SM-G970U1 / Android 12 received the signed APK by replacement
with saved data retained; exact APK hash/certificate and version were verified.
On that phone, the borderless preview and all five circular controls were visually
checked. Zoom in/out worked and retained the overlay controls. Gallery save
created `SpaceCompass_20261005_174220_293.jpg` (4096 × 1474); its bytes matched
the locally inspected copy and original capture time/UTC offset were verified in
the JPEG EXIF sub-IFD. The image has a complete side table and credits, cardinal
dots, and no UI controls. That single photo is retained in the phone gallery.
The share chooser opened and was cancelled without sending the image. Back
returned to the main view. The destination picker and complex-script image fixtures
were not rerun. No GitHub publication, version change or data reset was performed.
SHA-256: `5c5b7aaa93d65bdfe5e2a42bbf01bebc7124bacb03c0248b35d636b1e08df31c`.

## Five-minute catalog distance cadence — 2026-10-05, unpublished

The open celestial catalog recalculates distance rows and distance-sort values
locally every five minutes. Actual new orbital models and rounded GPS cell changes
still update immediately using the current time. Horizon filtering retains its
minute cadence; the inspected object's live compass/viewer cadence and independent
two-hour provider-cache policy are unchanged. Keyed scroll and saved selections,
filters, units and ordering are retained.

Debug and release builds passed; all **480 JVM tests** passed. Android instrumentation
sources compiled. The existing catalog fixture now checks that its cached distance
stays unchanged at one minute and just before five minutes, then refreshes at five
minutes; its synthetic model covers the longer interval. The arriving-data fixture
is retained. These Android tests were **not executed for this change**: the API 35
tablet emulator did not complete startup, and the test runner was not reached.
No successful current instrumentation result or provider availability is claimed.

The authorized Samsung phone received the signed local APK by replacement;
its exact hash/certificate and version were verified with app data retained.
No physical-phone visual or five-minute timing check is claimed for this change.
SHA-256: `16452a38a7f80033151f761ca40b87b918b772b2696a55e788fe817ba4b453ba`.
No GitHub publication, version change or data reset was performed.

## Capture in the toolbar and Info in the menu — 2026-10-05, unpublished

The main toolbar places a shutter icon immediately before the menu button.
Capture retains its existing panorama action, localized accessible label, 48 dp
touch target and disabled state during generation. The last menu entry is now
Info, opening Info/news; its short label is present in all twenty locales.
The clickable app title, existing preferences and panorama save/share flow remain.

Debug and release builds passed; all **480 JVM tests** passed, including locale
key/placeholder parity. Android instrumentation sources compiled; instrumentation
was not executed for this small navigation change. The signed local APK was
installed by replacement on the authorized Samsung SM-G970U1 / Android 12;
the exact APK hash and authorized certificate were verified with app data retained.
SHA-256: `27814392dd82e8a7a31b9f7011484d09a31243c4501239c73be3c9b8b20d0dfe`.

On that phone, the header and menu were visually checked. The observed controls
confirmed the shutter precedes the menu, whose order is Appearance, Language,
Units, Info. Tapping Info opened Info/news; tapping the shutter opened the panorama
preview with Save and Share, and Back returned to the main screen. No gallery
save or external share was performed. Tablet/RTL/dark-mode checks were not
repeated. No GitHub publication, version change or data reset was performed.

## Main header with app name only — 2026-10-05, unpublished

The main toolbar now shows Space Compass without the preceding Info icon or
inline version number. The title retains its original 16 dp leading margin,
content-sized clickable area and navigation to Info/news without touch indication.
The obsolete Info-button component is removed. The toolbar's 48 dp height and
right-side menu remain unchanged; app/version information still belongs to Info.

Debug/release builds and all **480 JVM tests** passed. Android instrumentation
sources compiled; no instrumentation was executed for this small header change.
The signed APK was installed by replacement on the authorized Samsung phone;
its exact hash/signature and version were verified with saved data retained.
A foreground-app screenshot was visually checked on that phone: the header has
only the aligned app name and right-side menu, with neither Info glyph nor version.
Title navigation was preserved in the source but was not tapped in this check.
Tablet/RTL/dark-mode checks were not repeated for this change. No GitHub publication
or version change was performed.

## Shared internal spacing for status notices — 2026-10-05, unpublished

Compass accuracy and missing orbital-data notices now share one internal padding
value: 12 dp horizontally and 6 dp vertically. The previous asymmetric padding
around Retry is removed. The rounded cards retain their leading sky corner,
bounded width, inter-card spacing, live-region semantics and 48 dp Retry target.

Debug/release builds passed, all **480 JVM tests** passed and Android instrumentation
sources compiled. The existing notices Android test **passed on the API 35 tablet**:
twenty languages, 360/800 dp fixtures, regular/1.6 font scales, RTL, non-overlap,
bounded card width and a clickable Retry action. The existing compact-height bound
now accounts for the shared padding. Phone/tablet fixture screenshots were captured;
the phone-size rendering was visually checked for space around both messages.
The full instrumentation suite was not executed.

The signed local APK was installed by replacement on the already authorized
Samsung phone. Its exact hash/signature and release version were verified,
app data were retained and launch was requested; no phone visual check is claimed.
No GitHub publication, version change or data reset was performed.

## Day/night ordering and missing parameters — 2026-10-05, unpublished

Debug and release builds passed; all **480 JVM tests** passed, including locale
key/placeholder parity, all seven numeric criteria with multiple missing values
in both directions, stable missing-value order, and old atmosphere-sort preference
fallback while retaining type/visibility filters. Android instrumentation sources
compiled. **One targeted Android viewer test passed** on the Android 15/API 35
tablet, checking Moon day/night rows, Jupiter's atmospheric reference pressure
level, ISS missing-temperature presentation and body changes in both themes.
The full instrumentation suite was not run; no physical-phone UI check is claimed.

The ordering menu now exposes daytime and nighttime temperature only; the generic
atmosphere-temperature criterion and its translated sort label are removed in all
twenty locales. Existing saved atmosphere-sort names resolve to Default. Other
saved criterion names and filter preferences remain stable. All missing numerical
parameters stay at the end for ascending and descending order, using default
catalog order for ties. Visibility filtering continues to keep unknown positions
in All only; no unknown position is asserted above/below the horizon.

Object details were audited against every remaining physical criterion: mass,
diameter, atmospheric pressure, gravity and available day/night references use
the same underlying models and selected display units. The viewer retains mean,
range, 1-bar atmosphere and stellar temperature references with their physical
labels; they do not become invented daytime/nighttime values in sorting. Its
existing thermal UI fixture now formats the pressure placeholder like the viewer.
NASA's Mercury/Moon facts were rechecked for the existing surface-extreme labels;
no new temperature measurement or fabricated atmospheric pair was added.

The final signed local APK includes all preceding offline work. The authorized
Samsung phone reconnected during completion and was updated by replacement with
its existing data retained. The installed APK hash/signature, release flags and
version were verified, and launch was requested. No physical-phone visual check
was performed; the reconnection monitor was stopped after successful installation.
No data reset, GitHub publication or version change was performed.

## Automatic catalog warming and live distance refresh — 2026-10-05, unpublished

Debug and release builds passed; all **478 JVM tests** passed, including five new
scheduling regressions and the existing cache/backoff/localization checks. Android
instrumentation sources compiled. **Two targeted Android catalog tests passed**
on the Android 15/API 35 tablet with disposable synthetic Horizons data: an
unchecked Halley row receives a distance while the page stays open, without
clicking it, moving the list or changing selection; that same cached model updates
its displayed distance when a minute passes. These fixtures make no live provider
request and do not claim end-to-end availability of JPL or CelesTrak. The full
instrumentation suite was not run.

All seven remote catalog targets are included in the foreground queue even with
an empty selection. Checked targets retain priority; local planets and stars
require no extra download. Initial disk-cache loading precedes network scheduling.
The existing two-hour refresh, independent component retries, provider HTTP stops,
validated atomic caches and cancellation handling remain in effect. Data warming
does not add unchecked objects to the scene or start a background service.

The final local APK also includes the preceding position/map/panorama changes and
uses the already authorized signing identity. Emulator replacement preserves app
data; the deferred phone installer is pinned to the new APK hash. The disconnected
phone was not visually tested. Version **0.1.13/code 14** is unchanged; no GitHub
publication was performed.

## Local position details and landscape panorama preview — 2026-10-05, unpublished

Debug and release builds passed; all **473 JVM tests** passed, including complete
twenty-language key/placeholder checks. Android instrumentation sources compiled.
**Seven targeted Android tests passed** on the Android 15/API 35 tablet: six
panorama/rendering/gallery checks and one native position-table integration check.
The full instrumentation suite was not executed. Legacy Android 8/9 storage and
the phone's landscape transition were not exercised in this change.

Manual tablet checks used synthetic Rome GPS/altitude. Position details shows the
map above a tabular readout in portrait and on its left in landscape; the native
OpenStreetMap marker is red. Map zoom and an open-page rotation retain the viewport.
GPS accuracy is below its label, altitude/place/weather values align on the right,
and the notes remain scrollable beneath. The Info icon and title independently
open Info/news. The main header uses an anonymous Info glyph and smaller grey
version number; its previous logo/version presentation below is historical.

Capture first creates a private JPEG and opens the preview: the gallery remains
unchanged until Save. A portrait tablet uses the full rotated landscape viewport
instead of Android letterboxing; rotating the tablet shows the normal landscape
layout. Zoom keeps Save/Share accessible. Gallery save produced a **4096 × 2411**
JPEG with capture EXIF; the Android destination picker and share sheet opened and
were cancelled without externally sharing a photograph. Back returned to the
unrestricted main layout and original emulator rotation settings were restored.

The exported scene was visually checked for ±90°/intermediate elevation labels,
eight directions, azimuth ticks, all marked-point times, matching Sun illustration
and lunar phase, metadata and required credits. Current images/names reserve
their space before orbital labels, so nearby time pills remain visible. Additional
targeted tests verify the shared ISS model and illuminated lunar terminator. The
existing selected objects/preferences were retained; no app data was cleared.

The signed final local APK was verified against the already authorized Uvir signing
identity and installed only on the emulator. Version **0.1.13/code 14** remains
unchanged. The phone was disconnected and was not installed or visually tested.
A thread follow-up checks for reconnection and installs only this reviewed APK by
replacement, checking its signature/hash and preserving data, then stops. Its
installation record is separate from these UI checks. No source/tag/APK/release
was published to GitHub.

## Local main-header logo/version — 2026-10-05, unpublished

Debug and release builds passed, all **473 JVM tests** passed, and Android
instrumentation sources compiled. Instrumentation was not executed for this
presentation-only change.

The main toolbar now places the existing Space Compass vector logo before the
title, with the actual BuildConfig version centered at the bottom of the badge.
The logo and title each navigate to Info/news with no touch indication. The
48dp logo touch target fits the existing toolbar height; the separate title
target retains its content width. Shared adaptive density and relative Row
placement preserve tablet scaling and RTL ordering.

Manual Samsung SM-G970U1/Android 12 checks confirmed the visible logo/version,
title alignment, both Info entry points, Android Back and an inactive gap
between title and menu. No tablet/RTL/live dark-mode check is claimed for this
change. Only the two logo/spacing lines were added to the existing main screen;
previous astronomy, map and catalog changes remain intact.

The signed local APK was checked against the previously authorized certificate
and installed by replacement, retaining app data. Version 0.1.13/code 14 is
unchanged. Nothing was uploaded to GitHub.

## Local map, compact ordering and comets — 2026-10-05, unpublished

Debug and release builds passed, together with all **473 JVM tests** and Android
instrumentation source compilation. Android instrumentation was not executed
for this change. Twenty-language resource-key/format parity checks passed.

Manual checks used the Samsung SM-G970U1, Android 12/API 31. Estimated place
names use commas and natural wrapping. Tapping the field label stays in
environment details; tapping only its values opens the OpenStreetMap map. The
native fullscreen map was visually checked with its GPS marker, attribution,
zoom/panning, matching light header/system bars, toolbar Back and Android Back.
Returning restores the environment page and existing selection. The tapped GPS
fix is frozen while the map is open; the native window avoids the phone's
Compose/WebView canvas incompatibility. No tablet/RTL/dark-map live checks or
offline-error UI execution are claimed for this change.

The ordering popup fits all nine criteria on this phone, with paired ascending
and descending radio choices. The Comets filter lists Halley and 67P; sorting
by descending mass puts known 67P before unknown Halley. Both filter and sort
remain selected after a process stop/relaunch. Original filters, default order
and active Moon were restored; checked objects were never changed.

Pure tests exercise invalid/polar/dateline map coordinates, locale-independent
URLs, physical-unit sorting, missing values in both directions and distinct
temperature quantities. Four real Horizons fixtures for the two comets verify
pointing/motion identity and frames, interpolation and a changing daily path;
wrong-target/mixed-frame responses are rejected. The comet data were tested
against captured NASA/JPL responses; a live comet selection/download on the
phone is not claimed. References are documented in [COMETS.md](COMETS.md).

The local APK was verified against the previously authorized signing
certificate and installed with replacement, retaining app data. Version
0.1.13/code 14 is unchanged. No source, tag, APK or release was uploaded to GitHub.

## Local effective setting previews — 2026-10-05, unpublished

Debug/release builds, all 464 JVM tests and Android instrumentation compilation
passed. Instrumentation tests were not executed for this presentation-only
change. Complete twenty-language resource-key/placeholder checks passed.

Manual Android 15 tablet checks verified a right-aligned preview for every
choice in all eight Units islands, including every System row, in landscape and
portrait. Temperature choices now use translated Celsius/Fahrenheit names with
degree symbols on the right. Coordinate, number, date and time previews use the
existing application formatters. Distance/speed, mass/density and pressure show
their effective units; density follows the selected length family.

Temporary imperial/Fahrenheit/DMS selections verified that System previews
continue to show device defaults independently of explicit choices. Theme and
language System previews were also checked. Original preference values and free
rotation were restored. The signed local APK was installed on the Android 15
tablet and Android 12 phone without clearing data. Live UI verification in this
change used the tablet; no live phone UI check is claimed. Version 0.1.13/code 14
remains unchanged; nothing was published to GitHub.

## Local curved-name spacing correction — 2026-10-05, unpublished

Debug/release builds, all 464 JVM tests and Android instrumentation compilation
passed. Four targeted Android 15 panorama/rendering tests passed, including a
new inside-turn fixture for complete Alpha Centauri, Stephenson 2-18 and Rigel
labels, tight-bend rejection and the existing complex-script/gallery checks.
The complete Android instrumentation suite was not executed.

The shared live/export renderer now measures and draws text on the actual
parallel offset curve with zero Canvas vertical offset. This prevents glyph
advances from being compressed on inward bends. Excessive local curvature,
sharp offset corners and labels that turn through more than a quarter-circle
are rejected. The text outline is thinner. Three pure geometry regressions
cover actual offset-curve length, reversed directions, tight bends, straight
offsets and insufficient space.

Synthetic inside-turn fixtures and the live tablet scene were inspected.
Temporary simulated pointing was restored to 90 degrees azimuth / 20 degrees
elevation. The local signed update was installed on the Android 12 phone and
Android 15 tablet without clearing data. Version 0.1.13/code 14 stays unchanged;
no GitHub push or release publication was performed.

## Local compact status notices — 2026-10-05, unpublished

Debug/release builds, all 461 JVM tests and Android instrumentation compilation
passed. One targeted Android 15 UI test passed across twenty languages, phone
and tablet viewport widths, regular/enlarged fonts and LTR/RTL layouts. It checks
that the connection-data notice sits beneath the compass warning in the sky's
leading corner, remains bounded and compact, and retains a clickable 48 dp retry
target. Synthetic fixture screenshots were inspected. The complete Android
instrumentation suite was not executed for this change.

The shortened data-unavailable message is translated in all twenty languages.
Manual retry preserves fresh cached data, unrelated request backoff and provider
HTTP stops; a JVM regression test covers this behavior.

The signed local APK was installed over the existing Android 12 phone and Android
15 tablet installations with the same authorized certificate and without clearing
app data. Version 0.1.13/code 14 remains unchanged for local development. No code,
tag, APK or release was uploaded to GitHub for this change.

## Last published release checks — 0.1.13, 2026-10-05

| Check | Result |
| --- | --- |
| Debug APK build | Passed, version 0.1.13 |
| Release APK build | Passed, version 0.1.13 |
| Release lint | Completed, 0 errors; remaining findings retained in reports |
| JVM unit tests | 460 passed, 0 failures |
| Android instrumentation sources | Compiled; 3 targeted panorama tests passed on Android 15 |
| Phone UI checks | Samsung SM-G970U1, Android 12 / API 31 |
| Tablet UI checks | Pixel Tablet emulator, Android 15 / API 35 |
| GitHub automation | Debug/release builds, JVM tests, instrumentation compilation and release lint |

The current local command is `gradlew.bat :app:assembleDebug
:app:testDebugUnitTest :app:compileDebugAndroidTestKotlin`, using Android Studio's
JDK 25 and the installed Android SDK/cache. Release preparation additionally ran
:app:assembleRelease and :app:lintRelease. The Android UI checks above were manual checks. Three targeted panorama
instrumentation tests ran on Android 15; the complete instrumentation suite was
not executed.

Phone and tablet checks include shared toolbar dimensions, dropdown scaling,
catalog filters/sorting, the filter separator, title-only Information navigation,
point captions and offscreen-arrow colors. Tablet rotation was checked using
synthetic location and orientation. Existing application data was retained during
updates. The verification of one layout does not imply every Android version or
device configuration has been tested.

Source code and GitHub automation are published at
[mondiversi/SpaceCompass](https://github.com/mondiversi/SpaceCompass).
The repository contains no signing key or local SDK configuration. GitHub APK distribution is described in [GITHUB_RELEASES.md](GITHUB_RELEASES.md).
No Play Store submission is part of this release.
The current CI result is available on the repository's Actions page; the local
results above do not stand in for a successful hosted run.

## Orbit names and gallery capture — version 0.1.13

Debug/release builds, 460 JVM tests and Android test-source compilation passed;
release lint completed with zero errors. The three targeted Android 15 panorama
tests passed: frozen solar-position rendering, complete Latin/Arabic/Persian/Hebrew
names on curved paths and a JPEG gallery round trip with capture date/album checks.
The rest of the instrumentation suite was not executed.

Manual Android 15 tablet checks used synthetic Rome GPS and orientation. Capture
appears below Units and saved a 4096-pixel-wide JPEG in DCIM/SpaceCompass, including
the complete horizon, dashed underground paths, current Sun/Moon markers and
repeated curve names. The live viewport shows readable names near arrows when
they fit inside its actual bounds. The saved capture timestamp is backed by EXIF,
so MediaStore scanning retains it. Preferences and selections are preserved.
Android 8/9 legacy permission/storage handling is implemented and API-checked,
but no Android 8/9 device was available for a live storage check.

See [PANORAMA_CAPTURE.md](PANORAMA_CAPTURE.md) for geometry, storage and coverage.

## Estimated place — version 0.1.11

Debug/release builds, all 451 JVM tests, Android test-source compilation and
release lint (zero errors) passed. The instrumentation suite was not executed.
Twelve new tests cover readable/partial names, county/district fallback,
duplicate/Unicode whitespace handling, invalid/boundary coordinates, stable
GPS cells, locale/location isolation, successful/failed expiry and bounded LRU
reuse. Full twenty-language string-key/placeholder integrity checks passed.

Live Android 15 tablet checks used synthetic Rome and Milan GPS positions. The
device geocoder resolved city, region and country correctly; moving the position
replaced the old place and returning to Rome reused its cache. Reopening the
environment page retained the available label. Visual inspection confirmed the
island below GPS altitude and above weather, right-aligned names, unchanged
coordinate/altitude formats and portrait/landscape readability. Synthetic Rome
GPS and free rotation were restored. Final phone/tablet installation and any
additional device checks are recorded separately in the release notes.

## Info heading icons — version 0.1.10

Debug/release builds, all 439 JVM tests, Android test-source compilation and
release lint (zero errors) passed. The instrumentation suite was not executed.
The signed candidate was checked on the Android 15 tablet in light and dark
themes and portrait rotation. Visual inspection covered the GitHub and copyright
heading glyphs, spacing, readability and scrolling. The original System theme
and free rotation were restored. No new phone UI pass is claimed.

## Unit and numeric consistency — version 0.1.9

Fourteen additional regression tests cover reference conversions, mixed saved
preferences, all available catalogue gravity/density/temperature facts, formatted
pressure reference placeholders, all twenty app-language clocks, Arabic versus
explicit digit choices, localized AM/PM/date order, DST repeated hours, count/zoom
arguments and unavailable numbers. All 439 JVM tests passed. Debug/release builds,
instrumentation-source compilation and release lint (zero errors) succeeded.
The instrumentation suite was not executed. Complete twenty-language string-key
and formatting-argument parity passed. See [UNIT_FORMAT_AUDIT.md](UNIT_FORMAT_AUDIT.md).

The signed candidate was checked on the Android 15 tablet using synthetic GPS
and orientation. A mixed profile used imperial length/speed, kilograms, pascals,
Celsius, American numbers, international date order and a 12-hour clock. Sun
diameter/mass/gravity/density/temperature rows were verified in landscape and
portrait and retained their preferences after process restart. Jupiter's
temperature-layer label and reference note both showed the selected 100,000 Pa
level without creating a nonexistent surface-pressure row.

Arabic-interface checks covered the daily table, selected-point island, reticle
balloon, localized AM/PM and whole-minute T+/T− captions. Explicit American
numbers and System/Italian-device conventions both retained Latin clock digits;
the angles changed from dot to comma appropriately. Visual inspection confirmed
that negative-angle signs precede their values in RTL. Original automatic units,
number/date/time/language choices, Sun/Moon selection and free rotation were
restored. Final installation is recorded in the release notes; this does not
claim a new phone UI pass.

## Dynamic mass display — version 0.1.8

Seven new JVM tests cover both sides of the exact 10-solar-mass threshold in kg
and M☉ references, independence from category and pound choice, Sun conversion,
lower stellar/compact masses, converted uncertainties/qualifiers, binary totals
and separate components, unchanged high-mass references and invalid/missing data.
All 425 JVM tests passed, together with debug/release builds, instrumentation-source
compilation and release lint (zero errors). The instrumentation suite was not run.
Complete translatable-string parity passed for all twenty interface languages;
recent news and the plain estimate/model note are localized.

The signed candidate was checked on the Android 15 tablet: Sun kg/lb in portrait
and landscape, pound preference surviving process restart, Alpha Centauri A+B
and both resolved components in pounds with uncertainties, and Rigel retaining
solar masses with the pound choice. Original automatic mass units, free rotation
and Sun/Moon selection were restored. Final distribution installation is recorded
in the release notes. No new phone UI pass is implied by these tablet checks.

## Atmospheric pressure — version 0.1.7

Nine new JVM tests cover exact bar/Pa/psi conversion, regional defaults and explicit
pressure independence, saved preference observation/reload, invalid data omission,
published pressure scales, supported/unsupported bodies, qualified upper/night
limits, negative exponents and localized numeric conventions. All 418 JVM tests,
debug/release builds, instrumentation-source compilation and release lint passed;
lint reported zero errors. The instrumentation suite was not executed.

All twenty interface languages have complete translatable-string parity, including
nine new pressure/mass-density labels and the recent-news summary. The pressure
island follows mass/density and uses the shared island/radio/example layout.
Atmospheric/exosphere reference data and exclusions are documented in
[ATMOSPHERIC_PRESSURE.md](ATMOSPHERIC_PRESSURE.md).

The signed candidate was checked on the Android 15 tablet using synthetic GPS:
Moon pressure conversions to bar, Pa and psi; retained kg/m³ density; process
restart preserving PSI and active Moon; missing Sun pressure row; pressure radio
choices in light/dark themes and portrait/landscape. System pressure, original
theme, free rotation and active Sun were restored. The final distribution package
is rebuilt from the committed source; final phone/tablet installation is recorded
in the release notes. No new phone UI result is implied by tablet checks.

## Info and news — version 0.1.6

The renamed page and three-item recent-news summary are localized in all twenty
languages, with matching resource keys and version placeholders. The news island
follows the description/Earth-use note and precedes GitHub/credits; it uses the
existing app island component and UVIR's two-sparkle glyph.
Debug/release builds, all 409 JVM tests, instrumentation-source compilation and
release lint passed. No new test mirroring this reversible text/layout change
was added. The instrumentation suite was not executed. Focused signed-APK UI
checks are recorded in the release notes after installation.

## Mass units — version 0.1.5

All 409 JVM tests passed, including eight new checks for exact kg/lb conversion,
automatic regional defaults, explicit mass/length independence, presentation
preference reload, all available nonstellar mass readouts, unchanged solar masses,
invalid/unknown values and all four mass/volume combinations for density.
The twenty interface languages have complete translatable-string parity; the
new island follows distances/speeds and includes an icon, description and kg/lb examples.
Debug/release builds, instrumentation-source compilation and release lint passed.
The instrumentation suite was not executed. Focused signed-APK device checks are
recorded in the release notes after installation.

## Units and viewer controls — version 0.1.4

Distances, astronomical distances and speed share one persisted selection:
System, meters/kilometers or feet/miles. Regression checks cover migration of
older choices, precedence of the unified setting, device-region defaults,
temperature/coordinate independence and nearby/astronomical/physical-size output.
Older explicit distance choices take precedence, followed by nearby length then
speed; no other preferences or celestial selections are cleared.
Twenty-language string parity passed, including the renamed Units page.

Viewer tabs use an opaque light fill with a dark symbol when selected, versus
the existing dark fill and white symbol when inactive. Tests require at least
4.5:1 contrast for the glyph and between active/inactive fills, including presses.
Sky action buttons and the existing 48 dp touch targets remain covered by their
regression checks. Signed APK/device checks are recorded in release notes.

## Repository presentation — version 0.1.3

The repository island contains the project URL followed by **Open repository**
and **Check for updates**. The package identifier, explanatory sentence and
persistent check-result line were removed. Update results still use the localized
temporary notice or update dialog. The unused explanatory resource was removed
consistently from all twenty languages; existing parity checks passed.
Signed APK installation and device UI results are recorded in this version's
GitHub release validation notes.

## GitHub updater checks — version 0.1.2

The update feature adds 14 JVM checks covering signature tampering, different
signing keys, downgrade prevention, Android compatibility, restricted HTTPS URLs,
truncated/oversized/checksum-invalid transfers and interrupted-download cleanup.
The twenty-language resource-parity/placeholder checks also passed.

Repository buttons and the real manual update check were verified in landscape
on the Android 15 tablet emulator. The signed 0.1.2 APK was installed as an update
on the Android 12 Samsung phone, retaining data; the phone was locked during the
new Repository UI attempt, so that attempt is not reported as a passed UI check.
The earlier phone checks above describe the previous layout baseline.

The end-to-end download/Android-installation check is performed after the first
public signed index is available and reported in the release validation notes.
The local version-code-1 debug updater fixture used for this check is not a
distribution asset. No connected instrumentation suite was executed.

The following records describe the original extraction baseline. They are
historical and do not supersede the current development checks.

## Historical extraction checks — 2026-10-04

### Extraction results

| Check | Space Compass | UVIR after removal |
| --- | --- | --- |
| Debug APK build | Passed, version 0.1.2 | Passed, version 1.3.0 |
| JVM unit tests | 331 passed, 0 failures | 564 passed, 0 failures |
| Android instrumentation sources | Compiled | Compiled |
| Android lint | 0 errors, 52 warnings | Not rerun in this extraction |
| Real-device/emulator UI execution | Not performed | Not performed |

The final build tasks were `:app:assembleDebug`, `:app:testDebugUnitTest` and
`:app:compileDebugAndroidTestKotlin`, plus `:app:lintDebug` for Space Compass.
Builds used the installed Android Studio JDK/SDK and the existing offline
Gradle cache. A workstation-only initialization script resolved cached plugin
artifacts; it is not required by or distributed with this repository.

Regression checks cover celestial mathematics, satellite propagation, rendering
geometry, input contracts, all 20 resource catalogs and extraction boundaries.
The UVIR contract verifies that the logo is again an ordinary Info action and
that no celestial feature code, maps, resource catalogs or dedicated license
assets remain. Space Compass checks its independent launcher, package,
permissions, textures and license notices.

Two lint errors in inherited code were corrected without changing equations or
layout: explicit control-flow braces in TLE identifier normalization and removal
of an unused constraint-aware container in the trajectory layer.

### Historical lint follow-up

The 52 warnings comprise screen-size API guidance (7), explicit backup-rule
guidance (1), a Compose modifier convention (1), Java indentation suggestions
(3), typography suggestions (20), unused resources (14), KTX suggestions (5)
and a version-catalog suggestion (1). No insecure TLS workaround was introduced.
These warnings are follow-up cleanup, not a claim that release preparation is
complete. Application backup is currently disabled in the manifest.

### Extraction delivery boundaries

- A new local Git repository is initialized independently from UVIR.
- No GitHub remote, release, store listing or production signing was created.
- No installation, uninstall, archive deletion, pairing change or phone test was
  performed. The existing UVIR working-tree changes were preserved.
- The pre-extraction UVIR files are retained in a separate recovery snapshot.
- APKs are development/debug artifacts, not production releases.

Next device checks should follow [DEVICE_TESTING.md](DEVICE_TESTING.md), with
explicit authorization and without clearing either application's data.


## Local catalog manual refresh / ISS recovery — October 6, 2026

Debug and release assembly passed, with **541 JVM tests** (zero failures, errors or
skips). Android instrumentation sources compiled, including updated ten-minute
catalog cadence, manual within-interval refresh/scroll preservation and active-batch
disabled-action fixtures. Connected instrumentation was **not executed** for this
change. The resource audit found **394 matching translatable keys in all 20 locales**,
with no duplicate/missing/extra keys, argument mismatch or encoding errors.

Provider investigation reproduced repeated real-phone ISS CelesTrak connection
timeouts before installation; this is a provider-route failure with working general
Internet, not proof of a globally unavailable service. Bounded public Satcat GETs
returned fresh ISS and identified STARLINK-40083 TLEs; sequential public JPL Sedna
position/motion GETs returned valid 73-hour-sample responses covering October 5–8.
Captured public responses underpin the new offline regressions for exact identity,
checksums, SGP4 propagation, strict center/frame/time headers and coverage limits.

The shared validated satellite fallback restores ISS, while a stable conflated
foreground queue prevents selection/retry events cancelling catalog warm-up.
Manual refresh reloads all JPL components once; valid satellite downloads remain
subject to their two-hour cache/provider interval. Catalog recalculation is ten
minutes or immediate on the button, with no changes to selected-object live updates.

The release APK was signed with the previously authorized certificate and installed
with replacement only on the authorized Samsung Android 12 phone. Exact installed
SHA-256: `35e3a00e396c6930360c461515755c5ee0e2dfc3ba941b9c233ef0c5cb8bb1c0`. Data/preferences were retained; no uninstall/reset/publication.
Physical-phone checks found the refresh glyph after Filters, seven unselected online
objects (ISS, Starlink, Sedna, Halley, 67P and both Voyager probes) with numeric
distance rows, a disabled progress action during manual loading, and the same row
positions/checkbox states after completion. The new app process logged no provider
failures or crashes during these checks. The app was returned to its original live
Sun/Moon selection; no simulation/location/clock/rotation settings were changed.
A ten-minute real-time phone wait was not performed; the cadence UI regression was
compiled only. No new emulator/instrumentation run, Gallery output or sharing.


## Simulation autosave and investigated Set insets — October 6, 2026

Debug and release assembly passed; **551 JVM tests** passed with zero failures,
errors or skipped cases. Ten new JVM tests cover complete/incomplete input, live/
independent modes, date/zone/DST boundaries, ft-to-m conversion and actual observer
state persistence using an interface-only SharedPreferences spy. Reapplying an
identical value does not rewrite preferences or request another saved notice.
The complete Android instrumentation source set compiled, including three new
disposable editor scenarios for checkbox saving, typed-input validation/debounce
and flush-on-exit. Connected instrumentation was **not run**. The final XML audit
found **396 matching translatable keys in all 20 locales**, with no structural,
format-argument or encoding errors.

Release SHA-256 `d23086b953432e39aff436ad270fc5105db798f6c851e2db9bd06de858c1c992` was signed with the already authorized certificate and
installed with replacement on the authorized Samsung Android 12 phone. Exact
installed bytes were verified; app data were retained. The page-wide Set button
is absent. A physical manual check toggled only the existing date switch, confirmed
that the editor stayed open, visually captured the custom Settings saved Toast,
returned/reopened the editor and found the switch/date/time preserved. Mandatory
try/finally cleanup returned both switches to their verified initial OFF state
and the app to its original live Sun/Moon view. Coordinates, altitude, IANA zone,
date/time fields, Android GPS/clock/rotation and photo disclosure were not edited.
No process restart, emulator check, gallery save, external share or GitHub upload
was performed. Typed-field/fast-back scenarios were compiled only; real disk reboot
persistence is not claimed as a physical test.

The former Set inset was investigated in the previous source and on-screen layout:
8 dp page margin plus another 8 dp around Set gave 16 dp total on each side; card
outer edges use the page's 8 dp. Card internal padding is separately 14 dp. No
shared island/page padding or unrelated action alignment was changed for this
investigation. Modal date/time dialogs retain themed light/dark colors; bottom
messages retain the existing always-dark/white UVIR style and native Toast duration
flags. A new Toast replaces the preceding one rather than queuing rapid edit notices.


## Compact Simulation header checkboxes — October 6, 2026

Debug and release assembly passed; **551 JVM tests** passed with zero failures,
errors or skipped cases. Android instrumentation sources compiled; connected
instrumentation was **not executed** for this layout adjustment. The XML resource
audit found **396 matching translatable keys across all 20 locales**, with no
structural, format-argument or encoding errors. No translated resources changed.

The Position and Date and time headers now use decorative icon, compact checkbox
and title in one row, following the read-only UVIR reference. The full-width
48 dp minimum header target exposes one accessible checkbox with its translated
action label and checked state. Independent simulation callbacks and autosave
remain unchanged. Collapsed sections have no duplicate label row or empty divider.

Release SHA-256 `bbabc37c5c79eb3dcff866fd05ae6caeac3eb874a002ce1f246896e252d3f7e7` was signed with the already authorized
certificate and installed with replacement on the authorized Samsung Android 12
phone. Exact installed bytes were verified; app data were retained. A physical
read-only check and screenshot confirmed the icon/checkbox/title order, vertically
centered compact headings and a single labeled, clickable, checkable accessibility
node per section. Both switches were already OFF and remained OFF; no preference,
GPS, clock, rotation or theme was changed. The user's open Simulation page was
preserved. No connected UI test, gallery save, share or GitHub upload was performed.


## Panorama point-label toggle and vertical actions — October 6, 2026

Debug and release assembly passed. **551 JVM tests** passed with zero failures,
errors or skipped cases. Android instrumentation sources compiled, including the
new synthetic six-option label/disclosure export-cache regression, which also
checks pixel differences, immutable source bytes, capture credits/time and no
inherited GPS EXIF. Connected instrumentation was **not executed** for this change.
The XML audit found **397 matching translatable keys in all 20 locales**, with no
structural, format-argument or encoding errors. `git diff --check` passed.

The preview now has Point labels, Position, Save and Share vertically aligned at
the trailing edge. Point labels is a persistent, default-ON, accessible on/off
control with active-color ink/outline and a crossed-out OFF glyph. OFF omits only
orbital time/elevation pills and their leader lines in both preview and exported
JPEGs. Cached files are keyed by label state and disclosure mode together; export
actions remain disabled during generation or failure. Back's chevron uses the
page controls' optical correction with RTL mirroring. Touch targets remain 48 dp.

Release SHA-256 `608dde1b85655c2e3418c05b2621321ab73d730bf3e5ad507d9a586a30538897` was signed with the previously authorized
certificate and installed with replacement on the authorized Samsung Android 12
phone. Exact installed bytes were verified; app data were retained. Physical
manual checks verified all four controls' vertical order/alignment, no overlap
with Zoom, a single checkable labeled toggle and restored Save/Share readiness
after changing it. Screenshots were visually reviewed with labels ON and OFF:
point pills disappear while geometry, dots, names, frozen caption and the selected
regional disclosure remain. The initial ON preference was restored in mandatory
cleanup and Back returned to the main view. No location/disclosure/observer,
clock, theme or system rotation preference was changed; no Gallery output was
created and Save/Share were not pressed. No emulator/instrumentation run or GitHub
upload was performed. Physical process/device restart persistence and actual
Save/Share output are not claimed as executed checks for this change.

Workspace evidence: `panorama-label-toggle-phone-check.json` and the paired
`privacy-panorama-toggle-initial.png` / `privacy-panorama-toggle-opposite.png`.


## Neutral panorama label-tag icon — October 6, 2026

The point-label bubble is replaced by an outlined label tag with a hole. ON and
OFF use identical neutral foreground, circle background and border colors; only
the diagonal slash distinguishes hidden labels. Checkable accessibility state,
48 dp target, persistence, rendering and disclosure/export behavior are retained.
No resources, shared settings padding or simulation layout changed.

Debug/release assembly passed; **551 JVM tests** passed with no failures, errors
or skips. Android instrumentation sources compiled and were **not executed**.
`git diff --check` passed. Release SHA-256
`84d0ce50103c9bb909eda8a6bf914ce0d70dd514e7fa1e5e6ffaab782defcced` was signed with the already authorized certificate and
installed with replacement on the authorized Samsung Android 12 phone; exact
installed bytes and preserved app data were verified. Physical manual checks
and reviewed ON/OFF screenshots confirmed the tag/slash, identical neutral colors,
vertical action alignment, label hiding and Save/Share readiness. Mandatory
cleanup restored the initial ON preference and returned to the main page. No
disclosure, observer or system settings were changed; no Gallery file, Share,
connected instrumentation or GitHub publication was performed.

Workspace evidence: `panorama-label-tag-phone-check.json` and
`privacy-panorama-label-tag-initial.png` / `privacy-panorama-label-tag-opposite.png`.


## Simulation headings restored to natural height — October 6, 2026

Removed only the Simulation toggle header's explicit 48 dp layout minimum. The
visible row now follows the existing title and compact 24 dp checkbox; shared
14 dp island padding, icon/gaps, accessible action/state and autosave are retained.
Compose extends the interactive target into the existing card padding instead of
increasing the visible heading. Ordinary islands and panorama controls did not
change. No new tests were added for this small layout correction.

Debug/release assembly passed; **551 JVM tests** passed with zero failures, errors
or skips. Android instrumentation sources compiled; connected instrumentation
was **not executed**. `git diff --check` passed. The release was signed with the
already authorized certificate and installed with replacement on the authorized
Samsung Android 12 phone, preserving data and verifying exact installed bytes.
SHA-256: `74578013fe5f9fa4fcb4638cfcf99e6ab1c841dc90ff067c711babeee5b016cc`.

A physical read-only comparison and reviewed screenshot found both collapsed
islands reduced from **256 px to 183 px** at the phone's existing scale. Both
checkable/clickable accessible nodes retained their **162 px (48 dp)** height;
the targets remain within the card padding and do not overlap neighboring islands.
Both initial OFF states remained OFF; no checkbox, preference, clock, GPS or system
rotation was changed. The initial main page was restored after viewing Simulation.
Expanded-field tapping, connected UI tests and process restart were not performed
for this correction. No image was saved/shared and no GitHub upload occurred.

Workspace evidence: `compact-simulation-header-phone-check.json` and
`privacy-compact-simulation-header-page.png`.


## Panorama orbital events and light/dark pills — October 6, 2026

Export now draws rise/set triangles, culmination diamonds and minimum bars in
larger object-colored event dots, using the live orbit's event vocabulary.
Hourly dots retain their prior size. Event glyphs and time/elevation pills are
drawn after all orbit strokes; special-marker bounds reserve pill space and
seam-crossing glyphs repeat at 0/360 degrees. Event symbols remain with labels OFF.
No astronomy equations, event detection, selected bodies or preferences changed.

The snapshot freezes the effective app theme. Light pills have a near-white
background and dark text; dark pills have a dark background and white text.
Colored borders/leader lines remain. Preview, Save/Share and label/disclosure
variants all use that same immutable palette. No resources changed.

Debug/release assembly passed; **551 JVM tests** passed with zero failures, errors
or skipped cases. Existing Android instrumentation sources compiled; connected
instrumentation was **not executed** for this rendering follow-up. No new tests
were added for this reversible visual adjustment. `git diff --check` passed.
Release SHA-256 `61bea14ad0a95a0a9eb0b1311c9b5312de78c8fb0369e3a299f111fae0a3fbe3` was signed with the already authorized
certificate and installed with replacement on the authorized Samsung Android 12
phone; exact installed bytes and preserved app data were verified.

Physical manual checks generated app-private live Sun/Moon previews under explicit
light and dark themes. Reviewed screenshots confirm all four event glyph types,
light-background/dark-text and dark-background/light-text pills, and retained
symbols/geometry when labels are OFF. Night styling was checked by temporarily
selecting the app's dark theme during daytime, without changing Android's clock,
GPS or rotation settings. Mandatory cleanup restored the original Default theme,
original labels ON and the main page. Observer/disclosure preferences were not
changed, no Gallery file was created and Save/Share were not pressed. Actual
external export and connected instrumentation are not claimed for this update.
No GitHub publication was performed.

Workspace evidence: `panorama-events-theme-phone-check.json` and
`privacy-panorama-events-light-preview.png`, `privacy-panorama-events-dark-preview.png`
and `privacy-panorama-events-dark-labels-off.png`.


## Always-dark translucent panorama labels — October 6, 2026

The requested follow-up replaces the light/dark pill palette with one dark style:
ARGB `0xD9101418` (approximately 85% opacity / 15% transparency), opaque white text,
and the existing object-colored border/leader lines. The obsolete snapshot theme
flag and capture-time theme read are removed. Event symbols, point-label toggle,
layout, coordinates/disclosure and export behavior are unchanged. The same painter
serves preview, Save/Share and all cached disclosure/label variants.

Debug/release assembly passed; **551 JVM tests** passed with zero failures, errors
or skips. Android instrumentation sources compiled and were **not executed**.
No new tests were added for this small visual adjustment. `git diff --check` passed.
Release SHA-256 `a70b13787001623758a3d0d1a5466f4ee1a302173491fde5c1056a88238c3986` was signed with the already authorized
certificate and installed with replacement on the authorized Samsung Android 12
phone, preserving data and verifying exact installed bytes.

A physical live Sun/Moon preview in the user's unchanged light/default theme was
visually reviewed: dark translucent pills, white readable text and retained event
symbols/colored borders. Save/Share were ready but not pressed. Labels were already
ON and remained ON; no theme, observer/disclosure or system preference changed.
Back restored the main page; Gallery contents were unchanged. Dark-theme execution
was not repeated for this unconditional palette; external export, connected UI
tests and GitHub publication were not performed.

Evidence: `panorama-translucent-labels-phone-check.json` and
`privacy-translucent-labels-preview.png`.


## Panorama zoom column below Back — October 6, 2026

Zoom in and Zoom out now sit vertically below Back on the leading side, with the
same 8 dp gap and 48 dp circular targets. The right-hand Point labels, Position,
Save and Share column remains. Relative leading alignment preserves RTL mirroring;
the existing landscape viewport also carries both columns on portrait tablets.
Zoom range, pinch/pan, double-tap and exported-image rendering are unchanged.

Debug/release assembly passed; **551 JVM tests** passed with zero failures, errors
or skips. Android instrumentation sources compiled; connected instrumentation
was **not executed**. No new tests were added for this small layout adjustment.
`git diff --check` passed. Release SHA-256 `c5b0bb0870db3990ebb7725b7a0fa46e8914c5834616abf7cc5f407aebe38c40` was signed with
the already authorized certificate and installed with replacement on the Samsung
Android 12 phone, preserving data and verifying exact installed bytes.

Physical manual checks and a reviewed screenshot confirmed Back, + and - share
horizontal bounds and are vertically ordered without overlap. The four trailing
controls remain on the opposite side. Zoom in enabled Zoom out; Zoom out returned
to the fitted image and its disabled minimum state. Back returned to the main page.
No preferences or system settings changed, no Gallery file was created and
Save/Share were not pressed. Tablet/RTL execution was not repeated for this move;
no connected instrumentation or GitHub publication was performed.

Evidence: `panorama-zoom-column-phone-check.json` and `privacy-zoom-column-preview.png`.


## Landscape island columns restored — October 6, 2026

Appearance, Units, Simulation and Info/news now use one keyed island grid: one
column in portrait and at most two columns in landscape. The usable width after
page insets must accommodate two 280 dp cards and the unchanged 12 dp gap; the
minimum card width grows above font scale 1.3 for readable accessibility layouts.
Narrow windows retain one column. Language keeps its single full-width island.
Shared 8 dp page sides, 14 dp card padding, secondary toolbar and existing card
order are unchanged. Info pairs introduction/news and repository/credits.
Simulation pairs Position/Date and time and spans its external notes across the
grid; draft, lookup and autosave state remain owned above the keyed items.
No resources, astronomy, data refresh, saved preference keys or other split
detail/main layouts changed.

Debug/release assembly passed; **551 JVM tests** passed with zero failures,
errors or skips. Existing Android instrumentation sources compiled; connected
instrumentation was **not executed**. No new tests were added for this reversible
layout change. `git diff --check` passed. Release SHA-256 `c518c868aa8c2142bc5f4044e8286fffa9f58952f13799ccfec0f69c11b274d0` was signed
with the already authorized certificate and installed with replacement on the
authorized Samsung Android 12 phone; exact installed bytes and data preservation
were verified.

Read-only physical navigation and reviewed screenshots confirmed the four
two-column landscape pages, portrait single-column Appearance/Units/Simulation,
and Units/Info scrolling to later content. Portrait Info's layout was recorded in
UIA; a transient system notification obscured the first card in its screenshot.
Language's unchanged full-width single-column route was checked in source;
physical navigation stopped after the Info/main round trip when the phone's
foreground changed before Language, without operating outside Space Compass.
The two collapsed Simulation
checkboxes remained OFF while that page rotated portrait → landscape → portrait.
Temporary Android rotation policy was restored exactly in mandatory cleanup;
no app preference, location, date, Gallery file or share was changed. Samsung's
accessibility root clips some trailing landscape bounds to a stale portrait
width; full-resolution screenshots confirm equally wide, unclipped visual cards.
Expanded Simulation editing, tablet/RTL/enlarged-font execution and connected
UI tests were not performed for this follow-up. No GitHub upload occurred.

Evidence: `landscape-island-columns-validation.json`,
`landscape-island-columns-phone-check.json` and
`privacy-island-columns-*-portrait/landscape*.png` in the local workspace.


## Equal heights for landscape island pairs — October 6, 2026

The four shared island-grid pages now group card pairs by row only when two
landscape columns are active. Each card Surface has the row's maximum natural
content height as its minimum; content remains top-aligned, with blank space
below shorter contents. The content measurement clears that inherited minimum
before recording its padded natural height, avoiding a stretched-height feedback
loop and permitting rows to shrink after content closes. Measurements reset for
column/width/density/font changes and disposed card members are removed. Stable
grid keys and page-owned Simulation state/callbacks remain. Portrait, narrow
single-column windows, unpaired last cards and full-width notes retain natural
height. No strings, units, calculations or preference keys changed.

Debug/release assembly passed; **551 JVM tests** passed with zero failures,
errors or skips. Android instrumentation sources compiled; connected
instrumentation was **not executed**. No new tests were added for this reversible
layout correction. `git diff --check` passed. Release SHA-256 `2c269347c515a49df00aeb4ddd203716b46c9d329ea9c7d45cf2120819c8e40b` was signed
with the authorized certificate and installed with replacement on the Samsung
Android 12 phone, retaining data and verifying exact installed bytes. Installation
did not launch the app over the user's other screen; the user then opened it for QA.

Read-only physical Appearance navigation and reviewed screenshots confirm natural
portrait heights of **881 / 719 px**. In landscape both card
bottoms are at **1026 px** after scrolling to expose their lower rounded
borders. No preference was selected. Mandatory cleanup restored Android rotation
policy exactly and navigation returned to the main page. Other card pairs,
expanded Simulation editing, RTL/enlarged-font execution and exports were not
repeated. The temporary read-only tablet emulator exited before completing boot,
including a software-renderer retry; no tablet UI check or AVD data change is
claimed. No GitHub publication occurred.

Evidence: `equal-island-heights-validation.json`, `equal-islands-phone-check.json`
and `privacy-island-columns-equal-height-appearance-*.png` in the local workspace.


## Natural island heights restored — October 6, 2026

The requested follow-up removes row-height matching entirely. The five affected
Kotlin files were restored to their pre-matching contents after verifying each
current file was exactly the previously applied height-only transform. The row
measurement maps, composition locals, card wrappers and minimum-height modifiers
are removed. The shared keyed two-column landscape grid remains; every island
keeps its own natural content height in every orientation. Current presentation
documentation now describes this behavior. No resource, preference or calculation
changed.

Debug/release assembly passed; **551 JVM tests** passed with zero failures,
errors or skips. Android instrumentation sources compiled; connected
instrumentation was **not executed**. No new tests were added for this reversible
visual rollback. `git diff --check` passed. The authorized-key release SHA-256
`c518c868aa8c2142bc5f4044e8286fffa9f58952f13799ccfec0f69c11b274d0` is byte-identical to the earlier validated natural-height landscape build.
Replacement installation on the Samsung Android 12 phone preserved app data and
verified exact installed bytes.

Read-only physical Appearance navigation and reviewed screenshots confirmed
different natural heights in both portrait and landscape. After landscape
scrolling, the card bottoms are at **1026 / 864 px**, rather than
stretched to match. Navigation returned to the main page and mandatory cleanup
restored Android rotation policy exactly. No app preference was selected; no image
was exported or shared. Other page pairs, expanded Simulation, tablet/RTL and
enlarged-font execution were not repeated. No GitHub upload occurred.

Evidence: `natural-island-heights-validation.json`,
`natural-islands-phone-check.json` and
`privacy-island-columns-natural-height-appearance-*.png` in the local workspace.


## Settings confirmations and uniform sky-notice padding — October 6, 2026

Presentation choices previously saved values but did not announce that save.
The retained preferences provider now compares actual presentation settings and
shows the shared short dark bottom message after deriving the newly selected app
resources. It stays silent on initial loading, automatic configuration changes,
unrelated preference writes and unchanged selections. Radio rows no longer write
an already selected choice. Simulation retains its separate existing autosave
confirmation, without duplicate presentation announcements. The generic
settings_saved key replaces observer_saved in all 20 catalogues; translation
texts and preference keys are unchanged.

The Retry island already shared the 12 dp horizontal / 6 dp vertical outer
padding, but Material TextButton added content padding and forced 48 dp of visible
height. It is replaced by a compact primary-colored text action with the same
10 sp / 13 sp styling as the other notices, no extra content padding and a 48 dp
minimum width. Foundation expands touch bounds without consuming layout height;
the existing labelled accessibility action and retry callback are preserved.
The existing notice fixture now checks expanded touchBoundsInRoot rather than
visible layout bounds for its 48 dp target assertion. Width ordering and gaps
are unchanged.

Debug/release assembly passed; **551 JVM tests** passed with zero failures,
errors or skips. Android instrumentation sources compiled; connected
instrumentation was **not executed**. `git diff --check` passed. The structural
resource audit found **397 matching translatable keys in 20 catalogues**, with no
missing/extra keys, argument/quantity mismatches, blank values or encoding errors.
Release SHA-256 `8bb5fd7fce3759a7650e22467059d95e303f2f6998b45c052a3dde121b05c26f` was signed with the authorized key and installed with
replacement on the Samsung Android 12 phone, preserving data and verifying exact
installed bytes. No GitHub publication occurred.

Physical checks temporarily changed Theme, Language and the first Units choice.
Reviewed immediate screenshots show the confirmation in dark and light views,
English immediately after choosing English, Italian after restoring Default,
and a save message after selecting feet/miles. Re-clicking the selected Theme
produced no new message. All three original semantic Default choices were
asserted restored and the main page was restored. Raw absent/default storage
representation was not compared. No simulation plan or system setting changed.
The live main page had no Retry notice, so its unavailable-data branch and touch
activation were not forced or executed physically. Other unit groups, simulation,
tablet/RTL and connected UI tests were not re-executed for this follow-up.

Evidence: `settings-confirmation-validation.json`,
`settings-confirmation-phone-check.json`, `final-localization-audit.json` and
`settings-confirmation-*-toast.png` / `settings-confirmation-appearance-unchanged-no-toast.png`
in the local workspace.


## Live rear-camera background — 2026-10-06

- Debug/release compilation, all 558 JVM tests (including seven camera geometry
  tests) and Android instrumentation source compilation passed. Connected
  instrumentation was not run. All 20 language catalogues have 402 matching
  translatable keys, with no missing/duplicate/format errors.
- Same-key APK `421d91842ef89a083418cde0ba07ea3f9d6b6cb2e0a1ba450e2b9a6bc1453a04` was replacement-installed and its installed bytes verified;
  preferences and app data were preserved. The final install did not launch over
  the user's current foreground screen. No GitHub publication occurred.
- Manual Samsung Android 12 checks on camera APK `e5ef949641787f847d83ab5f4f7e72fbd906d1bbfca18fe92c564d8bef776aeb`
  confirmed initially off/no camera client, the on-demand native permission dialog,
  live rear camera 0 with reticle/orbits/gravity horizon, and Capture immediately
  before Simulation. Camera service state confirmed release in Appearance,
  reopening after return, and release when the app left the foreground. No
  presentation/simulation preferences or system rotation settings were modified.
- Final APK additionally refines anisotropic horizon/off-screen geometry and the
  translated camera accessibility description. Those changes passed all tests;
  its final foreground UI check subsequently confirmed initially off, active rear
  stream and translated accessibility description, landscape layout, Capture
  opening the panorama with camera release, return reopening the camera, and
  toggle-off restoring the virtual sky/releasing the camera. The camera was left
  off. Landscape framing/stream lifecycle was checked; an upright landscape image
  was not claimed because the lens was covered with the phone lying face up.
  Permission denial and real-sky astrometric accuracy were not claimed verified.
  Geometry tests cover all four sensor orientations and all four display rotations.
  Absolute pointing remains limited by the phone's compass/attitude and optical
  metadata accuracy, as documented in CAMERA_VIEW.md.


## Full-scene shutter and natural preview orientation — 2026-10-06

- Debug/release builds, all 566 JVM tests (eight additional full-photo geometry,
  exposure-attitude and preview framing tests) and Android test-source compilation
  passed. Connected instrumentation was not executed. The 20 resource catalogues
  have 402 matching translatable keys with no audit errors; no strings were added.
- Same-key APK `fbfc8a3a5f272ef797d05017b14a11e1a6cb4302cbb2108f8db1dc981133102e` was replacement-installed with matching installed bytes,
  preserving app data. No GitHub publication occurred.
- Samsung Android 12 manual checks verified the 48 dp shutter immediately after
  the object selector and its removal from the menu; a portrait 360-degree preview
  stayed portrait and moved with a finger drag. A real camera photograph of the
  room displayed its projected orbit and time/elevation labels. Turning labels off
  and back on regenerated the frozen image and restored the original preference;
  an unaffected photograph region differed by 0.0000 average RGB
  levels, confirming that the scene was not recaptured. Camera service state
  confirmed release in the preview and reopening on return.
- Landscape still capture and preview were verified at 2280×1080 screen pixels.
  Temporary rotation policy was restored to the original auto=1/user=0, including
  a delayed retry for Samsung's asynchronous rotation-setting observer. The image
  uses the complete JPEG field rather than the split main layout; JVM tests also
  verify a body hidden by the data column remains visible in the full photograph.
- Save/share and gallery writes were not invoked. Other presentation/simulation
  preferences were not edited. The user navigated to a secondary page during QA;
  their chosen view was left untouched. Temporary room screenshots were removed
  from the workspace. No real-sky astrometric accuracy claim is made; magnetic
  and attitude accuracy still limit absolute pointing.


### Celestial export reference guides — 2026-10-06

- Debug and release builds succeeded; 574 JVM tests passed with no failures,
  errors, or skipped tests. The eight new geometry tests cover constant
  declination, both hemispheres and geographic poles, equator crossings,
  obliquity of simulated date, panorama seam/zenith/nadir, full-lens photograph
  clipping through four display rotations, and invalid observers/canvas sizes.
- Android instrumentation sources compiled; connected instrumentation was not
  executed for this change. The localization audit found 402 matching
  translatable keys in all 20 catalogues, with no errors. Reference captions
  are international English export annotations, not untranslated app controls.
- Signed with the existing authorized certificate and installed with replacement
  on Samsung SM-G970U1 / Android 12; exact installed APK verified and app data retained.
- Manual visual checks: portrait panorama, full landscape panorama displaying
  all five dashed guides and both pole crosses, and a camera photo with equator
  and Capricorn guides inside its actual lens field. The same photo re-rendered
  with hourly captions hidden retained the reference guides and names.
- Hour-label preference and temporary rotation settings restored; camera turned
  back off. No GPS/simulation preferences changed; no gallery save/share invoked.
  This checks rendering/projection; real-sky astrometric alignment was not measured.
- APK SHA-256: `e59c31737af9f129db6e114df806848b86b50ecdeaade26da6872d7736e24e1c`. No GitHub publication.


### Simulation metadata cards and action-button press feedback — 2026-10-06

- Debug/release builds succeeded; 574 existing JVM tests passed with zero failures,
  errors or skips. Android test sources compiled; connected instrumentation was
  not executed. No new tests were added for this reversible presentation change.
- All 20 translation catalogues retain 402 matching translatable keys, with no
  structural/formatting errors. Existing translated altitude/time-zone/button
  labels are reused; no strings were added.
- Manual Samsung SM-G970U1 / Android 12 checks verified separate Altitude and
  Time zone islands, their icons, padding, field values and the existing data/time
  toggle. The native map confirmation was disabled until choosing a draft point.
  While held, its surface filled with the app's primary color and its label/icon
  became contrasting. A drag-out cancelled the press, restored the outline and
  left the picker open, without accepting the draft point.
- Original simulated coordinates were compared before/after; no saved fields,
  theme, rotation, GPS source or simulated time were changed. The app returned
  to the main view. Repository/update network/navigation commands were not run.
  Repository and calendar/dial confirmations use the shared Compose component;
  their press appearance was compiled but not separately exercised on device.
- Local APK signed with the existing authorized key and installed using
  replacement; exact installed bytes verified and app data retained.
  SHA-256: `8b43a546629b256a4db629074a329442760325a443a04261db50372ebc7fd7d2`. No GitHub publication.


### Observer metadata action coordinate sources — 2026-10-06

Debug/release builds succeeded; 574 JVM tests passed without failures/errors/skips.
Android test sources compiled; connected instrumentation was not run. All 20
catalogues retain 402 matching keys with no structural/format errors. Source
review confirms detection uses current editor coordinates, while the renamed
current-location action reads devicePlace, populated from the last valid physical
fix before simulation overrides; that action changes coordinates and metadata
together. Existing lookup validation, terrain estimates and autosave are retained.

Manual Samsung SM-G970U1 / Android 12 inspection verified both translated labels
and natural wrapping without clipping. Detection actions were not invoked during
this visual check to preserve the saved simulated location. Returned to the main
view. Signed local update installed with replacement and exact installed bytes
verified; app data retained. SHA-256: `d80ce6ead58695c205fa6fb81e2bf3200ed708232304043ced894e3a4d41a9fd`. No GitHub publication.


### Main-view celestial guides — 2026-10-06

- Debug/release builds succeeded. 576 JVM tests passed with no failures, errors
  or skips; two new checks cover null camera calibration in virtual perspective
  and equivalence of cached live/export closed circles. Android instrumentation
  sources compiled; connected instrumentation was not executed.
- All 20 catalogues have 409 matching translatable keys with no structural/format
  errors. Seven new main-view guide names are translated. Native text layout
  uses left-aligned paint under centered StaticLayout to avoid double centering
  and shape bidirectional scripts correctly.
- Manual Samsung SM-G970U1 / Android 12 checks observed the virtual equator and
  Capricorn guides, and the final virtual South celestial pole cross with the
  Antarctic Circle. A camera-mode check observed equator/Capricorn guides with
  corrected labels before the final label-order refinement. The shared final
  layer draws captions after trajectories without a second geometry pass;
  final camera label order was not separately exercised because the user opened
  their panorama during QA. That view and user-selected settings were left alone.
- No GPS/simulation/theme changes were made by QA; no gallery save/share invoked.
  Physical real-sky astrometric accuracy was not measured. Live references require
  a valid observer and usable orientation and wait for camera calibration.
- Exact installed APK verified, same authorized signing certificate, replacement
  installation with app data retained. SHA-256: `553f0b7c8c5c0bd873b64884f6a8d45cd3b84cb15925bc2e5fb71713b07d2ea1`. No GitHub publication.


### Saturated simulation notice orange — 2026-10-06

The simulation notice now uses #AB3D00 in light mode and #FF8F1F in dark mode,
replacing the muted/pale orange while retaining its padding, typography and
background. Palette contrast checks exceed 4.5:1 for the evaluated light/dark
islands. Debug/release compilation succeeded; 576 existing JVM tests passed.
Android test sources compiled; connected instrumentation was not executed.
No strings or layout changes and no new tests for this reversible color change.
Manual Samsung SM-G970U1 / Android 12 screenshot inspection confirmed exact new
night-palette pixels in the banner. No theme/simulation preferences changed by QA.
Signed replacement installed with app data retained; exact bytes verified.
APK SHA-256: `c49cd07b1e096fa25cc13abd51bfd83f4d04ca3086ccb210c3f4d7d648928648`. No GitHub publication.


### Scenario name and menu icon — 2026-10-06

Renamed the observer feature, page title, active banner and export caption to
Scenario. Replaced the menu pin with a compact pin-and-clock glyph. All twenty
catalogues retain parity (409 translatable keys); other observer strings,
internal routes and persistence keys are unchanged. No new tests for this copy/icon
change. Debug/release builds succeeded; 576 existing JVM tests passed. Android
test sources compiled; connected instrumentation was not executed.
Manual Samsung SM-G970U1 / Android 12 inspection confirmed the menu icon/name,
page title and active banner. The existing selected scenario survived the signed
replacement installation; QA did not change its preferences. Export caption
changes were checked in both capture paths; no new capture was taken for this change.
Installed APK bytes match SHA-256 `ae2ca71cfae998339822d25f1697c13701d42bc50362ab3f00eb5ba3f4cc755d`. No GitHub publication.


### Celestial guide toggle — 2026-10-06

Added the default-enabled persistent guide toggle before the object selector,
using a slash for disabled state and the existing neutral round control styling.
The live layer and both frozen capture renderers use the same visibility. All
twenty catalogues retain parity (411 translatable keys). No new mirror tests for
this reversible visibility setting. Debug/release builds succeeded; 576 existing
JVM tests passed. Android test sources compiled; instrumentation was not executed.
Manual Samsung SM-G970U1 / Android 12 inspection confirmed default-on state,
48 dp checkable control, disabled slash and reference suppression while body paths
remained visible. Virtual panorama previews were inspected with guides on/off;
no gallery save/share was performed. A guarded cold app restart retained the off
preference; the initial enabled state was restored at the end. Camera capture
gating and copied save/share variants were source-checked; a new camera photograph
was not taken for this change. Signed replacement retained app data; exact APK
bytes verified. SHA-256: `205b6c3059468b8892f5a328a10f34cb0ec0fbac0427757bf8103ca36a182467`. No GitHub publication.


### Independent altitude/time-zone Scenario switches — 2026-10-06

Added compact checkbox headings for Altitude and Time zone. All four overrides
work independently and are persisted; separate automatic metadata avoids losing
manual values. Legacy five/seven-field plans preserve their effective values and
resolve unknown automatic metadata only when explicitly requested. Real GPS
coordinates/accuracy survive a height-only override; simulated height is not
labelled as GPS height. Zone edits preserve the selected instant, including DST
overlaps, while explicit date/time edits retain normal validation semantics.
Debug/release builds succeeded; 585 JVM tests passed (nine added regression tests
and all fifteen active switch combinations through real observer state persistence).
Android test sources compiled; connected instrumentation was not executed.
All twenty catalogues retain parity (413 translatable keys).
Samsung SM-G970U1 / Android 12 manual checks exercised both new switches,
field expansion/collapse, unchanged manual values and unchecked state on page
reopen on APK 873964ee0466704556873e8b060d5209492c775ada3a59db19723fe2322316ab.
After the final DST refinement, a fresh signed install and screenshot confirmed
both checkable headings and restored original height/zone flags/values. Position
and date flags were not changed by QA. Fixed-time manual zone typing was not
separately exercised on the phone; DST behavior is covered by JVM tests.
Final APK SHA-256: `e8ccfdcd2e28253be38e7648949be51397f1d276d8879039670c0f8598d0468e`. Data retained; exact installed bytes verified.
No gallery save/share and no GitHub publication.


### All capture annotations controlled by Labels — 2026-10-06

Extended the existing label switch to object and celestial-guide names in both
capture renderers. Reference curves and pole crosses remain when only labels are
hidden; hidden text reserves no collision space. Kept the stored key, snapshot/
variant workflow, geometry, axis/header controls and main-view labels unchanged.
Localized the short Labels caption in twenty catalogues (413 translatable keys).
Debug/release compilation succeeded; 585 existing JVM tests passed. Android test
sources compiled; connected instrumentation was not executed. No new mirror tests
for this reversible annotation visibility change.
Samsung SM-G970U1 / Android 12 virtual-preview screenshots confirmed time, object,
equator/tropic/polar-circle and pole names disappearing while curves and pole
crosses remained. The camera renderer and copied save/share variant path were
source-checked; a new camera photograph was not taken for this change. The user
continued navigating during QA; further actions stopped on their Scenario page.
An initial-on preference restoration was requested, but final preference state
was not read after that navigation. No gallery save/share or GitHub publication.
Signed replacement retained app data; exact installed bytes verified.
APK SHA-256: `5485176f2e70ed7b3d28207d846de38ed6fb686a46efc3b2bc4d13627c19764b`.


### Sparse degree annotations on celestial references — 2026-10-06

Added declination values to guide/pole names and sparse numeric repeats to the
shared live/panorama/camera renderer. Four new JVM tests verify observer-independent
parallel values, hemispheric signs and numeric format, minimum spacing around the
north wrap, clipped camera fragments, reserved name spacing and invalid/duplicate
samples. A first test expectation required two labels even for a compact polar
circle; corrected that expectation to permit one without reducing the minimum
spacing. Final debug/release builds succeeded; all 589 JVM tests passed. Android
test sources compiled; connected instrumentation was not executed. String parity
remains valid across twenty catalogues (413 translatable keys); existing translated
names are reused without new prose strings.

Samsung SM-G970U1 / Android 12 screenshots confirmed signed degree values in the
live view and virtual panorama, generous numeric repeat spacing, and values/names
disappearing with Labels while curves and pole crosses remained. Restored the
initial Labels-on preference and returned to the main view. Scenario position,
date and unit preferences were not edited. A new physical camera photograph was
not taken; its call to the same updated renderer and snapshot label flag were
source-checked. No gallery save/share or GitHub publication. Signed replacement
retained app data and exact installed bytes were verified.
APK SHA-256: `3bc53a001219bca609fc27ee02d1ff6674d60a11f460ed1189522bf977bcb9ad`.


### Per-island altitude/time-zone detection and zone list — 2026-10-06

Moved the independent set-position/current-GPS lookup actions into Altitude and
Time zone, changed the altitude icon to mountains, and replaced free zone text
with a searchable themed list and trailing scenario-date UTC offsets. Reused the
bounded HTTPS provider and observer autosave. Geographic IDs and existing manual
values/cache remain distinct. Selected fixed/legacy zone IDs remain available;
ICU deduplication removes only equivalent aliases in the visible list. Added five
strings translated in all twenty catalogues (418 translatable keys; parity valid).

Debug/release compilation succeeded; 598 JVM tests passed, including nine new
tests for seasonal northern/southern DST offsets, half/quarter hours, historical
seconds, retained aliases, ordering, field-specific metadata application, meters/
feet conversion and absolute-instant preservation across a DST overlap. Android
test sources compiled; connected instrumentation was not executed.

Samsung SM-G970U1 / Android 12 visual checks covered the mountain glyph, field-
specific actions and list layout. Exercised altitude lookup from the existing
set position and time-zone lookup from the physical GPS position; observed initial
coordinates, altitude, zone and scenario date/time retained. Distinct distant
simulated/current-position independence is additionally covered by the JVM model
tests. Search for Kathmandu showed one geographic entry and UTC+05:45 after ICU
alias deduplication. No different zone selected during phone QA; returned to the
main view. Tablet/landscape/RTL were not manually retested for this change. Signed
replacement retained app data; exact installed APK bytes verified. No publication.
APK SHA-256: `fe20a734b727932d00331f1ebe049390f14550f23ab78100855f742c1a147662`.

### Observer-facing model visibility and first static frame — 2026-10-06

Corrected two separately reproduced issues: a nearly black new-phase current face,
and a native SurfaceView that had no buffer on first static opening until Rotation
started drawing. Added current-face presentation fill without changing the physical
phase/observer geometry, moved native view ownership into AndroidView.factory, and
requested a native root traversal after the child receives a positive size. Temporary
surface diagnostic logs were removed from the final APK.

Debug/release compilation succeeded; all 598 JVM tests passed. Android test sources
compiled; connected instrumentation was not executed. No resources changed; parity
audit passed for twenty catalogues with 418 translatable keys each.

Samsung SM-G970U1 / Android 12 final-APK screenshots confirmed Moon Current face
on the first opening without touching Rotation, including the user's below-horizon
(about -32.9 degrees) and new-phase (about 0.1% illuminated) scenario. Confirmed the
native SurfaceView layer exists, Rotation and return to Current face work, the Sun
is visible on its first opening, and Moon remains visible when reopened. Restored
the originally inspected Moon and left its Current face open. Scenario position,
date, units and checked-object selection were not edited. Tablet/landscape/RTL were
not manually retested. No gallery save/share or GitHub publication. Matching-signed
replacement retained app data and exact installed APK bytes were verified.
APK SHA-256: `0a59caf8f657a7dd088b329ff660bd7a351d2b8561d7c9f2e16527b7585a71c0`.

### Catalog refresh for Scenario dates — 2026-10-06

Reproduced the existing future Scenario showing missing ISS/Starlink distances and
JPL rows despite restored data. Root cause: remote download/coverage used today's
clock while the catalog/scene used the overridden observation time. Wired that
override into JPL request windows and validation, preserved real download/retry
clocks and satellite validity, and distinguished known date-range gaps from missing
models with two labels translated in twenty catalogues (420 keys; parity valid).

Fetched ten public JPL responses serially for 2027-04-06T22:00Z; all returned HTTP
200 with valid position/geometric-motion tables. Seven new JVM tests cover request
dates for all five targets, clock separation, day-change/same-day backoff, satellite
HTTP stops/fresh caches, parsing real future responses, missing-model status, and
live satellite availability versus rejection of months-ahead positions. Debug and
release builds succeeded; all 605 JVM tests passed. Android test sources compiled;
connected instrumentation was not executed.

On Samsung SM-G970U1 / Android 12 pressed Refresh in Objects and verified the button
disabled during the serial batch, then reenabled. Unchecked Sedna, Halley, 67P and
both Voyager rows displayed numeric Mkm/AU distances without selecting them; the
title retained the full 31-object count. ISS/Starlink remained listed and explicitly
showed Date not covered for the existing future Scenario. Kept the user's selected
objects, inspected Moon, Scenario values and units; left the catalog showing all
five corrected JPL rows. Personal-phone live mode was not toggled; current satellite
calculations are covered by public-data JVM fixtures. Tablet/landscape/RTL were not
manually retested. Matching-signed install retained data and exact installed bytes
were verified. No gallery save/share or GitHub publication.
APK SHA-256: `60b0329136aac0baa63e2366caf98bbfaee206422458425614c5d9742677633c`.

### Empty-selection celestial selector — 2026-10-06

The main ringed-planet selector now draws a same-tint diagonal slash when no
objects are selected. Its existing nonempty-only count badge, neutral styling,
48 dp clickable target and accessible zero-count state remain. No strings changed
or new mirror tests added for this reversible icon change. Debug/release builds
succeeded; all 605 JVM tests passed. Android test sources compiled; connected
instrumentation was not executed.

Samsung SM-G970U1 / Android 12 visual QA temporarily unchecked the original Sun
and Moon, confirmed the crossed icon with no badge, reopened Objects through that
icon, then restored Moon followed by Sun. Confirmed normal icon/count 2 and the
original inspected Moon. Scenario and other preferences were not edited. Tablet,
landscape and RTL were not manually retested. Exact matching-signed replacement
retained app data; no GitHub publication.
APK SHA-256: `52320137f26224c0d456af4187669a64803c15f6d39157cac380cd4504c24386`.

### Main camera and capture control placement — 2026-10-06

Moved the opt-in camera toggle out of the title toolbar into the circular upper
sky actions, ordered Camera, Celestial references, Objects. The existing shutter
capture now occupies the bottom trailing corner of the sky in portrait and the
left sky column in landscape. Retained its readiness/busy state and both capture
modes. Reserved its footprint for captions and moved the portrait selected-point
panel above it. No strings changed or new mirror tests added for this control move.

Debug/release compilation succeeded; all 605 JVM tests passed. Android test
sources compiled; connected instrumentation was not executed. Samsung SM-G970U1 /
Android 12 screenshots confirmed order and placement in portrait and landscape,
camera preview start/stop, virtual panorama opening from the moved shutter, and a
selected-point panel without shutter overlap. Camera JPEG capture was not repeated.
Closed the temporary point selection and preview, restored camera off and portrait
presentation with the original free rotation policy. Scenario values, units,
checked objects and the inspected Moon were not edited. Tablet/RTL were not manually
retested. No gallery save/share or GitHub publication. Matching-signed replacement
retained app data and exact installed APK bytes were verified.
APK SHA-256: `888986e03eb33779592af5a6a253e4f517b9153b114789828968c8d33e3f386d`.

### Unified panorama export and presentation profiles — 2026-10-06

Replaced preview Save/Share controls with Uvir's Export arrow/tray glyph. The shared
theme-aware dialog offers current app language/formats and Scientific international,
then three primary icon actions: Gallery, Save on phone and Share. Both profiles
are built from frozen raw capture inputs; switching profiles translates object,
cardinal, weather, guide and pole labels and formats dates, clocks, elevations,
coordinates and lengths. Disclosure and label toggles remain independent; cache
and saved-gallery identities include all three options. International remains the
first-use default; the chosen profile is stored. Legacy gallery filenames are
reserved atomically to save both profiles without overwriting an existing image.

Debug/release compilation succeeded; all 614 JVM tests passed, including nine new
regressions for conversions, frozen locale/time, disclosure, cache identity and
legacy filenames. Android sources and one additional real-JPEG/profile fixture
compiled; connected instrumentation was not executed. Six new keys in all twenty
catalogues passed parity/argument checks (426 translatable keys each); the three
profile labels reuse Uvir translations. Uvir itself was read only.

Samsung SM-G970U1 / Android 12 final-APK QA confirmed the portrait dialog and its
three reachable actions after landscape scrolling. The current-settings JPEG uses
Italian labels, the device's date pattern and decimal commas; switching back shows
English labels, ISO date and decimal points at the same Scenario instant. Explicit
Gallery save produced one 4096 x 2192 JPEG with preserved capture EXIF and no GPS
EXIF. The document picker and share chooser opened directly and were cancelled;
no external sharing. Restored the original international profile, portrait/free
rotation and main view; Scenario, units, checked objects and active Moon were not
edited. Camera JPEG capture, tablet, RTL and light-theme visual checks were not
repeated for this change. Matching-signed install retained data and exact installed
APK bytes were verified. No GitHub publication.
APK SHA-256: `9042517c9b110a5e0e0aeb071dd73f4549d9d6342f8cf4400f7419c198126bb8`.

### Celestial-reference first-use default off — 2026-10-06

Corrected the celestial-guide preference fallback from true to a shared false
default, used by the live layer, viewport, capture action and frozen snapshot.
Explicit saved choices remain respected. Verified the existing cold-start camera
state is off and the fresh object selection contains only Sun and Moon; selected
objects and active-object persistence remain unchanged. No strings changed or new
mirror tests added for this reversible default change.

Debug/release compilation succeeded; all 614 JVM tests passed, including existing
initial-selection/persistence coverage. Android test sources compiled; connected
instrumentation was not executed. Samsung SM-G970U1 / Android 12 retained a prior
explicit guide-on preference after the matching-signed update. Disabled it through
the observed UI as requested, then visually confirmed crossed camera/guide icons,
count 2 and a virtual panorama without guide lines. Closed the preview and left the
main view with the original active Moon. No personal data clearing/fresh-install
simulation, camera JPEG capture, gallery save/share or GitHub publication. Scenario,
units and other preferences were not edited; tablet/landscape/RTL checks were not
repeated. App data retained and exact installed APK bytes verified.
APK SHA-256: `541e154df88bb35c3068a94d70adfea74022c0957e682d3dbcab16b525daaa9b`.

### Catalog value column follows ordering — 2026-10-06

Numeric catalog orders now update both the right-column header and each row's
value. Shared detail formatters preserve selected units, numeric formats, solar
mass thresholds and uncertainty/upper-limit notation. Missing keys display a dash
and stay last; no mean-temperature or spacecraft-size substitution. Explicit
Distance from Sun order uses the same heliocentric range as its sort key, while
default/alphabetical order retain the existing distance summaries. No strings or
selection/Scenario settings changed.

Debug/release compilation succeeded; all 619 JVM tests passed, including five new
conversion/reference regressions. Android test sources and the new column-update
fixture compiled; connected instrumentation was not executed.

Samsung SM-G970U1 / Android 12 final-APK checks confirmed Mass, Atmospheric pressure
and Night temperature descending headers, formatted values, right alignment and
unavailable-value dashes. Restored the visually recorded original Default order,
confirmed the distance header/rows, and left Objects open. Checked objects, units,
filters and Scenario were not edited. Tablet, landscape, RTL and light theme were
not manually retested. Matching-signed replacement retained data and exact installed
APK bytes were verified. No gallery save/share or GitHub publication.
APK SHA-256: `bff6de010da60765f693b7bb990d513e0001937368bedb1c9268eb39f5527e55`.

### Hidden LV-426 object — 2026-10-06

Added a three-second Objects-title hold with no visible affordance. LV-426 is
checked on reveal, stays saved while checked, and disappears on deselection.
Ordinary Select all cannot discover it. It joins normal filters, sorting,
navigation, paths and captures, with one original generated surface texture.
Fictional reference values, the disputed diameter and the illustrative Zeta²
Reticuli direction are disclosed in translated details. Unknown physical/orbit
parameters remain unavailable; no invented real moon ephemerides are downloaded.
Artwork prompt and source qualifications are in docs/LV426.md.

Debug/release compilation succeeded; all 627 JVM tests passed, including eight
new regressions covering defaults, reveal/removal, navigation, persistence,
filters/sorting, data limits and a future daily path without network. Android test
sources and the new held-title fixture compiled; connected instrumentation was
not executed. All twenty catalogues passed parity checks with 429 translatable
keys each. The three new translated descriptions and proper-name resource were
checked; original real-object image/scientific credits remain intact.

Final-APK manual Samsung SM-G970U1 / Android 12 results are recorded in the local
lv426-phone-check.json, with screenshots and scope limitations. A 2.4-second hold
did not reveal the object; a 3.8-second hold revealed a checked row and count 32.
Checked reveal persisted across an app process restart.
Phone navigation changed during follow-up steps, so removal, the sphere view and
capture were not manually confirmed; removal and data/path behavior have JVM
coverage. Additional phone navigation stopped pending availability.
No personal data clearing,
gallery save, external share or GitHub publication. Matching-signed replacement
retained data and exact installed APK bytes were verified.
APK SHA-256: `921b21203975e20c6c33e71acbdfc29fd6f3fe06c4a0e3a88f45f11303b99309`.


## Qualified thermal references and catalog coverage — 2026-10-06

Both debug and local release APKs compile. All 637 JVM tests pass without errors,
failures or skipped tests, including ten new reference/conversion regressions.
Android test sources compile; connected instrumentation was not executed.
All twenty language catalogs have the same 444 translatable keys, including
fifteen new full-detail labels, explanatory notes and compact catalog qualifiers.

The audit identifies why day/night sorting hid already available means, ranges,
atmospheric and stellar references. A shared resolver now prefers actual extrema,
then exposes a labelled reference; range ordering uses a midpoint and unknowns
remain last. Twenty-three regular objects have defensible temperature references.
Pressure's seven physical surface/exosphere references are retained; missing
atmospheres and undefined surfaces do not receive fabricated numeric values.
See THERMAL_REFERENCE_AUDIT.md for the complete coverage table and primary sources.

Samsung SM-G970U1 / Android 12 received the exact signed local APK with the same
authorized certificate and retained data. Final-APK UI checks covered Sedna,
surface means/ranges, atmospheric layer labels, compact stars, Rigel, Polaris,
Alpha Centauri's component qualifier, the Sun, Stephenson 2-18, Proxima and Venus.
The initial APK also showed dated comet observations and the exoplanet uncertainty.
The default catalog ordering was restored; checked objects were not changed.
Object-detail screens were not visually rechecked for this change. A final return
to main was not confirmed because the foreground guard stopped the check.
The local SHA-256 is recorded in the development validation artifact. No GitHub
publication, gallery save, sharing or connected instrumentation was performed.


## LV-426 catalog order and unlock feedback — 2026-10-06

Debug and local release APKs compile. All 643 JVM tests pass without failures,
errors or skips, including six new regressions for default insertion, restored
navigation, immediate offline distance, ascending/descending distance sorting,
preservation of nearby km summaries and missing remote data. Android test sources
compile; connected instrumentation was not executed. All twenty language catalogs
have the same 445 translated keys, including the unlock message.

LV-426 appears between Alpha Centauri AB and TRAPPIST-1e in default order. A
reference fallback makes its distance available before async catalog refresh, so
explicit distance sorting never temporarily treats the newly revealed row as
missing. Other sort fields and existing filters continue to use ordinary behavior.
Successful reveal calls the existing customized Android bottom-message helper;
repeating a hold on an already selected object does not repeat the toast.

The exact signed local APK was installed on Samsung SM-G970U1 / Android 12 with
the same authorized key and preserved data. A 3.3-second title hold displayed
“Hai sbloccato il pianeta segreto: LV-426!”; the message was captured visually and
the revealed object appeared in main navigation while the active Sun stayed
unchanged. The user returned to main during verification, so catalog position
was verified by the numerical/default-order regressions rather than a phone
list screenshot. No GitHub publication, gallery save or sharing occurred.


## Panorama centering and photographic angular grid — 2026-10-07

Debug and signed local release APKs compile. All 657 JVM tests pass without
failures, errors or skips, including fourteen new regressions for panorama
centering, seam clipping, variant identity and photographic angular projection.
Android test sources compile; connected instrumentation was not executed.
All twenty language catalogues have the same 446 translated keys.

The panorama-only compass opens four radio choices, without a Default row.
Manual choice is persistent; first use follows the capture's actual/Scenario
hemisphere. Object images, paths, captions, cardinals and angular axes rotate
together and exports include the center in their cache identity. Camera photos
have no center selector and use the photographed field, never a 360-degree scale.
The angular grid adds top/bottom azimuth, left elevation and visible cardinal
marks on the true horizon. See PANORAMA_CENTER_AND_PHOTO_GRID.md.

Manual Samsung SM-G970U1 / Android 12 checks exposed and fixed insufficient
top-axis text margin and an existing landscape capture-axis inversion. The final
APK shows positive elevations above the horizon, readable azimuths on both edges,
and Southeast/South with SE/S on the photographed horizon. The photo preview
has no center selector. The final panorama has its compass above Labels and
the popup retained South after package replacement/process restart. On the first
APK, all four popup options were checked, West visibly recentered the complete
scene, and South was restored. The center-related source did not change between
these builds. The final main screen was restored with camera disabled and active
Sun unchanged. Current photograph contained no selected celestial object;
astronomical overlay geometry, portrait/roll and both landscape rotations have
numerical coverage rather than a new sky calibration. Gallery/document save,
external sharing and southern-hemisphere Scenario were not exercised manually.

The exact installed APK bytes and authorized signing certificate were checked;
replacement retained app data. No GitHub publication, automatic gallery save,
external share or connected instrumentation occurred. Complete evidence and
scope are in the local capture-grid-validation.json artifact.
APK SHA-256: `cff89d9986813633e07ab55b4744af39272b162c7b5d4d557ff6709297ac5ca6`.


## Retired Earth-centre object and guide crosses — 2026-10-07

Debug/release compilation and Android-test source compilation pass. All 663 JVM
tests pass with no failures/errors/skips, including six new catalog/preference,
hemisphere/altitude geocentre, zenith, rotated-panorama and camera-ray regressions.
All twenty language catalogs have the same 447 translated keys. Connected
instrumentation was not executed.

Samsung SM-G970U1 / Android 12 received the exact matching-signed local APK;
installed bytes were verified and data retained. Manual portrait checks showed
Zenith +90 degrees and its cross on the panorama's upper angular edge. Catalog
title reads 30 Oggetti, with Venus followed directly by ISS. Sun/Moon checks,
active Sun, camera disabled and previously enabled guide state were retained.
The app was returned to main. The preview closed before the guarded pan helper
could act, so the Earth-centre cross was numerically verified rather than visually
rechecked. Camera/live visibility and label hiding were not manually exercised
for this change; the shared renderer and front/behind geometry have coverage.
No phone data reset was used to exercise migration; restoration tests cover it.
Celestial circle/pole directions are unchanged. See EARTH_REFERENCE_POINTS.md.

No GitHub publication, gallery save, external sharing or connected instrumentation.
Evidence/scope are recorded in the local earth-guide-points-validation.json.
APK SHA-256: `6bfb50dd14c00cadda21f52a8eb3a17bfbce9c473dfc9b63b8e2602d174aa590`.


## Curved reference-guide captions — 2026-10-07

Debug and signed local release APKs compile. All 669 JVM tests pass without
failures/errors/skips, including six new curve-layout regressions. Android test
sources compile; connected instrumentation was not executed. All twenty language
catalogs retain 447 translated keys with no parity errors or new resources.
The shared orbit/reference painter preserves shaped glyph spacing and outlines.
Reference curves/calculations and horizontal point captions are unchanged.

The exact matching-signed APK was installed on Samsung SM-G970U1 / Android 12;
installed bytes were verified and user data preserved. Manual portrait checks
showed curved Italian Capricorn in the live virtual view; curved equator, tropic,
Antarctic names/degrees in the panorama; horizontal Zenith and south celestial-pole
captions; Labels off/on suppression and restoration; and curved Capricorn plus a
horizontal Earth-centre cross caption in a real-camera photo. All geometry/crosses
remain when annotations are hidden. Main was restored with camera disabled,
guides/labels enabled and active Sun plus Sun/Moon checks unchanged. No new
landscape/tablet or non-Latin-language visual run was performed. Not every circle
was inside the actual photograph; full circle/hemisphere/center/rolled-camera
geometry has numerical coverage. Gallery/document save and external sharing were
not exercised. No GitHub publication or connected instrumentation.

See CURVED_REFERENCE_LABELS.md and local curved-guides-validation.json for scope.
APK SHA-256: `4739d5e1df98ee430b435697317ca68fff88c2366bdd5a7c2fa728bf00db8594`.
