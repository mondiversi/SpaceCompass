package me.mondiversi.spacecompass

/** Complete a portable copy from one effective instant; never change the running observer or its preferences. */
internal fun SpaceCompassScenarioArchive.withAutomaticValues(values: SpaceCompassScenarioCurrentValues): SpaceCompassScenarioArchive? {
    val place = values.place ?: return null
    val altitude = values.altitudeMeters ?: return null
    val advance = configuration.simulateTime && configuration.advanceTime
    val snapshot = spaceCompassObserverPlan(place.latitude, place.longitude, altitude, values.instant, values.zone.id,
        simulatePosition = true, simulateAltitude = true, simulateZone = true, simulateDate = true, simulateTime = true,
        automaticAltitudeMeters = altitude, automaticZoneId = values.zone.id, momentZoneId = values.zone.id,
        advanceTime = advance, clockOffsetMs = if (advance) 0L else null) ?: return null
    return copy(configuration = snapshot)
}

/** Export an otherwise automatic location as a place, without borrowing dormant values or temporal preferences. */
internal fun SpaceCompassScenarioArchive.withAutomaticLocation(values: SpaceCompassScenarioCurrentValues): SpaceCompassScenarioArchive? {
    if (custom || timeSettings.enabled) return this
    val place = values.place ?: return null
    val altitude = values.altitudeMeters ?: return null
    val snapshot = spaceCompassObserverPlan(place.latitude, place.longitude, altitude, 0L, values.zone.id,
        simulatePosition=true, simulateAltitude=true, simulateZone=true, simulateDate=false, simulateTime=false,
        automaticAltitudeMeters=altitude, automaticZoneId=values.zone.id, momentZoneId="UTC") ?: return null
    return copy(configuration=snapshot, timeSettings=SpaceCompassScenarioTimeSettings(), temporalSettingsIncluded=false)
}
