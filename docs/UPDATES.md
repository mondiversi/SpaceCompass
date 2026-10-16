# App updates

From 0.1.2, Space Compass performs a silent check on each fresh Activity launch,
including an Activity restored in a fresh process. Recreation inside a live process
does not duplicate that check.
Returning from an internal page does not restart the check or the main compass.
The Info & credits page has separate repository-open and manual-update-check actions.
Only a newer compatible release opens the global **Updates available** dialog.
Manual checks report current-version, unsupported-Android or connection results.
All update UI strings are localized in the same twenty interface languages.

## Public release discovery

The app calls the unauthenticated GitHub releases API for `mondiversi/SpaceCompass`.
Drafts are excluded. Public previews are included, because the project currently
distributes preview releases and GitHub's `/releases/latest` excludes those.
Only releases containing `space-compass-update.json` participate. The three newest
semantic versions in the returned twenty-release page are examined, with the highest
supported signed `versionCode` chosen. A release must use a `vMAJOR.MINOR.PATCH` tag.
The monotonic Android version code determines whether an update is newer; displayed
version strings alone cannot trigger a downgrade or a repeated same-version offer.

GitHub publication must retain the exact app/package/signature and increase the code.
The first preview, 0.1.0, does not contain an updater and requires one manual upgrade.

## Signed index

`space-compass-update.json` is a UTF-8 envelope with `algorithm`, `payload` and
`signature`. The algorithm is `SHA256withRSA`; payload/signature use standard Base64.
The decoded payload has this schema (replace example size/hash with the final APK):

```json
{
  "schema": 1,
  "repository": "mondiversi/SpaceCompass",
  "app": {
    "package": "me.mondiversi.spacecompass",
    "version": "0.1.2",
    "code": 3,
    "min_sdk": 26,
    "url": "https://github.com/mondiversi/SpaceCompass/releases/download/v0.1.2/space-compass-0.1.2.apk",
    "bytes": 12345678,
    "sha256": "replace-with-64-lowercase-hex-digits"
  }
}
```

The payload is signed locally with the APK distribution key. The app verifies the
signature against the signing certificate of its own installed APK. Neither a
remote certificate nor an unsigned index can replace that identity. The index must
match the repository, package, release tag, download filename, size and minimum SDK.
Debug builds signed with another key cannot consume the official distribution index.

The existing distribution key is kept outside the repository at
`<user home>/Dropbox/Mondiversi/UvirSigning/uvir-release.jks`, shared with the
owner's UVIR signing setup. The current account's home directory is resolved at
runtime, without embedding a computer's username. If Dropbox is installed
elsewhere, set `MONDIVERSI_SIGNING_DIR` to the private `UvirSigning` directory.
An explicit `SPACE_COMPASS_KEYSTORE` still overrides both defaults.
Both override paths must be absolute.

The private `signing.properties` beside the key contains `storePassword`,
`keyAlias`, `keyPassword` and an optional absolute `storeFile`. A blank `storeFile`
uses `uvir-release.jks` in the same directory. Both Gradle release APK signing and
the update-index signer read this configuration automatically. An ignored
project-local `signing.properties` can override shared values. Optional
`SPACE_COMPASS_KEY_ALIAS`, `SPACE_COMPASS_STORE_PASSWORD`,
`SPACE_COMPASS_KEY_PASSWORD` and `SPACE_COMPASS_KEYSTORE` environment overrides
remain available, but are not required on a new computer.

For a new trusted computer, install the Android/JDK tools, use the updated project
sources and sync the whole private folder, keeping its files available offline.
No manual password setup is necessary. Run `gradlew.bat :app:assembleRelease`
to generate the automatically signed `app/build/outputs/apk/release/app-release.apk`.
Debug APKs retain their development-only signing behavior. The public CI has no
access to the private folder and continues to build an unsigned release.

Restrict Dropbox access and retain a separate protected backup: this folder now
contains everything needed to sign both applications. Never include it in the
repository, source archives, release metadata, logs or public CI. To sign an
update index locally:

```text
java scripts/SignUpdateIndex.java payload.json space-compass-update.json
```

To print only the resolved key path, without displaying credentials or signing
anything:

```text
java scripts/SignUpdateIndex.java --keystore-path
```

APK and update-index signing must retain the existing certificate. Manual
Android SDK `apksigner` usage remains optional; never embed a password directly
in a command line.

Standalone resolver checks, requiring no credentials or signing operations:

```text
javac -d build/signing-path-test scripts/SignUpdateIndex.java scripts/SpaceCompassSigningPathTest.java
java -cp build/signing-path-test SpaceCompassSigningPathTest
```

Add `--live` to check that the real Dropbox-only configuration opens the existing
key with the expected public certificate. This reads the private configuration
without printing it and does not sign or publish an asset.

## Download and Android installation

Downloads run off the UI thread and survive internal navigation. Requests retain
platform HTTPS certificate/hostname verification; no global trust override is added.
Initial URLs are restricted to the exact project API/download paths. Redirects are
bounded and accept only project download paths or GitHub's HTTPS release-asset hosts.
Index/API/APK sizes are bounded. Interrupted, truncated, oversized or checksum-invalid
downloads never become installable files.

Before installation, the app verifies the APK checksum and exact signed size, archive
package, version code/name, minimum SDK and installed signing identity. Incoming debug
or test-only APKs are rejected. The temporary APK remains in app-private cache and is
shared through a non-exported FileProvider limited to the `updates/` subdirectory.

The user confirms in the app before downloading. Android may then require **Allow from
this source** for Space Compass and its normal installation confirmation. The APK and
signed plan are revalidated after returning from settings, including after process
recreation. Cancelling or denying installation leaves the existing app/data intact.
No install is attempted merely because a background startup check found a release.

The application has no firmware updater, sensor credentials, background install
service or app-data migration dependency on UVIR.
