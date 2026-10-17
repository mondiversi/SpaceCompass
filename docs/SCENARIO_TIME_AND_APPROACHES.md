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

The discrete slider is symmetric: -3600× through -1× on the left, a paused 0× at
the exact centre, and +1× through +3600× on the right. Both directions use the
reviewed magnitudes 1, 2, 3, 6, 10, 30, 60, 120, 300, 600, 1800 and 3600. Physical
left/right direction is retained in RTL locales. A centre-origin track and compact
direction/rate/real-second equivalence rows distinguish rewind, pause and forward.
Clock input remains the standard accessible stepped Slider. Re-enabling Time resets
to +1×; changing direction or passing through pause retains the reached instant.
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
