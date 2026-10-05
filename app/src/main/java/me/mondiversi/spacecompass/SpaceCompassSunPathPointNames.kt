package me.mondiversi.spacecompass

/** Number only regular markers in time order; named events never consume an ordinal. */
internal class SpaceCompassSunPathPointNames(
    markers: List<SpaceCompassSunPathPoint>,
    private val rise: String,
    private val culmination: String,
    private val set: String,
    private val minimum: String,
    val current: String,
    private val numbered: (Int) -> String
) {
    private val numbers = markers.asSequence()
        .filter { it.event == SpaceCompassSunPathEvent.HOUR }
        .sortedBy { it.timeMs }
        .distinctBy { it.timeMs }
        .mapIndexed { index, point -> point.timeMs to index + 1 }
        .toMap()

    fun name(point: SpaceCompassSunPathPoint): String {
        if (point.event == SpaceCompassSunPathEvent.HOUR) return numbers[point.timeMs]?.let(numbered) ?: current
        return (listOf(point.event) + point.coincidentEvents.sortedBy { it.ordinal }).distinct()
            .filter { it != SpaceCompassSunPathEvent.HOUR }.joinToString(" · ") { event ->
                when (event) {
                    SpaceCompassSunPathEvent.SUNRISE -> rise
                    SpaceCompassSunPathEvent.CULMINATION -> culmination
                    SpaceCompassSunPathEvent.SUNSET -> set
                    SpaceCompassSunPathEvent.MINIMUM -> minimum
                    SpaceCompassSunPathEvent.HOUR -> error("Hourly dots are numbered separately")
                }
            }
    }
}
