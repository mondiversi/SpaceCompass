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
    val preservedMomentMs: Long? = null,
    val simulateDate: Boolean = simulateTime,
    val advanceTime: Boolean = false, val clockOffsetMs: Long? = null
) {
    fun resolve(previous: SpaceCompassObserverPlan?, nowMs: Long,
        deviceZoneId: String): Result<SpaceCompassObserverPlan?> = runCatching {
        if (!simulatePosition && !simulateDate && !simulateTime && !simulateAltitude && !simulateZone) return@runCatching null
        val interpretationZone = when {
                simulateZone -> zone.trim()
                simulatePosition -> automaticZoneId.orEmpty()
                else -> deviceZoneId
            }
        // A zone change must preserve the second occurrence of an overlapping DST hour too.
        // Explicit date/time edits clear this hint; mismatching fields never reuse it.
        val retainedMoment = preservedMomentMs?.takeIf { ms -> runCatching {
            val local = java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.of(interpretationZone))
            (!simulateDate || local.toLocalDate().toString() == date.trim()) &&
                local.toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) == time.trim()
        }.getOrDefault(false) }
        val moment = if (simulateDate && simulateTime) retainedMoment ?:
            spaceCompassObserverMoment(date.trim(), time.trim(), interpretationZone) else if (simulateDate || simulateTime) {
            val now = java.time.Instant.ofEpochMilli(nowMs).atZone(java.time.ZoneId.of(interpretationZone))
            val parsedDate = runCatching { java.time.LocalDate.parse(date.trim()).also {
                require(it.year in 1900..2100)
            } }
            val parsedTime = runCatching {
                require(time.trim().matches(Regex("\\d{2}:\\d{2}")))
                java.time.LocalTime.parse(time.trim())
            }
            val chosenDate = if (simulateDate) parsedDate.getOrThrow() else parsedDate.getOrNull() ?: now.toLocalDate()
            val chosenTime = if (simulateTime) {
                val parsed = parsedTime.getOrThrow()
                retainedMoment?.let { java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.of(interpretationZone)).toLocalTime() } ?: parsed
            } else parsedTime.getOrNull() ?: now.toLocalTime()
            // Store civil components in UTC while independent. DST must not rewrite a chosen wall time
            // or a disabled field kept for reuse; observationTimeMs resolves the actual observer zone.
            java.time.LocalDateTime.of(chosenDate, chosenTime).toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
        } else null
        val base = requireNotNull(spaceCompassObserverDraftPlan(simulatePosition, simulateTime,
            spaceCompassObserverInputNumber(latitude), spaceCompassObserverInputNumber(longitude),
            spaceCompassObserverInputNumber(altitude)?.let { if (feet) it * .3048 else it },
            moment, zone.trim(), previous, nowMs, deviceZoneId, simulateAltitude, simulateZone,
            automaticAltitudeMeters, automaticZoneId, simulateDate, if (simulateDate != simulateTime) "UTC" else interpretationZone)) { "Incomplete or invalid observer draft" }
        val offset = if (simulateTime && advanceTime) clockOffsetMs ?:
            Math.subtractExact(base.observationTimeMs(nowMs, java.time.ZoneId.of(deviceZoneId)), nowMs)
            else clockOffsetMs ?: previous?.clockOffsetMs
        offset?.let { require(it in -SPACE_COMPASS_MAX_CLOCK_OFFSET_MS..SPACE_COMPASS_MAX_CLOCK_OFFSET_MS) }
        base.copy(advanceTime = advanceTime, clockOffsetMs = offset)
    }
}
