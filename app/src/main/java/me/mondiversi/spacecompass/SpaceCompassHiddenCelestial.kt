package me.mondiversi.spacecompass

internal const val SPACE_COMPASS_HIDDEN_OBJECT_HOLD_MS = 3_000L
// The scenic 39.3-ly reference belongs between Sirius and TRAPPIST-1e.
internal val spaceCompassAllCelestialOrder = spaceCompassCelestialCatalogOrder.flatMap {
    if (it == SpaceCompassCelestialBody.TRAPPIST_1_E) listOf(SpaceCompassCelestialBody.LV_426, it) else listOf(it)
}
internal val SpaceCompassCelestialBody.isFictional: Boolean get() = this == SpaceCompassCelestialBody.LV_426

/** Hidden bodies are available only while checked; Select all cannot discover them. */
internal fun spaceCompassAvailableCelestialCatalog(selected: Set<SpaceCompassCelestialBody>): List<SpaceCompassCelestialBody> =
    if (SpaceCompassCelestialBody.LV_426 in selected) spaceCompassAllCelestialOrder else spaceCompassCelestialCatalogOrder

/** Repeated holds are idempotent and preserve the currently inspected object. */
internal fun SpaceCompassCelestialSelection.revealHiddenObject(): SpaceCompassCelestialSelection =
    if (SpaceCompassCelestialBody.LV_426 in selected) this else
        SpaceCompassCelestialSelection(selected + SpaceCompassCelestialBody.LV_426, active ?: SpaceCompassCelestialBody.LV_426)

/** A direction reference does not prevent an explicitly supplied illustrative surface texture. */
internal val SpaceCompassCelestialBody.usesDeepSkySymbol: Boolean
    get() = deepSkyReference != null && viewerTexture == null
