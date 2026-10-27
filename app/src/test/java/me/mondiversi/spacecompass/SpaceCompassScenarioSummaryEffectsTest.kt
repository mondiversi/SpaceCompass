package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class SpaceCompassScenarioSummaryEffectsTest {
    private val now = Instant.parse("2026-10-27T18:00:00Z").toEpochMilli()
    private val rome = ZoneId.of("Europe/Rome")
    private val canary = ZoneId.of("Atlantic/Canary")
    private val base = SpaceCompassObserverPlan(28.05, -14.35, 999.0, now, "Atlantic/Canary",
        simulatePosition = false, simulateTime = false, simulateAltitude = false, simulateZone = false,
        automaticAltitudeMeters = 21.0, automaticZoneId = "Atlantic/Canary", simulateDate = false)
    private fun effects(plan: SpaceCompassObserverPlan?, time: Long = now, zone: ZoneId = canary) =
        spaceCompassScenarioSummaryEffects(plan, time, zone, now, rome)

    @Test fun realDefaultsRemainNeutral() {
        assertEquals(SpaceCompassScenarioSummaryEffects(), effects(null))
        assertEquals(SpaceCompassScenarioSummaryEffects(), effects(base))
    }
    @Test fun positionAloneMarksItsInferredHeightAndZoneWithoutEnablingExtraSections() {
        val plan = base.copy(simulatePosition = true)
        val result = effects(plan)
        assertTrue(result.position && result.altitude && result.zone && result.weather)
        assertTrue(result.automaticAltitude && result.automaticZone)
        assertFalse(plan.simulateAltitude || plan.simulateZone || plan.simulateTime || plan.simulateDate)
        assertTrue(result.time); assertFalse(result.date)
        assertEquals(1, plan.activeSections.size)
    }
    @Test fun explicitHeightAndZoneReplaceTheirAutomaticOrigin() {
        val result = effects(base.copy(simulatePosition = true, simulateAltitude = true, simulateZone = true))
        assertTrue(result.altitude && result.zone)
        assertFalse(result.automaticAltitude || result.automaticZone)
    }
    @Test fun aHeightOverrideDoesNotAffectPositionClockOrWeather() {
        assertEquals(SpaceCompassScenarioSummaryEffects(altitude = true), effects(base.copy(simulateAltitude = true)))
    }
    @Test fun timezoneConversionCanChangeTheDisplayedDayWithoutCustomDateOrTime() {
        val moment = Instant.parse("2026-10-27T23:30:00Z").toEpochMilli()
        val result = spaceCompassScenarioSummaryEffects(base.copy(simulatePosition = true), moment, canary, moment, rome)
        assertTrue(result.date && result.time && result.automaticZone)
    }
    @Test fun aCustomDateDoesNotByItselfClaimACustomClockTime() {
        val result = effects(base.copy(simulateDate = true), now + 86400000, rome)
        assertTrue(result.date && result.weather)
        assertFalse(result.time || result.automaticAltitude || result.automaticZone)
    }
    @Test fun anAcceleratedClockAffectsTheMomentAndWeatherEvenWithoutPositionOverrides() {
        val plan = base.copy().also {
            it.scenarioClock = SpaceCompassScenarioClock(SpaceCompassScenarioTimeSettings(true, 60),
                "summary fixture", now, now, 0L, elapsed = { 0L })
        }
        val result = effects(plan, zone = rome)
        assertTrue(result.date && result.time && result.speed && result.weather)
        assertFalse(result.position || result.altitude || result.zone)
    }
}
