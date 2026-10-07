package me.mondiversi.spacecompass

internal const val SPACE_COMPASS_OBSERVER_AUTOSAVE_DELAY_MS = 600L
internal const val SPACE_COMPASS_OBSERVER_METADATA_DELAY_MS = 800L

internal fun spaceCompassObserverInputNumber(value: String): Double? =
    value.trim().replace(',', '.').toDoubleOrNull()?.takeIf(Double::isFinite)

/** Complete enabled values can be stored; incomplete typing must not replace the last valid observer. */
internal data class SpaceCompassObserverDraft(
    val simulatePosition: Boolean, val simulateTime: Boolean,
    val latitude: String, val longitude: String, val altitude: String, val zone: String,
    val date: String, val time: String, val feet: Boolean = false,
    val simulateAltitude: Boolean = simulatePosition, val simulateZone: Boolean = simulatePosition,
    val automaticAltitudeMeters: Double? = spaceCompassObserverInputNumber(altitude)?.let { if (feet) it * .3048 else it },
    val automaticZoneId: String? = zone,
    val preservedMomentMs: Long? = null
) {
    fun resolve(previous: SpaceCompassObserverPlan?, nowMs: Long,
        deviceZoneId: String): Result<SpaceCompassObserverPlan?> = runCatching {
        if (!simulatePosition && !simulateTime && !simulateAltitude && !simulateZone) return@runCatching null
        val interpretationZone = when {
                simulateZone -> zone.trim()
                simulatePosition -> automaticZoneId.orEmpty()
                else -> deviceZoneId
            }
        // A zone change must preserve the second occurrence of an overlapping DST hour too.
        // Explicit date/time edits clear this hint; mismatching fields never reuse it.
        val retainedMoment = preservedMomentMs?.takeIf { ms -> runCatching {
            val local = java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.of(interpretationZone))
            local.toLocalDate().toString() == date.trim() &&
                local.toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) == time.trim()
        }.getOrDefault(false) }
        val moment = if (simulateTime) retainedMoment ?: spaceCompassObserverMoment(date.trim(), time.trim(), interpretationZone) else null
        requireNotNull(spaceCompassObserverDraftPlan(simulatePosition, simulateTime,
            spaceCompassObserverInputNumber(latitude), spaceCompassObserverInputNumber(longitude),
            spaceCompassObserverInputNumber(altitude)?.let { if (feet) it * .3048 else it },
            moment, zone.trim(), previous, nowMs, deviceZoneId, simulateAltitude, simulateZone,
            automaticAltitudeMeters, automaticZoneId)) { "Incomplete or invalid observer draft" }
    }
}
