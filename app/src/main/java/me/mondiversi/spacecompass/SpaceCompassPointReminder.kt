package me.mondiversi.spacecompass

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

internal enum class SpaceCompassPointReminderKind { ALARM, CALENDAR }

internal data class SpaceCompassPointReminder(
    val kind: SpaceCompassPointReminderKind,
    val eventTimeMs: Long,
    val deviceZoneId: String,
    val alarmHour: Int,
    val alarmMinute: Int
)

/** Android's alarm handoff has no calendar date. Use it only when its next
 * unambiguous wall-clock occurrence is the selected event's minute. */
internal fun spaceCompassPointReminder(timeMs: Long, viewedNowMs: Long, realNowMs: Long,
    isCurrent: Boolean, deviceZone: ZoneId = ZoneId.systemDefault()): SpaceCompassPointReminder? {
    if (isCurrent || timeMs <= viewedNowMs || timeMs <= realNowMs || timeMs > Long.MAX_VALUE - 60_000) return null
    return runCatching {
        val event = Instant.ofEpochMilli(timeMs).atZone(deviceZone)
        val alarmMinute = event.truncatedTo(ChronoUnit.MINUTES)
        val realNow = Instant.ofEpochMilli(realNowMs).atZone(deviceZone)
        val localTime = alarmMinute.toLocalTime()
        var candidate = realNow.toLocalDate().atTime(localTime).atZone(deviceZone)
        if (candidate.toInstant().toEpochMilli() <= realNowMs)
            candidate = realNow.toLocalDate().plusDays(1).atTime(localTime).atZone(deviceZone)
        val unique = deviceZone.rules.getValidOffsets(alarmMinute.toLocalDateTime()).size == 1 &&
            deviceZone.rules.getValidOffsets(candidate.toLocalDateTime()).size == 1
        val kind = if (unique && alarmMinute.toInstant().toEpochMilli() > realNowMs &&
            candidate.toInstant() == alarmMinute.toInstant()) SpaceCompassPointReminderKind.ALARM
            else SpaceCompassPointReminderKind.CALENDAR
        SpaceCompassPointReminder(kind, timeMs, deviceZone.id, event.hour, event.minute)
    }.getOrNull()
}
