# Space Compass 1.2.4 release

Maintenance release dated 2026-10-09. Android/update version 1.2.4, version code 25,
application ID `me.mondiversi.spacecompass`, minimum Android API 26.

The original Mondiversi website space score is synthesized continuously on the
device. Overlapping pad envelopes, sparse bells, locally generated stereo reverb
and bounded reusable FFT buffers replace the recorded Ogg. Saved activation,
low initial volume, foreground/audio-focus handling and offline playback remain.
See [the music design and credits](AMBIENT_MUSIC.md); no sample library, download,
additional dependency, service or permission is introduced.

Main-view status actions follow their message in one paragraph. The text uses the
normal day/night foreground and links use the theme primary blue/cyan. Magnetic
interference, calibration and outdated orbital data retain orange. Messages have
localized sentence endings; actions remain unpunctuated. The stale-data message
is generic rather than naming a satellite and its age.

The in-app news resource files remain byte-for-byte unchanged in all twenty
languages, as requested. Existing celestial texture pack 1.1, its eighteen pinned
WebP files, individual URLs and all historical release assets remain valid.

## Validation and distribution

Required debug/release builds, 801 JVM regressions, Android instrumentation-source
compilation and release lint are recorded in release metadata. Connected
instrumentation is not claimed as executed. Earlier native Android status fixtures
verified the day/night colors and paragraph layout; music device checks verified
focus, saved choices, foreground transitions and continuous output without underruns.
Device performance measurements are not battery-life measurements.

The signed APK uses the existing distribution certificate, supports verified 16 KiB
alignment and excludes temporary preview activities, private signing material and
recorded audio. The locally signed update index specifies the exact APK URL, size,
checksum, code and minimum API. Optional information/graphics ZIPs are reference
archives; the app still downloads planetary images individually when needed.
GitHub-hosted checks, public-source archive and anonymous asset/hash checks are
recorded separately in the final release validation report.
