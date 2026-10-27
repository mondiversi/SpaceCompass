package me.mondiversi.spacecompass

/** Every listed object is immediately available; fiction has its own explicit category. */
internal val spaceCompassAllCelestialOrder get() = spaceCompassCelestialCatalogOrder
internal val SpaceCompassCelestialBody.isFictional: Boolean get() = fictionalReference != null

@Suppress("UNUSED_PARAMETER")
internal fun spaceCompassAvailableCelestialCatalog(selected: Set<SpaceCompassCelestialBody>): List<SpaceCompassCelestialBody> =
    spaceCompassCelestialCatalogOrder

/** A direction reference does not prevent an explicitly supplied illustrative surface texture. */
internal val SpaceCompassCelestialBody.usesDeepSkySymbol: Boolean
    get() = (deepSkyReference != null || this == SpaceCompassCelestialBody.SOUTHERN_CROSS) && viewerTexture == null
