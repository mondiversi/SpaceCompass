# Device testing

Use a disposable emulator for full instrumentation suites. Existing celestial UI
fixtures inject synthetic GPS/orientation and do not need a real location.

Build first. On a physical device, install only after user authorization, using
an archive-preserving update (`adb install -r`) for an existing installation. Never
uninstall or clear data to work around a signature error. Space Compass must be
installed as `me.mondiversi.spacecompass`, never over the UVIR package.

The Gradle project retains test options that prevent uninstalling the target app
after instrumentation or silently uninstalling an incompatible package. Preserve
those options. A compilation of instrumentation tests is not their execution.

Before release, check permission refusal/approximate location, offline and expired
ephemerides, compass reliability, both themes, RTL, accessibility font scale,
rotation, tablet scaling, popup overflow/stability, lunar phase markers and
foreground screen-awake behavior. No OS rotation or screen-timeout preferences
should be modified to simulate app behavior.
