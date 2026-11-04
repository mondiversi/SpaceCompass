package me.mondiversi.spacecompass

/** Checked objects bypass type, horizon and search filters without changing the saved filters.
 * Partition the sorted catalogue stably so both groups retain the user's ordering, including ties
 * and unavailable values. A visit can retain its entry priority while checks remain live.
 * Only the supplied available catalogue can expose hidden objects. */
internal fun spaceCompassCatalogDisplayOrder(sortedAvailable: List<SpaceCompassCelestialBody>,
    matching: Set<SpaceCompassCelestialBody>, selected: Set<SpaceCompassCelestialBody>,
    prioritySelected: Set<SpaceCompassCelestialBody> = selected,
    favorites: Set<SpaceCompassCelestialBody>? = null): List<SpaceCompassCelestialBody> {
    val (checked, others) = sortedAvailable.filter {
        (favorites == null || it in favorites) && (it in selected || it in matching)
    }.partition { it in prioritySelected }
    return checked + others
}

internal data class SpaceCompassCatalogDisplayRow(
    val body: SpaceCompassCelestialBody,
    val comparison: SpaceCompassTemporalComparison? = null
) {
    val key: String get() = comparison?.let { "comparison:${it.id}" } ?: body.name
}

/** Variants follow their visible, selected parent and retain independent lazy-list keys. */
internal fun spaceCompassCatalogDisplayRows(visibleBodies: List<SpaceCompassCelestialBody>,
    selectedBodies: Set<SpaceCompassCelestialBody>, comparisons: List<SpaceCompassTemporalComparison>):
    List<SpaceCompassCatalogDisplayRow> {
    val byBody = comparisons.filter { it.body in selectedBodies }
        .sortedWith(compareBy({ it.source == SpaceCompassComparisonSource.PERSONAL }, { it.personalDate },
            { it.personalDate != null && it.hasReference }, { it.timeMs }, { it.id })).groupBy { it.body }
    return buildList {
        for (body in visibleBodies) {
            add(SpaceCompassCatalogDisplayRow(body))
            for (comparison in byBody[body].orEmpty()) add(SpaceCompassCatalogDisplayRow(body, comparison))
        }
    }
}
