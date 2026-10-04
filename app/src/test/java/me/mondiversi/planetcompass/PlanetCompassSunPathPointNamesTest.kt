package me.mondiversi.planetcompass

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class PlanetCompassSunPathPointNamesTest {
    private fun point(time: Long, event: PlanetCompassSunPathEvent = PlanetCompassSunPathEvent.HOUR) =
        PlanetCompassSunPathPoint(time, PlanetCompassSunPosition(150.0, 20.0), event)
    private fun names(markers: List<PlanetCompassSunPathPoint>) =
        PlanetCompassSunPathPointNames(markers, "Sorge", "Culmine", "Tramonta", "Minimo", "Posizione attuale") { "Punto $it" }

    @Test fun namedEventsDoNotConsumeRegularPointNumbers() {
        val markers = listOf(point(0), point(5, PlanetCompassSunPathEvent.SUNRISE), point(10),
            point(15, PlanetCompassSunPathEvent.CULMINATION), point(20), point(25, PlanetCompassSunPathEvent.SUNSET))
        val labels = names(markers)
        assertEquals(listOf("Punto 1", "Sorge", "Punto 2", "Culmine", "Punto 3", "Tramonta"),
            markers.map(labels::name))
    }

    @Test fun numberingUsesChronologyNotInputOrderOrAnHourOfTheClock() {
        val markers = listOf(point(100), point(30), point(60))
        assertEquals(listOf("Punto 3", "Punto 1", "Punto 2"), markers.map(names(markers)::name))
    }

    @Test fun anEventCoincidingWithAnHourlyPointKeepsItsOwnName() {
        val markers = listOf(point(10), point(10, PlanetCompassSunPathEvent.CULMINATION), point(20))
        assertEquals(listOf("Punto 1", "Culmine", "Punto 2"), markers.map(names(markers)::name))
    }

    @Test fun duplicateRegularInstantsDoNotCreateHolesInNumbering() {
        val markers = listOf(point(10), point(10), point(20))
        assertEquals(listOf("Punto 1", "Punto 1", "Punto 2"), markers.map(names(markers)::name))
    }

    @Test fun ordinaryAndDstDaysKeepAllActualHoursAndUniqueNumbers() {
        for ((date, count) in listOf("2026-10-04" to 24, "2026-03-29" to 23, "2026-10-25" to 25)) {
            val path = calculatePlanetCompassSunDailyPath(LocalDate.parse(date), ZoneId.of("Europe/Rome"), 45.0, 9.0)
            val regular = path.markers.filter { it.event == PlanetCompassSunPathEvent.HOUR }
            assertEquals(count, regular.size)
            assertEquals((1..count).map { "Punto $it" }, regular.map(names(path.markers)::name))
        }
    }

    @Test fun twentyFourSubHourlyIssSamplesAreStillNumberedOneToTwentyFour() {
        val markers = (0 until 24).map { point(1_791_000_000_000L + it * 230_000L) }
        assertEquals((1..24).map { "Punto $it" }, markers.map(names(markers)::name))
    }

    @Test fun arbitraryLiveInstantAndBodyWithoutAPathAreNotCalledANumberedPoint() {
        assertEquals("Posizione attuale", names(listOf(point(10))).name(point(11)))
        assertEquals("Posizione attuale", names(emptyList()).name(point(11)))
        // The UI uses current explicitly even if the live snapshot coincides with a regular dot.
        assertEquals("Posizione attuale", names(listOf(point(10))).current)
    }

    @Test fun minimumHasItsOwnNameAndNeverConsumesAnHourlyNumber() {
        val markers = listOf(point(10), point(15, PlanetCompassSunPathEvent.MINIMUM), point(20))
        assertEquals(listOf("Punto 1", "Minimo", "Punto 2"), markers.map(names(markers)::name))
    }

    @Test fun genuinelyCoincidentEventsRetainAllNamesInOnePoint() {
        val markers = mergeCoincidentPlanetCompassPathEvents(listOf(point(1000, PlanetCompassSunPathEvent.SUNRISE),
            point(1100, PlanetCompassSunPathEvent.MINIMUM), point(1200, PlanetCompassSunPathEvent.SUNSET), point(1100)))
        val minimum = markers.single { it.event == PlanetCompassSunPathEvent.MINIMUM }
        assertEquals("Minimo · Sorge · Tramonta", names(markers).name(minimum))
        assertEquals("Punto 1", names(markers).name(markers.single { it.event == PlanetCompassSunPathEvent.HOUR }))
    }
}
