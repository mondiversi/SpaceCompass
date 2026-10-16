# External point reminders

The focused sky-point bubble and every daily-path row have a bell action. This
action is independent of the existing point-selection gesture. Current, past or
expired points remain disabled, comparing both the viewed scenario time and the
real wall clock. Daily-row bells retain 48 dp touch targets; the main callout uses the shared compact floating-action geometry. The angle columns reserve the
same narrower widths in the fixed header and rows to retain caption space.

## Handoff policy

The existing point timestamp is an absolute instant. Convert it into the phone
time zone before deciding whether Android Clock can represent it. A non-repeating
alarm is offered only if the next unambiguous occurrence of its hour/minute is
the chosen event minute. Other future dates use a Calendar event editor with
the exact begin timestamp and a one-minute duration. Repeated DST hours and
events whose minute has already started also use Calendar, preventing accidental
alarms on another day. Clock reminders have minute precision and may ring up to
59 seconds before an event with fractional-minute timing.

The launch handler rechecks eligibility and the phone zone on every click.
A shared one-second foreground ticker refreshes expiry even for frozen scenarios;
there is no per-row timer, and it stops when an external app is opened.
No selection or scenario preference is changed by the handoff.

## Android integration

Clock uses ACTION_SET_ALARM with hour, minute, event title and EXTRA_SKIP_UI=false.
The receiving Clock app may create the alarm as it handles the request, with its
own UI. Calendar uses ACTION_INSERT with the event MIME type, title, exact start,
end, all-day=false and device zone. Its editor owns confirmation and reminder
options. Cancellation does not imply success, and Space Compass never displays
an unverified alarm/event-created notification.

The manifest adds only the normal SET_ALARM permission and two narrow package
visibility queries. Calendar read/write permission, exact-alarm access, background
services and local scheduled alarms are unnecessary. No coordinates, place names,
attendees or invitations are added to either handoff. Missing receiving apps are
handled with the existing short themed message.

## Validation and references

JVM regressions cover real/viewed past and current points, near/future dates,
time-zone conversion, fractional offsets, DST overlaps/gaps, minute boundaries,
expiry, every body/event and all twenty translations. Android intent-construction
tests only build intents; they do not create real alarms or calendar entries.
Connected instrumentation is not run without separate class-specific consent.

- https://developer.android.com/guide/components/intents-common#Clock
- https://developer.android.com/guide/components/intents-common#Calendar

Available from release 1.4.0; no background reminder scheduler is added.

## Main-view visibility

General > Events follows Fluidity and contains Show the alert button on the main screen, enabled by default.
Its presentation preference is saved immediately and restored across app/device restarts.
It only hides the floating reminder beside the main-view point caption, including its
spacing and active ticker. Daily-path and satellite-pass reminder controls stay visible;
their eligibility and key-event emphasis are unaffected. All twenty languages include
the title, checkbox label and scope description, using the shared settings island style.

## Reminder glyph rendering

Hourly, current and key-event bells all retain the original 2 dp stroke. Key-event
row text/background emphasis remains independent. Disabled glyphs render their
strokes opaquely first, then composite the complete icon at its disabled opacity;
intersections no longer multiply opacity. Shared monochrome settings/menu and
capture/export glyph families use the same treatment without changing geometry,
dimensions or touch targets.
