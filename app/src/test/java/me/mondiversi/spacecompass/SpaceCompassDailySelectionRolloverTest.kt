package me.mondiversi.spacecompass

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassDailySelectionRolloverTest {
    private val zone = ZoneId.of("UTC")
    private val date = LocalDate.parse("2026-11-07")
    private val now = date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
    private fun point(time: Long = now) = SpaceCompassSunPathPoint(time, SpaceCompassSunPosition(180.0, 30.0))
    private fun path(body: SpaceCompassCelestialBody = SpaceCompassCelestialBody.MOON) =
        SpaceCompassSunDailyPath(date, zone, listOf(point(), point(now + 3_600_000L)), listOf(point()), body)

    @Test fun nextDayClearsOrdinaryKeyAndCurrentPinsForEveryLiveObjectWithoutFeedback() {
        var feedback = 0
        val state = SpaceCompassSunDailyPathUiState { feedback++ }
        state.refreshObservationDate(date)
        val moon = path(); val sun = path(SpaceCompassCelestialBody.SUN)
        state.togglePoint(moon, point())
        state.togglePoint(sun, point().copy(event = SpaceCompassSunPathEvent.CULMINATION))
        state.toggleCurrent(moon.body, point(), moon)
        state.openMenu(moon, point()); state.toggleAntipodes()
        val before = feedback
        state.refreshObservationDate(date.plusDays(1))
        assertTrue(state.selectedPoints.isEmpty())
        assertFalse(state.antipodesVisible)
        assertNull(state.menuFocusPointId)
        assertSame(moon, state.menuPath)
        assertEquals(before, feedback)
    }

    @Test fun fixedHeightEventAndPersonalPinsRemainSelectedWithTheirAntipodes() {
        val state = SpaceCompassSunDailyPathUiState()
        state.refreshObservationDate(date)
        state.select(path(), point())
        val fixed = SpaceCompassComparisonSource.entries.map { source ->
            val comparison = SpaceCompassTemporalComparison.create(SpaceCompassCelestialBody.SUN,
                now, zone, source, "fixture-${source.name}",
                personalName = "Personal".takeIf { source == SpaceCompassComparisonSource.PERSONAL },
                personalDate = date.takeIf { source == SpaceCompassComparisonSource.PERSONAL })
            val p = path(comparison.body).copy(comparison = comparison)
            state.select(p, point()); state.selectCurrent(p.body, point(), p)
            comparison.id
        }.toSet()
        state.toggleAntipodes()
        val pins = state.selectedPoints.filter { it.key.comparisonId != null }
        state.refreshObservationDate(date.plusDays(500))
        assertEquals(pins.toSet(), state.selectedPoints.toSet())
        assertEquals(fixed, state.selectedPoints.map { it.key.comparisonId }.toSet())
        assertTrue(state.antipodesVisible)
    }

    @Test fun sameDayRefreshAndReopeningTheDailyPageKeepSelections() {
        val state = SpaceCompassSunDailyPathUiState(); val p = path()
        state.refreshObservationDate(date); state.select(p, point())
        repeat(4) {
            state.openMenu(p); state.closeMenu(); state.refreshObservationDate(date)
            assertTrue(state.isSelected(p, point()))
        }
        state.refreshObservationDate(date.minusDays(1))
        assertTrue(state.selectedPoints.isEmpty())
    }

    @Test fun forward24HoursStillExpiresLivePinsAtTheNextCivilDay() {
        val state = SpaceCompassSunDailyPathUiState()
        state.refreshObservationDate(date)
        val future = point(now + 18 * 3_600_000L)
        val p = path().copy(forward24Hours = true, samples = listOf(point(), point(now + 86_400_000L)), markers = listOf(future))
        state.select(p, future)
        assertEquals(date.plusDays(1), state.selectedPoints.single().key.date)
        state.refreshObservationDate(date.plusDays(1))
        assertTrue(state.selectedPoints.isEmpty())
    }

    @Test fun theLocationZoneControlsMidnightWhileRepeatedDstHoursAreTheSameDay() {
        val state = SpaceCompassSunDailyPathUiState(); val p = path()
        val losAngeles = ZoneId.of("America/Los_Angeles")
        fun day(instant: String, zone: ZoneId) = Instant.parse(instant).atZone(zone).toLocalDate()
        // Both instants have November 7 in UTC, but straddle local midnight in Los Angeles.
        state.refreshObservationDate(day("2026-11-07T07:59:59Z", losAngeles)); state.select(p, point())
        state.refreshObservationDate(day("2026-11-07T08:00:00Z", losAngeles))
        assertTrue(state.selectedPoints.isEmpty())
        val rome = ZoneId.of("Europe/Rome")
        state.refreshObservationDate(day("2026-10-25T00:30:00Z", rome)); state.select(p, point())
        state.refreshObservationDate(day("2026-10-25T01:30:00Z", rome))
        assertTrue(state.isSelected(p, point()))
    }

    @Test fun restoredFixedPinsAreNotDependentOnAnInMemoryPath() {
        val source = SpaceCompassSunDailyPathUiState(); val p = path()
        source.select(p, point())
        val c = SpaceCompassTemporalComparison.create(p.body, now, zone, SpaceCompassComparisonSource.HEIGHT, "fixed")
        source.select(p.copy(comparison = c), point())
        val receiver = SpaceCompassSunDailyPathUiState()
        receiver.restoreImportedPoints(source.selectedPoints)
        receiver.refreshObservationDate(date) // Establish a baseline; merely loading must not clear a document.
        assertEquals(2, receiver.selectedPoints.size)
        assertTrue(receiver.selectedPoints.all { it.path == null })
        receiver.refreshObservationDate(date.plusDays(1))
        assertEquals(c.id, receiver.selectedPoints.single().key.comparisonId)
    }
}
