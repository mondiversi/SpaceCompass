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
