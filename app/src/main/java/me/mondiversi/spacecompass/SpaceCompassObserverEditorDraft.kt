package me.mondiversi.spacecompass

/** Validate the edited field without enabling it. Other inactive fields retain their saved values. */
internal fun SpaceCompassObserverDraft.resolveForEditor(section: SpaceCompassScenarioEditorSection?,
    previous: SpaceCompassObserverPlan?, nowMs: Long, deviceZoneId: String): Result<SpaceCompassObserverPlan?> = runCatching {
    val current = resolve(previous,nowMs,deviceZoneId).getOrThrow()
    val candidate = when (section) {
        SpaceCompassScenarioEditorSection.POSITION -> if (simulatePosition) return@runCatching current else copy(simulatePosition=true)
        SpaceCompassScenarioEditorSection.ALTITUDE -> if (simulateAltitude) return@runCatching current else copy(simulateAltitude=true)
        SpaceCompassScenarioEditorSection.ZONE -> if (simulateZone) return@runCatching current else copy(simulateZone=true)
        SpaceCompassScenarioEditorSection.DATE -> if (simulateDate) return@runCatching current else copy(simulateDate=true)
        SpaceCompassScenarioEditorSection.TIME -> if (simulateTime) return@runCatching current else copy(simulateTime=true)
        else -> return@runCatching current
    }.resolve(previous,nowMs,deviceZoneId).getOrThrow() ?: return@runCatching current
    // A first dormant edit must not replace the other draft defaults with neutral coordinates.
    val base = current ?: previous ?: candidate.copy(
        latitude = spaceCompassObserverInputNumber(latitude)?.takeIf { it in -90.0..90.0 } ?: candidate.latitude,
        longitude = spaceCompassObserverInputNumber(longitude)?.takeIf { it in -180.0..180.0 } ?: candidate.longitude,
        automaticAltitudeMeters = automaticAltitudeMeters?.takeIf { it.isFinite() && it in -500.0..20_000.0 },
        automaticZoneId = automaticZoneId?.takeIf { runCatching { java.time.ZoneId.of(it) }.isSuccess })
    val stored = when (section) {
        SpaceCompassScenarioEditorSection.POSITION -> base.copy(latitude=candidate.latitude,longitude=candidate.longitude,
            automaticAltitudeMeters=candidate.automaticAltitudeMeters,automaticZoneId=candidate.automaticZoneId)
        SpaceCompassScenarioEditorSection.ALTITUDE -> base.copy(altitudeMeters=candidate.altitudeMeters)
        SpaceCompassScenarioEditorSection.ZONE -> base.copy(zoneId=candidate.zoneId)
        SpaceCompassScenarioEditorSection.DATE,SpaceCompassScenarioEditorSection.TIME -> base.copy(
            timeMs=candidate.timeMs,momentZoneId=candidate.momentZoneId,
            clockOffsetMs=if (simulateTime) current?.clockOffsetMs else null)
        else -> base
    }
    stored.copy(simulatePosition=simulatePosition,simulateAltitude=simulateAltitude,simulateZone=simulateZone,
        simulateDate=simulateDate,simulateTime=simulateTime,advanceTime=advanceTime)
}
