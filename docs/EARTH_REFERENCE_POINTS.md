# Earth-centre and zenith guide crosses

The Earth centre is a reference point, removed from the selectable object catalog
on 2026-10-07. The ordinary catalog now contains 30 objects; the independently
hidden fictional object is unchanged. Counts, filtered master selection, ordering
and refresh all derive from this catalog, so the retired reference cannot appear.
Stored Earth-centre checks are discarded while restoring all remaining checks and
a valid active object. If it was the only checked item, the selection stays empty.
The obsolete ID is also removed from stored preferences; no other preference is
reset. Restored instance-state names are filtered through the same catalog.
The legacy enum ID/calculation remain internal for compatibility and numerical
regressions, with no selectable catalog entry.

The enabled celestial-guide layer now adds two crosses in the same white outlined
style as its north/south pole crosses: the Earth centre and the local zenith.
The centre uses a WGS84 geocentric direction from observer latitude and altitude.
It is slightly displaced from the ellipsoid-normal nadir at intermediate latitudes,
so it is not falsely labelled as exactly -90 degrees. The zenith is exactly local
ENU +Up, at +90 degrees. Neither point has object facts, thumbnails or a daily orbit.

Live virtual/camera views and panorama/photo rendering share the same point model
and cross renderer. Points in photographs follow the actual camera lens/attitude
and appear only in its field. A vertical has no meaningful azimuth; in a full
panorama it is anchored at mid-width on the top/bottom angular edge, independent
of the selected center. Captures freeze the names in the chosen/international
presentation. Zenith is translated in all twenty languages; Earth centre reuses
its existing translated name. The existing guide switch controls the crosses;
the existing capture-label toggle hides their captions while retaining geometric
marks, exactly as it does for pole/reference guides. Defaults are unchanged.

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
