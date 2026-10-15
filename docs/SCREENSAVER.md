# Earth screensaver

General contains a screensaver below background music. Its activation and delay
(30 s, 1, 2, 5 or 10 min; default 5 min) are saved. New installations enable it;
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

The independent renderer receives only derived lighting numbers. It refreshes
the Sun's geometric height from the retained effective observer and date, without
new GPS listeners; missing observer data uses the local clock. Daylight follows
solar altitude rather than forcing noon/midnight with the interface theme. Scene
time survives rotation. The decorative globe/orbits are not a live ephemeris.

The HTML and two 2048 x 1024 WebP maps are bundled for offline first use, at quality
90/method 6 with pins in SCREENSAVER_ASSETS.json. Its isolated WebView has no network,
file/content access, navigation, tracking, audio or JavaScript bridge. Closing the
scene destroys only its WebView; it never globally pauses other WebViews. A Canvas
fallback uses the same lighting and physical globe size if WebGL is unavailable.

Earth maps: NASA Blue Marble (SVS 57730) and NASA/NOAA Black Marble (SVS 79765).
No endorsement is implied. Original scene code and its copyright remain credited
under the application's GPL-3.0 distribution.


## Observer marker and seasonal view — 1.3.7

A surface pin follows the last valid coordinates actually used by the app, whether
GPS or scenario. Invalid/lost fixes do not erase them; the pin hides on the far
hemisphere. Coordinates remain in session memory, never a new persistent cache.
This does not request GPS during the screensaver. Solar observer and effective plan
are retained separately so date-only scenarios work without a location fix.

The maps retain their original orientation and hashes. East-positive longitude
uses atan(x,z) for texture longitude; the shared inverse spin produces eastward
rotation. The apparent north-axis roll and lean use solar ecliptic longitude and
mean obliquity of the effective date. Thus a northern location appears closer to
the disc centre in northern summer and higher in winter, from this Sun-facing
perspective. Scenario/advancing dates use the same calculation. Local illustrative
day/night shading and accelerated animation remain distinct from that geometry.

The same sphere transforms drive shader sampling and the marker, including the
offline software fallback for missing/lost WebGL. Fallback source/normals are
cached; rasterization is capped at one frame per second, with exact pin alignment
to its last frame. Reduced-motion and context-loss paths are verified. Seasonal
host updates run once per minute and do not restart live page work.

Reference: [NASA's seasonal Earth views](https://science.nasa.gov/earth/earth-observatory/an-epic-view-of-the-seasons/).
