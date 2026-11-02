package me.mondiversi.spacecompass

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassAntipodesLifecycleTest {
    private val zone = ZoneId.of("UTC")
    private val date = LocalDate.parse("2026-11-02")
    private val now = date.atTime(12,0).atZone(zone).toInstant().toEpochMilli()
    private val first = SpaceCompassSunPathPoint(now,SpaceCompassSunPosition(180.0,30.0))
    private val second = first.copy(timeMs=now+3_600_000)
    private val moon = SpaceCompassSunDailyPath(date,zone,listOf(first,second),listOf(first,second),SpaceCompassCelestialBody.MOON)

    @Test fun startsOffAndCannotEnableWithoutASelectedPoint() {
        val state=SpaceCompassSunDailyPathUiState()
        assertFalse(state.antipodesVisible)
        state.toggleAntipodes(); assertFalse(state.antipodesVisible)
        state.select(moon,first); assertFalse(state.antipodesVisible)
    }
    @Test fun removingOneOfSeveralSelectionsKeepsAntipodesUntilTheLastPointIsRemoved() {
        val state=SpaceCompassSunDailyPathUiState()
        state.select(moon,first); state.select(moon.copy(body=SpaceCompassCelestialBody.SUN),second)
        state.toggleAntipodes(); assertTrue(state.antipodesVisible)
        state.togglePoint(moon,first); assertTrue(state.antipodesVisible)
        state.togglePoint(moon.copy(body=SpaceCompassCelestialBody.SUN),second)
        assertTrue(state.selectedPoints.isEmpty()); assertFalse(state.antipodesVisible)
    }
    @Test fun clearingSelectionsAlwaysTurnsAntipodesOffAndDoesNotReactivateThemForNewPins() {
        val state=SpaceCompassSunDailyPathUiState()
        state.select(moon,first); state.toggleAntipodes(); assertTrue(state.antipodesVisible)
        state.clearSelection(); assertFalse(state.antipodesVisible)
        state.select(moon,second); assertFalse(state.antipodesVisible)
    }
    @Test fun deselectingTheLastLivePositionAlsoTurnsAntipodesOff() {
        val state=SpaceCompassSunDailyPathUiState()
        state.selectCurrent(moon.body,first,moon); state.toggleAntipodes()
        state.toggleCurrent(moon.body,first.copy(timeMs=now+1000),moon)
        assertTrue(state.selectedPoints.isEmpty()); assertFalse(state.antipodesVisible)
    }
    @Test fun aNewSessionDoesNotRestoreAnEnabledChoiceFromThePreviousSession() {
        val old=SpaceCompassSunDailyPathUiState()
        old.select(moon,first); old.toggleAntipodes(); assertTrue(old.antipodesVisible)
        val fresh=SpaceCompassSunDailyPathUiState()
        fresh.select(moon,first); assertFalse(fresh.antipodesVisible)
    }
    @Test fun liveSelectionKeepsItsIdentityAsTheObservationClockAdvances() {
        val state=SpaceCompassSunDailyPathUiState()
        state.selectCurrent(moon.body,first,moon)
        assertTrue(state.isCurrentSelected(moon.body,first.copy(timeMs=now+60_000,
            position=SpaceCompassSunPosition(181.0,31.0)),zone))
        assertFalse(state.isCurrentSelected(SpaceCompassCelestialBody.SUN,first,zone))
        assertFalse(state.isCurrentSelected(moon.body,first,ZoneId.of("Europe/Rome")))
    }
}
