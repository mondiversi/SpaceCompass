# Space Compass 1.4.10 release review

Application `me.mondiversi.spacecompass`, version name/display **1.4.10**, Android
version code **48**, minimum API 26. A new release uses the existing distribution
certificate and signed update protocol. Installation retains settings, selections
and offline caches.

## Readability and layout

- Object names use normal theme text instead of colored outlined lettering.
  A leading dot retains the exact above-horizon trajectory identity color, even
  below the horizon, with a 0.75 dp theme-text border for contrast. Its diameter
  follows visible letter height and font scaling. The dot stays visible while
  long names truncate only on their trailing edge. RTL is preserved.
- Caption-to-chevron gaps have a shared 12 dp visible minimum, matching the
  neighboring action spacing. The name still opens the catalogue; Observatory
  and Daily path island actions remain available.
- Object, selected-point and scenario-section counters share a borderless solid
  badge, 22 dp minimum (10% above the original 20 dp baseline), bold digits and
  content-aware sizing. Day blue/night cyan backgrounds use white/dark digits.
  The scenario icon retains its independent orange attention accent.
- Daily path reminder slots are 32 dp wide, retaining their 48 dp height, 20 dp
  glyph, existing touch-target support and alarm/calendar behavior. The trailing
  header allowance tracks that width. Every table bell uses the same 1.4 dp base
  stroke. Key-event rows retain their bold/highlighted treatment.
- Three concise news items are refreshed in all twenty interface languages.

## Verification and distribution

Release checks cover debug/release builds, JVM tests, Android-test source
compilation, release lint, translation key/placeholder parity, package identity,
signing certificate, 16 KiB alignment and the embedded source revision. Device
checks and hosted Android checks are recorded separately in release metadata.
Connected instrumentation is not executed. The layout changes were checked on
the existing phone in light/dark themes and portrait/landscape during development;
the final release receives an installation and version/news smoke check when the
device is available, with any limitation recorded explicitly.

The release contains the signed APK, signed update index, optional reference
information ZIP, metadata and checksums. The public tagged source and anonymous
downloads are compared with the local artifacts. Previously published releases
are preserved and draft duplication is checked.

Immutable graphics pack **1.5** is reused unchanged: 56 full WebP images and 56
independent previews, all quality 90/method 6, pinned individually for integrity
and cached offline. No new bundled celestial photograph or complete texture ZIP
is added. The information ZIP is a reference snapshot, not a runtime package.
Astronomy, phases, trajectories, saved point selections, exports and privacy
choices retain their existing behavior.
