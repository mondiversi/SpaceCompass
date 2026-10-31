package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class SpaceCompassAnnualExtremaTest {
    private val utc = ZoneId.of("UTC")
    private val now = Instant.parse("2026-10-31T12:00:00Z").toEpochMilli()
    private fun solar(latitude: Double) = calculateSpaceCompassObservationMoments(SpaceCompassCelestialBody.SUN,
        now, utc, latitude, 0.0, 0.0, SpaceCompassCelestialRemoteData())!!
    private fun date(moment: SpaceCompassObservationMoment) = Instant.ofEpochMilli(moment.point.timeMs).atZone(utc)
    private fun List<SpaceCompassObservationMoment>.of(kind: SpaceCompassObservationMomentKind) = filter { it.kind == kind }
    private fun points(vararg heights: Double) = heights.mapIndexed { index, height ->
        SpaceCompassSunPathPoint(now + index * 86_400_000L, SpaceCompassSunPosition(180.0, height),
            SpaceCompassSunPathEvent.CULMINATION)
    }

    @Test fun equatorialSunKeepsBothEquinoxPeaksBothSolsticeTroughsAndFourMidHeightPassages() {
        val result = solar(0.0)
        val maxima = result.of(SpaceCompassObservationMomentKind.MAXIMUM)
        val minima = result.of(SpaceCompassObservationMomentKind.MINIMUM)
        assertEquals(8, result.size)
        assertEquals(listOf(3, 9), maxima.map { date(it).monthValue })
        assertEquals(listOf(6, 12), minima.map { date(it).monthValue })
        assertTrue(maxima.all { it.point.position.elevationDegrees > 89.7 })
        assertTrue(minima.all { it.point.position.elevationDegrees in 66.4..66.8 })
        val midpoint = (maxima.maxOf { it.point.position.elevationDegrees } + minima.minOf { it.point.position.elevationDegrees }) / 2
        val middle = result.filter { it.kind == SpaceCompassObservationMomentKind.MID_RISING ||
            it.kind == SpaceCompassObservationMomentKind.MID_SETTING }
        assertEquals(listOf(2, 4, 8, 10), middle.map { date(it).monthValue })
        assertEquals(2, middle.count { it.kind == SpaceCompassObservationMomentKind.MID_RISING })
        assertEquals(2, middle.count { it.kind == SpaceCompassObservationMomentKind.MID_SETTING })
        assertTrue(middle.all { kotlin.math.abs(it.point.position.elevationDegrees - midpoint) < .25 })
        assertTrue(result.all { kotlin.math.abs(spaceCompassObservationHourAngle(it.point.position, 0.0)!!) < .00002 })
    }

    @Test fun tropicalObserverKeepsTheUnequalSolsticeMinimaEvenWhenOnlyTwoMidHeightCrossingsExist() {
        val result = solar(15.0)
        val maxima = result.of(SpaceCompassObservationMomentKind.MAXIMUM)
        val minima = result.of(SpaceCompassObservationMomentKind.MINIMUM)
        assertEquals(2, maxima.size)
        assertTrue(maxima.all { it.point.position.elevationDegrees > 89.7 })
        assertEquals(listOf(6, 12), minima.map { date(it).monthValue })
        assertTrue(minima[0].point.position.elevationDegrees - minima[1].point.position.elevationDegrees > 29.0)
        assertEquals(6, result.size) // Do not force four midpoint passages just because there are two peaks.
    }

    @Test fun outsideTheTropicsBothHemispheresRetainOneSolarMaximumAndOneMinimum() {
        for (latitude in listOf(-45.0, 45.0)) {
            val result = solar(latitude)
            assertEquals(4, result.size)
            val maximum = result.single { it.kind == SpaceCompassObservationMomentKind.MAXIMUM }
            val minimum = result.single { it.kind == SpaceCompassObservationMomentKind.MINIMUM }
            assertEquals(if (latitude > 0) 6 else 12, date(maximum).monthValue)
            assertEquals(if (latitude > 0) 12 else 6, date(minimum).monthValue)
        }
    }

    @Test fun lunarDeclinationCyclesRetainTheLocalPeaksAndTroughsThroughoutTheYear() {
        val result = calculateSpaceCompassObservationMoments(SpaceCompassCelestialBody.MOON,
            now, utc, 45.0, 0.0, 0.0, SpaceCompassCelestialRemoteData())!!
        for (kind in listOf(SpaceCompassObservationMomentKind.MAXIMUM, SpaceCompassObservationMomentKind.MINIMUM)) {
            val extrema = result.of(kind)
            assertTrue("Lunar cycles must not be reduced to one absolute pair", extrema.size in 12..16)
            assertTrue(extrema.zipWithNext().all { (a, b) -> a.point.timeMs < b.point.timeMs })
        }
        assertTrue(result.all { it.point.event == SpaceCompassSunPathEvent.CULMINATION })
    }

    @Test fun unequalInteriorExtremaAndAbsoluteYearEdgeAreKeptInChronologicalGroups() {
        val input = points(30.0, 50.0, 20.0, 40.0, 10.0)
        val result = spaceCompassAnnualObservationMoments(input)!!
        assertEquals(listOf(input[1], input[3]), result.of(SpaceCompassObservationMomentKind.MAXIMUM).map { it.point })
        assertEquals(listOf(input[2], input[4]), result.of(SpaceCompassObservationMomentKind.MINIMUM).map { it.point })
        assertTrue(result.all { it.point in input })
        assertEquals(result.size, result.distinctBy { it.kind to it.point.timeMs }.size)
    }

    @Test fun shallowRipplesDoNotCreateExtraExtremaBesideTheTruePeak() {
        val input = points(10.0, 40.0, 39.998, 40.001, 39.999, 20.0)
        val result = spaceCompassAnnualObservationMoments(input)!!
        assertEquals(listOf(input[3]), result.of(SpaceCompassObservationMomentKind.MAXIMUM).map { it.point })
        assertEquals(listOf(input[0]), result.of(SpaceCompassObservationMomentKind.MINIMUM).map { it.point })
    }

    @Test fun flatTopsChooseOneRealMiddleTransitAndAreNotDuplicatedByTheAbsoluteFallback() {
        val input = points(10.0, 40.0, 40.0, 40.0, 10.0, 10.0, 10.0, 30.0)
        val result = spaceCompassAnnualObservationMoments(input)!!
        assertEquals(listOf(input[2]), result.of(SpaceCompassObservationMomentKind.MAXIMUM).map { it.point })
        assertEquals(listOf(input[5]), result.of(SpaceCompassObservationMomentKind.MINIMUM).map { it.point })
    }

    @Test fun monotonicYearUsesItsActualBoundariesWithoutCyclicJoining() {
        val input = points(50.0, 40.0, 30.0, 20.0)
        val result = spaceCompassAnnualObservationMoments(input)!!
        assertEquals(listOf(input.first()), result.of(SpaceCompassObservationMomentKind.MAXIMUM).map { it.point })
        assertEquals(listOf(input.last()), result.of(SpaceCompassObservationMomentKind.MINIMUM).map { it.point })
    }

    @Test fun malformedTimeOrderAndNonFiniteHeightsAreRejected() {
        val input = points(10.0, 50.0, 20.0)
        assertNull(spaceCompassAnnualObservationMoments(input.reversed()))
        assertNull(spaceCompassAnnualObservationMoments(listOf(input.first(), input.first())))
        assertNull(spaceCompassAnnualObservationMoments(points(10.0, Double.NaN, 20.0)))
        assertNull(spaceCompassAnnualObservationMoments(points(10.0, Double.POSITIVE_INFINITY, 20.0)))
    }
}
