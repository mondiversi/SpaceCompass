# Tesla Roadster (2018) / Starman

The public catalogue adds Tesla Roadster (2018) as an artificial heliocentric payload,
between the Moon and Mars in the stable default presentation order. It is not an Earth satellite
or a scientific space probe. Existing enum identifiers retain their positions;
Sun/Moon defaults and all saved selections remain unchanged.

## Ephemerides

NASA/JPL Horizons target `-143205` currently returns `SpaceX Roadster (spacecraft)
(-143205)`, source `tesla_s11`. The 2025-01-03 revision incorporates 374 optical
measurements from 2018-02-08 to 2018-03-19. Its current predicted coverage runs
from 2018-02-07 03:00 to 2090-01-01 00:00 TDB; uncertainty grows with time.
This is a prediction, not current telemetry or a known spacecraft attitude.

The existing guarded public API/cache pipeline downloads hourly Earth-centred
ICRF LT+S vectors for apparent pointing, and separate Sun-centred geometric
states for heliocentric distance and full orbital speed (not Voyager radial speed).
No observer coordinates are transmitted. Missing or out-of-range data remain
unavailable; there is no extrapolation or substituted planetary position.
Automatic ten-minute catalogue warmup and manual refresh include the new object.
Daily tracks, selection, sorting, filtering and shared frozen exports use these
same existing models. Searches also accept Starman and 2018-017A.

The approximately 1250 kg reference mass is for the car with batteries, explicitly
excluding the rocket second stage and mount. We do not invent a current rotation,
temperature, surface pressure, diameter or a mass for the whole attached stack.

## Image

`tesla_roadster.webp` is a historical SpaceX launch photograph dated 2018-02-06,
not a current optical view. It remains a flat photograph in the observatory,
thumbnails, main sky markers and exported images, never a spherical texture or
a Voyager dish. Its frame is 2048 x 1024, lossy WebP quality 90, method 6,
157614 bytes. The original 1920 x 1080 proportions are retained with black framing.
The image stays outside APK assets, is SHA-256 pinned and downloads individually
from the existing `celestial-textures-v1.1` release, with offline verified caching.
The three additive distribution files are tesla_roadster.webp,
manifest-roadster.json and CREDITS-roadster.txt; existing assets are preserved.

CC0 evidence: Commons Flickr review dated 2018-02-08 confirmed cc-zero;
SpaceX VRT ticket 2015032410033985. The original author, source and processing
are retained in both the app licence text and external package credits.

## References

- https://ssd.jpl.nasa.gov/api/horizons.api?format=text&COMMAND=%27-143205%27&MAKE_EPHEM=%27NO%27
- https://ssd-api.jpl.nasa.gov/doc/horizons.html
- https://commons.wikimedia.org/wiki/File:Elon_Musk%27s_Tesla_Roadster_(40143096241).jpg
- https://www.flickr.com/photos/spacex/40143096241/
- https://creativecommons.org/publicdomain/zero/1.0/

Real JPL fixtures dated 2026-10-15 through 2026-10-18 exercise parser identity,
reference frames, solar/observer distances, velocity, date bounds and daily tracks.
All three new strings have full parity in the existing twenty supported languages.
The catalogue addition is included in application release 1.4.0. Runtime image URLs
and original graphic-release assets remain compatible with earlier builds.
