# Changelog

## 1.2.4 — 2026-10-09

- Generate the original website space score continuously on-device, replacing the bundled recording.
- Retain saved activation/volume, seamless overlapping voices, foreground/audio-focus behavior and offline playback.
- Place status actions immediately after their message, using theme-aware blue/cyan links.
- Use normal text for routine statuses and orange only for magnetic interference, calibration and outdated orbital data.
- Add localized sentence endings to notices, keep action labels unpunctuated and simplify the satellite warning.
- Preserve the existing in-app news summaries and image pack 1.1.


## 1.2.3 — 2026-10-09

- Distinguish compass calibration, magnetic interference, reduced precision and recovering readings.
- Consistent theme-aware orange notices with no trailing full stops/ellipses across twenty languages.
- Clearer localized 0°/90° pointing-reference glyphs with improved optical alignment.
- Panorama/shutter capture glyph follows camera mode; export chooser says Export image.
- Retain the existing in-app news summaries for this maintenance release.


## 1.2.2 — 2026-10-09

- Original meditative space music available offline, enabled initially at 20% volume.
- Saved music activation and volume, with ten-percent slider steps and foreground/audio-focus handling.
- Appearance renamed General in twenty languages, with a distinct cog icon.
- Music and Custom scenario cards expand/collapse over 300 ms; disabled music hides its controls.
- Clearer pointing-reference icons with straight camera-axis and phone-length arrows.
- Saved audio settings are applied before the initial foreground resume.


## 1.2.1 — 2026-10-09

Maintenance release. The in-app news summaries remain unchanged.

- Compact two-line daily-path captions and theme-aware backgrounds for key events.
- Frozen current/event labels in both capture modes, collision avoidance and clearer colored borders.
- A saved choice between camera-axis and physical phone-length pointing, with camera mode retaining its optical alignment.


## 1.2.0 — 2026-10-08

Space Compass 1.2.0 expands the catalog to 35 objects and improves the live sky, daily paths and capture controls.

- Sirius, Betelgeuse, Titan, the Orion Nebula (M42) and the Pleiades (M45), plus a new Andromeda galaxy image alongside its existing core entry.
- Image pack 1.1: eighteen 2048 x 1024 WebP images, downloaded individually from the exact pinned release and verified before offline reuse. Matching older cached images are reused without another download.
- Collapsible main information panels, reorganized floating controls and a clearer orange Custom scenario indicator.
- Current position included in daily paths, improved selected-point spacing and one priority status notice, with connection retry taking precedence.
- Weather lighting follows dawn, daytime, sunset and night consistently in the live sky and panoramas.
- Refined panorama controls and export margins, capture vibration and compact object-viewer zoom.
- Updated news and complete key/format-argument parity across all 20 language catalogs.

## 1.1.3 — 2026-10-08

- Add an observer/time-aligned bundled star atlas, with sharper panoramic sampling.
- Download verified planet maps on demand and reuse them offline; upgrade Io and Europa to 2K WebP.
- Match weather coverage between the main sky and panoramas; save the weather toggle and camera zoom.
- Refine floating controls, toolbar icons, borderless islands, settings and scenario fields.
- Add compact horizontal object-viewer zoom controls with the initial size as the lower limit.
- Rename the information page to Info & credits and confirm completed object refreshes with a short notice.
- Update release news in all 20 languages and distribute optional information/graphics archives.


## 1.1.0 — camera zoom, catalog search and lighter captures

- Add consistent camera zoom steps, ultrawide lens selection where available,
  pinch gestures and direct level selection alongside +/−.
- Add live object search by localized name or catalog identifier, combined with
  existing type/visibility filters and numeric sorting.
- Include exposure-time direction, inclination and horizontal/vertical field of
  view in camera-photo captions. Keep user-formatted previews separate from the
  persisted international/current-settings export choice.
- Compact floating controls and object-view mode switch, centre both capture
  action columns, and keep capture/zoom above a selected point panel in portrait.
- Match calibration warnings to theme-aware Scenario orange; keep reduced
  precision neutral.
- Separate camera UI and lifecycle controller; memoize camera choices and
  release unfinished bitmap decodes on cancellation or errors.
- Save 4,450,604 bundled asset bytes: cap Pluto at the existing 2048-pixel GPU
  texture limit and encode the fictional texture as pixel-identical lossless WebP.
- Update six concise in-app news items in all twenty languages; retain complete
  localization, credits, offline calculations and saved settings.

Android/display version: 1.1.0; version code: 17. Same distribution certificate.


## 1.0.1 — landscape island layout fix

- Pack natural-height settings and information islands into the shorter landscape
  column to remove unused row space. Portrait layout is unchanged.
- Retain the existing in-app news text and saved preferences.

Android/display version: 1.0.1; version code: 16.

## 1.0 — first stable release

Space Compass 1.0 is the first stable release, bringing together the full sky compass,
camera view, panoramas, scenarios and configurable catalog.

- Track 30 celestial objects, explore daily paths and inspect textured object views
  with qualified physical facts and sources.
- Use type/visibility filters and sorting by distance, mass, diameter, pressure,
  gravity and temperature; unavailable values sort last.
- Overlay orbits on the rear camera or capture a complete 360° panorama. Optional
  reference curves have names following the curves; pole/zenith/geocentre crosses
  remain point references.
- Export manually to the gallery, a file or sharing with current-language/unit or
  international formatting. Labels and location disclosure are configurable; the
  default shows only the general place, without precise coordinates.
- Explore day/night maps and scenarios with independent position, date/time,
  altitude and time zone. Catalog data refresh every ten minutes and on request.
- Keep selections, filters, sorting and settings across restarts. Twenty complete
  interface languages, automatic light/dark themes and adaptive phone/tablet layouts.
- Enable release code/resource optimization and fix API 26–32 simulated-altitude
  compatibility, localized feedback and image timestamp metadata.

Install `space-compass-1.0.0.apk` over the existing app; do not uninstall or clear data.
Requires Android 8.0/API 26 or newer. The visible version is 1.0; Android and the
signed update protocol use 1.0.0, version code 15, for compatibility with prior releases.
The APK uses the same authorized distribution certificate as earlier versions.

Validation: debug and optimized release builds; 669 JVM tests; Android-test source
compilation; release lint with zero errors; twenty-language parity/placeholder/encoding
audit; real Samsung phone smoke checks. Connected instrumentation was not executed.
APK, signed update index and `SHA256SUMS.txt` are included. Source and scientific/image
license notices are available under the matching tag.
