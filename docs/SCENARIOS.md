# Scenario files

The Scenario toolbar exports and imports one `.spacecompass` configuration file.
Saving uses Android's document picker, and sharing grants temporary read access
to the same file through the app's existing FileProvider. Importing leaves the
user in Scenario, with the editor rebuilt from the imported values.

Version 1 uses UTF-8 Java Properties with `format=space-compass-scenario` and
`version=1`. The file contains its name, configured coordinates and altitude in
metres, IANA time-zone IDs, configured civil-moment representation, all five
independent override flags, automatic position metadata, the clock-advance
option, the signed time-speed setting and its enabled flag, and the resume
policy (`RESTART`, `ACTIVE`, or `ALWAYS`). Inactive configured values are retained.
Numbers and booleans are locale independent; application units and language are
not changed by an import.

No live-clock anchor, elapsed counter or runtime checkpoint is exported.
Importing validates the entire bounded file before changing preferences, clears
the previous clock session, and starts from the configured values. A moving
clock offset is calculated anew at import. The time stored in a file does not
advance while that file is archived. The selected resume policy applies after
import, including across subsequent app and device restarts.

Readers accept at most 16 KiB and reject unsupported schema versions, missing
or invalid fields, nonfinite/out-of-range coordinates and heights, unsupported
time zones, dates outside the app's supported range, and unsupported clock
rates or policies. A rejected file does not change the current scenario.
