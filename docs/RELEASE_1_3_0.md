# Space Compass 1.3.0 release

Release dated 2026-10-12. Android/update version 1.3.0, version code 28,
application ID `me.mondiversi.spacecompass`, minimum Android API 26.

## Orbit selection and exports

One main-page selection owner retains multiple pinned points across orbits and
the daily path. Sky points receive theme-aware rings and persistent labels with
alternating time/T± captions. Daily rows toggle the same pins. The clear-points
action removes the selection together; captures freeze the pins at the shutter.

A shared export page handles images, CSV and readable TXT from captures, one-body
daily paths and the Observatory. Each opening starts with file choices empty;
only the scientific/selected formatting profile persists. Save/share returns to
the retained source view. Selected and key points are interleaved by absolute
timestamp, preserving current rows, repeated daylight-saving hours and all
hourly slots. Observatory exports support current and rotated images separately.

Complete disclosure includes available place, coordinates, accuracy and altitude.
Area-only disclosure includes region/country, without a city or precision fields.
Hidden disclosure uses UTC throughout the caption, point labels and table times.
Capture exports initialize disclosure from the preview. Each daily-path export
initializes it to area only; an export-specific override is not persisted.
Disclosure changes only presentation, never raw geometry, selections or T±.
Missing coarse geocoding never falls back to a precise address. Astronomy angles
can still constrain the observer area; disclosure is not anonymization of the sky.
JPEG variants are newly encoded without inherited GPS EXIF. Caches are separated
by disclosure and formatting so a restricted export cannot reuse a precise file.

## Observatory and presentation

The Observatory groups identity/classification, physical properties, stellar data,
atmosphere and qualified core temperatures, orbit data and spatial context.
Existing references, uncertainty notes, astronomical designations and licences
remain attached to the typed data and exports. Missing science is not fabricated.

Camera and panoramic images show azimuth on both horizontal edges and elevation
on both vertical edges. Camera ticks follow the actual exposure geometry, zoom
and roll, including narrow fields. A compact translucent heading shares its
typography across capture types; the panorama top degrees stay near the grid.
Label policies cover all labels, objects plus selected points, selected points
only and none. Floating controls are opaque, and main information cards use the
same day/night island surfaces as settings. Scrolling, spacing and position
choices are more compact; the main toolbar retains its translucent background.

## Verification and distribution

The release procedure builds debug/release APKs, runs JVM tests, compiles Android
instrumentation sources and runs release lint. Native phone checks verify the
installed identity and six new Italian news items; earlier local feature checks
are identified separately. Connected instrumentation is not represented as run.
Translation keys and format arguments are audited across all twenty catalogs;
image pins, source exclusions and module sizes are verified before publication.

The APK keeps its distribution certificate and 16 KiB native alignment. The
signed update index names this exact version, APK size and checksum. The optional
information archive reflects the tagged source. All eighteen WebP maps, optional
graphics archive and pinned celestial-textures-v1.1 URLs remain unchanged and
available to older APKs. Install over the existing app without clearing its data.
