# Background music

General places the music island directly below Theme in portrait and landscape.
The standard island heading, activation checkbox, descriptions and slider use the
existing Space Compass settings geometry and day/night colors. All four new strings
are present in the twenty interface languages; the percentage follows the selected
number format. Activation and volume show the existing short settings notification.
The disabled island shows only its activation heading. The divider, description and
volume controls reveal/collapse together over 300 ms with the same top-anchored
easing/fade as UVIR. Scenario toggle islands use the shared transition too; exiting
controls lose touch and accessibility actions immediately.

Fresh installations have music enabled and volume at 20%. A saved disabled choice
remains disabled after updates. The volume slider uses ten-percent steps, matching
the UVIR slider. Preferences store the
enabled choice and completed volume gestures; dragging previews volume immediately
without writing each animation frame to disk. Leaving the settings island clears
uncommitted preview volume. The device media volume is never modified.

`SpaceCompassAmbientMusic` owns one activity-scoped player for all app pages.
The existing pure policy still controls foreground, saved activation/volume, audio
focus, mute and headphone removal. A serialized `SpaceCompassAudio` worker streams
floating-point stereo PCM through Android AudioTrack; nothing is synthesized on the
UI thread. Stop/pause atomically cancels the session, pauses/flushes its native output
and abandons focus. Late preparation/failure callbacks cannot restart a stopped or
superseded session. Synthesis phase and reverb state persist across ordinary pauses;
resuming smoothly raises gain without changing device media volume.

The original Mondiversi website space score is synthesized continuously: the same
four MIDI-note chords begin every 19 seconds with 26-second pad envelopes (4.8-second
attack and 7-second release), paired gently detuned sine voices, and eight sparse
bell notes every 8.5 seconds. Chords overlap rather than restarting a recorded loop.
Equal-power stereo panning, a 2300 Hz low-pass with Web Audio's 0.35 dB Q, 70% dry/
40% wet mix and stereo-linked soft-knee compression preserve the website's character.
The 3.6-second power-2.8 stereo noise impulse is generated locally once and applied
with partitioned FFT convolution. Its noise is seeded for reproducibility; the
website's random impulse and browser-specific DSP mean the waveform is not claimed
bit-identical. There is no music file, download, sample bank or extra audio library.

The oscillator/effect engine works at 12 kHz because the score is concentrated below
1 kHz and the filter cutoff is 2300 Hz. Reusable FFT/voice/sample buffers bound work
and memory. Streaming linearly upsamples to supported 24/48 kHz PCM output, pre-fills
one block and uses a three-block-or-larger buffer. Actual device checks report render
cost and AudioTrack underruns separately; this is not a measured battery-life claim.

Six numerical JVM regressions exercise complex FFT round-trips, convolution against
a direct time-domain reference over partition boundaries, independent decaying stereo
tails, musical pitch, finite/continuous overlapping output and invalid/muted gain.
Existing policy tests cover saved preferences and focus/lifecycle transitions.

The original 96-second Quiet Orbit Ogg was removed from current APK resources.
Historical releases retain their recorded asset and attribution; the earlier
`scripts/GenerateAmbientMusic.py` remains a historical composition/reproduction tool.
The current reproducible synthesis is the `SpaceCompassAmbient*` Kotlin source,
distributed under the project's GPL-3.0 license with no external samples.

Implementation references: [Android AudioTrack](https://developer.android.com/reference/android/media/AudioTrack)
and the [Web Audio filter/convolution specification](https://www.w3.org/TR/webaudio/).
