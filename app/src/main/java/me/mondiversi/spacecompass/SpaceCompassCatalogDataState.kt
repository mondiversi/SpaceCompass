package me.mondiversi.spacecompass

internal enum class SpaceCompassCatalogDataState(val label: Int) {
    LOADING(R.string.pc_loading), STALE(R.string.catalog_data_stale),
    OUTSIDE_DATE(R.string.catalog_date_unavailable), UNAVAILABLE(R.string.catalog_value_unavailable)
}

/** Called only when a displayed quantity is missing; physical facts do not depend on orbital downloads. */
internal fun spaceCompassCatalogMissingValueState(body: SpaceCompassCelestialBody,
    field: SpaceCompassCatalogSortField, observationTimeMs: Long, nowMs: Long,
    remote: SpaceCompassCelestialRemoteData, calculationPending: Boolean = false): SpaceCompassCatalogDataState {
    if (field != SpaceCompassCatalogSortField.DISTANCE && field != SpaceCompassCatalogSortField.EARTH_DISTANCE)
        return SpaceCompassCatalogDataState.UNAVAILABLE
    if (calculationPending) return SpaceCompassCatalogDataState.LOADING
    val position = SpaceCompassCelestialRequest(body)
    val dependencies = if (body.isEarthSatellite) {
        val orbit = remote.satelliteOrbit(body)
        // A current TLE cannot satisfy an unrelated simulated date, even while a refresh is active.
        if (orbit?.usable(nowMs) == true && !orbit.usable(observationTimeMs))
            return SpaceCompassCatalogDataState.OUTSIDE_DATE
        if (orbit?.usable(observationTimeMs) == true) return SpaceCompassCatalogDataState.LOADING
        listOf(position)
    } else if (body.usesHorizons) {
        val positionReady = remote.ephemerides[body]?.at(observationTimeMs) != null
        val motionReady = field == SpaceCompassCatalogSortField.DISTANCE && body.horizonsMotionPrimary == null &&
            remote.motions[body]?.speedAt(observationTimeMs) != null
        if (positionReady || motionReady) return SpaceCompassCatalogDataState.LOADING
        if (field == SpaceCompassCatalogSortField.DISTANCE && body.horizonsMotionPrimary == null)
            listOf(position, SpaceCompassCelestialRequest(body, motion = true)) else listOf(position)
    } else return SpaceCompassCatalogDataState.UNAVAILABLE
    if (remote.activeRequest in dependencies || body in remote.requestedBodies &&
        dependencies.any { it !in remote.failedRequests }) return SpaceCompassCatalogDataState.LOADING
    if (spaceCompassCelestialDataOutsideDate(body, observationTimeMs, remote)) {
        return if (Math.floorDiv(observationTimeMs, 86_400_000L) != Math.floorDiv(nowMs, 86_400_000L))
            SpaceCompassCatalogDataState.OUTSIDE_DATE else SpaceCompassCatalogDataState.STALE
    }
    return SpaceCompassCatalogDataState.UNAVAILABLE
}
