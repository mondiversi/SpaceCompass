# Space Compass 1.4.11 release review

Application `me.mondiversi.spacecompass`, version name/display **1.4.11**, Android
version code **49**, minimum API 26. A new release uses the existing distribution
certificate and signed update protocol. Updating retains settings, selected
objects and points, and offline caches.

## Split-color orbit keys

- The main island's dot shows the above-horizon identity color on its upper half
  and the existing subdued below-horizon color on its lower half, regardless of
  the object's current elevation. Its thin outline matches the adjacent name.
  The name remains normal theme text; font scaling, arrow spacing, dot visibility,
  trailing truncation, click action, RTL and accessible touch targets are preserved.
- Photo and panorama legends replace line swatches with the same split circles.
  Their diameter follows visible letter height; their outlines use the legend's
  lettering color. Centered legend row wrapping and ellipsis remain available.
  Legends retain their separate footer without altering the optical frame or grid.
- One native circle renderer serves the Compose island and bitmap legends. It
  fills the upper color first, clips the lower treatment to the lower half, then
  draws one inset outline. This avoids a visible antialias gap at the center.
  Paint is remembered for the live dot and reused across legend entries.
- Both capture types and re-rendered image exports share the legend path.
  Astronomy, phases, positions, trajectories, horizon-color policy, selections,
  export formats and privacy behavior are unchanged.
- Three in-app news items are updated in all twenty languages.

## Verification and distribution

Release checks cover debug/release builds, JVM tests, Android-test source
compilation, release lint, translation key/placeholder parity, APK identity,
signing certificate, 16 KiB alignment and the embedded source revision. Device
checks and hosted Android checks are recorded separately in metadata. Connected
instrumentation is not executed. During local development the actual UI changes
were visually checked in light/dark themes and in photo/panorama previews on the
existing phone. Final version/news installation checks are recorded separately.

The release contains the signed APK, signed update index, optional reference
information ZIP, metadata and checksums. Public source and anonymous downloads
are compared with local artifacts. Prior releases are preserved and duplicate
drafts are checked before creation and after publication.

Immutable graphics pack **1.5** is reused unchanged: 56 full WebP images and 56
independent previews, all quality 90/method 6, with individual integrity pins,
downloads and offline caches. No new bundled celestial photo, image package or
complete texture ZIP is required. The information ZIP is a reference snapshot,
not a runtime graphics package.
