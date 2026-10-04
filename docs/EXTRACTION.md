# Extraction from UVIR — 2026-10-04

Planet Compass was separated from the current working UVIR source, including
unpublished celestial refinements, not merely copied from an older Git commit.

`extraction-inventory.json` records original paths and SHA-256 checksums for the
copied inputs. Kotlin files/classes/functions were mechanically renamed from
UVIR to PlanetCompass and assigned the independent namespace. Required shared
UI helpers were copied locally, with unrelated screens omitted. The hidden logo
entry gesture is not part of this application's launch flow.

The UVIR source loses its celestial entry, feature files, maps, translations,
satellite implementation, dedicated tests and astronomy-only dependencies.
UVIR retains location permission declarations needed for Wi-Fi network names,
but no celestial GPS/orientation listeners. Shared UI code remains in UVIR because its measurement screens
still use it. A separate pre-extraction snapshot preserves the exact original
files, including uncommitted changes; no existing UVIR Git history was reset.

No archive records, paired sensor credentials, Android application data, signing
keys or firmware are copied. There is no cross-application data migration.
The phone was disconnected for this operation; no device install is implied.

## Release preparation still separate

- Device/emulator regression checks, including compass, permissions, rotation and UI.
- Final app icon/onboarding/settings and any desired durable selection preferences.
- Independent signing key, release build, privacy policy and provider-term review.
- GitHub remote creation/publication, CI and release automation.

No GitHub repository, release or store listing has been created by this local extraction.
