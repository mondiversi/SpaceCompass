# Space Compass 1.2.0

Identity: version 1.2.0, version code 21, package me.mondiversi.spacecompass,
tag v1.2.0. Android 8.0/API 26 or newer. Updating retains the project signature,
preferences, selections and downloaded maps.

This release includes the local improvements since 1.1.3: 35 public catalog
objects, Titan ephemerides and axis orientation, flat archival deep-sky photographs,
collapsible information panels, consistent current daily-path positions, a single
priority status notice, solar-phase weather lighting and refined capture controls.
Its six news summaries are translated across all 20 supported language catalogs.

The app pins image pack 1.1 (`celestial-textures-v1.1`), containing eighteen
2048 x 1024 WebP images. It never chooses a graphics version through GitHub's
latest-application endpoint. Downloads are individual, limited to two concurrent
requests, verified against embedded byte counts, SHA-256 and dimensions, then
published atomically for offline use. Matching old cache files are verified and
imported without redownloading; failed imports preserve the old copy. Old release
URLs and assets remain available for earlier APKs.

Planet maps are external to the APK; its star atlas and original fictional LV-426
map remain bundled. The information and graphics ZIPs are optional inspection
archives with manifests and credits. Runtime GPS, weather and ephemeris caches,
private signing material and phone screenshots are excluded from publication.

Architecture keeps Titan calculations, image verification/storage, archival
photograph presentation, reference tables, preferences and shared controls in
separate modules. Filename lookup is indexed; map verification/migration/download
work runs on the IO dispatcher. No celestial algorithm or preference is reset for
release preparation.

Release gates: debug/release builds, 757 JVM tests, Android test-source compilation,
release lint without errors, signature/version/16 KiB alignment checks, hosted
Android checks, focused physical-phone review and anonymous download validation.
Connected Android UI instrumentation is compiled but its full suite is not run on
the personal phone. Exact final results and file sizes are in release-metadata.json;
SHA256SUMS.txt covers all application-release assets. The graphics pack is published
and verified before the application, and is never marked as the latest app release.
