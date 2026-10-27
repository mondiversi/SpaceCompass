package me.mondiversi.spacecompass

import java.time.Instant
import java.time.ZoneId

/** Effective values can come from a configured position without enabling its height/zone overrides. */
internal data class SpaceCompassScenarioSummaryEffects(
    val position: Boolean = false, val altitude: Boolean = false, val zone: Boolean = false,
    val date: Boolean = false, val time: Boolean = false, val speed: Boolean = false,
    val weather: Boolean = false, val automaticAltitude: Boolean = false, val automaticZone: Boolean = false
)

internal fun spaceCompassScenarioSummaryEffects(plan: SpaceCompassObserverPlan?, timeMs: Long, zone: ZoneId,
    deviceTimeMs: Long, deviceZone: ZoneId): SpaceCompassScenarioSummaryEffects {
    if (plan == null) return SpaceCompassScenarioSummaryEffects()
    val automaticAltitude = plan.simulatePosition && !plan.simulateAltitude && plan.automaticAltitudeMeters != null
    val automaticZone = plan.simulatePosition && !plan.simulateZone && plan.automaticZoneId != null
    val changedZone = plan.simulateZone || automaticZone
    val speed = plan.scenarioClock?.settings?.enabled == true
    val effective = Instant.ofEpochMilli(timeMs).atZone(zone)
    val live = Instant.ofEpochMilli(deviceTimeMs).atZone(deviceZone)
    val changedMoment = plan.simulateDate || plan.simulateTime || speed
    return SpaceCompassScenarioSummaryEffects(
        position = plan.simulatePosition, altitude = plan.simulateAltitude || automaticAltitude,
        zone = changedZone,
        date = plan.simulateDate || speed || (changedZone || plan.simulateTime) && effective.toLocalDate() != live.toLocalDate(),
        time = plan.simulateTime || speed || changedZone && effective.offset != live.offset,
        speed = speed, weather = plan.simulatePosition || changedMoment,
        automaticAltitude = automaticAltitude, automaticZone = automaticZone
    )
}
