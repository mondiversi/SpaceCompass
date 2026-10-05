# Orbit names and panorama capture

The main menu places Capture immediately below Units. A tap freezes the current
observer position, selected catalog set, time, cached remote inputs and available
trajectories. The export computes live positions for that exact instant using
the existing celestial model; it does not rotate the device, fetch new orbital
data or modify any preferences.

The image unfolds all 360 degrees of azimuth from north through east, south and
west, and all elevations from +90 to −90 degrees. It is a rendered sky map with
the app's solar-phase/weather sky palette and illustrative ground. Each selected
object has its usual orbit color, a live marker and a legend entry. Available
daily trajectories retain their samples, hourly/event dots and chronological
direction arrows. Paths below the geometric horizon are dashed. North-seam and
horizon crossings split the strokes rather than introducing false long chords.
Unavailable positions/paths are explicitly marked in the legend.

Object names appear on readable curved baselines near direction arrows in both
the live scene and export. Baselines are cut from the actual projected polyline,
kept upright and omitted when too short, clipped or colliding with another label.
Whole-string Android shaping preserves complex scripts instead of rotating
individual Unicode characters. Paint alignment is explicit so complete names fit.

Rendering and JPEG saving run off the main thread. Output is 4096 pixels wide
(3072 on lower-memory devices), with a 2:1 angular scene and a separate legend.
The header uses the saved number/date/time formats and current timezone offset.
All new menu, status, legend, cardinal and news strings exist in twenty locales.

## Gallery storage and compatibility

Capture writes a 95-quality JPEG to `DCIM/SpaceCompass`, visible as the
SpaceCompass gallery album. JPEG EXIF metadata retains the capture timestamp,
milliseconds, UTC offset and app version. It contains no GPS EXIF tags.

Android 10+ uses MediaStore with a pending row while bytes and metadata are
written, then publishes the complete image. It requires no camera or media-read
permission. Android 8/9 requests legacy write permission only when Capture is
pressed, creates a new file without replacing an existing image and scans it
into the gallery. Incomplete writes are deleted on failure; a denial/failure is
reported without altering preferences or existing photographs.

Platform behavior follows the official [shared-media documentation](https://developer.android.com/training/data-storage/shared/media),
[Canvas text-on-path API](https://developer.android.com/reference/android/graphics/Canvas)
and [ExifInterface API](https://developer.android.com/reference/android/media/ExifInterface).

## Validation

Eight JVM regression tests cover angular coordinates, both directions of the
north seam, simultaneous seam/horizon splits, invalid inputs, true curved
baseline length, upright reversed paths, disconnected runs, polar and DST days.
Three targeted Android tests exercise a frozen live position and rendering,
complete Latin/Arabic/Persian/Hebrew curve labels and a JPEG MediaStore round trip
with metadata and album checks. The storage test deletes only its own image.
Manual device checks and version-specific outcomes are recorded in VERIFICATION.md.
