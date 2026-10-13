# Space Compass 1.3.1 release

Maintenance release dated 2026-10-13. Android/update version 1.3.1, version code 29,
application ID `me.mondiversi.spacecompass`, minimum Android API 26.

## Included fixes

Capture snapshots freeze the target active in the main island. The object-and-selected
label mode shows its name, hourly/named/current labels and separately pinned points
from other included orbits. Formatting and privacy variants retain this frozen target.
Selecting a point by touch or accessibility changes only its pin, preserving the
active object. Navigation arrows and explicit catalogue selection still change it.

Explicit point selection and deselection produce one brief haptic cue. Capture and
clear-all actions retain their command cue; refresh, navigation and programmatic
resets stay silent. Android's haptic setting is respected. The clear-points action
uses the same compact corner count as the object selector and disappears when empty.

Photo and panorama captions share one formatter and collision-aware renderer. Named
events and current positions get a title row, followed by clock/T± and then both
angles; ordinary hourly captions keep two rows. Actual line heights participate
in placement. Number, language, time-zone and privacy conventions are unchanged.

## Verification and compatibility

Debug/release compilation, 969 JVM tests, Android instrumentation-source compilation
and release lint are checked. The connected instrumentation suite is not executed.
Earlier native checks cover independent selection, feedback and the count changing
1 → 2 → 1 → 0. Both actual capture previews were visually checked for the title row;
the final signed APK is installed over the existing app and its version is checked.

All twenty catalogs retain 579 translatable keys with matching format arguments.
The in-app news summaries, credit resources and licence files are unchanged; only
the displayed app version advances through BuildConfig. Public release notes and
this maintenance audit describe the fixes separately.

The APK retains the distribution certificate and 16 KiB native alignment. A newly
signed update index points to the exact 1.3.1 APK size/checksum. The information ZIP
reflects the tagged source. Texture pack 1.1, eighteen pinned WebP maps and the exact
optional graphics ZIP are reused, preserving every earlier release asset.
