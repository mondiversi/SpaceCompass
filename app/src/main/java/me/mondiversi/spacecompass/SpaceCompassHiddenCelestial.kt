package me.mondiversi.spacecompass

/** Every listed object, including fictional LV-426, is immediately available. */
internal val spaceCompassAllCelestialOrder = spaceCompassCelestialCatalogOrder
internal val SpaceCompassCelestialBody.isFictional: Boolean get() = this == SpaceCompassCelestialBody.LV_426

@Suppress("UNUSED_PARAMETER")
internal fun spaceCompassAvailableCelestialCatalog(selected: Set<SpaceCompassCelestialBody>): List<SpaceCompassCelestialBody> =
    spaceCompassCelestialCatalogOrder

/** A direction reference does not prevent an explicitly supplied illustrative surface texture. */
internal val SpaceCompassCelestialBody.usesDeepSkySymbol: Boolean
    get() = (deepSkyReference != null || this == SpaceCompassCelestialBody.SOUTHERN_CROSS) && viewerTexture == null
