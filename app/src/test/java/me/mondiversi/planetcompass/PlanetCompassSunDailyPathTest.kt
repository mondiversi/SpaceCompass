package me.mondiversi.planetcompass

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.*
import org.junit.Assert.*
import org.junit.Test

class PlanetCompassSunDailyPathTest {
    private val rome = ZoneId.of("Europe/Rome")
    private fun day(date: String = "2026-10-03", lat: Double = 45.0, lon: Double = 9.0,
        zone: ZoneId = rome) = calculatePlanetCompassSunDailyPath(LocalDate.parse(date), zone, lat, lon)
    private fun facing(az: Double, el: Double = 0.0): PlanetCompassSunOrientation {
        val a = Math.toRadians(az); val e = Math.toRadians(el)
        return PlanetCompassSunOrientation(PlanetCompassSunVector(cos(a), -sin(a), 0.0),
            PlanetCompassSunVector(-sin(e) * sin(a), -sin(e) * cos(a), cos(e)),
            PlanetCompassSunVector(cos(e) * sin(a), cos(e) * cos(a), sin(e)))
    }
    @Test fun ordinaryCivilDayContains24TrueHourlyMarkersAndBothSidesOfHorizon() {
        val p = day()
        val hours = p.markers.filter { it.event == PlanetCompassSunPathEvent.HOUR }
        assertEquals(24, hours.size)
        hours.forEachIndexed { hour, marker ->
            val local = Instant.ofEpochMilli(marker.timeMs).atZone(rome)
            assertEquals(hour, local.hour); assertEquals(0, local.minute)
            assertEquals(p.date, local.toLocalDate())
        }
        assertTrue(p.samples.any { it.position.elevationDegrees < 0 })
        assertTrue(p.samples.any { it.position.elevationDegrees > 0 })
        assertEquals(481, p.samples.size)
    }
    @Test fun dstSkipsNonexistentHourAndRetainsBothRepeatedHoursWithDifferentInstants() {
        val spring = day("2026-03-29").markers.filter { it.event == PlanetCompassSunPathEvent.HOUR }
        assertEquals(23, spring.size)
        assertFalse(spring.any { Instant.ofEpochMilli(it.timeMs).atZone(rome).hour == 2 })
        val autumn = day("2026-10-25").markers.filter { it.event == PlanetCompassSunPathEvent.HOUR }
        assertEquals(25, autumn.size)
        val repeated = autumn.filter { Instant.ofEpochMilli(it.timeMs).atZone(rome).hour == 2 }
        assertEquals(2, repeated.size)
        assertEquals(3_600_000L, repeated[1].timeMs - repeated[0].timeMs)
        assertNotEquals(Instant.ofEpochMilli(repeated[0].timeMs).atZone(rome).offset,
            Instant.ofEpochMilli(repeated[1].timeMs).atZone(rome).offset)
    }
    @Test fun sunriseAndSunsetAreRefinedAndCulminationIsActualDailyMaximum() {
        val p = day()
        val rise = p.markers.single { it.event == PlanetCompassSunPathEvent.SUNRISE }
        val set = p.markers.single { it.event == PlanetCompassSunPathEvent.SUNSET }
        val peak = p.markers.single { it.event == PlanetCompassSunPathEvent.CULMINATION }
        assertEquals(-0.833, rise.position.elevationDegrees, 0.001)
        assertEquals(-0.833, set.position.elevationDegrees, 0.001)
        assertTrue(rise.timeMs < peak.timeMs && peak.timeMs < set.timeMs)
        assertTrue(peak.position.elevationDegrees >= p.samples.maxOf { it.position.elevationDegrees } - 0.00001)
        for (delta in listOf(-60_000, 60_000)) {
            assertTrue(peak.position.elevationDegrees > calculatePlanetCompassSunPosition(peak.timeMs + delta, 45.0, 9.0).elevationDegrees)
        }
    }
    @Test fun noSunriseOrSunsetIsInventedDuringPolarDayOrNight() {
        for (date in listOf("2026-06-21", "2026-12-21")) {
            val p = day(date, 80.0, 0.0, ZoneId.of("UTC"))
            assertFalse(p.markers.any { it.event == PlanetCompassSunPathEvent.SUNRISE || it.event == PlanetCompassSunPathEvent.SUNSET })
            assertEquals(1, p.markers.count { it.event == PlanetCompassSunPathEvent.CULMINATION })
        }
    }
    @Test fun solarCoordinatesAreIdenticalForTheSameUtcInstantRegardlessOfTimezone() {
        val a = day(zone = ZoneId.of("UTC"))
        val b = day(zone = rome)
        val common = a.markers.filter { it.event == PlanetCompassSunPathEvent.HOUR }
            .first { hour -> b.markers.any { it.timeMs == hour.timeMs } }
        assertEquals(common.position, b.markers.first { it.timeMs == common.timeMs }.position)
    }
    @Test fun lastSampleBelongsToNextMidnightButLastHourlyDotRemainsInToday() {
        val p = day()
        assertEquals(p.date.plusDays(1), Instant.ofEpochMilli(p.samples.last().timeMs).atZone(rome).toLocalDate())
        assertTrue(p.markers.all { Instant.ofEpochMilli(it.timeMs).atZone(rome).toLocalDate() == p.date })
        assertTrue(p.samples.zipWithNext().all { (a,b) -> b.timeMs > a.timeMs })
    }
    @Test fun behindPhoneSegmentsAreRejectedInsteadOfBridgingTheViewport() {
        assertNull(projectPlanetCompassSunPathSegment(PlanetCompassSunVector(0.1, -1.0, 0.0),
            PlanetCompassSunVector(-0.1, -1.0, 0.0), facing(0.0), 400.0, 600.0, false))
    }
    @Test fun aSegmentCrossingViewportEdgesIsClippedEvenWhenBothEndsAreOffscreen() {
        val s = projectPlanetCompassSunPathSegment(PlanetCompassSunVector(-1.0, 1.0, 0.0),
            PlanetCompassSunVector(1.0, 1.0, 0.0), facing(0.0), 400.0, 600.0, false)!!
        assertEquals(0.0, s.start.x, 1e-7); assertEquals(400.0, s.end.x, 1e-7)
        assertEquals(300.0, s.start.y, 1e-7); assertEquals(300.0, s.end.y, 1e-7)
    }
    @Test fun clippingAndRollRemainFiniteAndInsidePortraitLandscapeAndTinyWindows() {
        val p = day()
        for ((w,h) in listOf(400.0 to 600.0, 900.0 to 300.0, 20.0 to 20.0)) {
            for (heading in 0..330 step 30) for (tilt in listOf(-85.0, 0.0, 85.0)) {
                val original = facing(heading.toDouble(), tilt)
                for (o in listOf(original, PlanetCompassSunOrientation(original.screenUp,
                    PlanetCompassSunVector(-original.right.east, -original.right.north, -original.right.up), original.forward))) {
                    for (s in projectPlanetCompassSunDailyPath(p, o, w, h)) for (v in listOf(s.start, s.end)) {
                        assertTrue(v.x.isFinite() && v.x in 0.0..w)
                        assertTrue(v.y.isFinite() && v.y in 0.0..h)
                    }
                }
            }
        }
    }
    @Test fun zeroSizedWindowsAreSafeAndVisiblePathContainsDayAndNightStyles() {
        assertNull(projectPlanetCompassSunPathSegment(PlanetCompassSunVector(0.0,1.0,0.0), PlanetCompassSunVector(0.0,1.0,0.0),
            facing(0.0), 0.0, 600.0, false))
        val all = (0..330 step 30).flatMap { projectPlanetCompassSunDailyPath(day(), facing(it.toDouble()), 400.0, 600.0) }
        assertTrue(all.any { it.belowHorizon }); assertTrue(all.any { !it.belowHorizon })
    }
}
