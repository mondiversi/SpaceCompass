# Live sky motion

General contains a Fluidity island directly below Theme. Low requests about 15 Hz,
Balanced about 30 Hz, and High 50 Hz. Balanced is the default compromise for fresh
settings; existing explicitly saved levels are preserved. The choice is saved in presentation preferences;
changing it re-registers only the foreground gravity/accelerometer, calibrated
magnetometer and optional gyroscope listeners, preserving the GPS fix and online/ephemeris caches.
Interleaved callbacks are coalesced, up to 60 orientation updates per second in
High. The device may deliver a different rate. Magnetic north, declination,
sensor freshness/recovery and measured camera-capture attitudes retain their
existing trust rules. No independent gyro yaw replaces the magnetic reference.

The orientation filter uses elapsed monotonic sensor time, preserving the previous
0.35 response at 40 ms while damping faster or irregular samples consistently.
The accelerometer-only fallback also uses elapsed time. Invalid/stale/restarted
poses and screen-axis rotations do not blend stale axes. Ordinary GPS updates
rebase all filtered axes to the new declination instead of clearing motion history.
Sensors remain lifecycle-scoped and stop with the existing background/screensaver
energy-saving policy. No free-running interpolation timer is introduced.

In daily paths, Current always uses the selection background. This visual cue is
independent of explicit point selection and never adds a point to an export.

An optional calibrated gyroscope predicts the small physical rotation between
successive absolute magnetic/gravity poses. The predicted axes are corrected
toward the measured reference with a two-second time constant. Real panning is
carried by the short gyro increments without waiting for that correction. There
is no still/moving threshold that exposes magnetic jitter again during natural
handheld micro-movement. Gyro supplies relative motion, not an independent
absolute north or a free-running trusted orientation.

Gyro samples are timestamped in physical device axes and kept in a bounded
128-sample in-memory history. Only rates available at or before each integration
interval are used. Short intervals use Rodrigues rotations on orthonormal axes;
all display rotations are explicitly mapped to/from physical device axes. The
magnetic/gravity reference continuously corrects gyro bias. Neither celestial
time nor object motion is frozen by the orientation filter.

Missing, rejected, unreliable or stale (over 250 ms) gyro input, insufficient
history or a sensor gap falls back to the existing fast 93 ms absolute filter.
Gyro callbacks alone never publish a pose, establish compass trust or refresh
the gravity/magnetic watchdog. Invalid absolute references, display-axis
changes, re-registration and lifecycle stops reset the gyro history. All three
sensors use the selected Fluidity period and stop in background/screensaver.

Fresh camera-attitude history uses the same filtered axes as live projection,
converted back to physical device axes before shutter-time remapping. A display-
only held resume pose is never inserted into this history or trusted for capture.
Deterministic tests cover handheld magnetic jitter, slow/fast pans, bias correction,
timestamp ordering, fallback/reset, orthonormality, all three Fluidity rates and
live/capture agreement across the four display rotations.
