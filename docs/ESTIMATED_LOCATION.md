# Estimated place names

The Estimated location row belongs to Position details, below GPS altitude
and above estimated weather. It displays only available administrative names:
city (or county/district when no city is supplied), region and country. Duplicates
and provider whitespace are normalized without replacing proper names. The
normal data font is retained. Available names are joined with commas; long
names wrap naturally rather than reserving one line for each administrative level. Names are descriptive data,
not measurement values, so unit/number preferences do not alter them.

`SpaceCompassEstimatedPlace.kt` is the Android-independent formatting, coordinate
key and bounded cache. `SpaceCompassEstimatedPlaceLookup.kt` owns device lookup
and foreground lifecycle/coroutine state. The owner remains composed before any
child-page returns, so navigation reuses the cache.

Requests use Android's [Geocoder](https://developer.android.com/reference/android/location/Geocoder),
with its selected-locale constructor. Its provider can use network services and
localize names only on a best-effort basis. A backend can be absent or return
no names. Android 13/API 33+ uses callback results; earlier supported versions
use an interruptible I/O worker. Waiting is limited to ten seconds, cancellation
ignores late callbacks, and provider exception text is never displayed/logged.
No extra location permission, account or API credential is introduced.

Queries run only while the Position details page is visible and the lifecycle is
resumed. Coordinates are rounded to 0.001 degree (roughly 100 m cells) for this
lookup only. The displayed GPS/celestial calculations retain their existing
precision. Debouncing and a ten-second request interval across cells limit jitter.
The memory-only LRU cache holds at most sixteen locale/location keys. Successful
labels expire after thirty minutes; empty/failed results retry after one minute
while the page remains visible. Missing GPS cannot reuse an old area's name.

Only administrative fields are read from returned addresses. Streets/house
numbers are not displayed, and resolved names/coordinates are not written to
preferences, app files or error logs by this feature. The online-data note is
updated in all twenty languages. An unavailable geocoder never disables GPS,
compass pointing or local celestial calculations.

## Position details map and table

The main view's GPS/details action opens Position details. Portrait places a map
above a scrollable table; landscape places the map on the left and the table on
the right, including RTL locales. The native layout chooses its orientation before
measurement, so neither half is temporarily outside the viewport after rotation.
The split matches celestial object details: a 10 dp gap in portrait, 12 dp in
landscape, shared 10 dp side and 6 dp bottom insets, and no top inset below the
toolbar. Landscape requires the available width to exceed height and reach
540 dp. Inside the readout viewport, the table island has 8 dp external side
margins, matching the shared inset of its notes/attribution and celestial object
detail cards. This keeps the overlay scrollbar clear of the island background.
The split's outer margins and table's inner padding remain unchanged.
GPS accuracy appears beneath the GPS label; values share a right-aligned column.
Altitude, estimated place and weather follow, with existing notes and weather
attribution below. GPS value updates preserve the table's scroll position.

The native table suppresses Android's fading scrollbar and draws the same overlay
as celestial object facts: the caller's secondary color at 46% alpha, a 3 dp round
thumb, 2 dp edge inset and preferred 28 dp minimum constrained to the track. The
existing shared geometry computes its height. It remains visible whenever the
content overflows beyond 2 dp, and a 16 dp right-edge target supports track seek
and dragging. Ordinary scrolling and Android accessibility remain inherited
from ScrollView. Scroll offsets are compensated while drawing, keeping the thumb
inside its viewport after scrolling or rotation.

Opening this page loads bundled Leaflet 1.9.4 and HTTPS OpenStreetMap raster
tiles using the effective observer position. It does not load the map at app
startup and no runtime JavaScript/CSS CDN is needed. A red marker, localized
48 px zoom controls and visible OpenStreetMap contributor/Leaflet attribution
remain. Only the tile pane is recolored for the app's night theme; marker and
controls are styled separately, with Android's extra automatic inversion disabled.

A native Android window owns the secure WebView, avoiding the legacy
Compose/WebView canvas issue observed in the earlier Android 12 phone check.
JavaScript is enabled without an application interface, file/content access or
WebView geolocation permission. Mixed content is blocked. User-initiated HTTPS
attribution links can open externally. Missing/invalid real GPS cannot form a map
URL; a validated custom plan can supply the observer independently. GPS jitter
and orientation changes preserve the initial viewport/zoom. Map failures show
Retry, including initial tile loads for which no tile succeeds.

With a custom location, details identify coordinates and estimated altitude
without claiming GPS accuracy. Date-only simulation keeps real GPS labels and
accuracy. Weather follows the effective location and instant: current conditions
with a live date, hourly estimates when available with a fixed simulated date.
The Simulation page uses the same map in point-picking mode and deliberately
persists the applied custom plan. That independent feature, data transfer and metadata provider are
documented in [OBSERVATION_SIMULATION.md](OBSERVATION_SIMULATION.md).

Toolbar/Android Back return to the main scene. Leaving stops loading and destroys
the WebView. Ordinary map viewport coordinates are not persisted; an applied
custom observer plan is saved separately. Header controls inherit the app
palette, translations, RTL and adaptive size, with native system-bar/cutout insets.
The single-line title uses the same automatic overflow cadence and 14 dp fading
edges as the other page titles. Fitting text stays still; enlarged font settings
may increase the toolbar height above its normal 48 dp minimum to prevent clipping.
Tests and manual execution limits are recorded in [VERIFICATION.md](VERIFICATION.md).
