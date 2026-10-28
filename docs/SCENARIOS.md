# Scenario files

The Scenario management toolbar exports and imports one `.spacecompass` configuration file.
Saving uses Android's document picker, and sharing grants temporary read access
to the same file through the app's existing FileProvider. Importing leaves the
user in Scenario management, with the editor rebuilt from the imported values.

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

Readers accept at most 16 KiB and reject unsupported schema versions, missing
or invalid fields, nonfinite/out-of-range coordinates and heights, unsupported
time zones, dates outside the app's supported range, and unsupported clock
rates or policies. A rejected file does not change the current scenario.


## Scenario management navigation

The main scene Scenario button opens **Scenario management** directly; there is no intermediate summary dialog. Its single-column
menu lists Position, Altitude, Time zone, Date, Time, Time speed and Scenario resumption,
each with an icon, the current effective value, a secondary automatic status when applicable, and a direction-aware chevron. Position shows only its cached locality; coordinates remain in the Position child form. Date and time follow the running scenario clock.
Only this menu has Export and Import. Both actions remain available for an automatic scenario. The export dialog starts with “Include automatic values too” unchecked each time. Save and Share require a custom value (including Time speed), or a completed automatic snapshot; Resumption alone is not a payload. Child toolbars have their section title and Back.

The first six children have one checkbox at the trailing end of the toolbar, using the shared blue/cyan theme accent when enabled. There is no repeated activation header in the island. The complete form remains visible in both states. Off uses the real/effective values in disabled controls and a neutral grey 48% body treatment; on enables the retained configured inputs. The opacity transition takes 300 ms and the form does not collapse. Keyboard focus is cleared when disabling a section.
Inactive configured values are retained. Time speed still resets to 1x when re-enabled.
Resumption has its three existing radio choices and no activation switch. Active overrides
use the theme's orange value accent in the menu; ordinary labels and icons remain neutral.

One retained draft owner serves the whole hierarchy. Returning to the menu flushes valid
edits and retains incomplete drafts without replacing the last valid plan. The menu shows
validation feedback; the user can visit another dependent section before completing the draft.
Management exit/export requires a complete saved configuration.
System Back and the toolbar follow the same path. Menu position survives child navigation;
rotation retains the child and draft. Internal navigation does not emit saved-settings
notices; the standard confirmation occurs once when leaving the modified management flow.
Imports recreate the editor at the menu and keep all existing serialized fields and flags.
Leaving management returns directly to the main scene. No provider, clock or archive schema changes.


Scenario menu rows use a 56 dp minimum, 8 dp vertical padding and 18 sp title line
height; existing 12 dp directional island padding and text sizes remain shared.
Position keeps latitude, longitude and locality visible inside its form, using the existing language-specific reverse-geocoder cache.
No extra GPS-current prefix or repeated child-header icon is needed. A missing place
uses the existing loading/unavailable strings; no location is invented. Other toggle
islands keep their existing leading icons unless their caller explicitly omits one.

The effective-value summary refreshes once per second only while the editor is resumed and the screensaver is inactive. It reads the existing observer model without changing clock anchors, persisted drafts or providers.

Each management menu entry is an independent rounded card, separated by the shared 9 dp island gap. There is no common card or internal divider. Values, activation state, touch targets and navigation remain unchanged.

## Completing automatic values for export

The optional export checkbox captures one effective position, altitude, time zone and instant as explicit
overrides in the portable copy. It follows the applied scenario, including automatic metadata of a chosen
location and the running simulated clock, rather than falling back to the device for a custom value.
Configured-only export retains its previous flags and configured start. The complete copy has no runtime
anchor/checkpoint; its captured instant is a new starting point at import, with the existing time speed,
clock-advance choice and resumption policy. It does not accrue elapsed time while archived.
Changing this checkbox does not update the observer or its preferences. Each new export starts unchecked.
Missing or unsupported effective coordinates/altitude/time leave it unchecked and show a short notice;
zero coordinates or height are never substituted for missing values. Valid known zeros remain supported.
Names and the shared language/units export profile use the selected copy; a manually edited name is retained.
The canonical version-1 schema and older files remain compatible.
