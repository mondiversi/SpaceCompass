# Space Compass 1.8.7 validation

Version 1.8.7 uses Android version code 73 and the existing distribution certificate.

## Behaviour

- Tap empty sky to show or hide the reticle and floating controls together; double-tap to pause or resume the effective observation clock.
- Resume live time at the actual current instant. Active custom dates, times or non-1× speeds continue from the held instant without overwriting Location settings.
- Keep an orange date, time and UTC offset visible while paused, even with controls hidden. Draw the pause symbol as a theme-coloured vector with the same dark-grey outline as the text. Running Location dates on either side of today retain the date readout.
- Use a single tap to toggle photo/panorama preview controls; title-bar actions remain visible and pinch/drag navigation is preserved.
- Place clock and T together with a middle dot in main-sky, photo and panorama point bubbles. Separate dated comparison headings from their date, wrap long titles and avoid repeated Personal Moment names.
- Update the concise tutorial and in-app news in all twenty languages. Keep the graphics collection unchanged.

Playback is session-only and does not replace saved Location values. A configured
zero speed can be previewed at normal speed without overwriting the configured zero.
Spatial overrides alone use real time on resume. The readout compares civil days in
the Location zone; changing only the hour within today does not keep a moving date
readout visible. Fixed dated comparisons do not themselves trigger the readout.

## Verification scope

The workflow builds debug and signed release APKs, runs at least 1,706 JVM tests,
compiles Android instrumentation sources, checks twenty-language key/placeholder
parity, reproduces generated resources and runs release lint. Instrumentation
compilation is distinct from execution; no connected instrumentation execution is
claimed. Local trial checks verified sky gestures on a Samsung Galaxy S10e before
the release/news-only bump. Final version, news, installed APK identity, hosted CI,
public source and artifact checks are recorded separately in release metadata.

## Distribution

Code, APK, signed update index, optional information archive, metadata and checksums
refer to the same commit. Graphics collection 1.8 and its individual WebP downloads
are unchanged; no new graphics release or complete texture ZIP is required.
