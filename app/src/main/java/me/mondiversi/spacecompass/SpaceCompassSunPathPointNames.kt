package me.mondiversi.spacecompass

import java.time.Instant
import java.time.ZoneId

/** Daily hourly markers use their local hour; orbital samples count from zero. Named events stay named. */
internal class SpaceCompassSunPathPointNames(
    markers: List<SpaceCompassSunPathPoint>,
    private val rise: String,
    private val culmination: String,
    private val set: String,
    private val minimum: String,
    val current: String,
    hourlyZone: ZoneId? = null,
    private val numbered: (Int) -> String
) {
    private val numbers = markers.asSequence()
        .filter { it.event == SpaceCompassSunPathEvent.HOUR }
        .sortedBy { it.timeMs }
        .distinctBy { it.timeMs }
        .mapIndexed { index, point ->
            point.timeMs to (hourlyZone?.let { Instant.ofEpochMilli(point.timeMs).atZone(it).hour } ?: index)
        }
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
