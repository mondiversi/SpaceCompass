package me.mondiversi.spacecompass

/** The summary and badge count only saved, independently enabled override sections. */
internal val SpaceCompassObserverPlan.activeSections: List<SpaceCompassObserverSection>
    get() = buildList {
        if (simulatePosition) add(SpaceCompassObserverSection.POSITION)
        if (simulateAltitude) add(SpaceCompassObserverSection.ALTITUDE)
        if (simulateZone) add(SpaceCompassObserverSection.ZONE)
        if (simulateDate) add(SpaceCompassObserverSection.DATE)
        if (simulateTime) add(SpaceCompassObserverSection.TIME)
    }

internal val SpaceCompassObserverPlan?.activeSectionCount: Int
    get() = this?.activeSections?.size ?: 0

/** Validate both provider values before enabling either override; the date/time switches stay independent. */
internal fun SpaceCompassObserverDraft.withPositionMetadata(metadata: SpaceCompassObserverMetadata,
    numeric: SpaceCompassNumericFormat, deviceZoneId: String): SpaceCompassObserverDraft {
    require(simulatePosition)
    require(metadata.altitude.isFinite() && metadata.altitude in -500.0..20_000.0)
    val validZone = java.time.ZoneId.of(metadata.zoneId).id
    return withDetectedMetadata(SpaceCompassObserverMetadataField.ZONE, metadata, numeric, deviceZoneId)
        .withDetectedMetadata(SpaceCompassObserverMetadataField.ALTITUDE, metadata, numeric, deviceZoneId)
        .copy(simulateAltitude = true, simulateZone = true,
            automaticAltitudeMeters = metadata.altitude, automaticZoneId = validZone)
}
