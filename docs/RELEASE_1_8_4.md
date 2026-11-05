# Space Compass 1.8.4 validation

Version 1.8.4 uses Android version code 70 and the existing distribution certificate.

## Changes

The main menu separates Scene transfer from Location management. Scene archives
store selected objects, the active body/comparison, dated paths, active Personal
Moments, orbit points, Antipodes and portable display choices. Location is optional;
automatic parameter completion can be disabled. Location files contain only observer
settings. Legacy mixed documents remain supported through the appropriate importer.
Export checkboxes share compact settings padding and grouped spacing.

## Verification scope

The release workflow builds debug and signed release APKs, runs JVM tests, compiles
Android instrumentation sources, checks translation keys/placeholders in twenty
languages and runs release lint. Compilation is distinct from device execution.
Sixteen isolated transfer/device tests passed during development before the final
automatic-parameter checkbox refinements. The final checkbox behavior and alignment
were checked on a Samsung Galaxy S10e without clearing app data. Those earlier
instrumentation results are not presented as a rerun against this release APK.
Current build outcomes, source identity and artifact checksums are in release metadata.

## Distribution

The APK, signed update index, optional information archive, checksums and validation
metadata use the same source commit. News is updated in all twenty languages. The
existing graphics collection 1.8 and all individual-download integrity pins are
unchanged. No complete texture ZIP or replacement graphics release is created.
