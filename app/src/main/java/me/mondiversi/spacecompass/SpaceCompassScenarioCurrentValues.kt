package me.mondiversi.spacecompass

import java.time.ZoneId

internal data class SpaceCompassScenarioCurrentValues(val place: SpaceCompassDevicePlace?,
    val altitudeMeters: Double?, val zone: ZoneId, val instant: Long, val rate: Int)

/** Summaries describe the applied observer, including automatic metadata and the running clock. */
internal fun spaceCompassScenarioCurrentValues(plan: SpaceCompassObserverPlan?, device: SpaceCompassDevicePlace?,
    settings: SpaceCompassScenarioTimeSettings, now: Long, deviceZone: ZoneId): SpaceCompassScenarioCurrentValues =
    SpaceCompassScenarioCurrentValues(plan?.resolvePlace(device) ?: device,
        when {
            plan?.simulateAltitude == true -> plan.altitudeMeters
            plan?.simulatePosition == true -> plan.automaticAltitudeMeters
            else -> device?.altitude
        },
        plan?.observationZone(deviceZone) ?: deviceZone, plan?.observationTimeMs(now, deviceZone) ?: now,
        if (settings.enabled) settings.rate else plan?.scenarioClock?.settings?.multiplier ?:
            if (plan?.simulateTime == true && !plan.advancesClock) 0 else 1)
