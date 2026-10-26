# Assets and third-party code

The complete source-specific map credits, orientation conventions and scientific
limitations are retained in [the viewer notes](docs/CELESTIAL_VIEWER.md#maps-and-attribution)
and in the localized viewer UI.
The full map attribution and Astronomy Engine MIT notice are also bundled in
`app/src/main/assets/licenses/` for distribution with the APK.

- Sun, Mercury, Venus atmosphere, Mars, Jupiter, Saturn, Uranus and Neptune:
  Solar System Scope / INOVE, Creative Commons Attribution 4.0 International.
  https://www.solarsystemscope.com/textures/
  https://creativecommons.org/licenses/by/4.0/
- Moon: NASA / GSFC / Arizona State University, CGI Moon Kit.
  https://svs.gsfc.nasa.gov/4720/
- Io and Europa: NASA / JPL / USGS Voyager/Galileo SSI global reference mosaics.
- Pluto: NASA / JHUAPL / SwRI, New Horizons Ralph/MVIC global color mosaic.
  https://science.nasa.gov/resource/pluto-global-color-map/
- Astronomy Engine 2.1.19: Don Cross (CosineKitty), MIT license, Gradle dependency.
  https://github.com/cosinekitty/astronomy
- Vendored SGP4 Java implementation: retained license and provenance in
  `third_party/sgp4/LICENSE` and `third_party/sgp4/NOTICE.txt`.

NASA credits do not imply endorsement. Surface maps are reference composites,
not live photographs. Unknown surfaces, spacecraft attitude and the Polaris
model remain explicitly illustrative. No new bitmap artwork was introduced by
the extraction; the launcher and loading artwork are original code-native vectors featuring a ringed planet and compass pointer.


## 1.1.0 asset packaging

The credited Pluto map is resampled to the viewer's existing 2048 x 1024 texture
limit and JPEG-encoded at quality 95 with 4:4:4 sampling. Attribution and longitude
orientation are unchanged. The original fictional LV-426 artwork is packaged as
lossless `celestial/lv426.webp`; decoded RGB pixels and original dimensions are
identical to its previous PNG. No third-party artwork or licence is replaced.

## 1.1.1 asset packaging

The existing maps are encoded as lossy WebP at quality 90, without resizing,
reorienting or changing attribution. Europa retains its smaller original JPEG.
This supersedes the 1.1.0 packaging formats above, including lossless LV-426.
Original source maps remain credited; the Io source checksum in the bundled
notice refers to the original JPEG, not the compressed packaged WebP.

## 1.1.2 downloadable maps

Only the original fictional LV-426 map remains bundled in the APK. The twelve
credited maps above are distributed in the public `celestial-textures-v1` pack:
https://github.com/mondiversi/SpaceCompass/releases/tag/celestial-textures-v1
They retain their dimensions, orientation and source attribution. Reference
pack copies and full credits are retained in `textures/celestial-textures-v1/`
outside the Android asset tree. Verified downloads are stored in private
no-backup app files and reused offline, without being redownloaded on restart.

## Bundled celestial star background

Deep Star Maps 2020: NASA/Goddard SVS, Ernie Wright (USRA), ESA/Gaia/DPAC.
https://svs.gsfc.nasa.gov/4851/
The ICRF/J2000 celestial atlas is display-adapted and bundled as
`app/src/main/assets/sky/starmap.webp` (4096 x 2048, lossless WebP).
Full transformation, brightness limits and source credits are in
`app/src/main/assets/licenses/star-map.txt` and `docs/STAR_BACKGROUND.md`.
The star atlas is distributed within the APK; the planet texture pack is unchanged.

## Io and Europa 2K colour maps

The official NASA VTAD Io and Europa 3D models provide 4096 x 2048 albedo maps.
The original embedded PNGs are reduced to 2048 x 1024 and encoded as WebP at
quality 90. The derivatives use new filenames (`io_2048.webp`,
`europa_2048.webp`) so older pinned app downloads remain valid. Existing private
maps for other bodies and all old release attachments remain unchanged.
North-up orientation and longitude seams match the earlier reference mosaics.
NASA's visualization maps use processed colour and filled/blended coverage;
they are display reference maps, not complete measured surface datasets.
Source models and credits:
https://science.nasa.gov/resource/io-3d-model/
https://science.nasa.gov/resource/europa-3d-model/
NASA Visualization Technology Applications and Development (VTAD);
NASA/JPL/USGS source imagery, no endorsement implied.

## Release 1.2.0 / image pack 1.1

Six new WebP derivatives add Titan (NASA/JPL-Caltech/Space Science Institute),
Sirius and M42 (NASA/ESA/Hubble), Betelgeuse (ALMA/ESO/NAOJ/NRAO, CC BY 4.0),
the Pleiades (NASA/JPL-Caltech/UCLA, WISE) and Andromeda (NASA/ESA/Hubble).
Exact contributors, source URLs, processing, dimensions and hashes are retained
in [the pack credits](textures/celestial-textures-v1.1/CREDITS.txt),
[its manifest](textures/celestial-textures-v1.1/manifest.json) and
[the catalog sources](docs/CATALOG_ADDITIONS.md). All eighteen runtime images
are WebP, quality 90, method 6, 2048 x 1024. Legacy release assets remain intact.


## Original generative background music

The continuous space score reproduces the original Mondiversi website's chord and
bell sequences, envelopes, stereo placement and diffuse reverb. Current Android
builds synthesize it locally using `SpaceCompassAmbient*` Kotlin sources under
GPL-3.0; no third-party composition, recording, samples or external music library
is used. Attribution is bundled in `assets/licenses/ambient-music.txt`.

Quiet Orbit, the earlier original 96-second Ogg, remains credited in historical
releases. Its development source `scripts/GenerateAmbientMusic.py` is preserved;
its recorded audio is no longer included in current APK resources.

## Offline Mondiversi screensaver

The original Mondiversi.me rotating Earth, stars and decorative planetary orbits
are reused in the optional screensaver. Earth day/night imagery: NASA Blue Marble
(https://svs.gsfc.nasa.gov/57730/) and NASA/NOAA Black Marble
(https://svs.gsfc.nasa.gov/79765/), as credited by the source website.
Both 2048 x 1024 WebP derivatives retain orientation, quality 90/method 6 and
source attribution. Source and derivative hashes: docs/SCREENSAVER_ASSETS.json.
No endorsement implied. Scene code is original Mondiversi work (GPL-3.0);
only the two decorative background maps are bundled. Downloadable celestial
viewer textures and the existing image pack are unchanged.


## External texture pack 1.2

Twenty WebP images, including the unchanged original LV-426 illustration and
the SpaceX Roadster photograph, are distributed in `celestial-textures-v1.2`.
No celestial object image is now bundled in the APK. The star atlas and the
separate Earth screensaver maps remain bundled. Earlier image releases and
URLs remain intact for older APKs; verified matching cached maps are reused.
The complete ZIP, image manifest, credits and checksums describe the same set.


## External texture pack 1.2.1

Application 1.4.1 downloads all twenty celestial images from the versioned 1.2.1 release. Image hashes, pixels, quality-90 WebP encodings, credits and licences are unchanged from pack 1.2; consolidated manifest and credit headers now describe the complete set. Older release URLs remain intact. The fictional LV-426 image is external; the star atlas and separate screensaver assets remain bundled.


## External graphics pack 1.3

Application 1.4.2 pins 27 individual quality-90/method-6 WebP files. Seven new archival images of New Horizons, JWST, Parker Solar Probe, Apophis, Europa Clipper, Hubble and M87* retain their source credits, licence links and aspect ratio in the [manifest](textures/celestial-textures-v1.3/manifest.json) and [full credits](textures/celestial-textures-v1.3/CREDITS.txt). NASA images are historical photographs/radar observations; M87* is an EHT radio reconstruction under CC BY 4.0. JUICE, Tiangong, 3I/ATLAS and Proxima b retain code-rendered symbolic placeholders. Earlier images and URLs remain unchanged.


## External graphics pack 1.4

Application 1.4.3 pins 29 individual WebP files at quality 90/method 6. The previous 27 image files remain byte-identical to pack 1.3. New Dawn Ceres (PIA19562, flat archival photograph) and Cassini Enceladus (PIA18435, cylindrical composite) derivatives retain their exact NASA/JPL credits and source links in the [manifest](textures/celestial-textures-v1.4/manifest.json) and [full credits](textures/celestial-textures-v1.4/CREDITS.txt). Twenty-eight files are 2048 x 1024; the existing LV-426 stays 1774 x 887. All images remain outside the APK, individually downloaded and verified; no complete graphics ZIP is distributed.


## External graphics pack 1.5

Twenty-seven additional telescope, spacecraft, galaxy and natural-satellite archive
photographs are external-only WebP derivatives (quality 90, method 6, 2048 x 1024).
Full per-image source pages, contributors, processing and integrity pins are in
`textures/celestial-textures-v1.5/manifest.json` and `CREDITS.md`. M13 uses the
ESA/Hubble and NASA credit attached to its actual image. Old image files and
published pack URLs are preserved. No AI-generated image is presented as observed.

Reference `thumb_*.webp` previews (128 x 128, WebP Q90/method 6) are external
derivatives of these same credited images. Their parent/source, inherited licence
and independent integrity pins are recorded in the external manifest and credits.

## Local catalogue expansion

Data provenance and retained dataset licences: [catalogue/README.md](catalogue/README.md).
External graphics pack 1.6: [full image credits and transformations](textures/celestial-textures-v1.6/CREDITS.md).
No new celestial photograph or thumbnail is bundled in the APK.

The stable 1.6 release contains 174 full resources and 174 separate previews, all WebP quality 90/method 6.
Per-image provenance, licences and transformations remain in the versioned manifest and credits.
All celestial photographs and fictional LV-426 remain external; scientific catalogue text/data and
their dataset notices are bundled and documented in [catalogue provenance](catalogue/README.md).
