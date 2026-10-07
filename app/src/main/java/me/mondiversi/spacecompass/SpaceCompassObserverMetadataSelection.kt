package me.mondiversi.spacecompass

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** An explicit detect action updates one manual field; coordinates and automatic caches stay intact. */
internal fun SpaceCompassObserverDraft.withDetectedMetadata(field: SpaceCompassObserverMetadataField,
    metadata: SpaceCompassObserverMetadata, numeric: SpaceCompassNumericFormat,
    deviceZoneId: String): SpaceCompassObserverDraft = when (field) {
    SpaceCompassObserverMetadataField.ALTITUDE -> copy(altitude = formatSpaceCompassNumber(
        if (feet) metadata.altitude / .3048 else metadata.altitude, 2, numeric, grouping = false))
    SpaceCompassObserverMetadataField.ZONE -> {
        val sourceZone = when {
            simulateZone -> zone.trim()
            simulatePosition -> automaticZoneId.orEmpty()
            else -> deviceZoneId
        }
        val instant = preservedMomentMs ?: spaceCompassObserverMoment(date.trim(), time.trim(), sourceZone)
        val target = ZoneId.of(metadata.zoneId)
        val local = instant?.let { Instant.ofEpochMilli(it).atZone(target) }
        copy(zone = target.id, date = local?.toLocalDate()?.toString() ?: date,
            time = local?.toLocalTime()?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: time,
            preservedMomentMs = instant)
    }
}
