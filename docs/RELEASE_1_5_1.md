# Space Compass 1.5.1 release review

Application `me.mondiversi.spacecompass`, version/display **1.5.1**, code **51**, minimum API 26.
Update with the existing distribution certificate; settings, selections and caches are retained.

## Scope

1009 catalogue entries, including eleven clearly identified fictional references and all 88 constellations.
Independent science-fiction/type filters, qualified approximate ranges, Observatory travel-time comparisons,
portable named scenarios, cached place labels and one settings confirmation on leaving each changed page.
Current scenario remains a popup with 40/60 label/value columns, eight-dp icon gaps and no row dividers.
Its editor is titled Scenario. Automatic terrain altitude/time zone and optional manual overrides remain independent.

## Graphics

Collection 1.7: 184 full images and 184 128×128 previews, WebP quality 90/method 6.
Full files total 42,430,882 bytes; previews total 787,834 bytes.
The complete logical collection is 43.74 MB (41.71 MiB; 43,735,950 bytes) including metadata.
The incremental release has 24 assets: ten new images, ten previews and four metadata files,
4.98 MB (4.75 MiB; 4,984,704 bytes). Existing resources retain immutable pack-1.6 URLs, hashes, dimensions and cache compatibility.
New maps are 2048×1024, new previews 128×128. Illustrative provenance remains in per-file credits.
No complete texture ZIP and no new celestial image in the APK. The information ZIP is an optional reference snapshot.

## Verification

Validate generator reproducibility, every image/preview hash and dimension, exact per-release pins,
all twenty translation key/placeholder sets, debug/release compilation, JVM tests, Android-test source compilation
and release lint. Connected instrumentation is not executed. Validate APK identity, certificate, 16-KiB alignment,
non-debuggable/test-only flags and the embedded source revision. Check physical-phone installation,
public graphics downloads, signed update metadata, exact public source tree and hosted Android checks separately.
Publish and verify graphics before the matching application; preserve old assets/URLs and avoid duplicate drafts.
