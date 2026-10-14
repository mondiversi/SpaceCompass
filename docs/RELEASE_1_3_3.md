# Space Compass 1.3.3 release

Release dated 2026-10-14. Android/update version 1.3.3, version code 31,
application ID `me.mondiversi.spacecompass`, minimum Android API 26.

## Main scene and control placement

Landscape capture is one stable bottom-right overlay outside the animated panel
layout; the details column reserves its measured height. Portrait capture retains
its position in the sky. Collapsed landscape panels use a translucent central
title/menu between measured corner controls. Projection and label exclusions use
the actual optical frame and include the floating title region.

Floating controls retain 8 dp gaps, with 14 dp preview top/side insets. The
Observatory and daily-path circles use a 13 dp visible gap to give names more room.
The original control glyph sizes and 48 dp island action bounds are retained.

## Reticle, object outlines and colours

The reticle is always white. The shared focus detector highlights the current
object's existing outline; selection uses that same outline in the theme accent.
Photos and panoramas draw selected current-object outlines at the existing radius,
without the former extra outer circle. Ordinary orbital-point selection rings,
frozen export selections and the inspected target remain intact.

Day accent is #1B7E9F, slightly lighter than #176B88; night accent remains #55C8DB.
White text on the daytime accent has a 4.63:1 contrast ratio. Form controls, links,
volume, main controls and exported selection tints use the shared theme accent.
Selected-point projections are reused by the scene interaction layer.

## Verification and distribution

Validation includes debug/release compilation, JVM tests, Android-test source
compilation, release lint, twenty-language key/placeholder/encoding parity,
source-secret scan and pinned texture manifest/hash checks. Native phone checks
are recorded separately; the connected instrumentation suite is not executed.
Earlier native checks cover the functional changes before the version/news-only
rebuild; final signed-release checks verify the version, news and capture previews.

The APK keeps the distribution certificate and 16 KiB native alignment. Its signed
update index pins package, version, size and checksum. The optional information
archive follows this tagged source. Texture pack 1.1 and its eighteen 2048 x 1024
WebP images are unchanged, and older public release assets remain available.
