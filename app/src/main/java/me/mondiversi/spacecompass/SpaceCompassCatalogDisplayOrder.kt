package me.mondiversi.spacecompass

/** Checked objects bypass type, horizon and search filters without changing the saved filters.
 * Partition the sorted catalogue stably so both groups retain the user's ordering, including ties
 * and unavailable values. Only the supplied available catalogue can expose hidden objects. */
internal fun spaceCompassCatalogDisplayOrder(sortedAvailable: List<SpaceCompassCelestialBody>,
    matching: Set<SpaceCompassCelestialBody>, selected: Set<SpaceCompassCelestialBody>): List<SpaceCompassCelestialBody> {
    val (checked, others) = sortedAvailable.filter { it in selected || it in matching }.partition { it in selected }
    return checked + others
}
