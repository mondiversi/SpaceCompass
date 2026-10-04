package me.mondiversi.planetcompass

/** Number only regular markers in time order; named events never consume an ordinal. */
internal class PlanetCompassSunPathPointNames(
    markers: List<PlanetCompassSunPathPoint>,
    private val rise: String,
    private val culmination: String,
    private val set: String,
    private val minimum: String,
    val current: String,
    private val numbered: (Int) -> String
) {
    private val numbers = markers.asSequence()
        .filter { it.event == PlanetCompassSunPathEvent.HOUR }
        .sortedBy { it.timeMs }
        .distinctBy { it.timeMs }
        .mapIndexed { index, point -> point.timeMs to index + 1 }
        .toMap()

    fun name(point: PlanetCompassSunPathPoint): String {
        if (point.event == PlanetCompassSunPathEvent.HOUR) return numbers[point.timeMs]?.let(numbered) ?: current
        return (listOf(point.event) + point.coincidentEvents.sortedBy { it.ordinal }).distinct()
            .filter { it != PlanetCompassSunPathEvent.HOUR }.joinToString(" · ") { event ->
                when (event) {
                    PlanetCompassSunPathEvent.SUNRISE -> rise
                    PlanetCompassSunPathEvent.CULMINATION -> culmination
                    PlanetCompassSunPathEvent.SUNSET -> set
                    PlanetCompassSunPathEvent.MINIMUM -> minimum
                    PlanetCompassSunPathEvent.HOUR -> error("Hourly dots are numbered separately")
                }
            }
    }
}
