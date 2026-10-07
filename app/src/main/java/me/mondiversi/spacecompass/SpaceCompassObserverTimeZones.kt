package me.mondiversi.spacecompass

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

internal data class SpaceCompassObserverTimeZoneChoice(val id: String, val offset: ZoneOffset)

/** Keep geographic zones (and their DST rules), not just today's numeric offset.
 * Preserve a previously stored alias or fixed offset even when it is not in the standard list.
 */
internal fun spaceCompassObserverTimeZoneChoices(timeMs: Long, selected: String,
    available: Set<String> = ZoneId.getAvailableZoneIds()): List<SpaceCompassObserverTimeZoneChoice> {
    val instant = Instant.ofEpochMilli(timeMs)
    return (available.filter { '/' in it && !it.startsWith("Etc/") && !it.startsWith("SystemV/") } +
        listOf("UTC", selected)).distinct().mapNotNull { id ->
        runCatching { SpaceCompassObserverTimeZoneChoice(id, ZoneId.of(id).rules.getOffset(instant)) }.getOrNull()
    }.sortedWith(compareBy<SpaceCompassObserverTimeZoneChoice> { it.offset.totalSeconds }.thenBy { it.id })
}

/** Offsets can have half/quarter hours, and historical zones can even have seconds. */
internal fun formatSpaceCompassObserverUtcOffset(offset: ZoneOffset): String =
    if (offset == ZoneOffset.UTC) "UTC+00:00" else "UTC${offset.id}"

internal enum class SpaceCompassObserverMetadataField { ALTITUDE, ZONE }
