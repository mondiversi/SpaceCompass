# Space Compass 1.3.8 release

Maintenance release dated 2026-10-15. Android/update version 1.3.8, version code 36,
application ID `me.mondiversi.spacecompass`, minimum Android API 26.

## Changes

- Add compact symbols before rise, culmination, set, minimum and current-position names in daily paths, including satellite-pass events and coincident events. Preserve hourly rows and lunar-phase icons.
- Emphasize the complete key/current row: name, clock/T±, azimuth and elevation use bold text. Ordinary hourly rows retain their normal weight, and selections retain their existing colours.
- Use a plus sign for culmination and a minus bar for minimum consistently in the daily list, live orbit markers, photos, panoramas and selected/exported image variants.
- Make the shared photo/panorama title bar fully opaque using the standard page palette. Match the daily-path title bar to the Android status background in both themes, retaining the white light-mode table.
- Preserve all twenty in-app news files byte for byte from 1.3.7, together with translations, credits, preferences, selections, signing identity and immutable image pack 1.1.

## Implementation and compatibility

DailyPathEventIcons draws decorative native vectors alongside the localized point
name: rise/set triangles, culmination plus, minimum bar and current target. The
existing name supplies accessibility semantics. Caption and angle columns share
the key/current emphasis condition, including coincident events and satellite
pass rows. Selection keys, point times, astronomy and lunar phases are unchanged.
All main and capture marker renderers now draw the same culmination plus; camera,
panorama and selected-point image exports share the existing native helper.

Capture previews use the standard fully opaque page background for their title
bar, independently of image content. Daily paths use the same palette for their
title while retaining the white light-mode table. Both use the shared dark neutral
at night, matching the Android status area without deprecated status-colour APIs.

## Silent in-app update and verification

All twenty info_news.xml files and all other resources remain unchanged from
1.3.7. App version labels advance through BuildConfig; no new news or credits
are written. The optional information archive is refreshed for this exact source.
Graphics pack 1.1 retains all eighteen pinned WebP images, URLs and hashes.

Required validation: debug/release compilation, 1,061 JVM tests, Android-test
compilation, release lint, localization parity/format/encoding checks, source
privacy audit and verified image pins. Native icon/capture and day/night palette
checks were performed before the release rebuild; final version, retained news
and bold rows are checked separately on the signed APK. Connected instrumentation
is not run. Private native UI captures and observer coordinates are not published.

The distribution certificate, data-preserving installation and 16 KiB native
alignment remain unchanged. The signed update index pins version, package, size
and SHA-256. Earlier application and graphics releases remain available.
