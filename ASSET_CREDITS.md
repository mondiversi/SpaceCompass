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
