package me.mondiversi.spacecompass

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassDailyPathListEntriesTest {
    private val zone = ZoneId.of("Europe/Rome")
    private val date = LocalDate.parse("2026-10-08")
    private val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
    private val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    private fun point(time: Long, elevation: Double = 20.0) = SpaceCompassSunPathPoint(time, SpaceCompassSunPosition(170.0, elevation))
    private fun path(body: SpaceCompassCelestialBody = SpaceCompassCelestialBody.MOON) = SpaceCompassSunDailyPath(
        date, zone, listOf(point(start), point(end)), listOf(point(start), point(start + 3_600_000), point(start + 7_200_000)), body)

    @Test fun currentIsInsertedChronologicallyWithoutChangingHourlyNumbersForEverySupportedBody() {
        for (body in SpaceCompassCelestialBody.entries.filter { it.supportsDailyPath }) {
            val p = path(body);val original = p.markers.toList()
            val current = spaceCompassCurrentPathPoint(body, start + 5_400_000, SpaceCompassSunPosition(171.2, -18.4))!!
            val rows = spaceCompassDailyPathListEntries(p, current)
            assertEquals(body.name, listOf(start, start + 3_600_000, current.timeMs, start + 7_200_000), rows.map { it.point.timeMs })
            assertEquals(current, rows.single { it.isCurrent }.point)
            assertEquals(listOf(0, 1, 2), rows.filterNot { it.isCurrent }.map { it.markerIndex })
            assertEquals(original, p.markers)
        }
    }
    @Test fun exactHourlyCoincidenceRetainsBothCurrentIdentityAndTheNumberedPoint() {
        val p = path();val current = p.markers[1]
        val rows = spaceCompassDailyPathListEntries(p, current)
        val sameTime = rows.filter { it.point.timeMs == current.timeMs }
        assertEquals(2, sameTime.size);assertTrue(sameTime.first().isCurrent)
        assertEquals(1, sameTime.last().markerIndex)
    }
    @Test fun coincidentRiseOrMaximumIsNotRemovedOrRelabeledAsCurrent() {
        val p = path().let { it.copy(markers = listOf(it.markers[1].copy(event = SpaceCompassSunPathEvent.SUNRISE,
            coincidentEvents = setOf(SpaceCompassSunPathEvent.CULMINATION)))) }
        val rows = spaceCompassDailyPathListEntries(p, point(start + 3_600_000))
        assertEquals(2, rows.size)
        assertEquals(setOf(SpaceCompassSunPathEvent.SUNRISE, SpaceCompassSunPathEvent.CULMINATION), rows.last().point.events)
        assertFalse(rows.last().isCurrent)
    }
    @Test fun lunarCurrentPointHasItsOwnPhaseAtTheChosenScenarioInstant() {
        val time = Instant.parse("2027-04-06T13:17:00Z").toEpochMilli()
        val moon = spaceCompassCurrentPathPoint(SpaceCompassCelestialBody.MOON, time, SpaceCompassSunPosition(125.0, 33.0))!!
        assertEquals(time, moon.timeMs);assertEquals(calculateSpaceCompassMoonPhase(time), moon.moonPhase)
        assertNull(spaceCompassCurrentPathPoint(SpaceCompassCelestialBody.MARS, time, moon.position)!!.moonPhase)
    }
    @Test fun missingOrInvalidObservationsNeverInventACurrentPoint() {
        assertNull(spaceCompassCurrentPathPoint(SpaceCompassCelestialBody.MOON, start, null))
        for (position in listOf(SpaceCompassSunPosition(Double.NaN, 0.0), SpaceCompassSunPosition(0.0, Double.POSITIVE_INFINITY),
            SpaceCompassSunPosition(0.0, 91.0))) {
            assertNull(spaceCompassCurrentPathPoint(SpaceCompassCelestialBody.MOON, start, position))
            assertFalse(spaceCompassDailyPathListEntries(path(), SpaceCompassSunPathPoint(start, position)).any { it.isCurrent })
        }
        assertEquals(path().markers.size, spaceCompassDailyPathListEntries(path(), null).size)
    }
    @Test fun aStaleDailyCurveDoesNotLabelTomorrowAsCurrentInYesterdaysList() {
        assertFalse(spaceCompassDailyPathListEntries(path(), point(start - 1)).any { it.isCurrent })
        assertFalse(spaceCompassDailyPathListEntries(path(), point(end)).any { it.isCurrent })
        assertTrue(spaceCompassDailyPathListEntries(path(), point(start)).any { it.isCurrent })
        assertTrue(spaceCompassDailyPathListEntries(path(), point(end - 1)).any { it.isCurrent })
    }
    @Test fun satelliteOrbitCrossingMidnightStillIncludesItsValidCurrentInstant() {
        val p = path(SpaceCompassCelestialBody.ISS).copy(samples = listOf(point(end - 1_800_000), point(end + 1_800_000)))
        val current = point(end + 120_000)
        assertEquals(current, spaceCompassDailyPathListEntries(p, current).single { it.isCurrent }.point)
        assertFalse(spaceCompassDailyPathListEntries(p, point(end + 1_800_001)).any { it.isCurrent })
    }
    @Test fun daylightSavingRepeatedHourUsesInstantsRatherThanAmbiguousClockText() {
        val p = calculateSpaceCompassSunDailyPath(LocalDate.parse("2026-10-25"), zone, 45.0, 9.0)
        val current = point(Instant.parse("2026-10-25T01:30:00Z").toEpochMilli())
        val rows = spaceCompassDailyPathListEntries(p, current)
        assertEquals(25, rows.count { !it.isCurrent && it.point.event == SpaceCompassSunPathEvent.HOUR })
        assertEquals(rows.map { it.point.timeMs }.sorted(), rows.map { it.point.timeMs })
        assertEquals(current, rows.single { it.isCurrent }.point)
    }
    @Test fun consultingAndSelectingCurrentFreezesTheSnapshotAcrossLiveRefreshes() {
        val state = SpaceCompassSunDailyPathUiState();val p = path();val current = point(start + 5_400_000)
        state.openMenu(p, current)
        assertSame(current, state.menuCurrentPoint)
        state.selectCurrent(p.body, current);state.closeMenu()
        assertNull(state.menuCurrentPoint);assertNull(state.menuPath)
        assertSame(current, state.selectedCurrentFor(p.body))
        assertSame(current, state.selectedIn(p.copy(markers = emptyList())))
        state.openMenu(p, current.copy(timeMs = current.timeMs + 60_000))
        assertSame(current, state.selectedCurrentFor(p.body))
        assertNotEquals(current.timeMs, state.menuCurrentPoint!!.timeMs)
    }
    @Test fun anEmptyCurveDoesNotCreateAnUnboundedCurrentRow() {
        assertFalse(spaceCompassDailyPathListEntries(path().copy(samples = emptyList()), point(start)).any { it.isCurrent })
    }
}
