# Estimated place names

The Estimated location island belongs to environment details, below GPS altitude
and above estimated weather. It displays only available administrative names:
city (or county/district when no city is supplied), region and country. Duplicates
and provider whitespace are normalized without replacing proper names. The
normal data font is retained; long names can wrap. Names are descriptive data,
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

Queries run only while the environment page is visible and the lifecycle is
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

Tests and live-device evidence are recorded in [VERIFICATION.md](VERIFICATION.md).
