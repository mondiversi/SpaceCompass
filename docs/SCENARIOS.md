# Location files

The Location management toolbar exports and imports one `.spacecompass` configuration file.
Saving uses Android's document picker, and sharing grants temporary read access
to the same file through the app's existing FileProvider. Importing leaves the
user in Location management, with the editor rebuilt from the imported values.

Version 1 uses UTF-8 Java Properties with `format=space-compass-scenario` and
`version=1`. The file contains its name, configured coordinates and altitude in
metres, IANA time-zone IDs, configured civil-moment representation, all five
independent override flags, automatic position metadata, the clock-advance
option, the signed time-speed setting and its enabled flag, and the resume
policy (`RESTART`, `ACTIVE`, or `ALWAYS`). Inactive configured values are retained.
Numbers and booleans are locale independent; application units and language are
not changed by an import.

No live-clock anchor, elapsed counter or runtime checkpoint is exported.
Importing validates the entire bounded file before changing preferences, clears
the previous clock session, and starts from the configured values. A moving
clock offset is calculated anew at import. The time stored in a file does not
advance while that file is archived. The selected resume policy applies after
import, including across subsequent app and device restarts.

Readers accept at most 128 KiB and reject unsupported schema versions, missing
or invalid fields, nonfinite/out-of-range coordinates and heights, unsupported
time zones, dates outside the app's supported range, and unsupported clock
rates or policies. A rejected file does not change the current location.


## Location management navigation

The main scene Location button opens **Location management** directly; there is no intermediate summary dialog. Its adaptive
menu lists Position, Altitude, Time zone, Date, Time, Time speed and Location resumption,
each with an icon, the current effective value, a secondary automatic status when applicable, and a direction-aware chevron. Position shows only its cached locality; coordinates remain in the Position child form. Date and time follow the running location clock.
Only this menu has Export and Import. Both actions remain available for an automatic location. The export dialog keeps Name, the optional celestial-object checkbox, a divider and the shared data profile in that order. The checkbox starts off each time. Save and Share accept an explicit configuration, an automatic spatial snapshot when a valid fix is available, or an included object selection without GPS. Resumption alone does not make an otherwise automatic location a configured payload. Child toolbars have their section title and Back.

Only the management menu toggles the first six overrides. Child toolbars contain their section title and Back, without activation checkboxes. All forms stay fully visible and editable at normal opacity, even while their override is off. Valid prepared values are saved independently of activation; enabling the menu checkbox applies the retained value. Temporarily busy lookups still disable their own controls, and incomplete input never replaces the last valid configuration.
+
+Time speed resets to 1x when re-enabled. Resumption has a permanently checked, disabled grey checkbox in the management menu; its child still offers the same three radio choices. It is never counted as a custom override. Active values use the theme's orange accent in the menu, while ordinary titles and icons remain neutral. Compact natural-height cards preserve the leading 48 dp touch target and independent landscape columns.
+
+One retained draft owner serves the whole hierarchy. Returning to the menu flushes valid
edits and retains incomplete drafts without replacing the last valid plan. The menu shows
validation feedback; the user can visit another dependent section before completing the draft.
Management exit/export requires a complete saved configuration.
System Back and the toolbar follow the same path. Menu position survives child navigation;
rotation retains the child and draft. Internal navigation does not emit saved-settings
notices; the standard confirmation occurs once when leaving the modified management flow.
Imports recreate the editor at the menu and keep all existing serialized fields and flags.
Leaving management returns directly to the main scene. Providers and clock behavior are unchanged; optional version-1 fields carry scene objects.


Location cards use a compact 48 dp minimum header target above their effective-value
row, no added top inset, 8 dp bottom padding and 18 sp title line height. The shared 12 dp horizontal
island padding and standard text sizes remain unchanged.
Position keeps latitude, longitude and locality visible inside its form, using the existing language-specific reverse-geocoder cache.
No extra GPS-current prefix or repeated child-header icon is needed. A missing place
uses the existing loading/unavailable strings; no location is invented. Other toggle
islands keep their existing leading icons unless their caller explicitly omits one.

The effective-value summary refreshes once per second only while the editor is resumed and the screensaver is inactive. It reads the existing observer model without changing clock anchors, persisted drafts or providers.

Each management menu entry is an independent rounded card, separated by the shared 9 dp island gap. There is no common card or internal divider. Values, activation state, touch targets and navigation remain unchanged.

## Automatic observer values

An export with no enabled observer overrides or time-speed override captures the current
effective position, altitude and IANA time zone only. It never borrows inactive saved
coordinates, dates or clock anchors. Date and time remain live at the imported place;
speed is inactive, and the recipient's resumption preference is retained. Missing or
invalid position/altitude leaves delivery unavailable until a valid fix arrives, while
an optional objects-only file can still be delivered without GPS. Export does not enable
the sender's controls. Explicitly configured archives keep their existing behavior.

The optional version-1 `temporalSettingsIncluded=false` flag identifies spatial-only
exports. Required legacy clock properties retain neutral inactive defaults for older
readers; current readers validate them and omit temporal information from readable
comments and generated names. Existing archives without the flag retain full import
semantics. No runtime checkpoint or elapsed anchor is distributed.

## Optional celestial objects

The optional `objectsIncluded`, `objects` and `activeObject` properties preserve checked
stable catalogue IDs and the active object. Explicitly empty selections clear the recipient
scene; omitted fields leave it unchanged. Favorites and catalogue filters remain independent.
The 128 KiB reader preserves version-1 compatibility and discards unknown/retired IDs.

## Management touch targets

Each card uses two rows: the icon, activation checkbox and title form a compact leading
target on the first row; effective values and automatic status are below it. Only that
compact target changes activation. All other card space, including values and the
trailing chevron, opens the editor. The child toolbar uses the same activation handler.
Resumption has no checkbox. Logical order and chevrons follow RTL layouts.

The management menu uses the shared staggered island grid: one column in portrait,
up to two readable columns in landscape, natural card heights and keyed scroll state.
Header activation has no background indication; navigation keeps the card ripple.
The 48 dp activation target is retained. Compact decorative icon bounds and an optical
chevron offset align visible glyphs with the value row.

Catalogue option counts combine the other facets, the search scope and favorites.
They include checked objects which the existing display rule keeps visible. A type
option describes that type alone; the current field shows the selected type union.
Every dropdown option places its count at the trailing edge. The favorites control remains a plain heart with no count badge; existing counters elsewhere are unchanged.

The activation target remains 48 dp. The header has no additional internal top
padding; the larger bottom inset continues to give values more breathing room.

Catalogue rows have the same neutral background for favorites and other objects.
Favorites use the shared day/night heart color for their checkbox: an empty
outline when unchecked and a filled box when checked, with a contrasting checkmark.
The check still means scene selection. Thumbnails use their ordinary 2 dp orbit-color
outline only when checked; there is no favorite-specific thumbnail outline.
Astronomy and orbit colors elsewhere are unaffected.

The value row sits 2 dp closer to its heading by consuming lower header whitespace,
without moving header glyphs, reducing its 48 dp activation target, or changing outer insets.

Resumption is a direct, always-available policy choice with no activation checkbox.
The compact localized label and the original toolbar vector identify its menu entry.
The existing effective policy remains compatible with files and preferences from the
temporary checkbox design; obsolete optional activation fields are ignored.
