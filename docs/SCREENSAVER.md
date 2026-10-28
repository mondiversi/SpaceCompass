# Earth screensaver

General contains a screensaver below background music. Its activation and delay
(30 s, 1, 2, 5, 10 or 15 min; default 5 min) are saved. New installations enable it;
updates retain saved choices. One activity-owned monotonic inactivity deadline
handles touch, scroll, keys, held gestures, keyboard, focus and rotation. Sensor
and data updates never count as user interaction. Touch or Back dismisses the
cover without activating controls underneath.

The cover is immersive, including the Android system bars. An animated globe
stays centered at the same physical size across portrait/landscape, using 66% of
the display's short edge and a 94% viewport clamp for small windows. The native
footer shows local time, then abbreviated weekday/date, then battery status.
It follows the selected system clock/date conventions without notification access.

A child lifecycle caps the retained page at CREATED while the cover is active.
Sensors, GPS, camera, ephemeris/weather/place requests, scrolling animations and
main/object rendering pause; on dismissal they resume with retained page state.
Activity-owned foreground music follows its usual audio-focus policy and remains
available. This reduces unnecessary live work; battery endurance is not measured.

The independent renderer receives an Earth-fixed geocentric Sun direction derived
offline from the real UTC instant. Its solar and seasonal calculations update once
per idle minute, independently of GPS, system time zone, interface theme and any
scenario clock. Surface normals and sunlight share the same view transform, so
visual globe rotation does not move daylight to the wrong continents. Decorative
orbits, clouds and accelerated spin remain illustrative. Scene time survives rotation.

The HTML and two 2048 x 1024 WebP maps are bundled for offline first use, at quality
90/method 6 with pins in SCREENSAVER_ASSETS.json. Its isolated WebView has no network,
file/content access, navigation, tracking, audio or JavaScript bridge. Closing the
scene destroys only its WebView; it never globally pauses other WebViews. A Canvas
fallback uses the same lighting and physical globe size if WebGL is unavailable.

Earth maps: NASA Blue Marble (SVS 57730) and NASA/NOAA Black Marble (SVS 79765).
No endorsement is implied. Original scene code and its copyright remain credited
under the application's GPL-3.0 distribution.


## Real GPS marker and seasonal view

A surface pin follows only the last valid real-device GPS coordinates, never the
scenario position. Invalid/lost fixes do not erase it. It remains visible on the
far hemisphere in translucent grey, with a smooth style transition at the limb.
Coordinates remain in session memory, never a new persistent cache, and the saver
does not request GPS. Its location source has no scenario-date or fixed-time input.

The maps retain their original orientation and hashes. East-positive longitude
uses atan(x,z) for texture longitude; the shared inverse spin produces eastward
rotation. The apparent north-axis roll and lean use solar ecliptic longitude and
mean obliquity at the real date. Thus a northern location appears closer to the
disc centre in northern summer and higher in winter, from this Sun-facing
perspective. The geocentric solar vector uses Astronomy Engine's J2000-to-horizontal
rotation at the equator/Greenwich; north, west and zenith map into the globe's
Earth-fixed +Y, -X and +Z axes. No atmospheric refraction or surface-observer
parallax is applied to the global illumination.

The same sphere transforms drive shader sampling and the marker, including the
offline software fallback for missing/lost WebGL. Fallback source/normals are
cached; rasterization is capped at one frame per second, with exact pin alignment
to its last frame. Reduced-motion and context-loss paths are verified. Seasonal
host updates run once per minute and do not restart live page work.

Reference: [NASA's seasonal Earth views](https://science.nasa.gov/earth/earth-observatory/an-epic-view-of-the-seasons/).
