package me.mondiversi.spacecompass

internal data class SpaceCompassObservationMomentRow(val kind: SpaceCompassObservationMomentKind,
    val point: SpaceCompassSunPathPoint?, val annualExtreme: Boolean = false, val number: Int? = null)

/** One reference for each absolute extreme, then chronological cycle events without duplicates.
 * A tied extreme on another date remains a distinct cycle event, not a second summary row.
 * Only repeated events of the same kind receive ordinal numbers.
 */
internal fun spaceCompassObservationMomentRows(moments: List<SpaceCompassObservationMoment>):
    List<SpaceCompassObservationMomentRow> {
    val unique = moments.distinctBy { it.kind to it.point.timeMs }.sortedBy { it.point.timeMs }
    val maximum = unique.filter { it.kind == SpaceCompassObservationMomentKind.MAXIMUM }
        .maxByOrNull { it.point.position.elevationDegrees }
    val minimum = unique.filter { it.kind == SpaceCompassObservationMomentKind.MINIMUM }
        .minByOrNull { it.point.position.elevationDegrees }
    val absolute = listOfNotNull(maximum, minimum)
    val others = unique.filterNot { it in absolute }
    val counts = others.groupingBy { it.kind }.eachCount()
    val counters = mutableMapOf<SpaceCompassObservationMomentKind, Int>()
    return absolute.map { SpaceCompassObservationMomentRow(it.kind, it.point, annualExtreme = true) } +
        (others.filter { it.kind == SpaceCompassObservationMomentKind.MAXIMUM } +
            others.filter { it.kind == SpaceCompassObservationMomentKind.MINIMUM } +
            others.filter { it.kind == SpaceCompassObservationMomentKind.MID_RISING ||
                it.kind == SpaceCompassObservationMomentKind.MID_SETTING }).map {
            val number = counters.getOrDefault(it.kind, 0) + 1
            counters[it.kind] = number
            SpaceCompassObservationMomentRow(it.kind, it.point,
                number = number.takeIf { _ -> counts.getValue(it.kind) > 1 })
        }
}
