# Space Compass 1.2.5 release

Release dated 2026-10-10. Android/update version 1.2.5, version code 26,
application ID `me.mondiversi.spacecompass`, minimum Android API 26.

## Changes

The offline Earth screensaver is inspired by Mondiversi's website. Fresh settings
enable it after five minutes; explicit saved choices remain unchanged. Touch or
Back dismisses it without activating underlying controls. Its retained activity
policy handles focus, lifecycle, keyboard, held gestures and rotation. The globe
stays centered at a stable physical size. Its illustrative lighting follows the
observer/scenario solar height independently of the interface theme. Two credited
2048 x 1024 WebP maps and an isolated renderer are bundled for first-use offline
operation. No location or network bridge is exposed by its native WebView.

Sky, ground, weather and frozen panoramic captures share continuous solar-height
lighting. The model blends night, twilight, the horizon and daylight smoothly;
camera pixels and celestial coordinate calculations are unaffected.

Main controls form mirrored corner groups. Notices are centered below the reticle,
with the existing exclusive data/retry priority. Camera zoom is larger and semibold,
beside capture; its preset list can open above the anchor. A translucent main header
allows trajectories beneath it, while captions avoid controls. The landscape split
is 55/45 where the detail column fits. Object names have more space and symmetric
arrows retain their accessible action targets. The inactive volume track follows
the app's blue/cyan palette, initial music volume is 50%, and waiting messages use
consistent ellipses. All twenty in-app news summaries are refreshed.

## Verification and distribution

The local release checks build debug/release APKs, run 823 JVM regressions, compile
Android instrumentation sources and run release lint. Native phone checks are
recorded separately. Connected instrumentation is not claimed as executed. Existing
screensaver/solar-renderer device checks and current release identity/news checks
are listed with their scope in the release metadata. Automated source review checks
translation-key and format-argument parity, image pins and private-file exclusion.

The aligned signed APK retains the distribution certificate. Its signed update
index includes the exact URL, version code, size and SHA-256, and the bundled native
libraries are checked for 16 KiB alignment. No keys or passwords enter the public
source, APK or metadata. GitHub-hosted checks and anonymous source/asset downloads
are reported separately. Optional information/graphics archives describe this
release; external celestial texture pack 1.1 and older release assets retain their
existing URLs and content. Planetary images are still downloaded individually.
