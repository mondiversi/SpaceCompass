package me.mondiversi.spacecompass

import java.time.LocalDateTime
import java.time.ZoneId

internal const val SPACE_COMPASS_OBSERVER_MODE = "observer_mode"
internal const val SPACE_COMPASS_OBSERVER_PLAN = "observer_plan"

/** App-local observer: never injected into Android's GPS or the system clock. */
internal data class SpaceCompassObserverPlan(val latitude: Double, val longitude: Double,
    val altitudeMeters: Double, val timeMs: Long, val zoneId: String,
    val simulatePosition: Boolean = true, val simulateTime: Boolean = true,
    val simulateAltitude: Boolean = simulatePosition, val simulateZone: Boolean = simulatePosition,
    val automaticAltitudeMeters: Double? = altitudeMeters, val automaticZoneId: String? = zoneId) {
    val zone: ZoneId get() = ZoneId.of(zoneId)
    val position: SpaceCompassDevicePlace? get() = if (simulatePosition)
        SpaceCompassDevicePlace(latitude, longitude, if (simulateAltitude) altitudeMeters else automaticAltitudeMeters) else null
    fun resolvePlace(devicePlace: SpaceCompassDevicePlace?): SpaceCompassDevicePlace? =
        position ?: devicePlace?.let { if (simulateAltitude) it.copy(altitude = altitudeMeters) else it }
    val timeOverrideMs: Long? get() = timeMs.takeIf { simulateTime }
    fun observationZone(deviceZone: ZoneId): ZoneId = when {
        simulateZone -> zone
        simulatePosition -> ZoneId.of(requireNotNull(automaticZoneId))
        else -> deviceZone
    }
    fun encode(): String = listOf(latitude, longitude, altitudeMeters, timeMs, zoneId,
        simulatePosition, simulateTime, simulateAltitude, simulateZone,
        automaticAltitudeMeters ?: "", automaticZoneId ?: "").joinToString("|")
}

internal fun spaceCompassObserverPlan(latitude: Double, longitude: Double, altitude: Double,
    timeMs: Long, zoneId: String, simulatePosition: Boolean = true,
    simulateTime: Boolean = true, simulateAltitude: Boolean = simulatePosition,
    simulateZone: Boolean = simulatePosition, automaticAltitudeMeters: Double? = altitude,
    automaticZoneId: String? = zoneId): SpaceCompassObserverPlan? = runCatching {
    require(simulatePosition || simulateTime || simulateAltitude || simulateZone)
    require(latitude.isFinite() && latitude in -90.0..90.0)
    require(longitude.isFinite() && longitude in -180.0..180.0)
    require(altitude.isFinite() && altitude in -500.0..20_000.0)
    val zone = ZoneId.of(zoneId)
    automaticAltitudeMeters?.let { require(it.isFinite() && it in -500.0..20_000.0) }
    val automaticZone = automaticZoneId?.let(ZoneId::of)
    if (simulatePosition && !simulateAltitude) requireNotNull(automaticAltitudeMeters)
    if (simulatePosition && !simulateZone) requireNotNull(automaticZone)
    require(java.time.Instant.ofEpochMilli(timeMs).atZone(zone).year in 1900..2100)
    SpaceCompassObserverPlan(latitude, longitude, altitude, timeMs, zone.id, simulatePosition, simulateTime,
        simulateAltitude, simulateZone, automaticAltitudeMeters, automaticZone?.id)
}.getOrNull()

internal fun decodeSpaceCompassObserverPlan(value: String?): SpaceCompassObserverPlan? = runCatching {
    val fields = requireNotNull(value).split('|')
    require(fields.size in setOf(5, 7, 11))
    // Five-field plans predate independent switches: preserve both original overrides.
    val position = if (fields.size == 5) true else fields[5].toBooleanStrict()
    val time = if (fields.size == 5) true else fields[6].toBooleanStrict()
    // Legacy position overrides also supplied height/zone: keep their exact effective values.
    val altitude = if (fields.size == 11) fields[7].toBooleanStrict() else position
    val zone = if (fields.size == 11) fields[8].toBooleanStrict() else position
    // Old values may have been entered manually. Resolve automatic metadata only when requested.
    val automaticAltitude = if (fields.size == 11) fields[9].takeIf(String::isNotBlank)?.toDouble() else null
    val automaticZone = if (fields.size == 11) fields[10].takeIf(String::isNotBlank) else null
    spaceCompassObserverPlan(fields[0].toDouble(), fields[1].toDouble(), fields[2].toDouble(),
        fields[3].toLong(), fields[4], position, time, altitude, zone, automaticAltitude, automaticZone)
}.getOrNull()

/** Reject nonexistent DST times; an ambiguous time uses its first valid occurrence. */
internal fun spaceCompassObserverMoment(date: String, time: String, zoneId: String): Long? = runCatching {
    require(date.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) && time.matches(Regex("\\d{2}:\\d{2}")))
    val local = LocalDateTime.parse("${date}T$time")
    require(local.year in 1900..2100)
    val offsets = ZoneId.of(zoneId).rules.getValidOffsets(local)
    require(offsets.isNotEmpty())
    local.toInstant(offsets.first()).toEpochMilli()
}.getOrNull()

internal data class SpaceCompassDevicePlace(val latitude: Double, val longitude: Double, val altitude: Double?)

/** Validate enabled fields only; inactive draft edits cannot block the other override. */
internal fun spaceCompassObserverDraftPlan(simulatePosition: Boolean, simulateTime: Boolean,
    latitude: Double?, longitude: Double?, altitudeMeters: Double?, momentMs: Long?, zoneId: String,
    previous: SpaceCompassObserverPlan?, nowMs: Long, deviceZoneId: String,
    simulateAltitude: Boolean = simulatePosition, simulateZone: Boolean = simulatePosition,
    automaticAltitudeMeters: Double? = altitudeMeters, automaticZoneId: String? = zoneId): SpaceCompassObserverPlan? {
    if (!simulatePosition && !simulateTime && !simulateAltitude && !simulateZone) return null
    val latitudeValue = if (simulatePosition) latitude ?: return null else previous?.latitude ?: 0.0
    val longitudeValue = if (simulatePosition) longitude ?: return null else previous?.longitude ?: 0.0
    val altitudeValue = if (simulateAltitude) altitudeMeters ?: return null else previous?.altitudeMeters ?: automaticAltitudeMeters ?: 0.0
    val zoneValue = if (simulateZone) zoneId else previous?.zoneId ?: deviceZoneId
    val automaticAltitude = if (simulatePosition) automaticAltitudeMeters else previous?.automaticAltitudeMeters
    val automaticZone = if (simulatePosition) automaticZoneId else previous?.automaticZoneId
    val timeValue = if (simulateTime) momentMs ?: return null else previous?.timeMs ?: nowMs
    return spaceCompassObserverPlan(latitudeValue, longitudeValue, altitudeValue, timeValue, zoneValue,
        simulatePosition, simulateTime, simulateAltitude, simulateZone, automaticAltitude, automaticZone)
}
