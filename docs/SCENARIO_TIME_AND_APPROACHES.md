# Scenario time and close approaches

The Time island and Custom scenario title-bar resumption action follow the
existing settings controls, typography, theme, automatic saving and short notifications.
No Android time or GPS value is changed. Initial acceleration and approach support shipped in 1.4.3.

## Time ownership

`SpaceCompassScenarioClock` is a synchronized, lifecycle-controlled time source.
The observer ViewModel retains it across rotation; its runtime reference is
excluded from configured observer equality, copies and legacy serialization.
All existing observation, capture, weather and screen-saver consumers resolve
time through `observationTimeMs`, so acceleration also crosses civil midnights,
month/year boundaries and daylight-saving transitions in either direction.

The discrete slider has 25 positions and is symmetric: -86400× through -1× on the
left, a paused 0× at the exact centre, and +1× through +86400× on the right. Both
directions use the reviewed magnitudes 1, 5, 10, 30, 60, 300, 900, 3600, 7200,
21600, 43200 and 86400. The maximum corresponds to one elapsed 24-hour day per
real second; local civil days still follow the selected zone's daylight-saving rules.
Older saved or imported rates that no longer have a slider step (2, 3, 6, 120, 600,
1800, with either sign) migrate to the nearest slower magnitude, preserving the
direction, enabled flag and resumption policy. Unknown rates remain invalid in
archives. Physical
left/right direction is retained in RTL locales. A centre-origin track and compact
direction/rate/real-second equivalence rows distinguish rewind, pause and forward.
Clock input remains the standard accessible stepped Slider. Re-enabling Time restores
the last selected rate, including zero or rewind. Its new-install value remains +1×.
Changing direction or passing through pause retains the reached instant.
Location management edits use a retained, non-running observer preview. Every valid
option is still autosaved, but the live observer keeps its previous configuration
until navigation returns to the main sky. The preview and the existing scenario
clock are held while editing, including Always mode; a new rate never consumes
time spent choosing other fields. Returning from a parameter to the management
list does not apply it. Main-view return applies the prepared configuration and
reanchors its clock once. A prepared clock marker prevents process recreation
from counting the time spent editing; imports/exports within management use the
prepared values rather than the unchanged live observer.

Acceleration is off by default (effective +1×). Settings remain saved independently
of the date/time switches. Changing rate/resumption retains the reached instant;
editing a configured temporal component establishes a new origin. Position-only
edits do not rewind the clock.
Disabling every temporal override returns to real time and ends the temporal
checkpoint session; rate and resumption choices remain saved for the next session.

- Restart (default when no choice is saved): a new application session starts at the configured instant; rotation
  and ordinary page navigation do not start a new session.
- Active only: a monotonic clock advances while resumed and outside
  the screen saver. Pause/stop saves a synchronous checkpoint; a one-second
  foreground checkpoint also limits loss on abrupt process termination. An abrupt
  power loss can lose the uncommitted fraction of the last checkpoint interval.
- Always: a persisted instant/wall-time anchor accounts for time outside the app
  and across reboot, multiplied by the selected speed. No background timer, sensor
  service or wake lock is used. Manual system-clock adjustments affect this mode.

Legacy real-clock-offset scenarios retain Always until a resumption choice is
made. The supported timeline is bounded to the editor's 1900–2100 range. Online
weather/ephemeris request moments are sampled at most every 30 real seconds during
acceleration. Cached provider validity remains authoritative: fast traversal may
reach times without valid remote satellite/probe/weather data; unavailable values
are not extrapolated or invented.

## Close approaches

The threshold is 1.5 degrees of topocentric great-circle separation. Both bodies
must be evaluated at the same absolute instant. Intersections of daily curves at
different times do not count. Azimuth wrapping and zenith/nadir geometry use unit
vectors, not planar azimuth/elevation subtraction.

Daily event discovery uses already cached, time-interpolated directions to bracket
local minima. Exact observations for both bodies refine each candidate to about
100 milliseconds and validate its final separation against 1.5 degrees. Missing
exact data reject a candidate. Ordinary sampling is at most three minutes;
satellite candidates use exact five-second observations in the overlapping available path
windows. This is a numerical close-approach aid, not an authoritative occultation
or eclipse prediction service. Satellite paths retain the existing current-orbit
window rather than claiming a complete 24-hour prediction.

Each event carries its counterpart and angular separation, becomes a chronological
marker in both paths, uses a double-circle glyph and can be pinned independently
from another counterpart at the same minute. Live pairing is computed as one
atomic same-time batch away from sensor frames. No object selection is changed.
Dedicated event glyphs paint after hourly dots, all path strokes and object
thumbnails in the main view and both capture renderers. They remain visible when
image labels are disabled; close-approach captions qualify as key-point labels.

Ordinary daily-path rows show the direction of elevation change. Key-event rows
retain only their rise/set/extremum/approach glyphs, without an additional direction
arrow. Current shows its original circle-and-dot position marker and retains its key-point
emphasis and persistent highlighted background. A fixed symbol column and a 6 dp gap
keep glyphs apart from the aligned names. Unknown or flat samples do not invent a direction.
Night counters use dark digits on their cyan or orange fill; day counters remain white.

Captures freeze paths and pair events at their observation time, including after
a simulated midnight. Photograph and panorama markers/labels and CSV/TXT exports
share the same event payload, localized names and formatting. Approaches remain
subject to the existing image label mode. Four-star constellation geometry and
all older trajectory/event behavior remain available.

Verification includes pure clock persistence/reboot/DST/rate tests, deliberately
asynchronous path intersections, simultaneous intersections between samples,
missing data, stable close pairs, selection identity, real Sun/Moon directions,
chronological text/CSV formatting, existing tests and translation parity.

## Daily point selections

The main sky's point bubbles keep their object/event title and clock/countdown,
then show azimuth and elevation together on one row. Shared horizontal/vertical
double-arrow icons identify the two values; translated accessibility descriptions
remain complete.

The selection owner tracks the effective civil date in the Location zone. A date
change (real midnight, simulated progression, rewind or a different configured day)
clears all pins belonging to live tracks, including current positions, hourly and
key-event markers. This also applies in the forward-24-hour mode. Pins attached to
fixed height/event/personal comparisons keep their identity and remain selected.
The reset is silent; count badges update and antipodes turn off only if no pins
remain. Merely reopening a page or refreshing data within the same day preserves
selections. Scene loading establishes the incoming Location day before restoring
its pins, so imported live points survive loading and expire at the next date
change. An already generated image/export remains its original frozen snapshot.

## Capture clock columns

Photo and panorama bubbles share the same drawing/layout routine. Their clock
row places the observation time at the left edge and T at the right edge without
a visible separator, matching the main sky. Labels measure both fields and a
font-relative minimum gap before placement; wider event titles or angle rows
naturally spread the columns further apart. Angle rows keep their middle dot and
icons. Canonical label strings retain their clock/countdown boundary for internal
compatibility; only the clock row is rendered as columns, leaving personal titles
and translated event names untouched.
