# Catalogue expansion in 1.4.2

Eleven objects are new; Proxima Centauri was already present as a star, not as its planet b. No old enum identifiers, saved selections or default Sun/Moon checks were changed. There are 47 ordinary catalogue objects plus the existing hidden fictional object. Asteroid is a new explicit filter type.

## Tracking provenance

| Object | Authoritative identity | Model |
| --- | --- | --- |
| New Horizons | JPL -98 | Horizons apparent geocentric ICRF states |
| JWST | JPL -170 | Horizons; not an Earth-orbiting SGP4 satellite |
| Parker Solar Probe | JPL -96 | Horizons |
| 99942 Apophis | 99942; / 2004 MN4 | Horizons small-body solution |
| 3I/ATLAS | DES=3I;NOFRAG;CAP; / C/2025 N1 | Horizons hyperbolic comet solution; no invented revolution period |
| Europa Clipper | JPL -159 | Horizons |
| JUICE | JPL -28 | Horizons |
| Hubble Space Telescope | NORAD 20580 / 1990-037B | Recent CelesTrak or Satcat TLE; Vallado SGP4 |
| Tiangong | NORAD 48274 / 2021-035A | Tianhe core, CSS (TIANHE-1); same validated SGP4 pipeline |
| M87* | M87 nuclear direction, NGC 4486 | RA 12h30m49.42s, Dec +12°23′28.04″, ICRS/J2000 |
| Proxima Centauri b | α Centauri C b | Unresolved host-star catalogue direction; same reference/proper motion/parallax as existing Proxima |

Horizons targets validate identity, origin, ICRF axes, UTC timescale, output units, correction mode, finite values and bounded range. Pointing states are LT+S corrected relative to Earth; speeds use separate geometric Sun-centred states. The phone converts them to the user's topocentric geometric horizon locally, without transmitting GPS coordinates. Hourly states now retain velocities for cubic Hermite interpolation; old position-only disk caches remain readable using the original linear fallback. No extrapolation is permitted beyond a downloaded time window.

JPL mission kernels may contain predictions and have a finite coverage interval. Lack of current data is explicit; no fixed fake RA/Dec substitutes for a moving spacecraft. Satellite providers publish recent elements rather than arbitrary-date ephemerides. Both new satellites validate fixed NORAD/international IDs, TLE checksums, epoch, inclination and sane propagated state before use, with the existing 72-hour validity maximum. Two-hour satellite network limits and provider-specific HTTP stops are retained. All new remote objects participate in the ten-minute catalogue refresh and explicit refresh.

M87* is a nucleus direction and approximate distance scale, not a measured evolving 3-D worldline. The 55-million-ly distance and (6.5±0.7) billion solar mass reference follow EHT/ESO. Its Schwarzschild-equivalent horizon scale is labelled as the same model estimate used for other black holes, not as the ring's measured diameter. Proxima b is not resolved from its star; its apparent daily path is the host direction rotated into the observer's horizon. Its 11.2-day orbital period is a reference from NASA, not used to fabricate a spatial orbit. Unknown diameter, pressure, day/night temperatures and current spacecraft mass remain unavailable.

## Primary sources

- [JPL Horizons manual](https://ssd.jpl.nasa.gov/horizons/manual.html), including spacecraft prediction/coverage limits.
- [JPL Horizons API](https://ssd-api.jpl.nasa.gov/doc/horizons.html); captured actual responses in app/src/test/resources/catalog-142.
- [JPL small-body lookup](https://ssd.jpl.nasa.gov/tools/sbdb_lookup.html#/?sstr=99942): Apophis equivalent diameter 0.340 km and 30.56-hour reference rotation, also present in the Horizons data sheet. No invented asteroid mass or temperature.
- [NASA Proxima Centauri b catalogue](https://science.nasa.gov/exoplanet-catalog/proxima-centauri-b/): unresolved radial-velocity planet and reference 11.2-day period; estimated radius is not treated as measured.
- [ESO/EHT M87* image, coordinates and attribution](https://www.eso.org/public/images/eso1907a/).
- [ESO/EHT first M87* results](https://www.eso.org/public/news/eso1907/): approximate distance and black-hole mass.
- [CelesTrak general perturbations data](https://celestrak.org/NORAD/documentation/gp-data-formats.php).
- [Hubble record](https://www.satcat.com/sats/20580) and [Tianhe record](https://www.satcat.com/sats/48274), checked on 2026-10-16. The CelesTrak endpoint timed out on the verification network; the independent public source provided fresh valid records.
- [NASA Hubble design](https://science.nasa.gov/mission/hubble/observatory/design/): 13.2-m spacecraft length.

## External images

All seven new archival images are external WebP quality 90, method 6, at 2048×1024. Photographs preserve proportions with padding rather than stretching them into equirectangular maps. Images remain flat in the viewer. M87* is the credited EHT radio reconstruction, not an optical photo. The complete 27-file manifest, source links, exact credited authors and checksums are in textures/celestial-textures-v1.3. Older twenty image bytes and release URLs are preserved. No new image is bundled in the APK. JUICE, Tiangong, 3I/ATLAS and Proxima b use symbolic illustrations; no unverified photo or invented exoplanet surface was added. An unavailable download also retains a symbolic fallback and does not block calculations.

## Verification

Independent JPL observer tables use the synthetic site 45°N, 9°E, 100 m and 2026-10-16 12:30 UTC, plus Apophis during the 2029-04-13 close approach. Azimuth/elevation must agree within 0.02° and range within 10 km. Tests also cover real daily sky motion, remote refresh inclusion, missing/expired data, wrong-body/wrong-centre rejection, new SGP4 satellite propagation/identity/fallback and unresolved-direction behaviour. Frozen fixtures are test-only and never substituted for live runtime data. Full compilation, tests, lint and actual distribution results are reported separately.
