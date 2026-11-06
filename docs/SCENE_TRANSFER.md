# Location and Scene transfer

Location management transfers only the observer configuration: position, altitude,
time zone, date, time, speed and resumption. The default checked export option
completes automatic parameters using one effective observer instant. Unchecking it
retains automatic fields; an entirely automatic configuration cannot then be saved.
Missing GPS/altitude data is reported as pending rather than invented. A complete
snapshot does not drift as the export name, profile or checkbox is edited.

The main menu orders Units, Export, Load, Tutorial. Its new Scene document contains
checked live bodies, the active body, enabled comparisons and Personal Moments,
selected orbit points, antipode visibility, celestial guides, the 0–24/+24h mode,
weather visibility and camera-object translucency. Location is included by default,
using the same complete effective observer snapshot. Its automatic-parameter
checkbox is aligned with Include Location and always visible. It is disabled with
the standard muted style while Location is excluded, and retains its in-page choice.
It defaults to checked on each new export and can be unchecked to retain automatic
observer fields instead of fixing
their current values. This also permits an automatic Location without a GPS fix.
Excluding Location ignores that completion choice and omits dormant
observer values and leaves the recipient's observer and clock settings unchanged.
Camera access, favorites, application language, units and unrelated settings stay local.

Location documents retain `.spacecompass` and the existing version 1 observer schema.
Scene documents use `.spacecompass-scene` and a distinct versioned envelope wrapping
the validated canonical scene fields. Both enforce the existing document size limit
and use the shared export profile for readable fields and suggested filenames.
Scene import accepts previous mixed Location/object documents. Location import reads
their observer part only. New Scene files cannot be mistaken for legacy Locations.

Scene import restores the incoming personal-moment checkbox states. Other local
moment definitions remain stored but unchecked. Capacity and canonical-content
validation run before observer/preferences mutation. Local-only definitions are not
deleted. Legacy documents without optional selection sections retain those sections.
The main view observes imported display-switch preferences without restarting its
camera/sensor owners. Runtime clock anchors and elapsed archive time never travel.

The JVM transfer matrix and dedicated Scene tests cover typed round trips, automatic
and partial observer values, missing fixes, malformed/oversized inputs, legacy
compatibility, empty scenes, independent dated objects, selection isolation, local
moment-library capacity and both export profiles. Isolated Android tests cover the
menu, default and alternate export options in day/night themes, edited names and
actual FileProvider/content-resolver reads. Execution results are reported separately
from compilation; no test needs the user's saved Location or celestial selections.
