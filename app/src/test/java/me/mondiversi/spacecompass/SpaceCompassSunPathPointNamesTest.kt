package me.mondiversi.spacecompass

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassSunPathPointNamesTest {
    private fun point(time: Long, event: SpaceCompassSunPathEvent = SpaceCompassSunPathEvent.HOUR) =
        SpaceCompassSunPathPoint(time, SpaceCompassSunPosition(150.0, 20.0), event)
    private fun names(markers: List<SpaceCompassSunPathPoint>, hourlyZone: ZoneId? = null) =
        SpaceCompassSunPathPointNames(markers, "Sorge", "Culmine", "Tramonta", "Minimo", "Posizione attuale", hourlyZone) { "Punto $it" }

    @Test fun namedEventsDoNotConsumeRegularPointNumbers() {
        val markers = listOf(point(0), point(5, SpaceCompassSunPathEvent.SUNRISE), point(10),
            point(15, SpaceCompassSunPathEvent.CULMINATION), point(20), point(25, SpaceCompassSunPathEvent.SUNSET))
        val labels = names(markers)
        assertEquals(listOf("Punto 0", "Sorge", "Punto 1", "Culmine", "Punto 2", "Tramonta"),
            markers.map(labels::name))
    }

    @Test fun orbitalNumberingUsesChronologyNotInputOrderOrAnHourOfTheClock() {
        val markers = listOf(point(100), point(30), point(60))
        assertEquals(listOf("Punto 2", "Punto 0", "Punto 1"), markers.map(names(markers)::name))
    }

    @Test fun anEventCoincidingWithAnHourlyPointKeepsItsOwnName() {
        val markers = listOf(point(10), point(10, SpaceCompassSunPathEvent.CULMINATION), point(20))
        assertEquals(listOf("Punto 0", "Culmine", "Punto 1"), markers.map(names(markers)::name))
    }

    @Test fun duplicateRegularInstantsDoNotCreateHolesInNumbering() {
        val markers = listOf(point(10), point(10), point(20))
        assertEquals(listOf("Punto 0", "Punto 0", "Punto 1"), markers.map(names(markers)::name))
    }

    @Test fun ordinaryAndDstDaysNameAllActualHoursByTheirLocalClockHour() {
        for ((date, count) in listOf("2026-10-04" to 24, "2026-03-29" to 23, "2026-10-25" to 25)) {
            val path = calculateSpaceCompassSunDailyPath(LocalDate.parse(date), ZoneId.of("Europe/Rome"), 45.0, 9.0)
            val regular = path.markers.filter { it.event == SpaceCompassSunPathEvent.HOUR }
            assertEquals(count, regular.size)
            val expectedHours = when (count) {
                23 -> (0..23).filter { it != 2 }
                25 -> listOf(0, 1, 2, 2) + (3..23)
                else -> (0..23).toList()
            }
            assertEquals(expectedHours.map { "Punto $it" }, regular.map(names(path.markers, path.zone)::name))
            assertEquals(expectedHours, regular.map { Instant.ofEpochMilli(it.timeMs).atZone(path.zone).hour })
        }
    }

    @Test fun twentyFourSubHourlyIssSamplesAreNumberedZeroToTwentyThree() {
        val markers = (0 until 24).map { point(1_791_000_000_000L + it * 230_000L) }
        assertEquals((0..23).map { "Punto $it" }, markers.map(names(markers)::name))
    }

    @Test fun arbitraryLiveInstantAndBodyWithoutAPathAreNotCalledANumberedPoint() {
        assertEquals("Posizione attuale", names(listOf(point(10))).name(point(11)))
        assertEquals("Posizione attuale", names(emptyList()).name(point(11)))
        // The UI uses current explicitly even if the live snapshot coincides with a regular dot.
        assertEquals("Posizione attuale", names(listOf(point(10))).current)
    }

    @Test fun minimumHasItsOwnNameAndNeverConsumesAnHourlyNumber() {
        val markers = listOf(point(10), point(15, SpaceCompassSunPathEvent.MINIMUM), point(20))
        assertEquals(listOf("Punto 0", "Minimo", "Punto 1"), markers.map(names(markers)::name))
    }

    @Test fun genuinelyCoincidentEventsRetainAllNamesInOnePoint() {
        val markers = mergeCoincidentSpaceCompassPathEvents(listOf(point(1000, SpaceCompassSunPathEvent.SUNRISE),
            point(1100, SpaceCompassSunPathEvent.MINIMUM), point(1200, SpaceCompassSunPathEvent.SUNSET), point(1100)))
        val minimum = markers.single { it.event == SpaceCompassSunPathEvent.MINIMUM }
        assertEquals("Minimo · Sorge · Tramonta", names(markers).name(minimum))
        assertEquals("Punto 0", names(markers).name(markers.single { it.event == SpaceCompassSunPathEvent.HOUR }))
    }
}
