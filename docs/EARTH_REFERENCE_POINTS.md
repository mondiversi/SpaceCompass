# Nadir and zenith guide crosses

The Earth centre is a reference point, removed from the selectable object catalog
on 2026-10-07. The ordinary catalog now contains 104 objects; the independently
hidden fictional object is unchanged. Counts, filtered master selection, ordering
and refresh all derive from this catalog, so the retired reference cannot appear.
Stored Earth-centre checks are discarded while restoring all remaining checks and
a valid active object. If it was the only checked item, the selection stays empty.
The obsolete ID is also removed from stored preferences; no other preference is
reset. Restored instance-state names are filtered through the same catalog.
The legacy enum ID/calculation remain internal for compatibility and numerical
regressions, with no selectable catalog entry.

The enabled celestial-guide layer adds nadir and zenith crosses in the same
white outlined style as the north/south celestial-pole crosses. Nadir is exactly
local ENU -Up (-90 degrees), opposite zenith at +90 degrees. It replaces the
former WGS84 Earth-centre guide rather than merely renaming its slightly offset
geocentric vector. Neither vertical reference has object facts, a thumbnail or
a daily orbit. The retired Earth-centre catalogue ID/calculation stays internal.

Live virtual/camera views and panorama/photo rendering share this point model
and renderer. Exact verticals have no meaningful azimuth: panoramas anchor them
at mid-width on the top/bottom angular edge, independent of their chosen centre.
Camera frames show them only inside the actual field of view. Both names are
translated in all twenty languages and captured export profiles resolve the
same strings. The reference-guide switch and caption rules remain unchanged.

## Projection meaning

The equator, tropics and polar-circle samples are unchanged. They are celestial
declination circles/projections around Earth's rotation axis. The pole crosses
remain the celestial rotation-axis directions, not geographic pole locations.
The equator and celestial-pole meanings follow the USNO glossary:
https://aa.usno.navy.mil/faq/asa_glossary . Tropics/polar circles here are custom
sky projections of terrestrial parallel latitudes, not geographic navigation.

Showing the geographic circles would require separate Earth-surface vectors
from the observer, with a finite Earth model and curvature/occlusion policy.
Remote surface points generally lie below the local tangent horizon; navigation
on the surface would use a ground bearing/distance or map. No such layer was
implemented in this change, as the user requested explanation only.

## Validation

Both debug/release builds and Android-test source compilation pass. All 663 JVM
tests pass, including six new catalog/preference/point regressions. They verify
legacy removal, preserved checks/active object, empty selection, ignored retired
events, exact zenith, unchanged celestial-pole axes, normalized geocentre agreement
with Astronomy Engine at both hemispheres and multiple altitudes, azimuth wrap,
vertical panorama anchors at all four centers, and front/behind camera projection.
All twenty language catalogs have 447 translated keys and pass structural checks.
Manual phone evidence and its limits are recorded in VERIFICATION.md and the local
earth-guide-points-validation.json artifact. No connected instrumentation, gallery
save, external sharing, data clearing or GitHub publication was performed.


## Curved circle captions — 2026-10-07

Equator, tropic and polar-circle names/degrees now follow the projected reference
curves in live views and both capture modes. Point/cross captions remain horizontal.
See CURVED_REFERENCE_LABELS.md for shared orbit-text shaping, reserved-space rules,
annotation/export preferences and validation.
