package me.mondiversi.spacecompass

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassCelestialPathMinimumTest {
    private fun point(time: Long, event: SpaceCompassSunPathEvent, elevation: Double) =
        SpaceCompassSunPathPoint(time, SpaceCompassSunPosition(150.0, elevation), event)

    @Test fun theSolarMinimumIsRefinedAndIsNeitherSunriseNorSunset() {
        val path = calculateSpaceCompassSunDailyPath(LocalDate.parse("2026-10-04"), ZoneId.of("Europe/Rome"), 45.0, 9.0)
        val lowest = path.markers.single { it.event == SpaceCompassSunPathEvent.MINIMUM }
        assertTrue(lowest.position.elevationDegrees < -40.0)
        assertTrue(lowest.position.elevationDegrees <= path.samples.minOf { it.position.elevationDegrees } + 1e-6)
        for (offset in listOf(-60_000L, 60_000L)) assertTrue(lowest.position.elevationDegrees <
            calculateSpaceCompassSunPosition(lowest.timeMs + offset, 45.0, 9.0).elevationDegrees)
        assertFalse(lowest.events.contains(SpaceCompassSunPathEvent.SUNRISE))
        assertFalse(lowest.events.contains(SpaceCompassSunPathEvent.SUNSET))
    }

    @Test fun polarDayAndNightHaveAMinimumWithoutInventingHorizonCrossings() {
        for (latitude in listOf(-90.0, -80.0, 80.0, 90.0)) for (date in listOf("2026-06-21", "2026-12-21")) {
            val path = calculateSpaceCompassSunDailyPath(LocalDate.parse(date), ZoneId.of("UTC"), latitude, 0.0)
            val minimum = path.markers.single { SpaceCompassSunPathEvent.MINIMUM in it.events }
            assertTrue(minimum.position.elevationDegrees.isFinite())
            assertTrue(minimum.position.elevationDegrees <= path.samples.minOf { it.position.elevationDegrees } + 1e-6)
            assertFalse(path.markers.any { SpaceCompassSunPathEvent.SUNRISE in it.events || SpaceCompassSunPathEvent.SUNSET in it.events })
            assertEquals(path.date, Instant.ofEpochMilli(minimum.timeMs).atZone(path.zone).toLocalDate())
        }
        val summer = calculateSpaceCompassSunDailyPath(LocalDate.parse("2026-06-21"), ZoneId.of("UTC"), 80.0, 0.0)
        assertTrue(summer.markers.single { it.event == SpaceCompassSunPathEvent.MINIMUM }.position.elevationDegrees > 0.0)
    }

    @Test fun extremaStayInTheCivilDayWithoutChangingDstHourlyCounts() {
        for ((date, count) in listOf("2026-03-29" to 23, "2026-10-25" to 25)) {
            val path = calculateSpaceCompassSunDailyPath(LocalDate.parse(date), ZoneId.of("Europe/Rome"), 45.0, 9.0)
            assertEquals(count, path.markers.count { it.event == SpaceCompassSunPathEvent.HOUR })
            assertEquals(1, path.markers.count { SpaceCompassSunPathEvent.MINIMUM in it.events })
            assertTrue(path.markers.all { Instant.ofEpochMilli(it.timeMs).atZone(path.zone).toLocalDate() == path.date })
        }
    }

    @Test fun ordinaryCelestialPathsIncludingCircumpolarPolarisAllHaveTheirActualMinimum() {
        val date = LocalDate.parse("2026-10-03")
        val zone = ZoneId.of("Europe/Rome")
        val now = Instant.parse("2026-10-03T12:00:00Z").toEpochMilli()
        val begin = Instant.parse("2026-10-02T00:00:00Z").toEpochMilli()
        val remote = SpaceCompassCelestialRemoteData(ephemerides = mapOf(SpaceCompassCelestialBody.SEDNA to
            SpaceCompassHorizonsEphemeris(SpaceCompassCelestialBody.SEDNA,
                (0..72).map { SpaceCompassHorizonsSample(begin + it * 3_600_000L, 38.0, 72.0, 12.0) })))
        for (body in SpaceCompassCelestialBody.entries.filter { it.supportsDailyPath && !it.isEarthSatellite && !it.isVoyager }) {
            val path = calculateSpaceCompassCelestialPath(body, date, zone, now, 45.0, 9.0, 0.0, remote)!!
            val minimum = path.markers.single { SpaceCompassSunPathEvent.MINIMUM in it.events }
            assertTrue(body.name, minimum.position.elevationDegrees <= path.samples.minOf { it.position.elevationDegrees } + 1e-6)
            assertEquals(24, path.markers.count { it.event == SpaceCompassSunPathEvent.HOUR })
            if (body == SpaceCompassCelestialBody.MOON) assertNotNull(minimum.moonPhase)
            if (body == SpaceCompassCelestialBody.POLARIS) assertTrue(minimum.position.elevationDegrees > 0)
        }
    }

    @Test fun refiningEveryLocalCandidateFindsTheDeeperMinimumMissedByCoarseGlobalSampling() {
        fun height(time: Long): Double = minOf((time - 10_000.0).let { it * it / 1e6 },
            (time - 53_456.0).let { it * it / 1e6 - 0.1 })
        fun sample(time: Long, event: SpaceCompassSunPathEvent) = point(time, event, height(time))
        val samples = (0L..60_000L step 1000L).map { sample(it, SpaceCompassSunPathEvent.HOUR) }
        assertEquals(10_000L, samples.minBy { it.position.elevationDegrees }.timeMs)
        val refined = refineSpaceCompassPathExtremum(samples, 60_001, true, ::sample)!!
        assertTrue(abs(refined.timeMs - 53_456L) <= 125)
        assertTrue(refined.position.elevationDegrees < -0.09)
    }

    @Test fun monotonicOrFlatTrajectoriesUseAnActualEndpointWithoutInventingAnInteriorTransit() {
        for (slope in listOf(-1.0, 0.0, 1.0)) {
            fun sample(time: Long, event: SpaceCompassSunPathEvent) = point(time, event, slope * time / 1000)
            val samples = listOf(0L, 1000L, 2000L).map { sample(it, SpaceCompassSunPathEvent.HOUR) }
            val minimum = refineSpaceCompassPathExtremum(samples, 2000L, true, ::sample)!!
            assertEquals(if (slope < 0) 1999L else 0L, minimum.timeMs)
            assertEquals(SpaceCompassSunPathEvent.MINIMUM, minimum.event)
        }
    }

    @Test fun onlyTrulyCoincidentSpecialEventsMergeAndHourlyMarkersRemainIntact() {
        val low = point(1000, SpaceCompassSunPathEvent.MINIMUM, 0.0)
        val rise = point(1200, SpaceCompassSunPathEvent.SUNRISE, 0.0)
        val hour = low.copy(event = SpaceCompassSunPathEvent.HOUR)
        val merged = mergeCoincidentSpaceCompassPathEvents(listOf(rise, hour, low))
        assertEquals(2, merged.size)
        assertEquals(hour, merged.single { it.event == SpaceCompassSunPathEvent.HOUR })
        assertEquals(setOf(SpaceCompassSunPathEvent.MINIMUM, SpaceCompassSunPathEvent.SUNRISE),
            merged.single { it.event == SpaceCompassSunPathEvent.MINIMUM }.events)
        assertEquals(2, mergeCoincidentSpaceCompassPathEvents(listOf(low, rise.copy(timeMs = 1600))).size)
        assertEquals(2, mergeCoincidentSpaceCompassPathEvents(listOf(low,
            rise.copy(position = SpaceCompassSunPosition(180.0, 0.0)))).size)
    }

    @Test fun nearPolarEventsThatOnlyProjectTogetherKeepTheirOwnInstantsAndNames() {
        val minimum = point(1000, SpaceCompassSunPathEvent.MINIMUM, 0.0)
        val rise = minimum.copy(timeMs = 3_601_000L, event = SpaceCompassSunPathEvent.SUNRISE)
        assertEquals(listOf(minimum, rise), mergeCoincidentSpaceCompassPathEvents(listOf(rise, minimum)))
    }
}
