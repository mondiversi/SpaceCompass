# Space Compass 1.4.2

- Add eleven trackable catalogue objects: New Horizons, James Webb Space Telescope, Parker Solar Probe, 99942 Apophis, 3I/ATLAS, Europa Clipper, JUICE, Hubble Space Telescope, Tiangong, M87* and Proxima Centauri b.
- Use verified NASA/JPL Horizons identities for seven moving targets, with apparent geocentric pointing, independent heliocentric motion and velocity-aware interpolation. Preserve position-only legacy caches.
- Track Hubble and the Tianhe core of Tiangong with validated current NORAD elements, provider fallback and SGP4/TEME propagation. Do not extrapolate beyond the existing element-validity window.
- Add an asteroid filter, scientific designations and explicit catalogue-direction notes for M87* and unresolved Proxima b. Unmeasured physical quantities remain unavailable.
- Pin external graphics pack 1.3: 27 individual quality-90 WebP files, including seven new verified archival images. Keep all celestial imagery outside the APK, preserve matching offline caches and retain earlier pack URLs.
- Refresh news and explanatory strings in twenty languages. Stop attaching duplicate complete texture ZIPs; safely reuse release drafts instead of creating duplicates.

## Download

Install `space-compass-1.4.2.apk` over the existing app. Android 8.0 or newer; version code 39, unchanged package and distribution certificate. Settings and cached data are preserved. The signed update index pins the exact APK.

## Graphics

The APK pins `celestial-textures-v1.3`, containing 27 individually downloadable WebP files at quality 90/method 6. Seven new archival images show New Horizons, JWST, Parker Solar Probe, Apophis, Europa Clipper, Hubble and the EHT reconstruction of M87*. Previous twenty image files remain byte-identical. JUICE, Tiangong, 3I/ATLAS and Proxima b use symbolic placeholders. No new celestial image is inside the APK. No complete texture ZIP is attached to either release; source credits, manifest and checksums accompany the individual images.

## Tracking limits

- JPL mission trajectories are navigation predictions, not live telemetry. Availability is limited by each mission's actual Horizons coverage. Missing or out-of-window data never produce a fabricated position.
- Hubble and Tiangong use fresh elements with the established 72-hour maximum age and six-hour future-epoch allowance. Current providers cannot supply arbitrary historical/future satellite scenarios. Their path is one full revolution, as for existing Earth satellites.
- Proxima b follows its unresolved host-star direction with proper motion and parallax. Distance is to the stellar system; no measured three-dimensional planetary orbital location, surface or atmosphere is implied.
- M87* follows the catalogue direction of the galactic nucleus. Its roughly 55-million-light-year distance and 6.5-billion-solar-mass black-hole mass are reference estimates, not live 3-D tracking. The EHT image is a radio reconstruction.
- Daily sky paths are apparent motion in the observer's sky, not a physical orbit around Earth for probes or distant objects. Compass indication does not imply naked-eye visibility.

## Verification

Regression tests include independent JPL topocentric reference positions at a synthetic site, an Apophis 2029 close-approach comparison, SGP4 identity/expiry and source fallback checks, and daily paths for the new remote targets. Build, full JVM suite, Android-test compilation, release lint, translations, image pins, APK signature and public downloads are recorded in release metadata. Device installation and connected instrumentation are reported separately.
