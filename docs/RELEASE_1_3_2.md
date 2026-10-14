# Space Compass 1.3.2 release

Release dated 2026-10-14. Android/update version 1.3.2, version code 30,
application ID `me.mondiversi.spacecompass`, minimum Android API 26.

## Scene and point callouts

The full scene uses the existing optical frame to extend curves, points and object
images behind the translucent main islands. Foreground hit targets and the reticle
remain within the free sky area. Passive background objects cannot intercept
island actions, and the orbital paths are drawn once. Projection regression tests
cover portrait/landscape, RTL, sensor/display rotations and camera optics.

Only the reticle-focused point opens a callout. Clock and T± occupy separate columns,
and only named/current events have a heading. Existing pins, haptics, active target
selection and frozen exported selections remain unchanged.

## Captures and Observatory

Previews share the compact page toolbar with Back and Export actions, translucent
surface and explicit theme foreground. Side controls start directly below it,
with zoom on the left and labels/location/direction on the right. Direction remains
absent from real-camera photos. Label and location states use the app accent.

Photo and panorama images include a colour/name legend below their complete scene.
Photo metadata keeps a larger font and wraps in a dedicated opaque black band above
the entire lens frame. Canvas translation keeps the original optical dimensions,
zoom, angular scales and selected-point projection intact. The same renderer is used
for preview and both language/unit export profiles.

The first Observatory section is now Name and type in all twenty languages.
The in-app latest-news summaries are refreshed without modifying credits or licences.

## Verification and distribution

Validation covers debug/release builds, 972 JVM tests, Android-test source compilation,
release lint, translation key/placeholder/encoding parity, public-source secret scan
and image manifest/checksum consistency. The connected instrumentation suite is not
executed. Recent native phone checks cover both capture modes and orientations,
scene visibility through islands and retained island actions; final installed
release checks confirm the displayed version and new Observatory heading.

The APK retains the distribution certificate and 16 KiB native alignment.
Its signed update index pins the exact version, package, size and checksum.
An optional information archive matches the tagged source. Texture pack 1.1 and its
eighteen 2048 x 1024 WebP maps are unchanged; legacy public assets remain available.
