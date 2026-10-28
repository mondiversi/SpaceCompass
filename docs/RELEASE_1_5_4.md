# Space Compass 1.5.4 release review

Application `me.mondiversi.spacecompass`, version/display **1.5.4**, Android code **54**, minimum API 26.

## Scope

Catalogue toolbar filters use a funnel and sorting uses paired up/down arrows. Category backgrounds
are removed from rows and Type choices; neutral light/dark surfaces retain the existing selection
indicators. Search has a 40 dp outline in the unchanged 48 dp header touch space, shared input/hint
typography, a centered caret and an aligned clear action. All six news summaries are updated in
twenty languages. Existing astronomy, filters, selections, units, scenarios and caches are retained.

## Graphics

Reuse immutable `celestial-textures-v1.7`: 184 full WebP images and 184 independent previews.
The logical collection remains 43,735,950 bytes. No graphics files, manifests, download pins or
individual URLs change, and no complete graphics ZIP is created.

## Verification and distribution

Run debug/release compilation, JVM tests, Android-test source compilation, release lint and translation
key/placeholder parity. Verify the retained distribution certificate and APK alignment before installing
over the existing app. Check the version and news on the physical phone without clearing app data.
Record executed native checks separately from compiled instrumentation; connected instrumentation
is not executed as part of this release. Verify the exact committed source embedded in the
APK, hosted Android checks, anonymous source-tree equality and public asset downloads. Publish
one v1.5.4 release containing the signed APK/index, optional information archive, metadata and
checksums; retain every older application and graphics URL.
