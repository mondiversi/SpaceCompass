package me.mondiversi.spacecompass

import java.time.LocalDate
import java.time.LocalTime

internal enum class SpaceCompassObserverChange { ACTIVATED, MODIFIED, DEACTIVATED }
internal data class SpaceCompassObserverSectionChange(
    val section: SpaceCompassObserverSection, val change: SpaceCompassObserverChange)

/** Compare editor values, not derived timestamps or refreshed automatic metadata. Call only after validation. */
internal fun spaceCompassObserverSectionChanges(previous: SpaceCompassObserverDraft,
    next: SpaceCompassObserverDraft): List<SpaceCompassObserverSectionChange> =
    SpaceCompassObserverSection.entries.mapNotNull { section ->
        val before = previous.sectionEnabled(section)
        val after = next.sectionEnabled(section)
        val change = when {
            !before && after -> SpaceCompassObserverChange.ACTIVATED
            before && !after -> SpaceCompassObserverChange.DEACTIVATED
            after && previous.sectionValue(section) != next.sectionValue(section) -> SpaceCompassObserverChange.MODIFIED
            else -> null
        }
        change?.let { SpaceCompassObserverSectionChange(section, it) }
    }

private fun SpaceCompassObserverDraft.sectionEnabled(section: SpaceCompassObserverSection) = when (section) {
    SpaceCompassObserverSection.POSITION -> simulatePosition
    SpaceCompassObserverSection.ALTITUDE -> simulateAltitude
    SpaceCompassObserverSection.ZONE -> simulateZone
    SpaceCompassObserverSection.DATE -> simulateDate
    SpaceCompassObserverSection.TIME -> simulateTime
}

private fun SpaceCompassObserverDraft.sectionValue(section: SpaceCompassObserverSection): Any? = when (section) {
    SpaceCompassObserverSection.POSITION -> listOf(noticeNumber(latitude), noticeNumber(longitude))
    SpaceCompassObserverSection.ALTITUDE -> noticeNumber(altitude)?.let { if (feet) it * .3048 else it }
    SpaceCompassObserverSection.ZONE -> zone.trim()
    SpaceCompassObserverSection.DATE -> runCatching { LocalDate.parse(date.trim()) }.getOrNull()
    SpaceCompassObserverSection.TIME -> listOf(runCatching { LocalTime.parse(time.trim()) }.getOrNull(), advanceTime)
}

private fun noticeNumber(value: String) = spaceCompassObserverInputNumber(value)?.let { if (it == 0.0) 0.0 else it }

internal val SpaceCompassObserverSectionChange.messageId: Int
    get() = when (section) {
        SpaceCompassObserverSection.POSITION -> when (change) {
            SpaceCompassObserverChange.ACTIVATED -> R.string.observer_position_activated
            SpaceCompassObserverChange.MODIFIED -> R.string.observer_position_modified
            SpaceCompassObserverChange.DEACTIVATED -> R.string.observer_position_deactivated
        }
        SpaceCompassObserverSection.ALTITUDE -> when (change) {
            SpaceCompassObserverChange.ACTIVATED -> R.string.observer_altitude_activated
            SpaceCompassObserverChange.MODIFIED -> R.string.observer_altitude_modified
            SpaceCompassObserverChange.DEACTIVATED -> R.string.observer_altitude_deactivated
        }
        SpaceCompassObserverSection.ZONE -> when (change) {
            SpaceCompassObserverChange.ACTIVATED -> R.string.observer_zone_activated
            SpaceCompassObserverChange.MODIFIED -> R.string.observer_zone_modified
            SpaceCompassObserverChange.DEACTIVATED -> R.string.observer_zone_deactivated
        }
        SpaceCompassObserverSection.DATE -> when (change) {
            SpaceCompassObserverChange.ACTIVATED -> R.string.observer_date_activated
            SpaceCompassObserverChange.MODIFIED -> R.string.observer_date_modified
            SpaceCompassObserverChange.DEACTIVATED -> R.string.observer_date_deactivated
        }
        SpaceCompassObserverSection.TIME -> when (change) {
            SpaceCompassObserverChange.ACTIVATED -> R.string.observer_time_activated
            SpaceCompassObserverChange.MODIFIED -> R.string.observer_time_modified
            SpaceCompassObserverChange.DEACTIVATED -> R.string.observer_time_deactivated
        }
    }
