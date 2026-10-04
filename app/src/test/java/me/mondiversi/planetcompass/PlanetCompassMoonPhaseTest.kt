package me.mondiversi.planetcompass

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone
import org.junit.Assert.*
import org.junit.Test

class PlanetCompassMoonPhaseTest {
    private fun phase(utc: String) = calculatePlanetCompassMoonPhase(Instant.parse(utc).toEpochMilli())

    @Test fun knownNewAndFullMoonsHaveOppositeIllumination() {
        val new = phase("2024-04-08T18:21:00Z")
        val full = phase("2024-04-23T23:49:00Z")
        assertEquals(PlanetCompassMoonPhaseKind.NEW, new.kind)
        assertEquals(PlanetCompassMoonPhaseKind.FULL, full.kind)
        assertTrue(new.illuminatedFraction < 0.001)
        assertTrue(full.illuminatedFraction > 0.999)
    }

    @Test fun allEightPhaseNamesAndWaxingWaningSidesAreDistinct() {
        PlanetCompassMoonPhaseKind.entries.forEachIndexed { index, expected ->
            val angle = index * 45.0
            assertEquals(expected, PlanetCompassMoonPhase(angle, 0.5).kind)
        }
        assertEquals(PlanetCompassMoonPhaseKind.NEW, PlanetCompassMoonPhase(359.9, 0.001).kind)
        assertEquals(0.0 to 1.0, PlanetCompassMoonPhase(90.0, 0.5).litHorizontalBounds(0.0))
        assertEquals(-1.0 to -0.0, PlanetCompassMoonPhase(270.0, 0.5).litHorizontalBounds(0.0))
    }

    @Test fun nativeIconAreaMatchesTheContinuousIlluminatedFraction() {
        for (fraction in listOf(0.0, 0.01, 0.25, 0.5, 0.75, 0.99, 1.0)) {
            for (angle in listOf(60.0, 300.0)) {
                val moon = PlanetCompassMoonPhase(angle, fraction)
                var area = 0.0
                val steps = 10_000
                repeat(steps) { i ->
                    val (left, right) = moon.litHorizontalBounds(-1.0 + (i + 0.5) * 2 / steps)
                    assertTrue(left <= right)
                    area += (right - left) * 2 / steps
                }
                assertEquals(fraction, area / Math.PI, 0.00001)
            }
        }
    }

    @Test fun moonMarkersHaveTheirOwnHourlyPhasesIncludingDstDays() {
        val zone = ZoneId.of("Europe/Rome")
        for ((dateText, expected) in listOf("2026-03-29" to 23, "2026-10-03" to 24, "2026-10-25" to 25)) {
            val date = LocalDate.parse(dateText)
            val path = calculatePlanetCompassCelestialPath(PlanetCompassCelestialBody.MOON, date, zone,
                date.atStartOfDay(zone).toInstant().toEpochMilli(), 45.0, 9.0, 0.0, PlanetCompassCelestialRemoteData())!!
            val hours = path.markers.filter { it.event == PlanetCompassSunPathEvent.HOUR }
            assertEquals(expected, hours.size)
            assertTrue(path.samples.all { it.moonPhase == null })
            assertTrue(path.markers.all { it.moonPhase == calculatePlanetCompassMoonPhase(it.timeMs) })
            assertEquals(expected, hours.map { it.moonPhase }.distinct().size)
        }
    }

    @Test fun utcPhaseDoesNotDependOnThePhoneTimezone() {
        val original = TimeZone.getDefault()
        try {
            val a = phase("2026-10-03T12:00:00Z")
            for (zone in listOf("UTC", "America/New_York", "Pacific/Auckland")) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone))
                assertEquals(a, phase("2026-10-03T12:00:00Z"))
            }
        } finally { TimeZone.setDefault(original) }
    }
}
