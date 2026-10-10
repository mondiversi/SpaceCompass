# Mondiversi screensaver

General contains a screensaver below background music. Its activation and
delay (30 s, 1, 2, 5 or 10 min; default 5 min) are saved independently of
presentation settings. A fresh install enables it after five minutes of inactivity.
Updates retain explicitly saved activation and delay preferences.

An activity-scoped ViewModel owns one cancellable monotonic inactivity deadline.
Touch, scroll and key input restart it. A held gesture, keyboard, loss of window
focus, the launcher introduction and app background suspend activation. A pause
caused by rotation retains an active scene; leaving the app resets it. Sensor and
ephemeris updates never count as input. Tap or Back closes only the cover and
returns to the retained page. ACTION_DOWN alone cannot expose or click that page.

The renderer reuses Mondiversi.me's Earth shader, stars, orbital geometry and
30 fps pacing. The retained main sky publishes only its current geometric solar
height through an activity-scoped sink. While visible, the saver samples that height
every 30 seconds and maps the shared continuous solar-light bands to the globe's
illustrative shader angle. GPS/scenario position and time therefore drive daylight
without adding location/sensor listeners. A missing solar fix falls back to the
local clock. The interface theme never forces midnight/noon lighting. The non-WebGL
fallback also dims its day map and blends city lights. Globe rotation and the
accelerated orbits remain decorative, not a live ephemeris or surface weather model.
Scene time survives rotation.
The globe stays centered in the safe app viewport in both orientations,
including the non-WebGL fallback.
Both renderers use a globe diameter of 66% of the display short edge, keeping
its physical size stable across rotation. A 94% viewport clamp also keeps it
inside small split-screen windows.

The native view is mounted above the Compose canvas but inside its measured safe
window rectangle, avoiding legacy WebView functor incompatibilities. The covered
composition is unplaced and its main sky renderer is suspended. Music follows
its existing foreground and audio-focus policy. Closing or suspending the scene
destroys its WebView; it never calls the global WebView pauseTimers API.

The page and two 2048 x 1024 WebP maps are bundled, so it works offline on first
use. They use quality 90/method 6; dimensions, sources and hashes are recorded in
SCREENSAVER_ASSETS.json. WebView has network/file/content access disabled, a
restrictive CSP, no navigation, no JavaScript bridge and no website controls,
tracking or audio. Only derived lighting numbers enter the renderer; no GPS or simulated coordinates do.

Earth imagery: NASA Blue Marble (SVS 57730), NASA/NOAA Black Marble (SVS 79765),
as credited by the source website. No endorsement is implied. GLSL/Canvas scene
code is original Mondiversi work reused under the app's GPL-3.0 distribution.
WebGL failure falls back to the bundled circular Earth reference images.
