package me.mondiversi.spacecompass

import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.hourAngle
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.CancellationException
import kotlin.math.*

class SpaceCompassObservationMomentsTest {
    private val zone = ZoneId.of("Europe/Rome")
    private val now = Instant.parse("2026-10-31T12:00:00Z").toEpochMilli()
    private val remote = SpaceCompassCelestialRemoteData()
    private fun moments(body: SpaceCompassCelestialBody = SpaceCompassCelestialBody.SUN,
        lat: Double = 45.0, lon: Double = 9.0, height: Double = 130.0, instant: Long = now,
        timeZone: ZoneId = zone) = calculateSpaceCompassObservationMoments(body, instant, timeZone,
        lat, lon, height, remote)!!
    private fun date(moment: SpaceCompassObservationMoment, timeZone: ZoneId = zone) =
        Instant.ofEpochMilli(moment.point.timeMs).atZone(timeZone)

    @Test fun sunComparesAnnualNoonCulminationsAndNeverTheNightMinimum() {
        val values = moments()
        assertEquals(4, values.size)
        val maximum = values.single { it.kind == SpaceCompassObservationMomentKind.MAXIMUM }
        val minimum = values.single { it.kind == SpaceCompassObservationMomentKind.MINIMUM }
        assertEquals(6, date(maximum).monthValue)
        assertTrue(date(maximum).dayOfMonth in 20..23)
        assertEquals(13, date(maximum).hour) // Rome summer time.
        assertEquals(12, date(minimum).monthValue)
        assertTrue(date(minimum).dayOfMonth in 20..23)
        assertEquals(12, date(minimum).hour) // Rome winter time.
        assertTrue(maximum.point.position.elevationDegrees > 68.0)
        assertTrue(minimum.point.position.elevationDegrees in 20.0..23.0)
        assertTrue(values.all { date(it).year == 2026 })
        assertTrue(values.all { abs(spaceCompassObservationHourAngle(it.point.position, 45.0)!!) < .00002 })
    }

    @Test fun bothMidHeightPassagesStayAtRealCulminationsNearSpringAndAutumn() {
        val values = moments()
        val midpoint = (values[0].point.position.elevationDegrees + values[1].point.position.elevationDegrees) / 2
        val middle = values.drop(2)
        assertEquals(listOf(3, 9), middle.map { date(it).monthValue })
        assertEquals(SpaceCompassObservationMomentKind.MID_RISING, middle[0].kind)
        assertEquals(SpaceCompassObservationMomentKind.MID_SETTING, middle[1].kind)
        for (moment in middle) {
            assertEquals(midpoint, moment.point.position.elevationDegrees, .25)
            assertTrue(date(moment).hour in 12..13)
        }
    }

    @Test fun datesInTheSameYearReturnTheSameAnnualResultRatherThanTheSelectedDaysExtrema() {
        val january = moments(instant = Instant.parse("2026-01-02T00:00:00Z").toEpochMilli())
        val november = moments()
        assertEquals(january, november)
        assertTrue(moments(instant = Instant.parse("2027-01-02T00:00:00Z").toEpochMilli()).all { date(it).year == 2027 })
    }

    @Test fun southernHemisphereReversesTheAnnualSolarMaximumAndMinimum() {
        val values = moments(lat = -35.0, lon = 18.0, timeZone = ZoneId.of("Africa/Johannesburg"))
        assertEquals(12, date(values[0], ZoneId.of("Africa/Johannesburg")).monthValue)
        assertEquals(6, date(values[1], ZoneId.of("Africa/Johannesburg")).monthValue)
    }

    @Test fun changingLocationAndItsZoneChangesTheDisplayedCulminationClock() {
        val italy = moments()
        val canaries = moments(lat = 28.3, lon = -14.1, timeZone = ZoneId.of("Atlantic/Canary"))
        assertTrue(abs(italy[0].point.timeMs - canaries[0].point.timeMs) > 3_600_000)
        assertNotEquals(italy[0].point.position, canaries[0].point.position)
        assertEquals(1 * 3600, date(canaries[0], ZoneId.of("Atlantic/Canary")).offset.totalSeconds)
    }

    @Test fun lunarResultsUseTheActualObserverHeightAndMatchIndependentEngineHourAngles() {
        val low = moments(SpaceCompassCelestialBody.MOON, height = 0.0)
        val high = moments(SpaceCompassCelestialBody.MOON, height = 5000.0)
        assertNotEquals(low[0].point.position.elevationDegrees, high[0].point.position.elevationDegrees, 1e-8)
        assertTrue(high.size > 4) // Many lunar declination cycles in a year; keep every midpoint passage.
        for (moment in high) {
            assertEquals(calculateSpaceCompassCelestialObservation(SpaceCompassCelestialBody.MOON,
                moment.point.timeMs, 45.0, 9.0, 5000.0, remote)!!.position, moment.point.position)
            val hour = hourAngle(Body.Moon, spaceCompassAstronomyTime(moment.point.timeMs), Observer(45.0, 9.0, 5000.0))
            assertTrue("Every entry is an upper transit, including annual minimum", min(hour, 24 - hour) < .0002)
        }
    }

    @Test fun shortLivedSatelliteAndHorizonsCachesAreNotExtrapolatedIntoAnnualData() {
        assertNull(calculateSpaceCompassObservationMoments(SpaceCompassCelestialBody.ISS, now,
            zone, 45.0, 9.0, 100.0, remote))
        val sedna = SpaceCompassCelestialBody.SEDNA
        val cached = SpaceCompassHorizonsEphemeris(sedna, listOf(
            SpaceCompassHorizonsSample(now - 48 * 3600_000L, 80.0, 0.0, 0.0),
            SpaceCompassHorizonsSample(now + 48 * 3600_000L, 80.0, 0.0, 0.0)))
        assertNull(calculateSpaceCompassObservationMoments(sedna, now, zone, 45.0, 9.0, 100.0,
            remote.copy(ephemerides = mapOf(sedna to cached))))
    }

    private fun synthetic(year: Int, timeZone: ZoneId, latitude: Double): List<SpaceCompassSunPathPoint> {
        val start = LocalDate.of(year, 1, 1).atStartOfDay(timeZone).toInstant().toEpochMilli()
        return calculateSpaceCompassAnnualCulminations(year, timeZone, latitude, { t ->
            val day = (t - start) / 86_400_000.0
            val declination = Math.toRadians(20 * sin(2 * Math.PI * day / 365.25))
            val hour = 2 * Math.PI * (day - .5)
            val lat = Math.toRadians(latitude)
            val east = -cos(declination) * sin(hour)
            val north = cos(lat) * sin(declination) - sin(lat) * cos(declination) * cos(hour)
            val up = sin(lat) * sin(declination) + cos(lat) * cos(declination) * cos(hour)
            SpaceCompassSunPosition(Math.toDegrees(atan2(east, north)), Math.toDegrees(asin(up)))
        })!!
    }

    @Test fun leapYearsAndDstContainEveryTransitWithoutInventingMidnightEvents() {
        for (year in listOf(2024, 2026)) {
            val points = synthetic(year, zone, 45.0)
            assertEquals(LocalDate.of(year, 1, 1).lengthOfYear(), points.size)
            assertTrue(points.all { Instant.ofEpochMilli(it.timeMs).atZone(zone).year == year })
            assertTrue(points.zipWithNext().all { (a, b) -> abs(b.timeMs - a.timeMs - 86_400_000) <= 250 })
        }
    }

    @Test fun polarObserversStillUseDefinedUpperMeridianCrossings() {
        assertEquals(365, synthetic(2026, ZoneId.of("UTC"), 90.0).size)
        assertEquals(365, synthetic(2026, ZoneId.of("UTC"), -90.0).size)
    }

    @Test fun flatAnnualCurveHasNoInventedDistinctMidpointPassages() {
        val points = (0..10).map { SpaceCompassSunPathPoint(now + it * 86_400_000L,
            SpaceCompassSunPosition(180.0, 50.0), SpaceCompassSunPathEvent.CULMINATION) }
        assertEquals(2, spaceCompassAnnualObservationMoments(points)!!.size)
        assertNull(spaceCompassAnnualObservationMoments(emptyList()))
    }

    @Test fun midpointChoosesAnActualTransitRatherThanInterpolatingAnEveningTime() {
        val points = listOf(10.0, 80.0, 40.0, 5.0, 10.0).mapIndexed { index, height ->
            SpaceCompassSunPathPoint(now + index * 86_400_000L, SpaceCompassSunPosition(180.0, height),
                SpaceCompassSunPathEvent.CULMINATION)
        }
        val result = spaceCompassAnnualObservationMoments(points)!!
        assertTrue(result.all { it.point in points })
        assertEquals(listOf(SpaceCompassObservationMomentKind.MID_RISING, SpaceCompassObservationMomentKind.MID_SETTING),
            result.drop(2).map { it.kind })
    }

    @Test fun unavailableSamplesAndCancellationNeverPublishPartialYearResults() {
        var calls = 0
        assertNull(calculateSpaceCompassAnnualCulminations(2026, zone, 45.0, { null }))
        try {
            calculateSpaceCompassAnnualCulminations(2026, zone, 45.0,
                { SpaceCompassSunPosition(180.0, 45.0) }, { if (++calls > 5) throw CancellationException() })
            fail("Annual work must remain cancellable")
        } catch (_: CancellationException) { assertTrue(calls > 5) }
    }
}
