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
`SpaceCompassAmbientMusicPlayer` prepares the local Ogg asynchronously, loops it,
smooths volume changes, requests Android audio focus only while resumed, and releases
the track and focus when the app is backgrounded, disabled or muted. Transient focus
loss pauses the track; permanent loss or denial does not repeatedly steal focus.
Saved enable/volume choices remain intact during interruption. No background service,
new permission, network request or additional playback library is introduced.

The pure `SpaceCompassAmbientMusicPolicy` has tests for initial defaults, restored
preferences, foreground/focus transitions, mute, denial and late callbacks. Real-device
checks separately verify media playback, background silence and persistent settings.

Quiet Orbit is an original 96-second stereo ambient loop: slowly overlapping warm
chords, quiet sustained upper tones and diffuse reflections, without drums or vocals.
Integer-period oscillators and circular envelopes/reverberation preserve the loop
seam. The asset is Ogg Vorbis at 44.1 kHz; only this compressed file enters the APK.
No external samples or third-party recording licence is required. See ASSET_CREDITS.md.

To reproduce it, install NumPy and imageio-ffmpeg and run
`python scripts/GenerateAmbientMusic.py space_ambient.ogg`. An existing FFmpeg may be
supplied with `--ffmpeg`; it is only a development tool. The script also creates an
uncompressed master and a size/hash/decoded-loop verification report outside the APK.
