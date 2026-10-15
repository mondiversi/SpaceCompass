package me.mondiversi.spacecompass

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val observerClockFormat = DateTimeFormatter.ofPattern("HH:mm", java.util.Locale.ROOT)

/** Reopening a running scenario displays its progressed moment without changing the saved offset. */
internal fun SpaceCompassObserverPlan.editorMoment(nowMs: Long, deviceZone: ZoneId): ZonedDateTime {
    val zone = if (advancesClock || timeOverrideMs != null) observationZone(deviceZone) else ZoneId.of(momentZoneId)
    return Instant.ofEpochMilli(if (advancesClock) observationTimeMs(nowMs, deviceZone) else timeMs).atZone(zone)
}

private fun SpaceCompassObserverDraft.effectiveClockZone(deviceZoneId: String): ZoneId = ZoneId.of(when {
    simulateZone -> zone.trim()
    simulatePosition -> automaticZoneId.orEmpty()
    else -> deviceZoneId
})

/** The link changes the clock only; an explicitly selected calendar date remains selected. */
internal fun SpaceCompassObserverDraft.withCurrentTime(nowMs: Long,
    deviceZoneId: String): Result<SpaceCompassObserverDraft> = runCatching {
    val targetZone = effectiveClockZone(deviceZoneId)
    val now = Instant.ofEpochMilli(nowMs).atZone(targetZone)
    val selectedDate = if (simulateDate) LocalDate.parse(date.trim()) else now.toLocalDate()
    require(selectedDate.year in 1900..2100)
    val selected = ZonedDateTime.ofLocal(selectedDate.atTime(now.toLocalTime()), targetZone, now.offset)
        .toInstant().toEpochMilli()
    copy(time = now.toLocalTime().format(observerClockFormat), preservedMomentMs = selected,
        clockOffsetMs = if (advanceTime && simulateTime) Math.subtractExact(selected, nowMs) else null)
}

/** Today's date belongs to the observer's zone; keep a chosen clock time and re-anchor progression. */
internal fun SpaceCompassObserverDraft.withCurrentDate(nowMs: Long,
    deviceZoneId: String): Result<SpaceCompassObserverDraft> = runCatching {
    val targetZone = effectiveClockZone(deviceZoneId)
    val now = Instant.ofEpochMilli(nowMs).atZone(targetZone)
    require(now.year in 1900..2100)
    val chosenTime = if (simulateTime) LocalTime.parse(time.trim()) else now.toLocalTime()
    val local = now.toLocalDate().atTime(chosenTime)
    // Do not silently replace a manually chosen clock time inside a daylight-saving gap.
    require(targetZone.rules.getValidOffsets(local).isNotEmpty())
    val preferredOffset = preservedMomentMs?.let { Instant.ofEpochMilli(it).atZone(targetZone).offset }
        ?: now.offset
    val selected = ZonedDateTime.ofLocal(local, targetZone, preferredOffset).toInstant().toEpochMilli()
    copy(date = now.toLocalDate().toString(), preservedMomentMs = selected,
        clockOffsetMs = if (advanceTime && simulateTime) Math.subtractExact(selected, nowMs) else null)
}

/** Starting establishes an offset once; stopping freezes the progressed clock instead of rewinding. */
internal fun SpaceCompassObserverDraft.withClockAdvance(enabled: Boolean, previous: SpaceCompassObserverPlan?,
    nowMs: Long, deviceZoneId: String): SpaceCompassObserverDraft {
    if (enabled) return copy(advanceTime = true, clockOffsetMs = null)
    val instant = resolve(previous, nowMs, deviceZoneId).getOrNull()?.observationTimeMs(nowMs, ZoneId.of(deviceZoneId))
    val local = instant?.let { Instant.ofEpochMilli(it).atZone(effectiveClockZone(deviceZoneId)) }
    return copy(advanceTime = false, clockOffsetMs = null,
        date = if (simulateDate) local?.toLocalDate()?.toString() ?: date else date,
        time = local?.toLocalTime()?.format(observerClockFormat) ?: time,
        preservedMomentMs = instant)
}
