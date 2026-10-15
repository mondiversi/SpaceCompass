package me.mondiversi.spacecompass

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** Fixed components or a persisted real-clock offset; no ticking state is written to preferences. */
internal fun SpaceCompassObserverPlan.observationTimeMs(nowMs: Long,
    deviceZone: ZoneId = ZoneId.systemDefault()): Long {
    if (advancesClock) return Math.addExact(nowMs, requireNotNull(clockOffsetMs))
    timeOverrideMs?.let { return it }
    if (!hasCustomMoment) return nowMs
    val zone = observationZone(deviceZone)
    val now = Instant.ofEpochMilli(nowMs).atZone(zone)
    val selected = Instant.ofEpochMilli(timeMs).atZone(ZoneId.of(momentZoneId))
    val local = LocalDateTime.of(if (simulateDate) selected.toLocalDate() else now.toLocalDate(),
        if (simulateTime) selected.toLocalTime() else now.toLocalTime())
    // Moving clocks can cross DST gaps; advance through the gap and prefer today's offset at overlaps.
    return ZonedDateTime.ofLocal(local, zone, now.offset).toInstant().toEpochMilli()
}

/** Hourly provider windows remain stable while an unchecked clock keeps ticking. */
internal fun SpaceCompassObserverPlan.dataRequestTimeMs(nowMs: Long,
    deviceZone: ZoneId = ZoneId.systemDefault()): Long? = if (!hasCustomMoment) null else
    Math.floorDiv(observationTimeMs(nowMs, deviceZone), SPACE_COMPASS_WEATHER_HOUR_MS) * SPACE_COMPASS_WEATHER_HOUR_MS
